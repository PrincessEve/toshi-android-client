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

package com.toshi.crypto.util

import com.toshi.crypto.util.HashUtil.sha3

fun usesChecksum(address: String?): Boolean {
    return address?.substring(2)?.let {
        it != it.toLowerCase() && it != it.toUpperCase()
    } == true
}

fun hasValidChecksum(paymentAddress: String?): Boolean {
    return paymentAddress?.let { paymentAddress == toAddressWithChecksum(paymentAddress) } ?: false
}

fun toAddressWithChecksum(address: String): String {
    val stripped = address.replace("0x", "").toLowerCase()
    val digested = sha3(stripped.toByteArray()).toHex()
    val hexChars = "0123456789abcdef".toCharArray()
    val buffer = StringBuffer()

    for (c in 0 until stripped.length) {
        if (hexChars.indexOf(digested[c]) >= 8) buffer.append(stripped[c].toUpperCase())
        else buffer.append(stripped[c])
    }

    return "0x" + buffer.toString()
}

private fun ByteArray.toHex() = joinToString("") {
    it.toInt().and(0xff).toString(16).padStart(2, '0')
}

fun isPaymentAddressValid(paymentAddress: String?): Boolean {
    val regex = Regex("^0x[a-fA-F0-9]{40}\$")
    return paymentAddress?.let {
        regex.matches(paymentAddress) && !hasInvalidChecksum(paymentAddress)
    } ?: false
}

private fun hasInvalidChecksum(paymentAddress: String): Boolean {
    return usesChecksum(paymentAddress) && !hasValidChecksum(paymentAddress)
}
