package com.djalel.android.qurankeyboard.qsearch;

import java.util.List;
import java.util.ArrayList;

public class BruteForceMethod implements  SearchMethod {

    public List<SearchMatch> search(String text, String pattern, int max)
    {
        long start = System.nanoTime();

        List<SearchMatch> matches = new ArrayList<>();

        int m = text.length();
        int n = pattern.length();
        if (n > 0 && m > 0) {
            if (max < 0) {
                max = Integer.MAX_VALUE;
            }

            for(int i = 0; (i < m - n + 1) && (matches.size() < max); ++i) {
                boolean found = true;
                for(int j = 0; j < n; ++j) {
                    if (text.charAt(i+j) != pattern.charAt(j)) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    long stop = System.nanoTime();
                    SearchMatch match = new SearchMatch(text, i, stop - start);
                    matches.add(match);
                    start = System.nanoTime();
                }
            }
        }

        return matches;
    }
}
