/*
 * Copyright (C) 2016 Benjamin Dobell
 *
 * Licensed under Creative Commons Attribution-ShareAlike 3.0 Unported with attribution required
 * https://creativecommons.org/licenses/by-sa/3.0/ 
 *
 * Copied from:
 * http://stackoverflow.com/questions/4819049/how-can-i-use-typefacespan-or-stylespan-with-a-custom-typeface
 * 
 */

package com.djalel.android.qurankeyboard;


import android.graphics.Paint;
import android.graphics.Typeface;

import android.text.style.MetricAffectingSpan;
import android.text.TextPaint;

public class CustomTypefaceSpan extends MetricAffectingSpan {
    private final Typeface typeface;

    public CustomTypefaceSpan(final Typeface typeface)
    {
        this.typeface = typeface;
    }

    @Override
    public void updateDrawState(final TextPaint drawState)
    {
        apply(drawState);
    }

    @Override
    public void updateMeasureState(final TextPaint paint)
    {
        apply(paint);
    }

    private void apply(final Paint paint)
    {
        final Typeface oldTypeface = paint.getTypeface();
        final int oldStyle = oldTypeface != null ? oldTypeface.getStyle() : 0;
        final int fakeStyle = oldStyle & ~typeface.getStyle();

        if ((fakeStyle & Typeface.BOLD) != 0)
        {
            paint.setFakeBoldText(true);
        }

        if ((fakeStyle & Typeface.ITALIC) != 0)
        {
            paint.setTextSkewX(-0.25f);
        }

        paint.setTypeface(typeface);
    }
}
