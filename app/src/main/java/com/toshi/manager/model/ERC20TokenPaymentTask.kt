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

package com.toshi.manager.model

import com.toshi.model.local.EthAndFiat
import com.toshi.model.network.UnsignedTransaction
import com.toshi.model.sofa.payment.Payment

data class ERC20TokenPaymentTask(
        override val paymentAmount: EthAndFiat,
        override val gasPrice: EthAndFiat,
        override val totalAmount: EthAndFiat,
        override val payment: Payment,
        override val unsignedTransaction: UnsignedTransaction,
        val tokenSymbol: String,
        val tokenValue: String
) : PaymentTask(
        paymentAmount,
        gasPrice,
        totalAmount,
        payment,
        unsignedTransaction
) {
    constructor(paymentTask: PaymentTask, tokenSymbol: String, tokenValue: String) : this(
            paymentTask.paymentAmount,
            paymentTask.gasPrice,
            paymentTask.totalAmount,
            paymentTask.payment,
            paymentTask.unsignedTransaction,
            tokenSymbol,
            tokenValue
    )
}