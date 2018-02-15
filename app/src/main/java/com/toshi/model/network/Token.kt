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

package com.toshi.model.network

import android.content.Intent
import com.squareup.moshi.Json

data class Token(
        val symbol: String,
        val name: String,
        val decimals: Int,
        val value: String,
        @Json(name = "contract_address")
        val contractAddress: String,
        val format: String,
        val icon: String
) {

    companion object {
        private const val SYMBOL = "symbol"
        private const val NAME = "name"
        private const val DECIMALS = "decimals"
        private const val VALUE = "value"
        private const val CONTRACT_ADDRESS = "contract_address"
        private const val FORMAT = "format"
        private const val ICON = "format"

        fun buildIntent(intent: Intent, token: Token): Intent {
            return intent.apply {
                putExtra(SYMBOL, token.symbol)
                putExtra(NAME, token.name)
                putExtra(DECIMALS, token.decimals)
                putExtra(VALUE, token.value)
                putExtra(CONTRACT_ADDRESS, token.contractAddress)
                putExtra(FORMAT, token.format)
                putExtra(ICON, token.icon)
            }
        }

        fun getTokenFromIntent(intent: Intent): Token {
            return Token(
                    intent.getStringExtra(SYMBOL),
                    intent.getStringExtra(NAME),
                    intent.getIntExtra(DECIMALS, 0),
                    intent.getStringExtra(VALUE),
                    intent.getStringExtra(CONTRACT_ADDRESS),
                    intent.getStringExtra(FORMAT),
                    intent.getStringExtra(ICON)
            )
        }
    }
}