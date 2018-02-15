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
import com.toshi.model.network.Balance
import com.toshi.model.network.token.ERCToken
import com.toshi.model.network.token.EtherToken
import com.toshi.model.network.token.Token
import com.toshi.util.EthUtil
import com.toshi.util.LogUtil
import com.toshi.view.BaseApplication
import rx.Single
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription

class TokenViewModel : ViewModel() {

    private val balanceManager by lazy { BaseApplication.get().balanceManager }
    private val subscriptions by lazy { CompositeSubscription() }

    val tokens by lazy { MutableLiveData<List<Token>>() }
    val erc721Tokens by lazy { MutableLiveData<List<ERCToken>>() }

    fun fetchERC20Tokens() {
        val sub = Single.zip(
                        balanceManager.getERC20Tokens(),
                        createEtherToken(),
                        { tokens, etherToken -> Pair(tokens.tokens, etherToken) }
                )
                .map { addEtherTokenToTokenList(it.first, it.second) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { tokens.value = it },
                        { LogUtil.e(javaClass, "Error $it") }
                )

        subscriptions.add(sub)
    }

    private fun addEtherTokenToTokenList(tokens: List<ERCToken>, etherToken: EtherToken): List<Token> {
        val tokenList = mutableListOf<Token>()
        tokenList.add(etherToken)
        tokenList.addAll(tokens)
        return tokenList
    }

    private fun createEtherToken(): Single<EtherToken> {
        return balanceManager.balanceObservable
                .first()
                .toSingle()
                .flatMap { it.getBalanceWithLocalBalance() }
                .map { mapBalance(it) }
    }

    private fun mapBalance(balance: Balance): EtherToken {
        val ethAmount = EthUtil.weiAmountToUserVisibleString(balance.unconfirmedBalance)
        return EtherToken.create(etherValue = ethAmount, fiatValue = balance.localBalance ?: "0.0")
    }

    fun fetchERC721Tokens() {
        val sub = balanceManager
                .getERC721Tokens()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { erc721Tokens.value = it.collectibles },
                        { LogUtil.e(javaClass, "Error $it") }
                )

        subscriptions.add(sub)
    }

    override fun onCleared() {
        super.onCleared()
        subscriptions.clear()
    }
}