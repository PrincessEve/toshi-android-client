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

package com.toshi.crypto.util;


import com.toshi.util.LocaleUtil;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.text.DecimalFormat;

public class TypeConverter {

    public static BigInteger StringHexToBigInteger(final String input) {
        if (input == null) {
            return BigInteger.ZERO;
        }

        final String hexa = input.startsWith("0x") ? input.substring(2) : input;
        try {
            return new BigInteger(hexa, 16);
        } catch (final NumberFormatException ex) {
            return BigInteger.ZERO;
        }
    }

    public static byte[] StringHexToByteArray(String x) {
        if (x.startsWith("0x")) {
            x = x.substring(2);
        }
        if (x.length() % 2 != 0) x = "0" + x;
        return Hex.decode(x);
    }

    public static String jsonStringToString(final String jsonString) {
        if (jsonString.startsWith("\"") && jsonString.endsWith("\"")) return jsonString.substring(1, jsonString.length() - 1);
        if (jsonString.startsWith("\"")) return jsonString.substring(1);
        if (jsonString.endsWith("\"")) return jsonString.substring(0, jsonString.length() - 1);
        return jsonString;
    }

    public static String toJsonHex(final byte[] x) {
        return "0x"+Hex.toHexString(x);
    }

    public static String toJsonHex(final String x) {
        if (x.startsWith("0x")) return x;
        return "0x"+x;
    }

    public static String toJsonHex(final long n) {
        return "0x"+Long.toHexString(n);
    }

    public static String toJsonHex(final BigInteger n) {
        return "0x"+ n.toString(16);
    }

    public static String skeletonAndSignatureToRLPEncodedHex(final String skeleton, final String signature) {
        final Object[] decoded = (Object[])RLP.decode(TypeConverter.StringHexToByteArray(skeleton), 0).getDecoded();

        if (decoded.length != 9) {
            throw new IllegalStateException("Invalid Transaction Skeleton: Decoded RLP length is wrong");
        }

        if (!isEmptyString(decoded[decoded.length - 2])
            ||!isEmptyString(decoded[decoded.length - 1])) {
            throw new IllegalStateException("Transaction is already signed!");
        }

        final BigInteger r = TypeConverter.StringHexToBigInteger(signature.substring(2, 66));
        final BigInteger s = TypeConverter.StringHexToBigInteger(signature.substring(66, 130));
        final int v = TypeConverter.StringHexToBigInteger(signature.substring(130)).intValue();
        final int vee = getVee(decoded[decoded.length - 3]);

        decoded[decoded.length - 3] = v + vee;
        decoded[decoded.length - 2] = r;
        decoded[decoded.length - 1] = s;
        return TypeConverter.toJsonHex(RLP.encode(decoded));
    }

    private static int getVee(final Object obj) {
        if (obj instanceof byte[]) {
            int networkId = RLP.decodeInt((byte[]) obj, 0);
            return 35 + networkId * 2;
        }
        return 27;
    }

    private static boolean isEmptyString(final Object obj) {
        return obj instanceof String && ((String) obj).length() == 0;
    }

    public static String formatHexString(final String value, final int decimals, final String format) {
        final String decimal = StringHexToBigInteger(value).toString();
        final DecimalFormat nf = new DecimalFormat(format);
        final String formattedString;
        if (decimals > 0) {
            final char separator = LocaleUtil.getDecimalFormatSymbols().getMonetaryDecimalSeparator();
            final int decimalPosition = decimal.length() - decimals;
            final String decimalNumberWithDecimals = new StringBuilder(decimal)
                    .insert(decimalPosition, separator)
                    .toString();
            formattedString = nf.format(Double.valueOf(decimalNumberWithDecimals));
        } else formattedString = nf.format(Double.valueOf(decimal));
        return formattedString;
    }
}
