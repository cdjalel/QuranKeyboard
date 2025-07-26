/*
 * Copyright (C) 2008-2009 The Android Open Source Project
 * Copyright (C) 2017 Djalel Chefrour
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.djalel.android.qurankeyboard;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import android.text.Layout;
import android.text.SpannableString;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
//import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.djalel.android.qurankeyboard.qsearch.AyaMatch;
import com.djalel.android.qurankeyboard.qsearch.Rasm;

import java.util.ArrayList;
import java.util.List;

public class CandidateView extends View {

    private static final int OUT_OF_BOUNDS = -1;

    private final QuranKeyboardIME mService;  // back to the service to communicate with the text field
    private List<String> mSuggestions;              // completion
    private List<AyaMatch> mQuranSuggestions;       // quran search results
    private int mSelectedIndex;
    private int mTouchX = OUT_OF_BOUNDS;
    private final Drawable mSelectionHighlight;
    private boolean mTypedWordValid;
    
    private Rect mBgPadding;

    private static final int SCROLL_PIXELS = 20;

    private static final int X_GAP = 10;
    
    private static final List<String> EMPTY_LIST = new ArrayList<>();
    private static final List<AyaMatch> EMPTY_MLIST = new ArrayList<>();

    private final int mColorNormal;
    private final int mColorRecommended;
    private final int mColorOther;
    private final int mVerticalPadding; // This field holds the pixel value for TOTAL vertical padding
    private final TextPaint mPaint;
    private final Typeface mDefaultTf;
    private final Typeface mUthamniTf;
    private boolean mScrolled;
    private int mTargetScrollX;
    
    private int mTotalWidth;

    private final GestureDetector mGestureDetector;

    /**
     * Construct a CandidateView for showing suggested words for completion.
     * @param context context
     */
    public CandidateView(Context context) {
        this(context, null); // Chain to the next constructor
    }

    // THIS IS THE CRUCIAL ONE THAT IS MISSING OR INCORRECT
    public CandidateView(Context context, AttributeSet attrs) {
        this(context, attrs, 0); // Chain to the next constructor, passing 0 for default style
    }

    // Good practice to include this one as well
    public CandidateView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mService = (QuranKeyboardIME) context;

        mSelectionHighlight = ContextCompat.getDrawable(context,
                android.R.drawable.list_selector_background);
        if (null != mSelectionHighlight) {
            mSelectionHighlight.setState(new int[] {
                    android.R.attr.state_enabled,
                    android.R.attr.state_focused,
                    android.R.attr.state_window_focused,
                    android.R.attr.state_pressed
            });
        }

        Resources r = context.getResources();
        
        setBackgroundColor(ContextCompat.getColor(context, R.color.candidate_background));
        
        mColorNormal = ContextCompat.getColor(context, R.color.candidate_normal);
        mColorRecommended = ContextCompat.getColor(context, R.color.candidate_recommended);
        mColorOther = ContextCompat.getColor(context, R.color.candidate_other);
        mVerticalPadding = r.getDimensionPixelSize(R.dimen.candidate_vertical_padding);

        mPaint = new TextPaint();
        mPaint.setColor(mColorNormal);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(r.getDimensionPixelSize(R.dimen.candidate_font_height));
        mPaint.setStrokeWidth(0);
        mDefaultTf = mPaint.getTypeface();
        mUthamniTf = mService.getUthmaniTypeFace();

        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2,
                    float distanceX, float distanceY) {
                mScrolled = true;
                int sx = getScrollX();
                sx += (int) distanceX;
                if (sx < 0) {
                    sx = 0;
                }
                if (sx + getWidth() > mTotalWidth) {                    
                    sx -= (int) distanceX;
                }
                mTargetScrollX = sx;
                scrollTo(sx, getScrollY());
                invalidate();
                return true;
            }
        });
        setHorizontalFadingEdgeEnabled(true);
        setWillNotDraw(false);
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);

        setLayoutDirection(LAYOUT_DIRECTION_RTL);
        setTextDirection(TEXT_DIRECTION_RTL);
    }

    @Override
    public int computeHorizontalScrollRange() {
        return mTotalWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        // 1. Get the measured width, typically parent's width for a match_parent view
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);

        // 2. Get padding from highlight drawable (with null check)
        Rect padding = new Rect();
        if (mSelectionHighlight != null) {
            mSelectionHighlight.getPadding(padding);
        }

        // 3. Calculate the desired height, mVerticalPadding is TOTAL padding
        float textRenderHeight = mPaint.descent() - mPaint.ascent();
        // This calculates the height needed for ONE line of text with padding on top and bottom.
        final int calculatedDesiredHeight = (int) (textRenderHeight + mVerticalPadding + padding.top + padding.bottom);

        // 4. Set the measured dimensions
        // resolveSize will respect the EXACTLY spec (if layout_height is fixed in XML),
        // or use calculatedDesiredHeight if layout_height is wrap_content or unspecified.
        setMeasuredDimension(measuredWidth, resolveSize(calculatedDesiredHeight, heightMeasureSpec));
    }

    private void drawSuggestions(Canvas canvas)
    {
//        mPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.candidate_font_height));
//        mPaint.setTypeface(mDefaultTf);

        if (mBgPadding == null) {
            mBgPadding = new Rect(0, 0, 0, 0);
            if (getBackground() != null) {
                getBackground().getPadding(mBgPadding);
            }
        }
        int x = 0;
        final int count = mSuggestions.size();
        final int height = getHeight();
        final Rect bgPadding = mBgPadding;
        final Paint paint = mPaint;
        final int touchX = mTouchX;
        final int scrollX = getScrollX();
        final boolean scrolled = mScrolled;
        final boolean typedWordValid = mTypedWordValid;

        //  Calculate text baseline for vertical centering using TOTAL padding
        float textRenderHeight = paint.descent() - paint.ascent();
        float totalContentHeight = textRenderHeight + mVerticalPadding; // mVerticalPadding is TOTAL padding
        float contentBlockStartY = (height - totalContentHeight) / 2.0f;
        // Baseline = content block's start Y + (half of total vertical padding) - text ascent
        final float textBaselineY = contentBlockStartY + (mVerticalPadding / 2.0f) - paint.ascent();

        for (int i = 0; i < count; i++) {
            String suggestion = mSuggestions.get(i);
            float textWidth = paint.measureText(suggestion);
            final int wordWidth = (int) textWidth + X_GAP * 2;

            paint.setColor(mColorNormal);
            if (touchX + scrollX >= x && touchX + scrollX < x + wordWidth && !scrolled) {
                if (canvas != null) {
                    canvas.translate(x, 0);
                    mSelectionHighlight.setBounds(0, bgPadding.top, wordWidth, height);
                    mSelectionHighlight.draw(canvas);
                    canvas.translate(-x, 0);
                }
                mSelectedIndex = i;
            }

            if (canvas != null) {
                if ((i == 1 && !typedWordValid) || (i == 0 && typedWordValid)) {
                    paint.setColor(mColorRecommended);
                } else if (i != 0) {
                    paint.setColor(mColorOther);
                }
                canvas.drawText(suggestion, x + X_GAP, textBaselineY, paint);
                paint.setColor(mColorOther);
                canvas.drawLine(x + wordWidth, bgPadding.top, x + wordWidth, height + 1, paint);
            }
            x += wordWidth;
        }
        mTotalWidth = x;
        if (mTargetScrollX != getScrollX()) {
            scrollToTarget();
        }
    }

    // TODO display vertical candidates of 3 entries MAX view with horizontal scrolling per entry
    // TODO center search pattern in the view if the aya is longer than view width
    // and display text starting at the right.
    private void drawMatchSuggestions(Canvas canvas)
    {
        if (mService.getPrefRasm() == Rasm.UTHMANI) {
            if (mPaint.getTypeface() != mUthamniTf) {
                mPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.candidate_uthmani_font_height));
                mPaint.setTypeface(mUthamniTf);
            }
        }
        else if (mPaint.getTypeface() != mDefaultTf) {
            mPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.candidate_font_height));
            mPaint.setTypeface(mDefaultTf);
        }

        if (mBgPadding == null) {
            mBgPadding = new Rect(0, 0, 0, 0);
            if (getBackground() != null) {
                getBackground().getPadding(mBgPadding);
            }
        }
        int x = 0;
        final int count = mQuranSuggestions.size();
        final int height = getHeight();
        final Rect bgPadding = mBgPadding;
        final TextPaint paint = mPaint;
        final int touchX = mTouchX;
        final int scrollX = getScrollX();
        final boolean scrolled = mScrolled;

        for (int i = 0; i < count; i++) {
            AyaMatch match = mQuranSuggestions.get(i);
            float textWidth = paint.measureText(match.strBld.toString());
            final int wordWidth = (int) textWidth + X_GAP * 2;

            paint.setColor(mColorNormal);
            if (touchX + scrollX >= x && touchX + scrollX < x + wordWidth && !scrolled) {
                if (canvas != null) {
                    canvas.translate(x, 0);
                    mSelectionHighlight.setBounds(0, bgPadding.top, wordWidth, height);
                    mSelectionHighlight.draw(canvas);
                    canvas.translate(-x, 0);
                }
                mSelectedIndex = i;
            }

            if (canvas != null) {
                SpannableString spanStr = new SpannableString(match.strBld);
                for(Integer oc : match.indexes) {
                    spanStr.setSpan(new ForegroundColorSpan(
                            mColorRecommended), oc, oc + match.mlen, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
                    // Next has a problem with arabic letter joining.
                    // spanStr.setSpan(new StyleSpan(Typeface.BOLD), oc, oc + match.mlen, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                paint.setColor(mColorNormal);
                StaticLayout layout = new StaticLayout(spanStr, paint, (int) textWidth + X_GAP,
                        Layout.Alignment.ALIGN_OPPOSITE, 1, 0, true);
                canvas.translate(x + X_GAP, 0);
                layout.draw(canvas);
                canvas.translate(-(x + X_GAP), 0);

                paint.setColor(mColorOther);
                canvas.drawLine(x + wordWidth + 0.5f, bgPadding.top,
                        x + wordWidth + 0.5f, height + 1, paint);
            }
            x += wordWidth;
        }
        mTotalWidth = x;

        if (mTargetScrollX != getScrollX()) {
            scrollToTarget();
        }
    }

    /**
     * If the canvas is null, then only touch calculations are performed to pick the target
     * candidate.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas != null) {
            super.onDraw(canvas);
        }

        mTotalWidth = 0;
        if (!mSuggestions.isEmpty()) {
            drawSuggestions(canvas);
            return;
        }

        if (!mQuranSuggestions.isEmpty()) {
            drawMatchSuggestions(canvas);
        }
    }
    
    private void scrollToTarget() {
        int sx = getScrollX();
        if (mTargetScrollX > sx) {
            sx += SCROLL_PIXELS;
            if (sx >= mTargetScrollX) {
                sx = mTargetScrollX;
                //requestLayout();
            }
        } else {
            sx -= SCROLL_PIXELS;
            if (sx <= mTargetScrollX) {
                sx = mTargetScrollX;
                //requestLayout();
            }
        }
        scrollTo(sx, getScrollY());
        invalidate();
    }
    
    private void updateSuggestions()
    {
        scrollTo(0, 0);
        mTargetScrollX = 0;
        invalidate();
        requestLayout();
    }

    public void setSuggestions(List<String> suggestions,
            boolean typedWordValid) {
        clear();
        if (suggestions != null) {
            mSuggestions = suggestions;
            mQuranSuggestions = EMPTY_MLIST;
        }
        mTypedWordValid = typedWordValid;
        updateSuggestions();
    }

    public void setQuranSuggestions(List<AyaMatch> suggestions) {
        clear();
        if (suggestions != null) {
            mQuranSuggestions = suggestions;
            mSuggestions = EMPTY_LIST;
        }
        updateSuggestions();
    }

    public void clear() {
        mSuggestions = EMPTY_LIST;
        mQuranSuggestions = EMPTY_MLIST;
        mTouchX = OUT_OF_BOUNDS;
        mSelectedIndex = -1;
        invalidate();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent me) {
        if (mGestureDetector.onTouchEvent(me)) {
            // If GestureDetector consumed it, especially for scroll,
            // you might not need to do much more here for that specific event.
            // However, ACTION_DOWN needs to propagate to mGestureDetector.
            // And you'll need ACTION_UP for selection logic if not a scroll.
            if (me.getAction() == MotionEvent.ACTION_DOWN) {
                mScrolled = false; // Reset scroll flag on new touch sequence
            }
            // if it was a scroll, onScroll in the listener would have handled it.
            // if it was a fling, onFling would handle it.
            // return true to indicate you've handled the touch.
            return true;
        }

        int action = me.getAction();
        int x = (int) me.getX();
        int y = (int) me.getY();
        mTouchX = x;

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mScrolled = false;
            invalidate();
            break;
        case MotionEvent.ACTION_MOVE:
            if (y <= 0) {
                // Fling up!?
                if (mSelectedIndex >= 0) {
                    mService.pickSuggestionManually(mSelectedIndex);
                    mSelectedIndex = -1;
                }
            }
            invalidate();
            break;
        case MotionEvent.ACTION_UP:
            if (!mScrolled) {
                if (mSelectedIndex >= 0) {
                    mService.pickSuggestionManually(mSelectedIndex);
                }
            }
            mSelectedIndex = -1;
            removeHighlight();
            //requestLayout();
            break;
        }
        return true;
    }
    
//    /**
//     * For flick through from keyboard, call this method with the x coordinate of the flick
//     * gesture.
//     * @param x x
//     */
//    /*
//    public void takeSuggestionAt(float x) {
//        mTouchX = (int) x;
//        // To detect candidate
////        onDraw(null);
//        if (mSelectedIndex >= 0) {
//            mService.pickSuggestionManually(mSelectedIndex);
//        }
//        invalidate();
//    }
//    */

    private void removeHighlight() {
        mTouchX = OUT_OF_BOUNDS;
        invalidate();
    }
}
