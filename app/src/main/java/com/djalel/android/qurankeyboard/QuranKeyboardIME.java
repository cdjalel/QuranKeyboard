/*
 * Copyright (C) 2008-2009 The Android Open Source Project
 * Copyright (C) 2015 Menny Even-Danan
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.CallSuper;
import android.support.annotation.DrawableRes;
import android.text.InputType;
//import android.util.Log;
import android.text.SpannableStringBuilder;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class QuranKeyboardIME extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {
    private static final boolean DEBUG = false;
//    public static final String TAG = "QuranKeyboardIME";

    private InputMethodManager mInputMethodManager;

    private ArabicKeyboardView mInputView;
    private CandidateView mCandidateView;
    private CompletionInfo[] mCompletions;
    
    private StringBuilder mComposing = new StringBuilder();
    private boolean mPredictionOn;
    private boolean mCompletionOn;
    private int mLastDisplayWidth;
    
    private ArabicKeyboard mSymbolsKeyboard;
    private ArabicKeyboard mSymbolsShiftedKeyboard;
    private ArabicKeyboard mArabicKeyboard;
    private ArabicKeyboard mCurKeyboard;
    
    private String mWordSeparators;
    private List<String> mSuggestions;
    private List<AyaMatch> mQuranSuggestions;
    private static final List<String> EMPTY_LIST = new ArrayList<>();
    private static final List<AyaMatch> EMPTY_MLIST = new ArrayList<>();
    private QuranSearch mQuranSearch;
    private boolean mDoQuranSearch;
    private int mSavedPreSpaces;

    private Typeface mUthmaniTypeFace;

    private AlertDialog mOptionsDialog;

    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override public void onCreate() {
        super.onCreate();
        mInputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        mWordSeparators = getResources().getString(R.string.word_separators);
        mSuggestions = EMPTY_LIST;
        mQuranSuggestions = EMPTY_MLIST;
        mSavedPreSpaces = 0;

        try {
            mQuranSearch = new QuranSearch(this);
        } catch (IOException |SecurityException e) {
            mQuranSearch = null;
            Toast.makeText(this, "Quran Search disabled, File not found!", Toast.LENGTH_LONG).show();
            // TODO disable/gray the Moshaf/shift key & disable/grey Prefs
            if (DEBUG) e.printStackTrace();
        }

        if (mQuranSearch != null) {
            PreferenceManager.setDefaultValues(this, R.xml.ime_preferences, false);

            setUthmaniTypeFace(Typeface.createFromAsset(getAssets(), "UthmanicHafs.otf"));
        }
    }
    
    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override public void onInitializeInterface() {
        if (mArabicKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }
        mArabicKeyboard = new ArabicKeyboard(this, R.xml.arabic);
        mSymbolsKeyboard = new ArabicKeyboard(this, R.xml.symbols);
        mSymbolsShiftedKeyboard = new ArabicKeyboard(this, R.xml.symbols_shift);
    }
    
    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override public View onCreateInputView() {
        mInputView = (ArabicKeyboardView) getLayoutInflater().inflate(
                R.layout.input, null);
        mInputView.setOnKeyboardActionListener(this);
        setArabicKeyboard(mArabicKeyboard);

        // Determine Moshaf key (old shift key) status depending on Prefs and mPredictions
        if (!mPredictionOn) {
            // The alphabetic keyboard Moshaf key must start disabled/greyed when prediction is OFF.
            mDoQuranSearch = false;
            // TODO Disable/grey shift key
        } else {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            mDoQuranSearch = sharedPref.getBoolean("pref_start_shifted", true);
        }
        mArabicKeyboard.setShifted(mDoQuranSearch);

        // resetting token users
        mOptionsDialog = null;

        return mInputView;
    }

    private void setArabicKeyboard(ArabicKeyboard nextKeyboard) {
        final boolean shouldSupportLanguageSwitchKey =
                mInputMethodManager.shouldOfferSwitchingToNextInputMethod(getToken());
        nextKeyboard.setLanguageSwitchKeyVisibility(shouldSupportLanguageSwitchKey);

        mInputView.setKeyboard(nextKeyboard);
    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override public View onCreateCandidatesView() {
        mCandidateView = new CandidateView(this);
        return mCandidateView;
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        
        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        mComposing.setLength(0);
        updateCandidates();

        mPredictionOn = false;
        mCompletionOn = false;
        mCompletions = null;

        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
                // Numbers and dates default to the symbols keyboard, with
                // no extra features.
                mCurKeyboard = mSymbolsKeyboard;
                break;
                
            case InputType.TYPE_CLASS_PHONE:
                // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
                mCurKeyboard = mSymbolsKeyboard;
                break;
                
            case InputType.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                mCurKeyboard = mArabicKeyboard;
                mPredictionOn = true;

                // We now look for a few special variations of text that will
                // modify our behavior.
                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false;
                }
                
                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        || variation == InputType.TYPE_TEXT_VARIATION_URI
                        || variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    mPredictionOn = false;
                }

                if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    mPredictionOn = false;
                    mCompletionOn = isFullscreenMode();
                }
                break;
                
            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                mCurKeyboard = mArabicKeyboard;
        }

        // Update the label on the enter key, depending on what the application
        // says it will do.
        mCurKeyboard.setImeOptions(this, attribute.imeOptions);
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override public void onFinishInput() {
        super.onFinishInput();
        
        // Clear current composing text and candidates.
        mComposing.setLength(0);
        updateCandidates();
        
        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false);
        
        mCurKeyboard = mArabicKeyboard;
        if (mInputView != null) {
            mInputView.closing();
        }
    }
    
    @Override public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.
        setArabicKeyboard(mCurKeyboard);
        mInputView.closing();
    }

    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override public void onUpdateSelection(int oldSelStart, int oldSelEnd,
            int newSelStart, int newSelEnd,
            int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
        
        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        if (mComposing.length() > 0 && (newSelStart != candidatesEnd
                || newSelEnd != candidatesEnd)) {
            mComposing.setLength(0);
            updateCandidates();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        }
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override public void onDisplayCompletions(CompletionInfo[] completions) {
        if (mCompletionOn) {
            mCompletions = completions;
            if (completions == null) {
                setSuggestions(null, false);
                return;
            }
            
            List<String> stringList = new ArrayList<>();
            for (CompletionInfo ci : completions) {
                if (ci != null) stringList.add(ci.getText().toString());
            }
            setSuggestions(stringList, true);
        }
    }
    
    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our keyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.
                if (event.getRepeatCount() == 0 && mInputView != null) {
                    if (mInputView.handleBack()) {
                        return true;
                    }
                }
                break;
                
            case KeyEvent.KEYCODE_DEL:
                // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                if (mComposing.length() > 0) {
                    onKey(Keyboard.KEYCODE_DELETE, null);
                    return true;
                }
                break;
                
            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these.
                return false;

            case KeyEvent.KEYCODE_SEARCH: // google samples of how this one is used
                return true;

            default:
                break;
        }
        
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    private void commitTyped(InputConnection inputConnection) {
        if (mComposing.length() > 0) {
            inputConnection.commitText(mComposing, mComposing.length());
            mComposing.setLength(0);
            updateCandidates();
        }
    }
    
    /**
     * Helper to determine if a given character code is alphabetic.
     */
    private boolean isLetter(int code) { return Character.isLetter(code); }

    private boolean isSpace(int code) { return code == (int)' '; }
    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }
    
    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }

    // Implementation of KeyboardViewListener
    public void onKey(int primaryCode, int[] keyCodes) {
        if (isWordSeparator(primaryCode)) {
            // Handle separator. DCH: removed the space ' ' 0x0020
            if (mComposing.length() > 0) {
                commitTyped(getCurrentInputConnection());
            }
            sendKey(primaryCode);
        } else if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            handleShift();
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
            handleClose();
        } else if (primaryCode == ArabicKeyboardView.KEYCODE_LANGUAGE_SWITCH) {
            handleLanguageSwitch();
        } else if (primaryCode == ArabicKeyboardView.KEYCODE_OPTIONS) {
            showOptionsMenu();
        } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE
                && mInputView != null) {
            Keyboard current = mInputView.getKeyboard();
            if (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard) {
                setArabicKeyboard(mArabicKeyboard);
            } else {
                setArabicKeyboard(mSymbolsKeyboard);
                mSymbolsKeyboard.setShifted(false);
            }
        } else {
            handleCharacter(primaryCode);
        }
    }

    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        if (mComposing.length() > 0) {
            commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
    }

    private String preTrimPattern (StringBuilder p)
    {
        // return p.toString().replaceAll("^\\s*", ""));
        for (mSavedPreSpaces = 0;
             mSavedPreSpaces < p.length() && isSpace(p.charAt(mSavedPreSpaces));
             mSavedPreSpaces++) ;

        return p.substring(mSavedPreSpaces);
    }

    /** main buisness
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private void updateCandidates() {
        if (!mCompletionOn && mDoQuranSearch) {
            if (mComposing.length() == 0) {
                setSuggestions(null, false);
                return;
            }

            String pattern = mComposing.toString();
            String trimedPattern = preTrimPattern(mComposing);
            if (mQuranSearch.searchable(trimedPattern)) {
                int limit = getPrefSearchLimit();
                ArrayList<AyaMatch> qlist = mQuranSearch.search(trimedPattern, limit);
                if (!qlist.isEmpty()) {
                    setQuranSuggestions(qlist);
                    return;
                }
                // FALLTHROUGH
            }
            ArrayList<String> list = new ArrayList<>();
            list.add(pattern);
            setSuggestions(list, true);
        }
    }
    
    private void setSuggestions(List<String> suggestions, boolean typedWordValid) {
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            setCandidatesViewShown(true);
        }
        mSavedPreSpaces = 0;
        mQuranSuggestions = EMPTY_MLIST;
        mSuggestions = EMPTY_LIST;
        if (mCandidateView != null) {
            if (suggestions != null) {
                mSuggestions = suggestions;
            }
            mCandidateView.setSuggestions(suggestions, typedWordValid);
        }
    }

    private void setQuranSuggestions(List<AyaMatch> suggestions) {
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            setCandidatesViewShown(true);
        }
        mSuggestions = EMPTY_LIST;
        mQuranSuggestions = EMPTY_MLIST;
        mSavedPreSpaces = 0;
        if (mCandidateView != null) {
            if (suggestions != null) {
                mQuranSuggestions = suggestions;
            }
            mCandidateView.setQuranSuggestions(suggestions);
        }
    }

    private void handleBackspace() {
        final int length = mComposing.length();
        if (length > 1) {
            mComposing.delete(length - 1, length);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateCandidates();
        } else if (length == 1) {
            mComposing.setLength(0);
            getCurrentInputConnection().commitText("", 0);
            updateCandidates();
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
    }

    private void handleShift() {
        if (mInputView == null) {
            return;
        }
        
        Keyboard currentKeyboard = mInputView.getKeyboard();
        if (mArabicKeyboard == currentKeyboard) {
            // Alphabet keyboard
            mDoQuranSearch = !mDoQuranSearch;
            mInputView.setShifted(mDoQuranSearch);
            if (!mDoQuranSearch) {
                setSuggestions(null, false);
            }
        } else if (currentKeyboard == mSymbolsKeyboard) {
            mSymbolsKeyboard.setShifted(true);
            setArabicKeyboard(mSymbolsShiftedKeyboard);
            mSymbolsShiftedKeyboard.setShifted(true);
        } else if (currentKeyboard == mSymbolsShiftedKeyboard) {
            mSymbolsShiftedKeyboard.setShifted(false);
            setArabicKeyboard(mSymbolsKeyboard);
            mSymbolsKeyboard.setShifted(false);
        }
    }
    
    private void handleCharacter(int primaryCode) {
        // TODO support wildcard * by using RegEx
        if (mDoQuranSearch && (isLetter(primaryCode) || isSpace(primaryCode))) {
            mComposing.append((char) primaryCode);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateCandidates();
        } else {
            getCurrentInputConnection().commitText(
                    String.valueOf((char) primaryCode), 1);
        }
    }


    private void handleClose() {
        commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        mInputView.closing();
    }

    private void handleLanguageSwitch() {
        mInputMethodManager.switchToNextInputMethod(getToken(), false /* onlyCurrentIme */);
    }
    
    private String getWordSeparators() {
        return mWordSeparators;
    }
    
    private boolean isWordSeparator(int code) {
        String separators = getWordSeparators();
        return separators.contains(String.valueOf((char)code));
    }

    private void pickDefaultCandidate() {
        pickSuggestionManually(0);
    }
    
    public void pickSuggestionManually(int index) {
        if (index < 0) {
            // assert
            return;
        }

        if (mCompletionOn && mCompletions != null && index < mCompletions.length) {
            CompletionInfo ci = mCompletions[index];
            getCurrentInputConnection().commitCompletion(ci);
            if (mCandidateView != null) {
                mCandidateView.clear();
            }
            return;
        }
        
        if (mComposing.length() > 0) {
            String s;
            // Commit the candidate suggestion for the current text.
            if(index < mSuggestions.size()) {
                s = mSuggestions.get(index);
                getCurrentInputConnection().commitText(s, s.length());
            }
            else if (index < mQuranSuggestions.size()) {
                AyaMatch m = mQuranSuggestions.get(index);
                SpannableStringBuilder txt = new SpannableStringBuilder(m.strBld);
                int len = m.len;

                txt.insert(0, "﴿");
                len++;
                txt.insert(len - m.slen, "﴾");
                len++;

                // separate Quran from following text with a dot
                txt.append('.');
                len++;
                if (getPrefRasm() == Rasm.UTHMANI /*&& mUthmaniTypeFace != null*/) {
                    txt.setSpan(new CustomTypefaceSpan(getUthmaniTypeFace()), 0, len, 0);
                }
                Toast.makeText(this, txt.subSequence(0, len), Toast.LENGTH_LONG).show();

                for (int i = 0; i < mSavedPreSpaces; i++) txt.insert(0, " ");
                len += mSavedPreSpaces;
                getCurrentInputConnection().commitText(txt.subSequence(0, len), len);
                mSavedPreSpaces = 0;
            }
            mComposing.setLength(0);
            updateCandidates();
        }
    }

    public void swipeRight() {
        pickDefaultCandidate();
    }
    
    public void swipeLeft() {
        handleBackspace();
    }

    public void swipeDown() {
        handleClose();
    }

    public void swipeUp() {
    }
    
    public void onPress(int primaryCode) {
    }
    
    public void onRelease(int primaryCode) {
    }

    public boolean isPrefAyaBegin() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getBoolean("pref_aya_begin", true);
    }

    public boolean isPrefSurahAyaNbrs() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getBoolean("pref_surah_aya_nbrs", true);
    }

    public Rasm getPrefRasm() {
        Rasm rasm;
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String rasmStr = sharedPref.getString("pref_quran_rasm_type", "");

        switch (rasmStr) {
            case "Uthmani":
                rasm = Rasm.UTHMANI;
                break;
            case "ImlaMashkul":
                rasm = Rasm.IMLA_MASHKUL;
                break;
            default:
                rasm = Rasm.IMLA;
                break;
        }
        return rasm;
    }

    private int getPrefSearchLimit() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String str =sharedPref.getString("pref_search_limit",
                Integer.toString(QuranSearch.DEF_SEARCH_LIMIT));
        return Integer.parseInt(str);
    }

    public Typeface getUthmaniTypeFace() {
        return mUthmaniTypeFace;
    }

    private void setUthmaniTypeFace(Typeface mUthmaniTypeFace) {
        this.mUthmaniTypeFace = mUthmaniTypeFace;
    }

    // Code below this point, for Options dialog, is from 
    // https://github.com/AnySoftKeyboard/
    // Copyright (c) 2015 Menny Even-Danan, Apache 2.0
    private boolean closeOptionsDialog()
    {
        if (mOptionsDialog != null && mOptionsDialog.isShowing()) {
            mOptionsDialog.dismiss();
            mOptionsDialog = null;
            return true;
        } else {
            return false;
        }
    }

    @CallSuper
    private boolean handleCloseRequest() {
        // call handleClose()? POLA says NO :)
        return closeOptionsDialog();
    }
    private IBinder getToken() {
        final Dialog dialog = getWindow();
        if (dialog == null) {
            return null;
        }
        final Window window = dialog.getWindow();
        if (window == null) {
            return null;
        }
        return window.getAttributes().token;
    }
     
    private void launchSettings() {
        hideWindow();
        Intent intent = new Intent();
        intent.setClass(this, ImePrefsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void showOptionsMenu() {
        showOptionsDialogWithData(getText(R.string.ime_name), R.mipmap.ic_launcher,
                new CharSequence[]{
                        getText(R.string.settings_name),
                        getText(R.string.change_ime)},
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int position) {
                        switch (position) {
                            case 0:
                                launchSettings();
                                break;
                            case 1:
                                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showInputMethodPicker();
                                break;
                        }
                    }
                }
        );
    }

    private void showOptionsDialogWithData(CharSequence title, @DrawableRes int iconRedId,
                                             final CharSequence[] entries, final DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setIcon(iconRedId);
        builder.setTitle(title);
        builder.setNegativeButton(android.R.string.cancel, null);

        builder.setItems(entries, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface di, int position) {
                di.dismiss();
                if (di == mOptionsDialog) mOptionsDialog = null;

                if ((position >= 0) && (position < entries.length)) {
//                    Log.d(TAG, "User selected '%s' at position %d", entries[position], position);
                    listener.onClick(di, position);
                }
//                else {
//                    Log.d(TAG, "Selection dialog popup canceled");
//                }
            }
        });

        if (mOptionsDialog != null && mOptionsDialog.isShowing()) mOptionsDialog.dismiss();
        mOptionsDialog = builder.create();
        Window window = mOptionsDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        if (lp != null) {
            lp.token = mInputView.getWindowToken();
            lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
            window.setAttributes(lp);
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        mOptionsDialog.show();
    }
}
