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

import android.arch.lifecycle.ViewModel
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Pair
import com.toshi.manager.model.ERC20TokenPaymentTask
import com.toshi.manager.model.PaymentTask
import com.toshi.manager.model.W3PaymentTask
import com.toshi.model.local.UnsignedW3Transaction
import com.toshi.model.network.Balance
import com.toshi.model.sofa.SofaAdapters
import com.toshi.util.LogUtil
import com.toshi.util.SingleLiveEvent
import com.toshi.view.BaseApplication
import com.toshi.view.fragment.PaymentConfirmationFragment.Companion.CALLBACK_ID
import com.toshi.view.fragment.PaymentConfirmationFragment.Companion.DAPP_FAVICON
import com.toshi.view.fragment.PaymentConfirmationFragment.Companion.DAPP_TITLE
import com.toshi.view.fragment.PaymentConfirmationFragment.Companion.DAPP_URL
import com.toshi.view.fragment.PaymentConfirmationFragment.Companion.ETH_AMOUNT
import com.toshi.view.fragment.PaymentConfirmationFragment.Companion.PAYMENT_ADDRESS
import com.toshi.view.fragment.PaymentConfirmationFragment.Companion.PAYMENT_TYPE
import com.toshi.view.fragment.PaymentConfirmationFragment.Companion.SEND_MAX_AMOUNT
import com.toshi.view.fragment.PaymentConfirmationFragment.Companion.TOKEN_ADDRESS
import com.toshi.view.fragment.PaymentConfirmationFragment.Companion.TOKEN_DECIMALS
import com.toshi.view.fragment.PaymentConfirmationFragment.Companion.TOKEN_SYMBOL
import com.toshi.view.fragment.PaymentConfirmationFragment.Companion.TOSHI_ID
import com.toshi.view.fragment.PaymentConfirmationFragment.Companion.UNSIGNED_TRANSACTION
import rx.Single
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription

class PaymentConfirmationViewModel : ViewModel() {

    private val toshiManager by lazy { BaseApplication.get().toshiManager }
    private val recipientManager by lazy { BaseApplication.get().recipientManager }
    private val transactionManager by lazy { BaseApplication.get().transactionManager }
    private val balanceManager by lazy { BaseApplication.get().balanceManager }

    private val subscriptions by lazy { CompositeSubscription() }

    val isGasPriceLoading by lazy { SingleLiveEvent<Boolean>() }
    val paymentTask by lazy { SingleLiveEvent<PaymentTask>() }
    val paymentTaskError by lazy { SingleLiveEvent<Unit>() }
    val balance by lazy { SingleLiveEvent<Balance>() }
    val error by lazy { SingleLiveEvent<Int>() }

    lateinit var bundle: Bundle

    fun init(bundle: Bundle) {
        this.bundle = bundle
        getPaymentTask()
        getBalance()
    }

    private fun getPaymentTask() {
        val unsignedW3Transaction = getUnsignedW3Transaction()
        val toshiId = getToshiId()
        val encodedEthAmount = getEncodedEthAmount()
        val paymentAddress = getPaymentAddress()
        val tokenAddress = getTokenAddress()
        val tokenSymbol = getTokenSymbol()
        val tokenDecimals = getTokenDecimals()

        val isUnsignedW3Transaction = unsignedW3Transaction != null
        val isToshiPayment = toshiId != null
        val isERC20Payment = tokenAddress != null && paymentAddress != null && tokenSymbol != null
        val isExternalPayment = paymentAddress != null

        when {
            isUnsignedW3Transaction -> getPaymentTaskWithUnsignedW3Transaction(unsignedW3Transaction)
            isToshiPayment -> getPaymentTaskWithToshiId(toshiId, encodedEthAmount)
            isERC20Payment -> getPaymentTaskWithTokenAddress(tokenAddress, tokenSymbol, tokenDecimals, paymentAddress, encodedEthAmount)
            isExternalPayment -> getPaymentTaskWithPaymentAddress(paymentAddress, encodedEthAmount)
            else -> LogUtil.exception(javaClass, "Unhandled payment unsignedW3Transaction, toshiId and paymentAddress is null")
        }
    }

