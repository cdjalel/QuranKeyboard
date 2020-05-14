/*
 * Copyright (C) 2008-2009 The Android Open Source Project
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
import android.content.res.XmlResourceParser;
import android.inputmethodservice.Keyboard;
import androidx.core.content.ContextCompat;
import android.view.inputmethod.EditorInfo;

public class ArabicKeyboard extends Keyboard {
//    public static final int KEYCODE_SPACE = 32;

    private Key mEnterKey;
    /**
     * Stores the current state of the mode change key. Its width will be dynamically updated to
     * match the region of mModeChangeKey when mModeChangeKey becomes invisible.
     */
    private Key mModeChangeKey;
    /**
     * Stores the current state of the language switch key (a.k.a. globe key). This should be
     * visible while InputMethodManager#shouldOfferSwitchingToNextInputMethod(IBinder)
     * returns true. When this key becomes invisible, its width will be shrunk to zero.
     */
    private Key mLanguageSwitchKey;
    /**
     * Stores the size and other information of {@link #mModeChangeKey} when
     * {@link #mLanguageSwitchKey} is visible. This should be immutable and will be used only as a
     * reference size when the visibility of {@link #mLanguageSwitchKey} is changed.
     */
    private Key mSavedModeChangeKey;
    /**
     * Stores the size and other information of {@link #mLanguageSwitchKey} when it is visible.
     * This should be immutable and will be used only as a reference size when the visibility of
     * {@link #mLanguageSwitchKey} is changed.
     */
    private Key mSavedLanguageSwitchKey;
    
    public ArabicKeyboard(Context context, int xmlLayoutResId) {
        super(context, xmlLayoutResId);
    }

    @Override
    protected Key createKeyFromXml(Resources res, Row parent, int x, int y, 
            XmlResourceParser parser) {
        Key key = new ArabicKey(res, parent, x, y, parser);
        if (key.codes[0] == 10) {
            mEnterKey = key;
        } else if (key.codes[0] == KEYCODE_MODE_CHANGE) {
            mModeChangeKey = key;
            mSavedModeChangeKey = new ArabicKey(res, parent, x, y, parser);
        } else if (key.codes[0] == ArabicKeyboardView.KEYCODE_LANGUAGE_SWITCH) {
            mLanguageSwitchKey = key;
            mSavedLanguageSwitchKey = new ArabicKey(res, parent, x, y, parser);
        }

        return key;
    }

    /**
     * Dynamically change the visibility of the language switch key (a.k.a. globe key).
     * @param visible True if the language switch key should be visible.
     */
    void setLanguageSwitchKeyVisibility(boolean visible) {
        if (visible) {
            // The language switch key should be visible. Restore the size of the mode change key
            // and language switch key using the saved layout.
            mModeChangeKey.width = mSavedModeChangeKey.width;
            mModeChangeKey.x = mSavedModeChangeKey.x;
            mLanguageSwitchKey.width = mSavedLanguageSwitchKey.width;
            mLanguageSwitchKey.icon = mSavedLanguageSwitchKey.icon;
            mLanguageSwitchKey.iconPreview = mSavedLanguageSwitchKey.iconPreview;
        } else {
            // The language switch key should be hidden. Change the width of the mode change key
            // to fill the mode change of the language key so that the user will not see any strange gap.
            mModeChangeKey.width = mSavedModeChangeKey.width + mSavedLanguageSwitchKey.width;
            mLanguageSwitchKey.width = 0;
            mLanguageSwitchKey.icon = null;
            mLanguageSwitchKey.iconPreview = null;
        }
    }

    /**
     * This looks at the ime options given by the current editor, to set the
     * appropriate label on the keyboard's enter key (if it has one).
     */
    void setImeOptions(Context context, int options) {
        if (mEnterKey == null) {
            return;
        }

        switch (options&(EditorInfo.IME_MASK_ACTION|EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
            case EditorInfo.IME_ACTION_GO:
                mEnterKey.iconPreview = null;
                mEnterKey.icon = null;
                mEnterKey.label = context.getResources().getText(R.string.label_go_key);
                break;
            case EditorInfo.IME_ACTION_NEXT:
                mEnterKey.iconPreview = null;
                mEnterKey.icon = null;
                mEnterKey.label = context.getResources().getText(R.string.label_next_key);
                break;
            case EditorInfo.IME_ACTION_SEARCH:
                mEnterKey.icon = ContextCompat.getDrawable(context, R.drawable.sym_keyboard_search);
                mEnterKey.label = null;
                break;
            case EditorInfo.IME_ACTION_SEND:
                mEnterKey.iconPreview = null;
                mEnterKey.icon = null;
                mEnterKey.label = context.getResources().getText(R.string.label_send_key);
                break;
            default:
                mEnterKey.icon = ContextCompat.getDrawable(context, R.drawable.sym_keyboard_return);
                mEnterKey.label = null;
                break;
        }
    }

    static class ArabicKey extends Keyboard.Key {
        
        public ArabicKey(Resources res, Keyboard.Row parent, int x, int y,
                         XmlResourceParser parser) {
            super(res, parent, x, y, parser);
        }
        
        /**
         * Overriding this method so that we can reduce the target area for the key that
         * closes the keyboard. 
         */
        @Override
        public boolean isInside(int x, int y) {
            return super.isInside(x, codes[0] == KEYCODE_CANCEL ? y - 10 : y);
        }
    }

}
