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

public class IndexOfMethod implements  SearchMethod {

    public List<SearchMatch> search(String text, String pattern, int max)
    {
        //long start = System.nanoTime();
        //long stop = 0;
        
        List<SearchMatch> matches = new ArrayList<>();

        int plen = pattern.length();
        if (plen > 0) {
            if (max <= 0) {
                max = Integer.MAX_VALUE;
            }

            int found = text.indexOf(pattern);
            while (found != -1 && matches.size() < max) {
                //stop = System.nanoTime();
                SearchMatch match = new SearchMatch(text, found/*, stop - start*/);
                matches.add(match);
                //start = System.nanoTime();
                found = text.indexOf(pattern, found + plen);
            }
        }
        
        return matches;
    }
}