    private fun getToshiId() = bundle.getString(TOSHI_ID)
    private fun getUnsignedW3Transaction() = bundle.getString(UNSIGNED_TRANSACTION)
    fun getPaymentAddress() = bundle.getString(PAYMENT_ADDRESS)
    fun getTokenAddress() = bundle.getString(TOKEN_ADDRESS)
    fun getTokenSymbol() = bundle.getString(TOKEN_SYMBOL)
    fun getTokenDecimals() = bundle.getInt(TOKEN_DECIMALS)
    fun getPaymentType() = bundle.getInt(PAYMENT_TYPE)
    fun getEncodedEthAmount() = bundle.getString(ETH_AMOUNT)
    fun getCallbackId() = bundle.getString(CALLBACK_ID)
    fun getDappUrl() = bundle.getString(DAPP_URL)
    fun getDappTitle() = bundle.getString(DAPP_TITLE)
    fun getDappFavicon() = bundle.getParcelable<Bitmap>(DAPP_FAVICON)
    fun isSendingMaxAmount() = bundle.getBoolean(SEND_MAX_AMOUNT, false)

    private fun getPaymentTaskWithToshiId(toshiId: String, ethAmount: String) {
        val sub = Single.zip(
                toshiManager.wallet,
                recipientManager.getUserFromToshiId(toshiId),
                { wallet, recipient -> Pair(wallet, recipient) }
        )
        .flatMap { getPaymentTask(it.first.paymentAddress, it.second.paymentAddress, ethAmount, isSendingMaxAmount()) }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe { isGasPriceLoading.value = true }
        .doAfterTerminate { isGasPriceLoading.value = false }
        .subscribe(
                { paymentTask.value = it },
                { paymentTaskError.value = Unit }
        )

        this.subscriptions.add(sub)
    }

    private fun getPaymentTaskWithTokenAddress(tokenAddress: String,
                                               tokenSymbol: String,
                                               tokenDecimals: Int,
                                               toPaymentAddress: String,
                                               ethAmount: String) {
        val sub = toshiManager.wallet
                .flatMap { getPaymentTask(it.paymentAddress, toPaymentAddress, ethAmount, tokenAddress, tokenSymbol, tokenDecimals) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { isGasPriceLoading.value = true }
                .doAfterTerminate { isGasPriceLoading.value = false }
                .subscribe(
                        { paymentTask.value = it },
                        { paymentTaskError.value = Unit }
                )
        this.subscriptions.add(sub)
    }

    private fun getPaymentTaskWithPaymentAddress(toPaymentAddress: String, ethAmount: String) {
        val sub = toshiManager.wallet
                .flatMap { getPaymentTask(it.paymentAddress, toPaymentAddress, ethAmount, isSendingMaxAmount()) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { isGasPriceLoading.value = true }
                .doAfterTerminate { isGasPriceLoading.value = false }
                .subscribe(
                        { paymentTask.value = it },
                        { paymentTaskError.value = Unit }
                )

        this.subscriptions.add(sub)
    }

    private fun getPaymentTaskWithUnsignedW3Transaction(unsignedW3Transaction: String) {
        val transaction = SofaAdapters.get().unsignedW3TransactionFrom(unsignedW3Transaction)
        val sub = getPaymentTaskWithUnsignedW3Transaction(transaction)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { isGasPriceLoading.value = true }
                .doAfterTerminate { isGasPriceLoading.value = false }
                .subscribe(
                        { paymentTask.value = it },
                        { paymentTaskError.value = Unit }
                )

        this.subscriptions.add(sub)
    }

    private fun getPaymentTask(fromPaymentAddress: String,
                               toPaymentAddress: String,
                               ethAmount: String,
                               sendMaxAmount: Boolean): Single<PaymentTask> {
        return transactionManager.buildPaymentTask(
                fromPaymentAddress,
                toPaymentAddress,
                ethAmount,
                sendMaxAmount
        )
    }

    private fun getPaymentTask(fromPaymentAddress: String,
                               toPaymentAddress: String,
                               ethAmount: String,
                               tokenAddress: String,
                               tokenSymbol: String,
                               tokenDecimals: Int
    ): Single<ERC20TokenPaymentTask> {
        return transactionManager.buildPaymentTask(
                fromPaymentAddress,
                toPaymentAddress,
                ethAmount,
                tokenAddress,
                tokenSymbol,
                tokenDecimals
        )
    }

    private fun getPaymentTaskWithUnsignedW3Transaction(unsignedW3Transaction: UnsignedW3Transaction): Single<W3PaymentTask> {
        val callbackId = getCallbackId()
        return transactionManager.buildPaymentTask(callbackId, unsignedW3Transaction)
    }

    private fun getBalance() {
        balanceManager.refreshBalance() // Initiate a balance request to make sure the balance is updated
        val sub = balanceManager
                .balanceObservable
                .filter { it != null }
                .flatMap { it.getBalanceWithLocalBalance().toObservable() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { balance.value = it },
                        { LogUtil.e(javaClass, "Error while fetching balance $it") }
                )

        subscriptions.add(sub)
    }

    override fun onCleared() {
        super.onCleared()
        subscriptions.clear()
    }
}