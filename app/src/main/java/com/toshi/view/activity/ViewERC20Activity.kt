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

package com.toshi.view.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.toshi.R
import com.toshi.crypto.util.TypeConverter
import com.toshi.extensions.isVisible
import com.toshi.extensions.startActivity
import com.toshi.extensions.toast
import com.toshi.model.network.token.ERCToken
import com.toshi.model.network.token.EtherToken
import com.toshi.model.network.token.Token
import com.toshi.util.EthUtil
import com.toshi.util.ImageUtil
import com.toshi.view.fragment.DialogFragment.ShareWalletAddressDialog
import kotlinx.android.synthetic.main.activity_view_erc20.*

class ViewERC20Activity : AppCompatActivity() {

    companion object {
        const val ETHER_TOKEN = "ether_token"
        const val ERC20_TOKEN = "erc_token"
        const val TOKEN_TYPE = "type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_erc20)
        init()
    }

    private fun init() {
        val tokenType = intent.getStringExtra(TOKEN_TYPE)
        when (tokenType) {
            ETHER_TOKEN -> initEtherView()
            ERC20_TOKEN -> initERC20View()
        }
    }

    private fun initEtherView() {
        val etherToken = EtherToken.getTokenFromIntent(intent)
        if (etherToken == null) {
            toast(R.string.invalid_token)
            finish()
            return
        }
        renderEtherTokenUi(etherToken)
        initClickListeners(etherToken)
    }

    private fun initERC20View() {
        val erc20Token = ERCToken.getTokenFromIntent(intent)
        if (erc20Token == null) {
            toast(R.string.invalid_token)
            finish()
            return
        }
        renderERCTokenUi(erc20Token)
        initClickListeners(erc20Token)
    }

    private fun renderEtherTokenUi(etherToken: EtherToken) {
        toolbarTitle.text = etherToken.name
        amount.text = getString(R.string.eth_balance, etherToken.etherValue)
        fiat.text = etherToken.fiatValue
        fiat.isVisible(true)
        avatar.setImageResource(etherToken.icon)
    }

    private fun renderERCTokenUi(ERCToken: ERCToken) {
        toolbarTitle.text = ERCToken.name
        ImageUtil.load(ERCToken.icon, avatar)
        val tokenAmount = TypeConverter.formatHexString(ERCToken.value, ERCToken.decimals, EthUtil.DECIMAL_FORMAT)
        amount.text = "$tokenAmount ${ERCToken.symbol}"
    }

    private fun initClickListeners(token: Token) {
        closeButton.setOnClickListener { finish() }
        send.setOnClickListener { startSendActivity(token) }
        receive.setOnClickListener { showShareWalletDialog() }
    }

    private fun startSendActivity(token: Token) {
        when (token) {
            is EtherToken -> startActivity<SendETHActivity> { EtherToken.buildIntent(this, token) }
            is ERCToken -> startActivity<SendERC20TokenActivity> { ERCToken.buildIntent(this, token) }
            else -> throw IllegalStateException(Throwable("Invalid token in this context"))
        }
    }

    private fun showShareWalletDialog() {
        ShareWalletAddressDialog.newInstance().apply {
            show(supportFragmentManager, ShareWalletAddressDialog.TAG)
        }
    }
}