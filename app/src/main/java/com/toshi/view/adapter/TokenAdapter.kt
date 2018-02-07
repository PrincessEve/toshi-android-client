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

package com.toshi.view.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.toshi.R
import com.toshi.model.network.ERC20Token
import com.toshi.view.adapter.viewholder.TokenType
import com.toshi.view.adapter.viewholder.TokensViewHolder

class TokenAdapter(private val tokenType: TokenType) : RecyclerView.Adapter<TokensViewHolder>() {

    private val tokens = mutableListOf<ERC20Token>()

    fun addTokens(tokens: List<ERC20Token>) {
        this.tokens.clear()
        this.tokens.addAll(tokens)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): TokensViewHolder {
        val layoutInflater = LayoutInflater.from(parent?.context)
        val view = layoutInflater.inflate(R.layout.list_item__token, parent, false)
        return TokensViewHolder(tokenType, view)
    }

    override fun onBindViewHolder(holder: TokensViewHolder?, position: Int) {
        val token = tokens[position]
        holder?.setToken(token)
    }

    override fun getItemCount() = tokens.size
}