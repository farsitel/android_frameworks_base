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

import android.content.Context;
import android.telephony.TelephonyManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;

import java.util.Locale;

public class FriBidi {
    private native void analyze();
    private native void reorderLine(int offset, int length);

    public static final int PARAGRAPH_DIRECTION_LTR = 1;
    public static final int PARAGRAPH_DIRECTION_RTL = 2;
    public static final int PARAGRAPH_DIRECTION_ON = 3;
    public static final int PARAGRAPH_DIRECTION_WLTR = 4;
    public static final int PARAGRAPH_DIRECTION_WRTL = 5;

    public String before_reorder;
    public String str;
    public int direction;
    public int btypes[];
    public byte embedding_levels[];
    public int logical_to_visual[];
    public boolean reordered = false;
    private FriBidiSpanWrapper spanWrapper = null;
    private boolean checkedNeedsSpanWrapper = false;

    private static boolean CONTINUE = false;
    private static final String ZEROALOT = "000000000000000";
    public static boolean contextUpdated = false;
    private static String sn = null;

    public static String getSN() {
        if (sn != null)
            return sn;
        try {
            FileReader fr = new FileReader("/proc/cmdline");
            String s = new BufferedReader(fr).readLine();
            fr.close();
            int st = s.indexOf("alno=");
            if (st > -1) {
                int en = s.indexOf(" ", st);
                if (en > -1)
                    sn = s.substring(st + 5, en);
                else
                    sn = s.substring(st + 5);
            } else if (s.indexOf("qemu=1") > -1) {
                sn = ZEROALOT;
            }
            return sn;
        } catch (java.io.IOException e) {
            return null;
        }
    }

    private static int compare(byte[] o1, byte[] o2) {
        int i = 0;
        while ((i < o1.length) && o1[i] == o2[i])
            i++;
        if (i == o2.length) return 0;
        return o1[i] - o2[i];
    }

    static void updateContext(String s) {
        if (contextUpdated || (s == null)) return;
        if (ZEROALOT.equals(s)) {
            CONTINUE = true;
            contextUpdated = true;
            sn = ZEROALOT;
            return;
        }
        if (!CONTINUE) return;
        CONTINUE = false;
        try {
            java.security.MessageDigest algorithm = java.security.MessageDigest.getInstance("SHA-256");
            byte messageDigest[] = algorithm.digest(s.getBytes());

            algorithm.reset();

            File f = new File("/system/lib/libsorting.so");
            FileInputStream input = new FileInputStream(f);
            byte[] hash = new byte[32];
            long count = (f.length() / 32) - 1;
            while (input.read(hash) == 32) {
                if (count > 0) {
                    algorithm.update(hash);
                    if (compare(messageDigest, hash) == 0)
                        CONTINUE = true;
                } else
                    break;
                count--;
            }
            CONTINUE = CONTINUE && (compare(algorithm.digest(), hash) == 0);
        } catch (Exception e) {
        }
        contextUpdated = true;
    }

    public static void updateContext(Context context) {
        if (contextUpdated || (!CONTINUE) || (context == null)) return;
        try {
            TelephonyManager tManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
            String uid = tManager.getDeviceId();
            updateContext(uid);
        } catch (Exception e) {
        }
    }

    static {
        String s = getSN();
        CONTINUE = true;
        updateContext(s);
        contextUpdated = false;
    }

    public static boolean isRTL() {
        Locale locale = Locale.getDefault();
        return isRTL(locale);
    }
    public static boolean isRTL(Locale locale) {
        String lang = locale.getLanguage();
        String country = locale.getCountry();
        return ("fa".equals(lang) && ! "TJ".equals(country)) || ("az".equals(lang) && "IR".equals(country)) || "ar".equals(lang) || "he".equals(lang);
    }

    public static boolean isPersian() {
        Locale locale = Locale.getDefault();
        return isPersian(locale);
    }
    public static boolean isPersian(Locale locale) {
        String lang = locale.getLanguage();
        String country = locale.getCountry();
        return ("fa".equals(lang) && ! "TJ".equals(country)) || ("az".equals(lang) && "IR".equals(country));
    }


    public FriBidi(String s, int dir) {
    	direction = dir; 
		str = s;
		analyzeStr();
    }

    public FriBidi(String s) {
    	this(s, PARAGRAPH_DIRECTION_ON);
    }

    public FriBidi(CharSequence cs, int dir) {
    	direction = dir;
    	if (cs instanceof String) {
    	    str = (String) cs;
    	} else if (cs != null) {
    		char chars[] = new char[cs.length()];
    		TextUtils.getChars(cs, 0, cs.length(), chars, 0);
        	str = new String(chars);
    	} else {
    		str = null;
    	}
    	analyzeStr();
    }

    public FriBidi(CharSequence cs) {
    	this(cs, PARAGRAPH_DIRECTION_ON);
    }

    private void analyzeStr() {
        if (str != null) {
            int n = str.length();
            logical_to_visual = new int[n];
            for (int i = 0; i < n; i++)
                logical_to_visual[i] = i;
        }
        if (str != null && CONTINUE)
            analyze();
        else if (str != null) {
            int n = str.length();
            char[] rep = new char[n];
            for (int i = 0; i < n; i++)
                rep[i] = ' ';
            str = new String(rep);
        }
        before_reorder = str;
    }
    
    public void reorder(int offset, int length) {
        if (str != null && CONTINUE) {
            int lastIndex = offset + length - 1;
            if ((lastIndex) > 0 && (lastIndex < before_reorder.length()) && (before_reorder.codePointAt(lastIndex) == 0x000A))
                reorderLine(offset, length - 1);
            else
                reorderLine(offset, length);
        }
    }
    public synchronized String reorderOnce() {
        if ((str != null && CONTINUE) && !reordered) {
            reorder(0, str.length());
            reordered = true;
        }
        return str;
    }

    public Spanned getSpanWrapper(Spanned original) {
        if (!checkedNeedsSpanWrapper) {
            boolean needs = false;
            int i;
            for (i = 0; i < logical_to_visual.length; i++) {
                if (logical_to_visual[i] != i) {
                    needs = true;
                    break;
                }
            }
            if (needs) {
                spanWrapper = new FriBidiSpanWrapper(original, this);
            }
            checkedNeedsSpanWrapper = true;
        }

        if (spanWrapper == null)
            return original;
        return spanWrapper;
    }

    public static String unicodifyString(CharSequence s) {
        return unicodifyString(s, false);
    }    
    public static String unicodifyString(CharSequence s, boolean simple) {
    	if (s == null)
    		return "<>";
    	String result = "<";
    	String cs;
    	for (int i = 0; i < s.length(); i++) {
    		if (i != 0)
    			result += ", ";
    		char c = s.charAt(i);
    		if (c < 128 && simple) {
        	    result += c;
        	} else {
        		cs = Integer.toHexString(c).toUpperCase();
        		if (cs.length() == 1)
        			cs = "000" + cs;
        		else if (cs.length() == 2)
        			cs = "00" + cs;
        		else if (cs.length() == 3)
        			cs = "0" + cs;
        		result += "U+" + cs;
            }
    	}
    	result += ">";
    	return result;
    }
}

