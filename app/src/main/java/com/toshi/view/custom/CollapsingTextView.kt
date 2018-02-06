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

package com.toshi.view.custom

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.widget.TextView
import com.toshi.R
import com.toshi.extensions.getColorById

class CollapsingTextView : TextView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    fun setCollapsedText(value: String) {
        if (value.length <= 10) {
            text = value
            return
        }

        val collapsedText = "${value.substring(0, 5)}..${value.takeLast(5)}"
        val spannableString = SpannableStringBuilder(collapsedText)
        val dividerColor = ForegroundColorSpan(getColorById(R.color.textColorSecondary))
        spannableString.setSpan(dividerColor, 5, 7, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        text = spannableString
    }
}