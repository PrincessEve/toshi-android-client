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
import com.toshi.model.network.ERC20Token
import com.toshi.util.LogUtil
import com.toshi.view.BaseApplication
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription

class TokenViewModel : ViewModel() {

    private val balanceManager by lazy { BaseApplication.get().balanceManager }
    private val subscriptions by lazy { CompositeSubscription() }

    val erc20Tokens by lazy { MutableLiveData<List<ERC20Token>>() }
    val erc721Tokens by lazy { MutableLiveData<List<ERC20Token>>() }

    fun fetchERC20Tokens() {
        val sub = balanceManager
                .getERC20Tokens()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { erc20Tokens.value = it.tokens },
                        { LogUtil.e(javaClass, "Error $it") }
                )

        subscriptions.add(sub)
    }

    fun fetchERC721Tokens() {
        erc721Tokens.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        subscriptions.clear()
    }
}