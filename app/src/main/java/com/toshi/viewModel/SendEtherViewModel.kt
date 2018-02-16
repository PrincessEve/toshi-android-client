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

package com.toshi.viewModel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.toshi.R
import com.toshi.crypto.util.hasValidChecksum
import com.toshi.crypto.util.usesChecksum
import com.toshi.extensions.createSafeBigDecimal
import com.toshi.manager.model.ExternalPaymentTask
import com.toshi.manager.model.PaymentTask
import com.toshi.manager.model.ToshiPaymentTask
import com.toshi.model.network.Balance
import com.toshi.model.network.ExchangeRate
import com.toshi.util.CurrencyUtil
import com.toshi.util.EthUtil
import com.toshi.util.LogUtil
import com.toshi.util.SharedPrefsUtil
import com.toshi.view.BaseApplication
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat

class SendEtherViewModel : ViewModel() {

    private val subscriptions by lazy { CompositeSubscription() }
    private val balanceManager by lazy { BaseApplication.get().balanceManager }
    private val transactionManager by lazy { BaseApplication.get().transactionManager }
    private var exchangeRate: ExchangeRate? = null
    private var currencyMode = CurrencyMode.ETH
    val ethBalance by lazy { MutableLiveData<Balance>() }
    var sendMaxAmount = false

    init {
        getBalance()
        getExchangeRate()
    }

    private fun getBalance() {
        val sub = balanceManager
                .balanceObservable
                .observeOn(AndroidSchedulers.mainThread())
                .filter { balance -> balance != null }
                .flatMap { balance -> balance.getBalanceWithLocalBalance().toObservable() }
                .subscribe(
                        { ethBalance.value = it },
                        { LogUtil.e(javaClass, "Error while getting ethBalance $it") }
                )

        subscriptions.add(sub)
    }

    private fun getExchangeRate() {
        val sub = balanceManager
                .getLocalCurrencyExchangeRate()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { exchangeRate = it },
                        { LogUtil.e(javaClass, "Error while getting exchange rate $it") }
                )

