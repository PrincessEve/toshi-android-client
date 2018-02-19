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
import com.toshi.extensions.startActivity
import com.toshi.extensions.toast
import com.toshi.model.network.Token
import com.toshi.util.ImageUtil
import kotlinx.android.synthetic.main.activity_view_erc20.*

class ViewERC20Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_erc20)
        init()
    }

    private fun init() {
        val token = Token.getTokenFromIntent(intent)
        if (token == null) {
            toast(R.string.invalid_token)
            finish()
            return
        }
        renderUi(token)
        initClickListeners(token)
    }

    private fun renderUi(token: Token) {
        toolbarTitle.text = token.name
        ImageUtil.load(token.icon, avatar)
        amount.text = TypeConverter.formatHexString(token.value, token.decimals, "0.000000")
    }

    private fun initClickListeners(token: Token) {
        closeButton.setOnClickListener { finish() }
        receive.setOnClickListener {}
        send.setOnClickListener { startActivity<SendERC20TokenActivity> { Token.buildIntent(this, token) } }
    }
}