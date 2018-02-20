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
import com.squareup.moshi.Json
import com.toshi.view.activity.ViewERC20Activity

data class ERCToken(
        val symbol: String,
        val name: String,
        val value: String,
        val decimals: Int,
        @Json(name = "contract_address")
        val contractAddress: String,
        val icon: String?
) : Token() {

    companion object {
        private const val SYMBOL = "symbol"
        private const val NAME = "name"
        private const val DECIMALS = "decimals"
        private const val VALUE = "value"
        private const val CONTRACT_ADDRESS = "contract_address"
        private const val ICON = "icon"

        fun buildIntent(intent: Intent, ERCToken: ERCToken): Intent {
            return intent.apply {
                putExtra(SYMBOL, ERCToken.symbol)
                putExtra(NAME, ERCToken.name)
                putExtra(VALUE, ERCToken.value)
                putExtra(DECIMALS, ERCToken.decimals)
                putExtra(CONTRACT_ADDRESS, ERCToken.contractAddress)
                putExtra(ICON, ERCToken.icon)
                putExtra(ViewERC20Activity.TOKEN_TYPE, ViewERC20Activity.ERC20_TOKEN)
            }
        }

        fun getTokenFromIntent(intent: Intent): ERCToken? {
            if (!hasAllExtras(intent)) return null
            return ERCToken(
                    intent.getStringExtra(SYMBOL),
                    intent.getStringExtra(NAME),
                    intent.getStringExtra(VALUE),
                    intent.getIntExtra(DECIMALS, 0),
                    intent.getStringExtra(CONTRACT_ADDRESS),
                    intent.getStringExtra(ICON)
            )
        }

        private fun hasAllExtras(intent: Intent): Boolean {
            return intent.hasExtra(SYMBOL) && intent.hasExtra(NAME)
                    && intent.hasExtra(DECIMALS) && intent.hasExtra(VALUE)
                    && intent.hasExtra(CONTRACT_ADDRESS) && intent.hasExtra(ICON)
        }
    }
}