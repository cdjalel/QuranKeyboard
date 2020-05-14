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

package com.djalel.android.qurankeyboard.qsearch;


import java.util.Locale;
import java.util.regex.Matcher;

public class SearchMatch {
    // see format of quran.txt: 1 aya per line, prefixed with 'suraNbr|ayaNbr|'
    // Example: search match of pattern "ahman"
    //          \n1|1|bism allah arahman arahim\n
    //            ^ ^ ^          | ^           ^
    //            | | |          ^ index       end
    //            | | |          word
    //            | | begin
    //            | aya
    //            surah

    public int index;
    public int begin;
    public int end;
    public int word;
    public int surah;
    public int aya;
    public long time;

    public SearchMatch(String quran, int i, long t)
    {
        int n;

        index = i;
        time = t;

        // parse input for the other info
        begin = quran.lastIndexOf(System.lineSeparator(), index) + 1; // covers -1 too

        n = quran.indexOf('|', begin + 1);
        surah = Integer.parseInt(quran.substring(begin, n++));

        begin = quran.indexOf('|', n);
        aya = Integer.parseInt(quran.substring(n, begin++));

        word = quran.lastIndexOf(' ', index) + 1;
        if (word == 0 || word < begin) {
            word = begin;
        }

        end = quran.indexOf(System.lineSeparator(), index);
        // quran.txt does end with \n before EOF
    }

    public SearchMatch(String quran, int i) { this(quran, i, 0); }

    public SearchMatch(Matcher m, long t)
    {
        time = t;

        // RegEx matching patterns are always limited to '\|([0-9]+)\|([0-9]+)\|(.*(pattern).*)\n'
        // https://www.freeformatter.com/java-regex-tester.html#ad-output

        surah = Integer.parseInt(m.group(1));
        aya = Integer.parseInt(m.group(2));

        begin = m.start(3);
        end = m.end(3) + 1;

        index = m.start(4);

        word = m.group(0).lastIndexOf(' ', index - m.start(0)) + 1 + m.start(0);
        if (word == 0 || word < begin) {
            word = begin;
        }
    }

    public SearchMatch(Matcher m) { this(m, 0); }

    public SearchMatch(SearchMatch s)
    {
        index = s.index;
        time = s.time;
        begin = s.begin;
        end = s.end;
        word = s.word;
        surah = s.surah;
        aya = s.aya;
    }

    public void print()
    {
        String res = "[" + surah + "،" + aya + "] " + "quran[b=" + begin +
                "; w=" + word +"; i="+index +"; e="+ end + "].";
        System.out.println(String.format(Locale.US,
                "in %8d micro-sec, at %s", (int) (time/1000), res));

    }
}
