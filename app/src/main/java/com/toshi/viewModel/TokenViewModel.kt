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

class TokenViewModel : ViewModel() {

    val erc20Tokens by lazy { MutableLiveData<List<String>>() }
    val erc721Tokens by lazy { MutableLiveData<List<String>>() }

    init {
        fetchERC20Tokens()
        fetchERC721Tokens()
    }

    private fun fetchERC20Tokens() {
        erc20Tokens.value = listOf("REP", "0x", "OMG", "WTF", "LOL", "OMG", "ROFL", "WTH")
    }

    private fun fetchERC721Tokens() {
        erc721Tokens.value = listOf()
    }
}