/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.telephony;

import android.text.Editable;

/*
 * Iranian Phone number formatting rule is a bit complicated.
 * Here are some valid examples:
 *
 *    021-xxxx-xxxx
 *    0912-xxx-xxxx
 * +98-21-xxxx-xxxx
 * +98-912-xxx-xxxx
 *    0424-xxx-xxxx
 *
 * As you can see, there is no straight-forward rule here.
 * In order to handle this, a big array is prepared.
 */
/* package */ class IranianPhoneNumberFormatter {

    // TODO: Prefix

    public static void format(Editable text) {
        // TODO: First replace persian digits
        // Here, "root" means the position of "'":
        // 0'21, 0'912, and +98'-919
        // (dash will be deleted soon, so it is actually +98'90).
        int rootIndex = 1;
        int length = text.length();
        if (length > 3
                && (text.subSequence(0, 3).toString().equals("+98"))) {
            rootIndex = 3;
        } else if (length > 4
                && (text.subSequence(0, 4).toString().equals("0098"))) {
            rootIndex = 4;
        } else if (length < 1 || text.charAt(0) != '0') {
            return;
        }

        // Strip the dashes first, as we're going to add them back
        int i = 0;
        while (i < text.length()) {
            if (text.charAt(i) == '-') {
                text.delete(i, i + 1);
            } else {
                i++;
            }
        }

        int firstNum = rootIndex;

        if (rootIndex >= 3) {
            text.insert(rootIndex, "-");
            firstNum++;
        }

        if (text.length() > firstNum && (text.charAt(firstNum) == '0'))
            firstNum++;

        if (text.length() > (firstNum + 6)) {
            text.insert(firstNum + 6, "-");

            if (text.subSequence(firstNum, firstNum + 2).toString().equals("21")) {
               text.insert(firstNum + 2, "-");
            } else {
               text.insert(firstNum + 3, "-");
            }
        }
    }
}
