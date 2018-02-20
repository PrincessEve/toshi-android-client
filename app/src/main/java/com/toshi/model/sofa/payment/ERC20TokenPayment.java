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

package com.toshi.model.sofa.payment;

import com.squareup.moshi.Json;

public class ERC20TokenPayment extends Payment {
    @Json(name = "token_address")
    private String tokenAddress;

    public ERC20TokenPayment(final String value,
                             final String tokenAddress,
                             final String toAddress,
                             final String fromAddress) {
        this.value = value;
        this.toAddress = toAddress;
        this.fromAddress = fromAddress;
        this.tokenAddress = tokenAddress;
    }

    public String getTokenAddress() {
        return this.tokenAddress;
    }
}
