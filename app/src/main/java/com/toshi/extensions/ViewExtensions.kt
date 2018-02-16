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

package com.toshi.extensions

import android.support.annotation.ColorRes
import android.support.annotation.DimenRes
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import com.toshi.R
import com.toshi.view.custom.HorizontalLineDivider

fun View.isVisible(bool: Boolean?, nonVisibleState: Int = View.GONE) {
    visibility = if (bool == true) View.VISIBLE else nonVisibleState
}

fun RecyclerView.addHorizontalLineDivider(
        leftPadding: Int = getPxSize(R.dimen.avatar_size_small)
                + getPxSize(R.dimen.activity_horizontal_margin)
                + getPxSize(R.dimen.list_item_avatar_margin),
        rightPadding: Int = getPxSize(R.dimen.activity_horizontal_margin),
        color: Int = getColorById(R.color.divider),
        startPosition: Int = 0
): HorizontalLineDivider {
    val divider = HorizontalLineDivider(color)
            .setRightPadding(rightPadding)
            .setLeftPadding(leftPadding)
            .setStartPosition(startPosition)
    addItemDecoration(divider)
    return divider
}

fun View.getPxSize(@DimenRes id: Int) = resources.getDimensionPixelSize(id)

fun View.getColorById(@ColorRes id: Int) = ContextCompat.getColor(context, id)

fun View.getString(@StringRes id: Int): String = context.getString(id)
