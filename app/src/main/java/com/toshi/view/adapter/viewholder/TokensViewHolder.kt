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

package com.toshi.view.adapter.viewholder

import android.support.v7.widget.RecyclerView
import android.view.View
import com.toshi.R
import com.toshi.crypto.util.TypeConverter
import com.toshi.extensions.getColorById
import com.toshi.extensions.isVisible
import com.toshi.model.network.token.ERCToken
import com.toshi.model.network.token.EtherToken
import com.toshi.model.network.token.Token
import com.toshi.util.ImageUtil
import kotlinx.android.synthetic.main.list_item__token.view.*

class TokensViewHolder(private val tokenType: TokenType, itemView: View?) : RecyclerView.ViewHolder(itemView) {

    fun setToken(token: Token, ERC20Listener: ((Token) -> Unit)?, ERC721Listener: ((ERCToken) -> Unit)?) {
        when (tokenType) {
            is TokenType.ERC20Token -> showToken(token, ERC20Listener)
            is TokenType.ERC721Token -> showERC721View(token, ERC721Listener)
        }
    }

    private fun showToken(token: Token, tokenListener: ((Token) -> Unit)?) {
        when (token) {
            is EtherToken -> showEtherToken(token, tokenListener)
            is ERCToken -> showERC20View(token, tokenListener)
            else -> throw IllegalStateException(Throwable("Invalid token type in this context"))
        }
    }

    private fun showERC721View(token: Token, tokenListener: ((ERCToken) -> Unit)?) {
        when (token) {
            is ERCToken -> showERC721View(token, tokenListener)
            else -> throw IllegalStateException(Throwable("Invalid token type in this context"))
        }
    }

    private fun showEtherToken(etherToken: EtherToken, tokenListener: ((Token) -> Unit)?) {
        itemView.erc20Wrapper.visibility = View.VISIBLE
        itemView.erc721Wrapper.visibility = View.GONE
        itemView.erc20Name.text = etherToken.name
        itemView.erc20Abbreviation.text = etherToken.symbol
        itemView.value.text = etherToken.etherValue
        itemView.fiatValue.text = etherToken.fiatValue
        itemView.fiatValue.isVisible(true)
        itemView.value.setTextColor(itemView.getColorById(R.color.textColorPrimary))
        itemView.setOnClickListener { tokenListener?.invoke(etherToken) }
        itemView.avatar.setImageResource(etherToken.icon)
    }

    private fun showERC20View(ERCToken: ERCToken, tokenListener: ((ERCToken) -> Unit)?) {
        itemView.erc20Wrapper.visibility = View.VISIBLE
        itemView.erc721Wrapper.visibility = View.GONE
        itemView.erc20Name.text = ERCToken.name
        itemView.erc20Abbreviation.text = ERCToken.symbol
        itemView.value.text = TypeConverter.formatHexString(ERCToken.value, ERCToken.decimals, "0.000000")
        itemView.fiatValue.isVisible(false)
        itemView.value.setTextColor(itemView.getColorById(R.color.textColorPrimary))
        ImageUtil.load(ERCToken.icon, itemView.avatar)
        itemView.setOnClickListener { tokenListener?.invoke(ERCToken) }
    }

    private fun showERC721View(ERCToken: ERCToken, tokenListener: ((ERCToken) -> Unit)?) {
        itemView.erc721Wrapper.visibility = View.VISIBLE
        itemView.erc20Wrapper.visibility = View.GONE
        itemView.erc721Name.text = ERCToken.name
        itemView.value.text = TypeConverter.formatHexString(ERCToken.value, ERCToken.decimals, "0")
        itemView.value.setTextColor(itemView.getColorById(R.color.textColorSecondary))
        ImageUtil.load(ERCToken.icon, itemView.avatar)
        itemView.setOnClickListener { tokenListener?.invoke(ERCToken) }
    }
}

sealed class TokenType {
    class ERC20Token : TokenType()
    class ERC721Token : TokenType()
}