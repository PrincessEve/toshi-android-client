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
import android.graphics.Canvas
import android.graphics.Paint
import android.support.v7.widget.AppCompatEditText
import android.text.TextPaint
import android.util.AttributeSet
import com.toshi.R
import com.toshi.extensions.getColorById
import com.toshi.extensions.getString

class PrefixSuffixEditText : AppCompatEditText {
    private lateinit var textPaint: TextPaint
    private var suffix = ""
    private var prefix = ""
    private var prefixPadding = 0

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    private fun init() {
        textPaint = TextPaint()
        textPaint.color = currentHintTextColor
        textPaint.textSize = textSize
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.typeface = typeface
    }

    fun setPrefix(prefix: String) {
        this.prefix = prefix
        prefixPadding = textPaint.measureText(prefix).toInt()
        invalidate()
    }

    fun setSuffix(suffix: String) {
        this.suffix = suffix
        invalidate()
    }

    public override fun onDraw(canvas: Canvas) {
        updatePaintColor()
        drawPrefix(canvas)
        drawSuffix(canvas)
        super.onDraw(canvas)
    }

    private fun updatePaintColor() {
        textPaint.color = getColor()
    }

    private fun drawPrefix(canvas: Canvas) = canvas.drawText(prefix, 0f, baseline.toFloat(), textPaint)

    private fun drawSuffix(canvas: Canvas) {
        val text = if (text.isNotEmpty()) text.toString()
        else getString(R.string._0_0)
        val x = textPaint.measureText("$text ").toInt() + compoundPaddingLeft + paddingLeft
        canvas.drawText(suffix, x.toFloat(), baseline.toFloat(), textPaint)
    }

    private fun getColor(): Int {
        val colorRes = if (text.isEmpty()) R.color.textColorHint else R.color.textColorPrimary
        return getColorById(colorRes)
    }

    override fun getCompoundPaddingLeft() = super.getCompoundPaddingLeft() + prefixPadding
}