/*
 * Copyright (C) 2004-2017 Wikipedia
 * Copyright (C) 2017 Djalel Chefrour
 *
 * Licensed under Creative Commons Attribution-ShareAlike 3.0 Unported
 * https://creativecommons.org/licenses/by-sa/3.0/ 
 *
 * Adapted from code snippets in:
 * https://en.wikipedia.org/wiki/Boyer%E2%80%93Moore_string_search_algorithm
 */

package com.djalel.android.qurankeyboard.qsearch;

import java.util.List;
import java.util.ArrayList;

public class BoyerMooreMethod implements SearchMethod {
    
    private int plen;

    public List<SearchMatch> search(String text, String pattern, int max)
    {
        //long start = System.nanoTime();
        //long stop = 0;

        List<SearchMatch> matches = new ArrayList<>();
		if (max <= 0) {
			max = Integer.MAX_VALUE;
		}

        plen = pattern.length();
        if (plen == 0) {
            return matches;
        }

        int badCharacter[] = makeBadCharacterShifts(pattern);
        int goodSuffix[] = makeGoodSuffixShifts(pattern);
        
        int lastIndex = plen - 1;
        int i = lastIndex;
        int j;
    outerloop:
        while ((i < text.length()) && (matches.size() < max)) {
            for (j = lastIndex; pattern.charAt(j) == text.charAt(i); i--, j--) {
                if (j == 0)  {
                    //stop = System.nanoTime();
                    SearchMatch match = new SearchMatch(text, i/*, stop - start*/);
                    matches.add(match);
                    //start = System.nanoTime();
                    i += plen * 2;
                    continue outerloop;
                }
            }

            i += Math.max(goodSuffix[lastIndex - j], badCharacter[text.charAt(i)]);
        }

        return matches;
    }

    private int[] makeBadCharacterShifts(String pattern) 
    {
        // 'ي' = 0x064a = 1610 is max of our alphabet 'ي'. checked
        // by parsing quran.txt with BoyerMooreQuranAlphabet.java
        // Arabic Keyboard shouldn't yield a bigger character. TBC.
        final int ALPHABET_SIZE = 'ي' + 1;

        int[] badCS = new int[ALPHABET_SIZE];
        for (int i = 0; i < ALPHABET_SIZE; i++) {
            badCS[i] = plen;
        }
        for (int i = 0; i < plen - 1; i++) {
            badCS[pattern.charAt(i)] = plen - 1 - i;
        }
        return badCS;
    }

    private int[] makeGoodSuffixShifts(String pattern) 
    {
        //assert (mlen > 0);
        int[] goodSuffix = new int[plen];
        int lastPrefixPosition = plen;
        int lastIndex = plen - 1;
        
        for (int i = lastIndex; i >= 0; i--) {
            if (isPrefix(pattern, i + 1)) {
                lastPrefixPosition = i + 1;
            }
            goodSuffix[lastIndex - i] = lastPrefixPosition - i + lastIndex;
            // if suffix is no prefix, goodSuffix (for now) shifts by 
            // pattern length + non matching suffix length. Otherwise 
            // it doesn't include the latter.
            //System.out.println("goodSuffix[" + (lastIndex - i) + "] = " + goodSuffix[lastIndex - i]);
        }

        //System.out.println("---");
        for (int i = 0; i < lastIndex; i++) {
            int slen = suffixLength(pattern, i);
            goodSuffix[slen] = lastIndex - i + slen;
            //System.out.println("goodSuffix[" + slen + "] = " + goodSuffix[slen]);
        }

        return goodSuffix;
    }

    /** Is pattern[p:end] a prefix of pattern? **/
    private boolean isPrefix(String pattern, int p) 
    {
        for (int i = p, j = 0; i < plen; i++, j++) {
            if (pattern.charAt(i) != pattern.charAt(j)) {
                return false;
            }
        }

        return true;
    }

    /** Returns the maximum length of the substring that ends at p and is a suffix **/
    private int suffixLength(String pattern, int p) 
    {
        int slen = 0;

        for (int i = p, j = plen - 1;
             i >= 0 && pattern.charAt(i) == pattern.charAt(j);
             i--, j--) { 
            slen += 1;
         }

        return slen;
    }
}
