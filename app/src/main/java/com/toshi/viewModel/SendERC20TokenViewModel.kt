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

import android.arch.lifecycle.ViewModel
import com.toshi.crypto.util.TypeConverter
import com.toshi.extensions.createSafeBigDecimal
import com.toshi.extensions.isValidDecimal
import com.toshi.model.network.token.ERCToken
import java.math.BigDecimal

class SendERC20TokenViewModel(val token: ERCToken) : ViewModel() {

    fun isAmountValid(inputAmount: String) = inputAmount.isNotEmpty() && isValidDecimal(inputAmount)

    fun hasEnoughBalance(amount: String): Boolean {
        val inputAmount = createSafeBigDecimal(amount)
        val balanceAmount = BigDecimal(TypeConverter.formatHexString(token.value, token.decimals, null))
        return inputAmount.compareTo(balanceAmount) == 0 || inputAmount.compareTo(balanceAmount) == -1
    }

    fun getMaxAmount(): String = TypeConverter.formatHexString(token.value, token.decimals, null)
}