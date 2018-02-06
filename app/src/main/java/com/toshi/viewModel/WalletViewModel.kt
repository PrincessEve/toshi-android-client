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
import com.toshi.util.SingleLiveEvent
import com.toshi.view.BaseApplication
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription

class WalletViewModel : ViewModel() {

    private val toshiManager by lazy { BaseApplication.get().toshiManager }
    private val subscriptions by lazy { CompositeSubscription() }

    val walletAddress by lazy { MutableLiveData<String>() }
    val error by lazy { SingleLiveEvent<Int>() }

    init {
        getWalletAddress()
    }

    private fun getWalletAddress() {
        val sub = toshiManager
                .wallet
                .toObservable()
                .filter { wallet -> wallet != null }
                .toSingle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { walletAddress.value = it.paymentAddress },
                        { error.value = R.string.wallet_address_error }
                )

        subscriptions.add(sub)
    }
}