        subscriptions.add(sub)
    }

    fun isPaymentAddressValid(paymentAddress: String?): Boolean {
        val regex = Regex("^0x[a-fA-F0-9]{40}\$")
        return paymentAddress?.let {
            regex.matches(paymentAddress) && !hasInvalidChecksum(paymentAddress)
        } ?: false
    }

    private fun hasInvalidChecksum(paymentAddress: String): Boolean {
        return usesChecksum(paymentAddress) && !hasValidChecksum(paymentAddress)
    }

    fun sendPayment(paymentTask: PaymentTask) {
        when (paymentTask) {
            is ToshiPaymentTask -> transactionManager.sendPayment(paymentTask)
            is ExternalPaymentTask -> transactionManager.sendExternalPayment(paymentTask)
            else -> LogUtil.e(javaClass, "Invalid payment task in this context")
        }
    }

    fun hasEnoughBalance(inputAmount: String): Boolean {
        val inputEthAmount = when (currencyMode) {
            CurrencyMode.ETH -> createSafeBigDecimal(inputAmount)
            CurrencyMode.FIAT -> createSafeBigDecimal(fiatToEth(inputAmount))
        }
        val ethBalance = ethBalance.value
        val etherBalance = if (ethBalance != null) ethBalance.getUnconfirmedBalance() else BigInteger("0")
        val decimalEtherBalance = EthUtil.weiToEth(etherBalance)
        return inputEthAmount.compareTo(decimalEtherBalance) == 0 || inputEthAmount.compareTo(decimalEtherBalance) == -1
    }

    fun isAmountValid(inputAmount: String): Boolean {
        return inputAmount.isNotEmpty() && isValidDecimal(inputAmount)
    }

    private fun isValidDecimal(inputAmount: String): Boolean {
        return try {
            BigDecimal(inputAmount)
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    fun getEncodedEthAmount(inputValue: String): String {
        return when (currencyMode) {
            CurrencyMode.ETH -> EthUtil.decimalStringToEncodedEthAmount(inputValue)
            CurrencyMode.FIAT -> EthUtil.decimalStringToEncodedEthAmount(fiatToEth(inputValue))
        }
    }

    fun ethToFiat(inputValue: String): String {
        if (exchangeRate == null) return ""
        return EthUtil.ethToFiat(exchangeRate, createSafeBigDecimal(inputValue))
    }

    fun fiatToEth(inputValue: String): String {
        if (exchangeRate == null) return ""
        return EthUtil.fiatToEth(exchangeRate, createSafeBigDecimal(inputValue))
    }

    fun getCurrencyCode(): String {
        return when (currencyMode) {
            CurrencyMode.ETH -> BaseApplication.get().getString(R.string.eth_currency_code)
            CurrencyMode.FIAT -> getFiatCurrencyCode()
        }
    }

    fun getFiatCurrencyCode(): String = SharedPrefsUtil.getCurrency()

    fun getFiatCurrencySymbol(): String {
        val currency = SharedPrefsUtil.getCurrency()
        return CurrencyUtil.getSymbol(currency)
    }

    fun switchCurrencyMode(updateEthValue: () -> Unit, updateFiatValue: () -> Unit) {
        currencyMode = when (currencyMode) {
            CurrencyMode.ETH -> CurrencyMode.FIAT
            CurrencyMode.FIAT -> CurrencyMode.ETH
        }
        updateBalanceView(updateEthValue, updateFiatValue)
    }

    private fun updateBalanceView(updateEthValue: () -> Unit, updateFiatValue: () -> Unit) {
        when (currencyMode) {
            CurrencyMode.ETH -> updateEthValue()
            CurrencyMode.FIAT -> updateFiatValue()
        }
    }

    fun getConvertedValue(inputValue: String): String {
        return when (currencyMode) {
            CurrencyMode.ETH -> {
                val fiatAmount = ethToFiat(inputValue)
                val currencyCode = getFiatCurrencyCode()
                val currencySymbol = getFiatCurrencySymbol()
                "$currencySymbol$fiatAmount $currencyCode"
            }
            CurrencyMode.FIAT -> {
                val ethAmount = fiatToEth(inputValue)
                BaseApplication.get().getString(R.string.eth_amount, ethAmount)
            }
        }
    }

    fun getEtherBalance(): String {
        val ethBalance = ethBalance.value
        val etherBalance = if (ethBalance != null) ethBalance.getUnconfirmedBalance() else BigInteger("0")
        val decimalEtherBalance = EthUtil.weiAmountToUserVisibleString(etherBalance)
        return decimalEtherBalance.toString()
    }

    fun getMaxAmount(): String {
        return when (currencyMode) {
            CurrencyMode.ETH -> getMaxEthAmount()
            CurrencyMode.FIAT -> getMaxFiatAmount()
        }
    }

    private fun getMaxEthAmount(): String {
        val balance = ethBalance.value ?: return "0"
        val ethAmount = EthUtil.weiToEth(balance.getUnconfirmedBalance())
        val df = DecimalFormat()
        df.maximumFractionDigits = EthUtil.BIG_DECIMAL_SCALE
        return df.format(ethAmount)
    }

    private fun getMaxFiatAmount(): String {
        val balance = ethBalance.value ?: return "0"
        return exchangeRate?.let {
            val ethAmount = EthUtil.weiToEth(balance.getUnconfirmedBalance())
            val localAmount = it.rate.multiply(ethAmount)
            val df = DecimalFormat()
            df.maximumFractionDigits = EthUtil.BIG_DECIMAL_SCALE
            return df.format(localAmount)
        } ?: "0"
    }

    override fun onCleared() {
        super.onCleared()
        subscriptions.clear()
    }
}

enum class CurrencyMode {
    ETH(),
    FIAT()
}
