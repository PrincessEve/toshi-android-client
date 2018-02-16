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

package com.toshi.util;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;

import com.toshi.R;

public class DialogUtil {
    public static AlertDialog.Builder getBaseDialog(final Context context,
                                                    final @StringRes int title,
                                                    final @StringRes int message,
                                                    final @StringRes int negativeButtonText) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogCustom);
        return builder
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(negativeButtonText, (dialog, which) -> dialog.dismiss());
    }

    public static AlertDialog.Builder getBaseDialog(final Context context,
                                                    final @StringRes int title,
                                                    final @StringRes int message,
                                                    final @StringRes int negativeButtonText,
                                                    final DialogInterface.OnClickListener listener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogCustom);
        return builder
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(negativeButtonText, listener);
    }

    public static AlertDialog.Builder getBaseDialog(final Context context,
                                                     final @StringRes int title,
                                                     final @StringRes int message,
                                                     final @StringRes int positiveButtonText,
                                                     final @StringRes int negativeButtonText,
                                                     final DialogInterface.OnClickListener listener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogCustom);
        return builder
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(negativeButtonText, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(positiveButtonText, listener);
    }

    public static AlertDialog.Builder getBaseDialog(final Context context,
                                                    final @StringRes int title,
                                                    final @StringRes int message,
                                                    final @StringRes int positiveButtonText,
                                                    final @StringRes int negativeButtonText,
                                                    final DialogInterface.OnClickListener positiveListener,
                                                    final DialogInterface.OnClickListener negativeListener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogCustom);
        return builder
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButtonText, (dialog, which) -> {
                    positiveListener.onClick(dialog, which);
                    dialog.dismiss();
                })
                .setNegativeButton(negativeButtonText, (dialog, which) -> {
                    negativeListener.onClick(dialog, which);
                    dialog.dismiss();
                });
    }

    public static AlertDialog.Builder getBaseDialog(final Context context,
                                                    final String title,
                                                    final String message,
                                                    final @StringRes int positiveButtonText,
                                                    final @StringRes int negativeButtonText,
                                                    final DialogInterface.OnClickListener listener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogCustom);
        return builder
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(negativeButtonText, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(positiveButtonText, listener);
    }
}
