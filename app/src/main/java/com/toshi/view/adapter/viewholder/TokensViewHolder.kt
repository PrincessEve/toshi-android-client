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
import kotlinx.android.synthetic.main.list_item__token.view.*

class TokensViewHolder(private val tokenType: TokenType, itemView: View?) : RecyclerView.ViewHolder(itemView) {

    fun setToken(token: String) {
        when (tokenType) {
            is TokenType.ERC20Token -> { showERC20View(token) }
            is TokenType.ERC721Token -> { showERC721View(token) }
        }
    }

    private fun showERC20View(token: String) {
        itemView.erc20Wrapper.visibility = View.VISIBLE
        itemView.erc721Wrapper.visibility = View.GONE
        itemView.erc20Name.text = token
        itemView.erc20Abbreviation.text = "OMG"
        itemView.value.text = "2.454954958"
    }

    private fun showERC721View(token: String) {
        itemView.erc721Wrapper.visibility = View.VISIBLE
        itemView.erc20Wrapper.visibility = View.GONE
        itemView.erc721Name.text = token
        itemView.value.text = "2.454954958"
    }
}

sealed class TokenType {
    class ERC20Token : TokenType()
    class ERC721Token : TokenType()
}