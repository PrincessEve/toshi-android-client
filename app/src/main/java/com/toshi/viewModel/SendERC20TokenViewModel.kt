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
import com.toshi.manager.model.ERC20TokenPaymentTask
import com.toshi.manager.model.PaymentTask
import com.toshi.model.network.Balance
import com.toshi.util.LogUtil
import com.toshi.view.BaseApplication
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription

class SendERC20TokenViewModel(fetchEthBalance: Boolean) : ViewModel() {

    private val subscriptions by lazy { CompositeSubscription() }
    private val balanceManager by lazy { BaseApplication.get().balanceManager }
    private val transactionManager by lazy { BaseApplication.get().transactionManager }
    val ethBalance by lazy { MutableLiveData<Balance>() }

    init {
        if (fetchEthBalance) getBalance()
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

    fun sendPayment(paymentTask: PaymentTask) {
        when (paymentTask) {
            is ERC20TokenPaymentTask -> transactionManager.sendERC20TokenPayment(paymentTask)
            else -> LogUtil.e(javaClass, "Invalid payment task in this context")
        }
    }
}