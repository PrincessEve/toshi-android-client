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

package com.toshi.view.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.v7.widget.AppCompatEditText;
import android.text.TextPaint;
import android.util.AttributeSet;

import com.toshi.R;

public class SuffixEditText extends AppCompatEditText {
    TextPaint textPaint;
    float fontHeight;
    String suffix = "";

    public SuffixEditText(Context context) {
        super(context);
        init();
    }

    public SuffixEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        if (textPaint == null) textPaint = new TextPaint();
        fontHeight = getTextSize();
        textPaint.setColor(getCurrentHintTextColor());
        textPaint.setTextSize(fontHeight);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    public void setTypeface(final Typeface typeface) {
        if (textPaint != null) textPaint.setTypeface(typeface);
        postInvalidate();
        super.setTypeface(typeface);
    }

    public void setSuffix(final String suffix) {
        this.suffix = suffix;
    }

    @Override
    public void onDraw(final Canvas canvas) {
        final String text = getText().length() > 0
                ? getText().toString()
                : getContext().getString(R.string._0_0);

        final int x = (int) textPaint.measureText(text) + getPaddingLeft();
        canvas.drawText(suffix, x, getBaseline(), textPaint);
        super.onDraw(canvas);
    }
}