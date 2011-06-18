/*
 * Copyright (C) 2011 Iranian Supreme Council of ICT, The FarsiTel Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASICS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.text;

import com.android.internal.util.ArrayUtils;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Vector;
import java.util.HashMap;

public class FriBidiSpanWrapper implements Spanned
{
    private Spanned mSpanned;
    private FriBidi mFriBidi;
    private Vector mSpans;
    private Vector<SpanPointer> pointers;
    private HashMap<Object, SpanPointer> pointersMap;
    private int[] the_reverse; // Yeah, I know, it's not a good name but believe me, it's less confusing

    public FriBidiSpanWrapper(Spanned sp, FriBidi fribidi) {
        mSpanned = sp;
        mFriBidi = fribidi;
        Object[] originalSpans = sp.getSpans(0, sp.length(), Object.class);
        int m = originalSpans.length;
        mSpans = new Vector(m);
        pointers = new Vector<SpanPointer>(m);
        int n = length();
        the_reverse = new int[n];
        for (int i=0; i<n; i++)
            the_reverse[mFriBidi.logical_to_visual[i]] = i;
        boolean[] mask = new boolean[n];
        for (int i=0; i<m; i++) {
            Object s = originalSpans[i];
            mSpans.add(s);

            int start = mSpanned.getSpanStart(s);
            int end = mSpanned.getSpanEnd(s);
            int flags = mSpanned.getSpanFlags(s);

            if ((start < 0) || (end < 1)) {
                pointers.add(new SpanPointer(s, start, end, flags));
                continue;
            }

            if (start == end) {
                int k = start;
                if (k < n)
                    k = the_reverse[start];
                pointers.add(new SpanPointer(s, k, k, flags));
                continue;
            }

            int min = Integer.MAX_VALUE;
            int max = -1;
            int k = start;
            while (k < end) {
                mask[the_reverse[k]] = true;
                if (the_reverse[k] < min)
                    min = the_reverse[k];
                if (the_reverse[k] > max)
                    max = the_reverse[k];
                k += 1;
            }
            int parts = 0;
            while (min <= max) {
                while ((min <= max) && !mask[min])
                    min += 1;
                if (min > max)
                    break;
                int part_start = min;
                while ((min <= max) && mask[min]) {
                    mask[min] = false;
                    min += 1;
                }
                SpanPointer ss = new SpanPointer(s, part_start, min, flags);
                pointers.add(ss);
                if (parts > 0)
                    mSpans.add(s);
                parts += 1;
            }
        }

        pointersMap = new HashMap<Object, SpanPointer>();
        m = mSpans.size();
        for (int i=0; i<m; i++)
            pointersMap.put(mSpans.get(i), pointers.get(i));

        Collections.sort(pointers);
    }

    public final int length() {
        return mFriBidi.str.length();
    }

    public final char charAt(int i) {
        return mFriBidi.before_reorder.charAt(mFriBidi.logical_to_visual[i]);
    }

    public final String toString() {
        return mFriBidi.str.toString();
    }

    public CharSequence subSequence(int start, int end) {
        return mFriBidi.before_reorder.subSequence(start, end);
    }

    public <T> T[] getSpans(int queryStart, int queryEnd, Class<T> kind) {
        Vector<T> results = new Vector<T>();
        int i = 0;

        int n = pointers.size();
        while(i < n && pointers.get(i).endInSequencce < queryStart)
            i++;

        for (; i < n; i++) {
            SpanPointer p = pointers.get(i);
            int spanStart = p.startInSequencce;
            int spanEnd = p.endInSequencce;

            if (spanStart > queryEnd) {
                continue;
            }
            if (spanEnd < queryStart) {
                continue;
            }

            if (spanStart != spanEnd && queryStart != queryEnd) {
                if (spanStart == queryEnd) {
                    continue;
                }
                if (spanEnd == queryStart) {
                    continue;
                }
            }

            if (kind != null && !kind.isInstance(p.tag))
                continue;

            results.add((T) (p.tag));
        }

        T[] t = results.toArray((T[]) ArrayUtils.emptyArray(kind));
        return t;
    }

    public int getSpanFlags(Object tag) {
        return pointersMap.get(tag).flags;
    }

    public int getSpanStart(Object tag) {
        return pointersMap.get(tag).startInSequencce;
    }

    public int getSpanEnd(Object tag) {
        return pointersMap.get(tag).endInSequencce;
    }

    public int nextSpanTransition(int start, int limit, Class kind) {
        int i = 0;

        if (kind == null) {
            kind = Object.class;
        }

        int n = pointers.size();
        while (i < n && pointers.get(i).endInSequencce < start)
            i++;

        for (; i < n; i++) {
            SpanPointer p = pointers.get(i);
            int st = p.startInSequencce;
            int en = p.endInSequencce;

            if (st > start && st < limit && kind.isInstance(p.tag))
                limit = st;
            if (en > start && en < limit && kind.isInstance(p.tag))
                limit = en;
        }

        return limit;
    }
}

class SpanPointer implements Comparable<SpanPointer> {
    public Object tag;
    public int startInSequencce;
    public int endInSequencce;
    public int flags;

    public SpanPointer(Object tag, int start, int end, int flags) {
        this.tag = tag;
        this.flags = flags;
        startInSequencce = start;
        endInSequencce = end;
    }

    public int compareTo(SpanPointer other) {
        if (this.startInSequencce == other.startInSequencce)
            return this.endInSequencce - other.endInSequencce;
        return this.startInSequencce - other.startInSequencce;
    } 
}

