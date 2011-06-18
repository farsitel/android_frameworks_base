/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (C) 2011 Iranian Supreme Council of ICT, The FarsiTel Project
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

package android.text.method;

import java.util.Locale;

import android.graphics.Rect;
import android.text.GetChars;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.view.View;


/**
 * This transformation method causes any latin digit to be replaced
 * by corresponding digits used in the current local (Persian for example)
 */
public class LocalizedDigitsTransformationMethod
extends ReplacementTransformationMethod {
    private static char[] ORIGINAL = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
    private static char[] REPLACEMENT_FA = new char[] { 0x06f0, 0x06f1, 0x06f2, 0x06f3, 0x06f4, 0x06f5, 0x06f6, 0x06f7, 0x06f8, 0x06f9 };

    /**
     * Latin digits
     */
    protected char[] getOriginal() {
        return ORIGINAL;
    }

    /**
     * Returns the suitable digit set related to the current locale
     */
    protected char[] getReplacement() {
        // FIXME: Use locale replacement
        if ("fa".equals(Locale.getDefault().getLanguage()))
            return REPLACEMENT_FA;            
        return ORIGINAL;
    }

    public static LocalizedDigitsTransformationMethod getInstance() {
        if (sInstance != null)
            return sInstance;

        sInstance = new LocalizedDigitsTransformationMethod();
        return sInstance;
    }

    private static LocalizedDigitsTransformationMethod sInstance;
}
