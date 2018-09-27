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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class QuranSearch {
    private static final boolean DEBUG = false;
    public static final int DEF_SEARCH_LIMIT = 10;
    private static final String SURAH_NAME [][] = {
            {"الفاتحة", "الفَاتِحَةِ"},
            {"البقرة", "البَقَرَةِ"},
            {"آل عمران", "آلِ عِمۡرَانَ"},
            {"النساء", "النِّسَاءِ"},
            {"المائدة", "المَائ‍ِدَةِ"},
            {"الأنعام", "الأَنعَامِ"},
            {"الأعراف", "الأَعۡرَافِ"},
            {"الأنفال", "الأَنفَالِ"},
            {"التوبة", "التَّوۡبَةِ"},
            {"يونس", "يُونُسَ"},
            {"هود", "هُودٍ"},
            {"يوسف", "يُوسُفَ"},
            {"الرعد", "الرَّعۡدِ"},
            {"إِبراهيم", "إِبۡرَاهِيمَ"},
            {"الحجر", "الحِجۡرِ"},
            {"النحل", "النَّحۡلِ"},
            {"الإِسراء", "الإِسۡرَاءِ"},
            {"الكهف", "الكَهۡفِ"},
            {"مريم", "مَرۡيَمَ"},
            {"طه", "طه"},
            {"الأنبياء", "الأَنبيَاءِ"},
            {"الحج", "الحَجِّ"},
            {"المؤمنون", "المُؤۡمِنُونَ"},
            {"النور", "النُّورِ"},
            {"الفرقان", "الفُرۡقَانِ"},
            {"الشعراء", "الشُّعَرَاءِ"},
            {"النمل", "النَّمۡلِ"},
            {"القصص", "القَصَصِ"},
            {"العنكبوت", "العَنكَبُوتِ"},
            {"الروم", "الرُّومِ"},
            {"لقمان", "لُقۡمَانَ"},
            {"السجدة", "السَّجۡدَةِ"},
            {"الأحزاب", "الأَحۡزَابِ"},
            {"سبأ", "سَبَإٍ"},
            {"فاطر", "فَاطِرٍ"},
            {"يس", "يسٓ"},
            {"الصافات", "الصَّافَّاتِ"},
            {"ص", "صٓ"},
            {"الزمر", "الزُّمَرِ"},
            {"غافر", "غَافِرٍ"},
            {"فصلت", "فُصِّلَتۡ"},
            {"الشورى", "الشُّورَىٰ"},
            {"الزخرف", "الزُّخۡرُفِ"},
            {"الدخان", "الدُّخَانِ"},
            {"الجاثية", "الجَاثِيةِ"},
            {"الأحقاف", "الأَحۡقَافِ"},
            {"محمد", "مُحَمَّدٍ"},
            {"الفتح", "الفَتۡحِ"},
            {"الحجرات", "الحُجُرَاتِ"},
            {"ق", "قٓ"},
            {"الذاريات", "الذَّارِيَاتِ"},
            {"الطور", "الطُّورِ"},
            {"النجم", "النَّجۡمِ"},
            {"القمر", "القَمَرِ"},
            {"الرحمن", "الرَّحۡمَٰن"},
            {"الواقعة", "الوَاقِعَةِ"},
            {"الحديد", "الحَدِيدِ"},
            {"المجادلة", "المُجَادلَةِ"},
            {"الحشر", "الحَشۡرِ"},
            {"الممتحنة", "المُمۡتَحنَةِ"},
            {"الصف", "الصَّفِّ"},
            {"الجمعة", "الجُمُعَةِ"},
            {"المنافقون", "المُنَافِقُونَ"},
            {"التغابن", "التَّغَابُنِ"},
            {"الطلاق", "الطَّلَاقِ"},
            {"التحريم", "التَّحۡرِيمِ"},
            {"الملك", "المُلۡكِ"},
            {"القلم", "القَلَمِ"},
            {"الحاقة", "الحَاقَّةِ"},
            {"المعارج", "المَعَارِجِ"},
            {"نوح", "نُوحٍ"},
            {"الجن", "الجِنِّ"},
            {"المزمل", "المُزَّمِّلِ"},
            {"المدثر", "المُدَّثِّرِ"},
            {"القيامة", "القِيَامَةِ"},
            {"الإِنسان", "الإِنسَانِ"},
            {"المرسلات", "المُرۡسَلَاتِ"},
            {"النبأ", "النَّبَإِ"},
            {"النازعات", "النَّازِعَاتِ"},
            {"عبس", "عَبَسَ"},
            {"التكوير", "التَّكۡوِيرِ"},
            {"الانفطار", "الانفِطَارِ"},
            {"المطففين", "المُطَفِّفِينَ"},
            {"الانشقاق", "الانشِقَاقِ"},
            {"البروج", "البُرُوجِ"},
            {"الطارق", "الطَّارِقِ"},
            {"الأعلى", "الأَعۡلَىٰ"},
            {"الغاشية", "الغَاشِيَةِ"},
            {"الفجر", "الفَجۡرِ"},
            {"البلد", "البَلَدِ"},
            {"الشمس", "الشَّمۡسِ"},
            {"الليل", "اللَّيۡلِ"},
            {"الضحى", "الضُّحَىٰ"},
            {"الشرح", "الشَّرۡحِ"},
            {"التين", "التِّينِ"},
            {"العلق", "العَلَقِ"},
            {"القدر", "القَدۡرِ"},
            {"البينة", "البَيِّنَةِ"},
            {"الزلزلة", "الزَّلۡزَلَةِ"},
            {"العاديات", "العَادِيَاتِ"},
            {"القارعة", "القَارِعَةِ"},
            {"التكاثر", "التَّكَاثُرِ"},
            {"العصر", "العَصۡرِ"},
            {"الهمزة", "الهُمَزَةِ"},
            {"الفيل", "الفِيلِ"},
            {"قريش", "قُرَيۡشٍ"},
            {"الماعون", "المَاعُونِ"},
            {"الكوثر", "الكَوثَرِ"},
            {"الكافرون", "الكَافِرُونَ"},
            {"النصر", "النَّصۡرِ"},
            {"المسد", "المَسَدِ"},
            {"الإِخلاص", "الإِخۡلَاصِ"},
            {"الفلق", "الفَلَقِ"},
            {"الناس", "النَّاسِ"},
    };
    private static final int EOF = -1;
    private static final int DEF_BUFFER_SIZE = 1024 * 4;
    private static final String AYA_SUFFIX_FMT = " \u200F[%s %d]";  //Unicode Right To Left Marker
    private static final int MIN_PATTERN_LEN = 3;
    // Threshold beyond which BoyerMoore becomes faster than IndexOf.
    // This is an empirical value from the benchmark code.
    private static final int MAX_INDEX_OF_LEN = 10;
    private static final int METHOD_INDEX_OF = 0;
    private static final int METHOD_BOYER_MOORE = 1;
    private static final int METHOD_REGEX = 2;
    private static final int METHOD_DEFAULT = METHOD_INDEX_OF;      // IndexOf is usually faster

    private String quran;
    private int currentMethod;
    private boolean surahAyaNbrs;
    private boolean ayaBegin;
    private Rasm rasm;
    private List<SearchMatch> specialCases;             // من أجل الحروف المقطعة الأقل من 2
    private QuranKeyboardIME ime;

    public QuranSearch(QuranKeyboardIME ime) throws IOException
    {
        this.ime = ime;
        readFile();
        currentMethod = METHOD_DEFAULT;

//        benchmark();
    }

    private void readFile() throws IOException
    {
        /*
         // Scanner is slow as it tests input to search for delimiter
         quran = new Scanner(is).useDelimiter("\\Z").next();
        */

       /*
        // Using zipped quran.txt to save 500kb isn't worth the latency of unzipping it here
        ZipInputStream zi = new ZipInputStream(ime.getResources().openRawResource(R.raw.quran));
        if (zi.getNextEntry() != null) {
            InputStreamReader in = new InputStreamReader(zi, StandardCharsets.UTF_8);
            while (EOF != (n = in.read(buffer))) {
                sb.append(buffer, 0, n);
                count += n;
            }
        }
         */

        /*
        Next is a from apache commons io IOUtils.toString v 2.5
        https://github.com/apache/commons-io/blob/commons-io-2.5/src/main/java/org/apache/commons/io/IOUtils.java
        */
        int n;
//        long count = 0;

//        long start = System.nanoTime();
        char [] buffer = new char[DEF_BUFFER_SIZE];
        StringBuilder sb = new StringBuilder();

        InputStream is = ime.getResources().openRawResource(R.raw.quran);
        InputStreamReader in = new InputStreamReader(is, StandardCharsets.UTF_8);
        while (EOF != (n = in.read(buffer))) {
            sb.append(buffer, 0, n);
//            count += n;
        }
//        long stop = System.nanoTime();
        quran = sb.toString();

//        System.out.println("//////////////////////////////////");
//        System.out.println("Read " + count + " characters, in " +
//                (stop - start) / 1000 + " microseconds\t" +
//                "Quran text length = " + quran.length());
    }


    public boolean oneLetterSpecialCase(String p)
    {
        // indexes below extracted with runs containing p and following lettres
        // grep -b -o '<p>' on quran.txt counts bytes and not unicode characters.
        switch (p.charAt(0)) {
            case 'ص':   // بسم الله الرحمن الرحيم ص والقرآن ذي الذكر
                specialCases = new ArrayList<>();
                specialCases.add(new SearchMatch(quran, 335061));
                break;
            case 'ق':   // بسم الله الرحمن الرحيم ق والقرآن المجيد
                specialCases = new ArrayList<>();
                specialCases.add(new SearchMatch(quran, 384642));
                break;
            case 'ن':   // بسم الله الرحمن الرحيم ن والقلم وما يسطرون
                specialCases = new ArrayList<>();
                specialCases.add(new SearchMatch(quran, 421495));
                break;
            default:
                return false;
        }

        return true;
    }

    public boolean twoLettersSpecialCase(String p)
    {
        if (p.equals("طه")) {       // طه
            specialCases = new ArrayList<>();
            specialCases.add(new SearchMatch(quran, 227524));
        }
        else if (p.equals("طس")) {  // طس تلك آيات القرآن وكتاب مبين
            specialCases = new ArrayList<>();
            specialCases.add(new SearchMatch(quran, 277440));
        }
        else if (p.equals("يس")) {  // يس
            specialCases = new ArrayList<>();
            specialCases.add(new SearchMatch(quran, 324531));
        }
        else if (p.equals("ص ")) {  // ص والقرآن ذي الذكر
            specialCases = new ArrayList<>();
            specialCases.add(new SearchMatch(quran, 335061));
        }
        else if (p.equals("حم")) {  // حم
            specialCases = new ArrayList<>();
            specialCases.add(new SearchMatch(quran, 346076));       // surah 40
            specialCases.add(new SearchMatch(quran, 353019));       // surah 41
            specialCases.add(new SearchMatch(quran, 357570));       // surah 42
            specialCases.add(new SearchMatch(quran, 362337));       // surah 43
            specialCases.add(new SearchMatch(quran, 367420));       // surah 44
            specialCases.add(new SearchMatch(quran, 369667));       // surah 45
            specialCases.add(new SearchMatch(quran, 372513));       // surah 46
        }
        else if (p.equals("ق ")) {  // ق والقرآن المجيد
            specialCases = new ArrayList<>();
            specialCases.add(new SearchMatch(quran, 384642));
        }
        else if (p.equals("ن ")) {  // ن والقلم وما يسطرون
            specialCases = new ArrayList<>();
            specialCases.add(new SearchMatch(quran, 421495));
        }
        else {
            return false;
        }

        return true;
    }

    public boolean searchable(String p)
    {
        if (null == quran) return false;

        boolean res;
        int plen = p.length();

        specialCases = null;

        if (plen > MAX_INDEX_OF_LEN) {
            res = true;
            currentMethod = METHOD_BOYER_MOORE;
            // TODO add support in IME and sanitize input
            //if (res && p.contains("*")) {
            //    currentMethod = METHOD_REGEX;
            //}
        }
        else {
            currentMethod = METHOD_DEFAULT;

            /* Below commented out code used to ignore أل التعريف, but
               then it doesn't work for surahs starting with ﻷام or ألر
               (plen > MIN_PATTERN_LEN) ||
               ((plen == MIN_PATTERN_LEN) && p.charAt(0) != 'ا' && (p.charAt(1) != 'ل'));
             */

            switch (plen) {
                case 1:
                    res = oneLetterSpecialCase(p);
                    break;

                case 2:
                    res = twoLettersSpecialCase(p);
                    break;

                default:
                    res = (plen >= MIN_PATTERN_LEN);
                    // TODO add support in IME and sanitize input
                    //if (res && p.contains("*")) {
                    //    currentMethod = METHOD_REGEX;
                    //}
                    break;
            }
        }

        return res;
    }

    // should be called after a searchable() that selects the appropriate search currentMethod
    public ArrayList<AyaMatch> search(String p, int max)
    {
        ArrayList<AyaMatch> results = null;

        if (null != quran) {
            ayaBegin = ime.isPrefAyaBegin();
            surahAyaNbrs = ime.isPrefSurahAyaNbrs();
            rasm = ime.getPrefRasm();

            if (null == specialCases) {
                SearchMethod method;
                switch (currentMethod) {
                    case METHOD_BOYER_MOORE:
                        method = new BoyerMooreMethod();
                        break;
                    case METHOD_REGEX:
                        method = new RegexMethod();
                        break;
                    case METHOD_INDEX_OF:
                    default:
                        method = new IndexOfMethod();
                        break;
                }

                results = buildResults(method.search(quran, p, max), p.length());
            }
            else {
                results = buildResults(specialCases, p.length());
            }

            if(DEBUG) for (AyaMatch am:results) am.print();

            // reset for next search
            currentMethod = METHOD_DEFAULT;
            specialCases = null;
        }

        return results;
    }

    // variant to search default max occurrences
    public ArrayList<AyaMatch> search(String p)
    {
        return this.search(p, DEF_SEARCH_LIMIT);
    }

    private ArrayList<AyaMatch> buildResults(List<SearchMatch> matches, int plen)
    {
        ArrayList<AyaMatch> results = new ArrayList<>();

        // Remember last read entry from zip file to avoid skipping it (with zi.getNextEntry()) in the
        // case of multiple matches per Aya.
        // Rewinding the stream would loose time and is not possible anyway as it is zipped and a call
        // to reset() would throw an exception
        int prev_surah = 0;
        int prev_aya = 0;
        int prev_index = 0;      // last match index
        int oc = 0;              // occurrence = index relative to aya

        for (SearchMatch m : matches) {
            if (m.surah != prev_surah || m.aya != prev_aya) {
                AyaMatch ayaMatch = new AyaMatch(quran, ayaBegin, m, plen);
                if (surahAyaNbrs) {
                    ayaMatch.appendNumber(getAyaSuffix(m.surah, m.aya));
                }
                oc = ayaMatch.getFirstOccurrence();
                results.add(ayaMatch);
                prev_surah = m.surah;
                prev_aya = m.aya;
                prev_index = m.index;
            }
            else { // multiple match per Aya
                oc += (m.index - prev_index);
                results.get(results.size()-1).addOccurrence(oc);
                prev_index = m.index;
            }
        }

        if (rasm != Rasm.IMLA) {
            readRasmFile(results);
        }

        return results;
    }

    private String getAyaSuffix(int surah, int aya)
    {
        return String.format(Locale.getDefault(), AYA_SUFFIX_FMT,
                SURAH_NAME[surah - 1][rasm == Rasm.IMLA ? 0 : 1], aya);
    }

    // use the surah & aya numbers to fetch Uthmani Rasm or Imla with or without Shakl
    private void readRasmFile(List<AyaMatch> results)
    {
        if (results.isEmpty()) return;

        try {
            int fileId = rasm == Rasm.UTHMANI ? R.raw.uthmani : R.raw.simple;
            ZipInputStream zi = new ZipInputStream(ime.getResources().openRawResource(fileId));
            InputStreamReader in = new InputStreamReader(zi, StandardCharsets.UTF_8);

            if (rasm == Rasm.IMLA_MASHKUL) {
                for (AyaMatch ayaMatch : results) {
                    StringBuilder rasmStrBld = readAyaRasm(zi, in, ayaMatch);
                    if (rasmStrBld != null) {
                        ayaMatch.useImlaShaklRasm(rasmStrBld, ayaBegin);
                        if (surahAyaNbrs) {
                            ayaMatch.appendNumber(getAyaSuffix(ayaMatch.nfo.surah, ayaMatch.nfo.aya));
                        }
                    }
                }
            }
            else {
                String uthmaniRegEx = results.get(0).buildUthmaniRegEx();
                Pattern uthmaniPattern = Pattern.compile(uthmaniRegEx);
                Pattern uthmaniWordPattern = ayaBegin? null: Pattern.compile(" [^ ]*"+uthmaniRegEx);

                for (AyaMatch ayaMatch : results) {
                    StringBuilder rasmStrBld = readAyaRasm(zi, in, ayaMatch);
                    if (rasmStrBld != null) {
                        ayaMatch.useUthmaniRasm(rasmStrBld, uthmaniPattern, uthmaniWordPattern);
                        if (surahAyaNbrs) {
                            ayaMatch.appendNumber(getAyaSuffix(ayaMatch.nfo.surah, ayaMatch.nfo.aya));
                        }
                    }
                }
            }
        } catch (IllegalStateException|SecurityException|IOException e) {
            // System.out.println("Failed to read Rasm file " + rasm);
            // Rasm is a best effort feature. Imla without shakl is kept as a fallback.
        }
    }

    private StringBuilder readAyaRasm(ZipInputStream zi, InputStreamReader in, AyaMatch am) throws IOException
    {
        ZipEntry ze;
        StringBuilder rasmStrBld = null;
        String ayaEntryName = String.format(Locale.US, "%03d_%03d.txt", am.nfo.surah, am.nfo.aya);

        while ((ze = zi.getNextEntry()) != null) {
            if (ze.getName().equals(ayaEntryName)) {
                rasmStrBld = new StringBuilder();
                char[] buffer = new char[DEF_BUFFER_SIZE];
                int n;
                while (EOF != (n = in.read(buffer))) {
                    rasmStrBld.append(buffer, 0, n);
                }
                break;
            }
        }

        return rasmStrBld;
    }

    /*// To use this, uncomment time calls in the search methods
    private void benchmark()
    {
        if (quran == null)
            return;

        String[] patterns = {
                "مالك",
                "ذلك الكتاب لا ريب فيه هدى",
                "ذي القرنين",
                "إن الذين آمنوا وعملوا الصالحات كانت",
                "مسد",
                "قل هو الله أحد"
        };

        SearchMethod[] methods = {
                new BruteForceMethod(),
                new IndexOfMethod(),
                new BoyerMooreMethod(),
                new RegexMethod(),
        };

        String[] methodNames = {
                "BruteForceMethod",
                "IndexOfMethod",
                "BoyerMooreMethod",
                "RegexMethod",
        };

        ayaBegin = ime.isPrefAyaBegin();
        surahAyaNbrs = ime.isPrefSurahAyaNbrs();
        rasm = ime.getPrefRasm();

        for (String p:patterns) {
            System.out.println("\n++++++++++++++++++++++++ " + p);
            int l = 0;
            for (SearchMethod m : methods) {
                System.out.println("------------------------ method = " + methodNames[l++]);
                for (int i = 0; i < 1; i++) {
                    ArrayList<AyaMatch> results = buildResults(m.search(quran, p, 1), p.length());
                    printMatches(p, results);
                }
            }
        }
    }

    private void printMatches(String pattern, List<AyaMatch> matches)
    {
        for (AyaMatch m:matches) {
            m.print();
        }
    }*/
}
