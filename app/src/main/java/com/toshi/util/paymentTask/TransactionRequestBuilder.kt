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

package com.toshi.util.paymentTask

import com.toshi.model.local.UnsignedW3Transaction
import com.toshi.model.network.TransactionRequest
import com.toshi.model.sofa.payment.ERC20TokenPayment
import com.toshi.model.sofa.payment.Payment

class TransactionRequestBuilder {

    companion object {
        private const val MAX = "max"
    }

    fun generateTransactionRequest(payment: Payment): TransactionRequest {
        return TransactionRequest(
                value = payment.value,
                from = payment.fromAddress,
                to = payment.toAddress
        )
    }

    fun generateMaxAmountTransactionRequest(payment: Payment): TransactionRequest {
        return TransactionRequest(
                value = MAX,
                from = payment.fromAddress,
                to = payment.toAddress
        )
    }

    fun generateTransactionRequest(payment: ERC20TokenPayment): TransactionRequest {
        return TransactionRequest(
                value = payment.value,
                from = payment.fromAddress,
                to = payment.toAddress,
                tokenAddress = payment.tokenAddress
        )
    }

    fun generateTransactionRequest(transaction: UnsignedW3Transaction): TransactionRequest {
        return TransactionRequest(
                value = transaction.value,
                from = transaction.from,
                to = transaction.to,
                data = transaction.data,
                gas = transaction.gas,
                gasPrice = transaction.gasPrice,
                nonce = transaction.nonce
        )
    }
}