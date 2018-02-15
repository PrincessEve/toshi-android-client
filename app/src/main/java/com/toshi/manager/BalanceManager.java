/*
 * 	Copyright (c) 2017. Toshi Inc
 *
 * 	This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.toshi.manager;


import android.content.Context;
import android.content.SharedPreferences;

import com.toshi.crypto.HDWallet;
import com.toshi.manager.network.CurrencyService;
import com.toshi.manager.network.EthereumService;
import com.toshi.model.local.Network;
import com.toshi.model.local.Networks;
import com.toshi.model.network.Balance;
import com.toshi.model.network.Currencies;
import com.toshi.model.network.token.ERC721Tokens;
import com.toshi.model.network.ExchangeRate;
import com.toshi.model.network.GcmDeregistration;
import com.toshi.model.network.GcmRegistration;
import com.toshi.model.network.ServerTime;
import com.toshi.model.network.token.ERC20Tokens;
import com.toshi.model.sofa.payment.Payment;
import com.toshi.util.CurrencyUtil;
import com.toshi.util.FileNames;
import com.toshi.util.GcmPrefsUtil;
import com.toshi.util.GcmUtil;
import com.toshi.util.LogUtil;
import com.toshi.util.SharedPrefsUtil;
import com.toshi.view.BaseApplication;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import rx.Completable;
import rx.Single;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

import static com.toshi.util.EthUtil.BIG_DECIMAL_SCALE;

public class BalanceManager {

    private final static BehaviorSubject<Balance> balanceObservable = BehaviorSubject.create();
    private static final String LAST_KNOWN_BALANCE = "lkb";

    private HDWallet wallet;
    private SharedPreferences prefs;
    private Networks networks;
    private Subscription connectivitySub;

    /* package */ BalanceManager() {
    }

    public BehaviorSubject<Balance> getBalanceObservable() {
        return balanceObservable;
    }

    public Completable init(final HDWallet wallet) {
        this.wallet = wallet;
        this.networks = Networks.getInstance();
        initPrefs();
        initCachedBalance();
        return registerEthGcm()
                .onErrorComplete()
                .doOnCompleted(this::attachConnectivityObserver);
    }

    private void initPrefs() {
        this.prefs = BaseApplication.get().getSharedPreferences(FileNames.BALANCE_PREFS, Context.MODE_PRIVATE);
    }

    private void initCachedBalance() {
        final Balance cachedBalance = new Balance(readLastKnownBalance());
        handleNewBalance(cachedBalance);
    }

    private void attachConnectivityObserver() {
        clearConnectivitySubscription();

        this.connectivitySub =
                BaseApplication
                .get()
                .isConnectedSubject()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .filter(isConnected -> isConnected)
                .subscribe(
                        __ -> handleConnectivity(),
                        throwable -> LogUtil.exception(getClass(), "Error checking connection state", throwable)
                );
    }

    private void handleConnectivity() {
        refreshBalance();
        registerEthGcm()
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> {},
                        throwable -> LogUtil.e(getClass(), "Error while registering eth gcm " + throwable)
                );
    }

    public void refreshBalance() {
        getBalance()
                .observeOn(Schedulers.io())
                .subscribe(
                        this::handleNewBalance,
                        this::handleBalanceError
                );
    }

    public Completable refreshBalanceCompletable() {
        return getBalance()
                .doOnSuccess(this::handleNewBalance)
                .toCompletable();
    }

    private Single<Balance> getBalance() {
        return EthereumService
                .getApi()
                .getBalance(this.wallet.getPaymentAddress())
                .subscribeOn(Schedulers.io());
    }

    public Single<ERC20Tokens> getERC20Tokens() {
        return EthereumService
                .getApi()
                .getTokens(this.wallet.getPaymentAddress())
                .subscribeOn(Schedulers.io());
    }

    public Single<ERC721Tokens> getERC721Tokens() {
        return EthereumService
                .getApi()
                .getCollectibles(this.wallet.getPaymentAddress())
                .subscribeOn(Schedulers.io());
    }

    private void handleNewBalance(final Balance balance) {
        writeLastKnownBalance(balance);
        balanceObservable.onNext(balance);
    }

    private void handleBalanceError(final Throwable throwable) {
        LogUtil.exception(getClass(), "Error while fetching balance", throwable);
    }

    public Single<ExchangeRate> getLocalCurrencyExchangeRate() {
        return getLocalCurrency()
                .flatMap((code) -> fetchLatestExchangeRate(code)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()));
    }

    private Single<ExchangeRate> fetchLatestExchangeRate(final String code) {
        return CurrencyService
                .getApi()
                .getRates(code);
    }

    public Single<Currencies> getCurrencies() {
        return CurrencyService
                .getApi()
                .getCurrencies()
                .subscribeOn(Schedulers.io());
    }

    public Single<String> convertEthToLocalCurrencyString(final BigDecimal ethAmount) {
        return getLocalCurrencyExchangeRate()
                 .map((exchangeRate) -> toLocalCurrencyString(exchangeRate, ethAmount));
    }

    public String toLocalCurrencyString(final ExchangeRate exchangeRate, final BigDecimal ethAmount) {
        final BigDecimal marketRate = exchangeRate.getRate();
        final BigDecimal localAmount = marketRate.multiply(ethAmount);

        final DecimalFormat numberFormat = CurrencyUtil.getNumberFormat();
        numberFormat.setGroupingUsed(true);
        numberFormat.setMaximumFractionDigits(2);
        numberFormat.setMinimumFractionDigits(2);

        final String amount = numberFormat.format(localAmount);
        final String currencyCode = CurrencyUtil.getCode(exchangeRate.getTo());
        final String currencySymbol = CurrencyUtil.getSymbol(exchangeRate.getTo());

        return String.format("%s%s %s", currencySymbol, amount, currencyCode);
    }

    private Single<String> getLocalCurrency() {
        return Single.fromCallable(SharedPrefsUtil::getCurrency);
    }

    public Single<BigDecimal> convertEthToLocalCurrency(final BigDecimal ethAmount) {
        return getLocalCurrencyExchangeRate()
                .flatMap((exchangeRate) -> mapToLocalCurrency(exchangeRate, ethAmount));
    }

    private Single<BigDecimal> mapToLocalCurrency(final ExchangeRate exchangeRate,
                                                  final BigDecimal ethAmount) {
        return Single.fromCallable(() -> {
            final BigDecimal marketRate = exchangeRate.getRate();
            return marketRate.multiply(ethAmount);
        });
    }

    public Single<BigDecimal> convertLocalCurrencyToEth(final BigDecimal localAmount) {
        return getLocalCurrencyExchangeRate()
                .flatMap((exchangeRate) -> mapToEth(exchangeRate, localAmount));
    }

    private Single<BigDecimal> mapToEth(final ExchangeRate exchangeRate,
                                        final BigDecimal localAmount) {
        return Single.fromCallable(() -> {
            if (localAmount.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }

            final BigDecimal marketRate = exchangeRate.getRate();
            if (marketRate.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }

            return localAmount.divide(marketRate, BIG_DECIMAL_SCALE, RoundingMode.HALF_DOWN);
        });
    }

    private Completable unregisterEthGcmWithTimestamp(final String token, final ServerTime serverTime) {
        if (serverTime == null) {
            return Completable.error(new IllegalStateException("Unable to fetch server time"));
        }

        return EthereumService
                .getApi()
                .unregisterGcm(serverTime.get(), new GcmDeregistration(token))
                .toCompletable();
    }

    public Single<Payment> getTransactionStatus(final String transactionHash) {
        return EthereumService
                .get()
                .getStatusOfTransaction(transactionHash);
    }

    private String readLastKnownBalance() {
        return this.prefs
                .getString(LAST_KNOWN_BALANCE, "0x0");
    }

    private void writeLastKnownBalance(final Balance balance) {
        this.prefs
                .edit()
                .putString(LAST_KNOWN_BALANCE, balance.getUnconfirmedBalanceAsHex())
                .apply();
    }

    //Don't unregister the default network
    public Completable changeNetwork(final Network network) {
        if (this.networks.onDefaultNetwork()) {
            return changeEthBaseUrl(network)
                    .andThen(registerEthGcm())
                    .subscribeOn(Schedulers.io())
                    .doOnCompleted(() -> SharedPrefsUtil.setCurrentNetwork(network));
        }

        return GcmUtil
                .getGcmToken()
                .flatMapCompletable(this::unregisterFromEthGcm)
                .andThen(changeEthBaseUrl(network))
                .andThen(registerEthGcm())
                .subscribeOn(Schedulers.io())
                .doOnCompleted(() -> SharedPrefsUtil.setCurrentNetwork(network));
    }

    public Completable unregisterFromEthGcm(final String token) {
        final String currentNetworkId = this.networks.getCurrentNetwork().getId();
        return EthereumService
                .getApi()
                .getTimestamp()
                .subscribeOn(Schedulers.io())
                .flatMapCompletable((st) -> unregisterEthGcmWithTimestamp(token, st))
                .doOnCompleted(() -> GcmPrefsUtil.setEthGcmTokenSentToServer(currentNetworkId, false));
    }

    private Completable changeEthBaseUrl(final Network network) {
        return Completable.fromAction(() -> EthereumService.get().changeBaseUrl(network.getUrl()));
    }

    public Completable forceRegisterEthGcm() {
        if (this.wallet == null || this.networks == null) {
            return Completable.error(new IllegalStateException("Unable to register GCM as class hasn't been initialised yet"));
        }
        final String currentNetworkId = this.networks.getCurrentNetwork().getId();
        GcmPrefsUtil.setEthGcmTokenSentToServer(currentNetworkId, false);
        return registerEthGcm();
    }

    private Completable registerEthGcm() {
        final String currentNetworkId = this.networks.getCurrentNetwork().getId();
        if (GcmPrefsUtil.isEthGcmTokenSentToServer(currentNetworkId)) return Completable.complete();

        return GcmUtil
                .getGcmToken()
                .flatMapCompletable(this::registerEthGcmToken);
    }

    private Completable registerEthGcmToken(final String token) {
        return EthereumService
                .getApi()
                .getTimestamp()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMapCompletable((st) -> registerEthGcmWithTimestamp(token, st))
                .doOnCompleted(this::setEthGcmTokenSentToServer)
                .doOnError(this::handleGcmRegisterError);
    }

    private void setEthGcmTokenSentToServer() {
        final String currentNetworkId = this.networks.getCurrentNetwork().getId();
        GcmPrefsUtil.setEthGcmTokenSentToServer(currentNetworkId, true);
    }

    private Completable registerEthGcmWithTimestamp(final String token, final ServerTime serverTime) {
        if (serverTime == null) {
            throw new IllegalStateException("ServerTime was null");
        }

        return EthereumService
                .getApi()
                .registerGcm(serverTime.get(), new GcmRegistration(token, this.wallet.getPaymentAddress()))
                .toCompletable();
    }

    private void handleGcmRegisterError(final Throwable throwable) {
        LogUtil.exception(getClass(), "Error during registering of GCM " + throwable.getMessage());
        final String currentNetworkId = this.networks.getCurrentNetwork().getId();
        GcmPrefsUtil.setEthGcmTokenSentToServer(currentNetworkId, false);
    }

    public void clear() {
        clearConnectivitySubscription();
        this.prefs
                .edit()
                .clear()
                .apply();
    }

    private void clearConnectivitySubscription() {
        if (this.connectivitySub == null) return;
        this.connectivitySub.unsubscribe();
    }
}
