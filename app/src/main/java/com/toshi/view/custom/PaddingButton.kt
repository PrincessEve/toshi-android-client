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
import android.util.AttributeSet
import android.widget.LinearLayout
import com.toshi.R
import com.toshi.extensions.isVisible
import kotlinx.android.synthetic.main.view_padding_button.view.*

class PaddingButton : LinearLayout {

    private var text = ""
    private var image: Int = 0

    constructor(context: Context): super(context) {
        init()
    }
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs) {
        parseAttributeSet(context, attrs)
        init()
    }
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int): super(context, attrs, defStyle) {
        parseAttributeSet(context, attrs)
        init()
    }

    private fun parseAttributeSet(context: Context, attrs: AttributeSet?) {
        val attributes = context.theme.obtainStyledAttributes(attrs, R.styleable.PaddingButton, 0, 0)
        text = attributes.getString(R.styleable.PaddingButton_paddingButtonText)
        image = attributes.getResourceId(R.styleable.PaddingButton_paddingButtonImage, 0)
        attributes.recycle()
    }

    private fun init() {
        inflate(context, R.layout.view_padding_button, this)
        setText()
        setImage()
        showSpace()
    }

    private fun setText() {
        if (text.isNotEmpty()) {
            btn.text = text
            btn.isVisible(true)
        } else btn.isVisible(false)
    }

    private fun setImage() {
        if (image != 0) {
            btnImage.isVisible(true)
            btnImage.setImageResource(image)
        } else btnImage.isVisible(false)
    }

    private fun showSpace() {
        if (image != 0 && text.isNotEmpty()) space.isVisible(true)
        else space.isVisible(false)
    }
}