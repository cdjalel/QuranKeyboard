/*
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

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AyaMatch {
    private static final String UTHMANI_CHARS = "\u0650\u06e1\u0671\u0651\u064e\u0670\u064f\u0653\u06db\u0657\u0652\u06d6\u064c\u065e\u06e2\u06d7\u06e5\u0656\u06da\u06e6\u06de\u06d8\u064d\u200d\u0654\u064b\u06e7\u06dc\u06e0\u06e4\u06e9\u0655\u065c\u06ec\u06e8\u0640";
    // above string contains unicode chars used in Uthmani text except the alphabet letters. Obtained from
    // getUthmaniChars.java and is  " ِ ۡ ٱ ّ َ ٰ ُ ٓ ۛ ٗ ْ ۖ ٌ ٞ ۢ ۗ ۥ ٖ ۚ ۦ ۞ ۘ ٍ ‍ ٔ ً ۧ ۜ ۠ ۤ ۩ ٕ ٜ ۬ ۨ ـ".  MIND THE SPACES

    private static final String DOTS_PREFIX = "... ";
    private static final int DOTS_PREFIX_LEN = DOTS_PREFIX.length();

    // make them private and add getters for the purist
    public StringBuilder strBld;   // aya imla without shakl text
    public final SearchMatch nfo;     // 1st SearchMatch
    public int len;             // aya length
    public int mlen;            // matched pattern len. same as plen for now (wld change with regex)
    public int slen;            // suffix length (surah name & aya number)

    // pattern occurrences relative to aya beginning
    public List<Integer> indexes;

    private int nbs;        // nbr of spaces preceding a matching word in one Aya.


    public AyaMatch(String quran, boolean ayaBegin, SearchMatch m, int mlen)
    {
        int oc;
        nfo = new SearchMatch(m);
        indexes = new ArrayList<>();

        // save this now, as strBld might be truncated with ayaBegin
        setNbrOfPreSpaces(quran);

        if (!ayaBegin && m.word > m.begin) {
            strBld = new StringBuilder(DOTS_PREFIX + quran.substring(m.word, m.end));
            len = DOTS_PREFIX_LEN + (m.end - m.word);
            oc = DOTS_PREFIX_LEN + (m.index - m.word);
        } else {
            strBld = new StringBuilder(quran.substring(m.begin, m.end));
            len = m.end - m.begin;
            oc = m.index - m.begin;
        }

        indexes.add(oc);
        this.mlen = mlen;
        slen = 0;
    }

    private void setNbrOfPreSpaces(String quran)
    {
        if (nfo.word > nfo.begin) {
            // count spaces ' ' before nfo.word-1 and skip them in aya
            nbs = 1;
            for (int i = nfo.begin + 1; i < nfo.word - 1; i++) {
                if (quran.charAt(i) == ' ') {
                    nbs++;
                }
            }
        }
        else {
            nbs = 0;
        }
    }

    public int getFirstOccurrence() { return indexes.get(0); }

    public void addOccurrence(int next)
    {
        indexes.add(next);
    }

    public void appendNumber(String suffix)
    {
//        System.out.println("strBld: "+strBld +"len = "+len);
//        System.out.println("suffix: "+suffix +"len = "+suffix.length());

        strBld.append(suffix);
        slen = suffix.length();
        len += slen;
    }

    // Analysis of diff between quran.txt (imla o shakl) and uthmani-letters.txt (stripped uthmani)
    // بعض الحروف يتم حذفها وقد تعوض بحركات من الرسم العثماني
    // مثلاً ا في مالك
    // مثلاً ي في يستحيي و إبراهيم
    // مثلاً ئ في أنبئوني و خطيئة
    // مثلاً أ في فادارأتم
    // مثلاً آ في فالآن
    // مثلاً الليل يُصبح اليل
    // مثلاً اللاتي يُصبح التي
    // مثلاً يُصبح اللذين مثلاً الذين
    // مثلاً واللذان يُصبح والذان
    // مثلاً والغاوون يُصبح والغاون
    // داوود  داود
    // يستوون  يستون
    // مثلاً أسألكم يُصبح أسلكم

    // الفراغ قد يحذف بعد يا وها وما
    // مثلاً ما منا يُصبح مَامِنَّا

    // بعض الحروف يصبح كل واحد منها حرفين في الإملائي:
    // آ يُصبح ءا، مثلاً آمنوا يُصبح ءامنوا

    // بعض الحروف من الإملائي تعوض بأخرى واحد لواحد:
    // مثلاً ا يُصبح و،  مثلاً الصلاة يُصبح الصلوة، مثلاً الحياة
    // مثلاً أ أو ئ يُصبح ء، مثلاً  آمنا
    // مثلاً خطأ يُصبح خطا
    // مثلاً ا يُصبح ى في فسواهن و إحداهما يُصبح إحدىهما
    // مثلاً آ في بآياتنا يُصبح بايتنا
    // مثلاً آ في الآخرة يُصبح أ
    // مثلاً ويبسط يُصبح ويبصط،
    // مثلاً بسطة يُصبح بصطة
    // مثلاً اصطفاه يُصبح اصطفىه
    // مثلاً تلقاء يُصبح تلقاي
    // مثلاً أبناء يُصبح أبنؤا
    // مثلاً جزاء يُصبح جزؤا
    // مثلاً ويلتا يُصبح يويلتى
    // مثلاً نبإ يُصبح نبإي

    // بعض الحروف موجودة فقط في العثماني:
    // مثلاً أندعو يُصبح أندعوا   6|71
    // مثلاً تبلو يُصبح تبلوا

    // بعض الفراغات بين الكلمات تحذف في العثماني
    // مثلاً يا لوط يُصبح يلوط
    // مثلاً أو لم  يُصبح أولم


    // في بعض المواضع يُعوّض حرفين متتالين بآخرين
    // مثلاً وملئه يُصبح وملإيه
    // مثلاً للرؤيا يُصبح للرءيا
    // مثلاً تفتأ يُصبح تفتؤا
    // مثلاً تيأسوا يُصبح تايسوا
    // مثلاً أإنك يُصبح أءنك

    // مثلاً نبأ يُصبح نبؤا
    // مثلاً وإيتاء يُصبح وإيتاي
    // مثلاً رأى يُصبح رءا
    // مثلاً  الأقصى يُصبح الأقصا

    // مثلاً بالآيات يُصبح بالأيت
    // مثلاً لشيء  يُصبح لشايء
    // مثلاً ورئيا يُصبح ورءيا
    // مثلاً أتوكأ  يُصبح أتوكؤا
    // مثلاً الملأ يُصبح الملؤا
    // مثلاً آناء يُصبح ءاناي

    // سآتيكم   ساتيكم


    // وأن لو استقاموا يُصبح وَأَلَّوِ ٱسۡتَقَٰمُواْ

    // لأذبحنه  لأاذبحنه
    // الملأ الملؤا

    // أولو  وأولوا

    // آتاني  ءاتىن

    // العلماء  العلمؤا
    // البلاء  البلؤا

    // برآء   برءؤا
    // أصلاتك    أصلوتك

    // وجيء  وجايء
    
    // ونأى  ونا

    public String buildUthmaniRegEx()
    {
        StringBuilder b = new StringBuilder();
        String p = strBld.substring(indexes.get(0), indexes.get(0) + mlen);

        for(int i = 0; i < p.length(); i++) {
            char c = p.charAt(i);
            switch (c) {
                case 'آ': b.append("([أا]|ءا|ءؤ)?"); break;
                case 'ا': b.append("[اؤوى]?"); break;
                case 'ي': b.append("ا?[يأء]?"); break;
                case 'ئ': b.append("[ئءإ]?ي?"); break;
                case 'أ': b.append("([أئءاي]?|ؤا)"); break;
                case 'ء': b.append("[ءيا]?"); break;
                case 'إ': b.append("[إء]ي?"); break;
                case 'ؤ': b.append("[ؤء]"); break;
                case 'ى': b.append("[ىا]"); break;
                case 'و': b.append("[وا]?ا?"); break;

                case 'س': b.append("[سص]"); break;
                case 'ش': b.append("شا?"); break;

                case 'ل':
                    if (i > 1 && i+1 < p.length() &&
                            p.charAt(i-1) == 'ل' && p.charAt(i-2) == 'ا' &&
                            (p.charAt(i+1) == 'ي' || p.charAt(i+1) ==  'ا'|| p.charAt(i+1) == 'ذ'))
                        b.append("ل?");
                    else
                        b.append(c);
                    break;

                case 'ن': if (i > 1 && p.charAt(i-1) == 'أ' && p.charAt(i-2) == 'و') b.append("ن?");
                          else b.append(c);
                    break;

                case 'ة': b.append("[ةت]"); break;

                case ' ':
                    if (i > 1 && p.charAt(i-1) == 'ا' &&
                            (p.charAt(i-2) == 'ي' || p.charAt(i-2) == 'ه' || p.charAt(i-2) == 'م')) {
                        b.append(" ?");
                    }
                    else b.append(c);
                    continue;

                default:  b.append(c); break;
            }

            b.append('['); b.append(UTHMANI_CHARS);
            b.append("]{0,5}");          // some letters are followed by up to 5 uthmani chars
        }

        //System.out.println("Uthamni pattern " + p + " -> " + b.toString());

        return b.toString();
    }

    public void useUthmaniRasm(StringBuilder rasmStrBld, Pattern p, Pattern wp)
    {
        int rasmLen;
        int rasmMLen;
        ArrayList<Integer> rasmIndexes = new ArrayList<>();
        ArrayList<Integer> rasmMLenSizes = new ArrayList<>();

        if (wp != null && nfo.word > nfo.begin) {
            truncateToWord(rasmStrBld, wp);
        }
        rasmLen = rasmStrBld.length();

        Matcher m = p.matcher(rasmStrBld);
        while(m.find() && rasmIndexes.size() < indexes.size()) {
            rasmIndexes.add(m.start());
            rasmMLenSizes.add(m.end() - m.start());
        }

        // safety net: take the minimum matching word length to avoid overflow
        rasmMLen = Integer.MAX_VALUE;
        for (Integer w : rasmMLenSizes) {
            if (w < rasmMLen) rasmMLen = w;
        }

        // another safety net
        if (rasmIndexes.size() > 0 && rasmIndexes.get(rasmIndexes.size()-1) + rasmMLen > rasmLen) {
            //System.out.println("[rasmMLen + rasmIndexes.get(-1)]=["+
            //        (rasmIndexes.get(rasmIndexes.size()-1) + rasmMLen)+"] > rasmLen[" + rasmLen+ "]");
            rasmIndexes = new ArrayList<>();
            rasmMLen = mlen;
        }

//        System.out.println("rasmIndexes: "+ rasmIndexes);
//        System.out.println("rasmMLenSizes: "+ rasmMLenSizes);
//        System.out.println("rasmMLen: "+ rasmMLen);

        // Replace existing fields. ? Might wanna save them
        strBld = rasmStrBld;
        len = rasmLen;
        mlen = rasmMLen;
        indexes = rasmIndexes;
    }

    private void truncateToWord(StringBuilder rasmStrBld, Pattern p)
    {
        Matcher m = p.matcher(rasmStrBld);
        if (m.find()) {
            rasmStrBld.delete(0, m.start()+1);
            rasmStrBld.insert(0, DOTS_PREFIX);
            nbs = 1;
        }
    }

    // Here be dragons
    public void useImlaShaklRasm(StringBuilder rasmStrBld, boolean ayaBegin)
    {
        int rasmLen;
        int rasmPLen;
        int i;
        int j;

        ArrayList<Integer> rasmIndexes = new ArrayList<>();

        if (!ayaBegin && nfo.word > nfo.begin) {
            truncateToWord(rasmStrBld);
        }

        rasmLen = rasmStrBld.length();

        // build rasm indexes
        for (i = j = 0; i < len; i++, j++) {
            // skip Shakl
            for (; j < rasmLen && strBld.charAt(i) != rasmStrBld.charAt(j); j++);

            // rasmStrBld occurrence
            if (indexes.contains(i)) { rasmIndexes.add(j); }
        }

        // calculate rasm pattern length
        int oc = indexes.get(0);
        int rasmOc = rasmIndexes.get(0);
        int max = oc + mlen;
        if (max == len) {
            rasmPLen = rasmLen - rasmOc;
        }
        else {
            max++;         // include pattern last char Shakl
            for (i = oc, j = rasmOc; i < max; i++, j++) {
                // skip Shakl
                for (; j < rasmLen && strBld.charAt(i) != rasmStrBld.charAt(j); j++) ;
            }
            rasmPLen =  j - rasmOc - 1;
        }

        // Replace existing fields. ? Might wanna save them ?
        strBld = rasmStrBld;
        len = rasmLen;
        mlen = rasmPLen;
        indexes = rasmIndexes;
    }

    private void truncateToWord(StringBuilder rasmStrBld)
    {
        // skip nbs spaces ' ' to find the matching word
        int n = 0;
        int i;
        for (i = 1; i < rasmStrBld.length(); i++) {
            if (rasmStrBld.charAt(i) == ' ') {
                n++;
                if (n == nbs) {
                    break;
                }
            }
        }

        if (i < rasmStrBld.length()) {
            rasmStrBld.delete(0, i+1);
            rasmStrBld.insert(0, DOTS_PREFIX);
            nbs = 1;
        }
//        else {
//            // impossible case, the 2 imla files contain exactly 6234 spaces each
//            Log.e(QuranKeyboardIME.TAG, "Mismatch in spaces of Aya [" + m.surah + "," +
//                    m.aya+"] between quran.txt & rasm file!");
//        }
    }

/*    public void print()
    {
        System.out.println(String.format(Locale.US,
                "AyaMatch: [%d, %d], len=%d, mlen=%d, indexes=%s",
                nfo.surah, nfo.aya, len, mlen, indexes));

        nfo.print();
    }*/

}
