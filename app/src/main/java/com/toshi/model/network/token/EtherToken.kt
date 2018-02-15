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

package com.toshi.model.network.token

import android.content.Intent
import com.toshi.view.activity.ViewERC20Activity

data class EtherToken(
        val symbol: String,
        val name: String,
        val etherValue: String,
        val fiatValue: String
) : Token() {
    companion object {
        fun create(symbol: String = "ETH",
                   name: String = "Ethereum",
                   etherValue: String,
                   fiatValue: String): EtherToken {
            return EtherToken(symbol, name, etherValue, fiatValue)
        }

        private const val SYMBOL = "symbol"
        private const val NAME = "name"
        private const val ETHER_VALUE = "eth"
        private const val FIAT_VALUE = "fiat"

        fun buildIntent(intent: Intent, token: EtherToken): Intent {
            return intent.apply {
                putExtra(SYMBOL, token.symbol)
                putExtra(NAME, token.name)
                putExtra(ETHER_VALUE, token.etherValue)
                putExtra(FIAT_VALUE, token.fiatValue)
                putExtra(ViewERC20Activity.TOKEN_TYPE, ViewERC20Activity.ETHER_TOKEN)
            }
        }

        fun getTokenFromIntent(intent: Intent): EtherToken? {
            if (!hasAllExtras(intent)) return null
            return EtherToken(
                    intent.getStringExtra(SYMBOL),
                    intent.getStringExtra(NAME),
                    intent.getStringExtra(ETHER_VALUE),
                    intent.getStringExtra(FIAT_VALUE)
            )
        }

        private fun hasAllExtras(intent: Intent): Boolean {
            return intent.hasExtra(SYMBOL) && intent.hasExtra(NAME) &&
                    intent.hasExtra(ETHER_VALUE) && intent.hasExtra(FIAT_VALUE)
        }
    }
}