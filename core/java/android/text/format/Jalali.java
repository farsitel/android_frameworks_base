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

package android.text.format;

import java.util.Date;
import java.util.Calendar;
import java.util.Locale;

import android.content.res.Resources;
import android.content.Context;
import android.provider.Settings;
import android.text.FriBidi;

/**
 * Utility class for converting between Jalali and Gregorian calendars.
 */

public class Jalali {
	private static int[] gDaysInMonth = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
	private static int[] jDaysInMonth = { 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 };

	public final static char PERSIAN_ZERO = 0x06f0;
	public final static char PERSIAN_NINE = 0x06f9;
	public final static char PERSIAN_DECIMAL_POINT = 0x066b;
    public final static char DIGIT_DIFF = PERSIAN_ZERO - '0';

	public static boolean isJalali(Context context) {
    	String type = null;
	    if (context != null)
		    type = Settings.System.getString(context.getContentResolver(),
				Settings.System.CALENDAR_TYPE);
		if ((type == null) || (type.length() == 0))
		    type = Settings.System.DEFAULT_CALENDAR_TYPE;
		return type.equals(Settings.System.JALALI_CALENDAR);
	}

	public static String format(String format, Time time) {

		JalaliDate jDate = Jalali.gregorianToJalali(time.year, time.month + 1, time.monthDay);

	    final char QUOTE = '%';

		int len = format.length();
		int count = 0;
		
		for (int i = 0; i < len; i += count) {
			count = 1;
			char ch = format.charAt(i);
			
			if (ch != QUOTE) {
				continue;
			}
			
			int hookStart = i;
			int hookEnd = hookStart + 1;
			while (!"aAbBcCdDeFgGhHIjklmMnpPrRsStTuUVwWxXyYzZ%".contains(format.subSequence(hookEnd, hookEnd + 1))) {
				hookEnd++;
			}
			
			String replacement = Jalali.replaceHook(format.substring(hookStart + 1, hookEnd + 1), jDate);
			format = format.replace(format.subSequence(hookStart, hookEnd + 1), replacement);
			len = format.length();
			count = replacement.length();
		}
		
		return format;
	}
	
	private static String replaceHook(String hook, JalaliDate jDate) {
		boolean padNumbers = true;
		int padLength = 0;
		String padChar = " ";
		
		char ch;
		
		if (hook.contains("_")) {
			padChar = " ";
		} else if (hook.contains("0")) {
			padChar = "0"; // TODO
		}

		if (hook.contains("-")) {
			padNumbers = false;
		}
		
		for (int i = hook.length() - 1; i > 0; i--) {
			ch = hook.charAt(i);
			if ((ch >= '1') && (ch <= '9')) {
				padLength = ch - '0';
				break;
			}
		}
		
		ch = hook.charAt(hook.length() - 1);
		String result;
		switch (ch) {
			case 'b':
			case 'h':
				result = getLongMonthName(jDate.month);
				break;
			case 'B':
				result = getLongMonthName(jDate.month);
				break;
			case 'C':
				if (padLength == 0)
					padLength = 2;
				result = Jalali.padNumber(jDate.year / 100, padLength, padNumbers, padChar);
				break;
			case 'd':
				if (padLength == 0)
					padLength = 2;
				result = Jalali.padNumber(jDate.day, padLength, padNumbers, padChar);
				break;
			case 'e':
				if (padLength == 0)
					padLength = 2;
				result = Jalali.padNumber(jDate.day, padLength, padNumbers, "0");
				break;
			case 'm':
				if (padLength == 0)
					padLength = 2;
				result = Jalali.padNumber(jDate.month, padLength, padNumbers, "0");
				break;
			case 'y':
				if (padLength == 0)
					padLength = 2;
				result = Jalali.padNumber(jDate.year % 100, padLength, padNumbers, padChar);
				break;
			case 'Y':
				if (padLength == 0)
					padLength = 4;
				result = Jalali.padNumber(jDate.year, padLength, padNumbers, padChar);
				break;
			case '%':
				result = "%";
				break;
			default:
				result = "%" + hook;
		}
		
		return result;
	}

	private static String padNumber(int number, int length, boolean pad, String ch) {
		String str = "" + number;
		int resultLength = str.length();
		int i;

		if (pad && (resultLength < length)) {
			String padding = "";
			for (i = 0; i < (resultLength - length); i++)
				padding += ch;
			str = padding + str;
		}

		return str;
	}

    @Deprecated
	public static String persianDigits(String str) {
		String result = "";
		char ch;
		int i;

		for (i = 0; i < str.length(); i++) {
			ch = str.charAt(i);
			if ((ch >= '0') && (ch <= '9'))
				result += Character.toString((char)(PERSIAN_ZERO + (ch - '0')));
			else
				result += ch;
		}

		return result;
	}

    @Deprecated
	public static String persianDigitsWithDecimalPointReplacement(String str) {
		String result = "";
		char ch;
		int i;

		for (i = 0; i < str.length(); i++) {
			ch = str.charAt(i);
			if ((ch >= '0') && (ch <= '9'))
				result += Character.toString((char)(PERSIAN_ZERO + (ch - '0')));
			else if (ch == '.')
			    result += PERSIAN_DECIMAL_POINT;
			else
				result += ch;
		}

		return result;
	}
	
    @Deprecated
    public static String persianDigitsIfPersian(String str, boolean replaceDecimalPoint) {
        if (! FriBidi.isPersian())
            return str;
        if (replaceDecimalPoint)
            return persianDigitsWithDecimalPointReplacement(str);
        else
            return persianDigits(str);
    }

    @Deprecated
    public static String persianDigitsIfPersian(String str) {
        if (! FriBidi.isPersian())
            return str;
        return persianDigits(str);
    }

    @Deprecated
	public static String replacePersianDigits(String str) {
		String result = "";
		char ch;
		int i;
		
		for (i = 0; i < str.length(); i++) {
			ch = str.charAt(i);
			if ((ch >= PERSIAN_ZERO) && (ch <= PERSIAN_NINE))
				result += Character.toString((char)('0' + (ch - PERSIAN_ZERO)));
			else if (ch == PERSIAN_DECIMAL_POINT)
				result += '.';
			else
				result += ch;
		}
		
		return result;
	}
	
	public static JalaliDate gregorianToJalali(Time gTime) {
		return gregorianToJalali(gTime.year, gTime.month + 1, gTime.monthDay);
	}

	public static JalaliDate gregorianToJalali(Date gDate) {
		return gregorianToJalali(gDate.getYear() + 1900, gDate.getMonth() + 1, gDate.getDate());
	}

	public static JalaliDate gregorianToJalali(int gYear, int gMonth, int gDay) {
		int gy, gm, gd;
		int jy, jm, jd;
		long g_day_no, j_day_no;
		int j_np;
		int i;

		gy = gYear - 1600;
		gm = gMonth - 1;
		gd = gDay - 1;

		g_day_no = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400;
		for (i = 0; i < gm; ++i)
			g_day_no += gDaysInMonth[i];
		if (gm > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)))
			/* leap and after Feb */
			++g_day_no;
		g_day_no += gd;

		j_day_no = g_day_no - 79;

		j_np = new Long(j_day_no / 12053).intValue();
		j_day_no %= 12053;

		jy = new Long(979 + 33 * j_np + 4 * (j_day_no / 1461)).intValue();
		j_day_no %= 1461;

		if (j_day_no >= 366) {
			jy += (j_day_no - 1) / 365;
			j_day_no = (j_day_no - 1) % 365;
		}

		for (i = 0; i < 11 && j_day_no >= jDaysInMonth[i]; ++i) {
			j_day_no -= jDaysInMonth[i];
		}
		jm = i + 1;
		jd = new Long(j_day_no + 1).intValue();
		return new JalaliDate(jy, jm, jd);
	}
	
	public static Time jalaliToGregorianTime(JalaliDate jDate) {
		Calendar resultCalendar = jalaliToGregorian(jDate.year, jDate.month, jDate.day);
		Time result = new Time();
		result.set(resultCalendar.get(Calendar.DAY_OF_MONTH), resultCalendar.get(Calendar.MONTH), resultCalendar.get(Calendar.YEAR));
		return result;
	}
	
	public static Time jalaliToGregorianTime(int jYear, int jMonth, int jDay) {
		Calendar resultCalendar = jalaliToGregorian(jYear, jMonth, jDay);
		Time result = new Time();
		result.set(resultCalendar.get(Calendar.DAY_OF_MONTH), resultCalendar.get(Calendar.MONTH), resultCalendar.get(Calendar.YEAR));
		return result;
	}
	
	public static Calendar jalaliToGregorian(JalaliDate jDate) {
		return jalaliToGregorian(jDate.year, jDate.month, jDate.day);
	}
	
	public static Calendar jalaliToGregorian(int jYear, int jMonth, int jDay) {
		int gy, gm, gd;
		int jy, jm, jd;
		long g_day_no, j_day_no;
		boolean leap;

		int i;

		jy = jYear - 979;
		jm = jMonth - 1;
		jd = jDay - 1;

		j_day_no = 365 * jy + (jy / 33) * 8 + (jy % 33 + 3) / 4;
		for (i = 0; i < jm; ++i)
			j_day_no += jDaysInMonth[i];

		j_day_no += jd;

		g_day_no = j_day_no + 79;

		gy = new Long(1600 + 400 * (g_day_no / 146097)).intValue(); /*
																	 * 146097 =
																	 * 365*400 +
																	 * 400/4 -
																	 * 400/100 +
																	 * 400/400
																	 */
		g_day_no = g_day_no % 146097;

		leap = true;
		if (g_day_no >= 36525) /* 36525 = 365*100 + 100/4 */
		{
			g_day_no--;
			gy += 100 * (g_day_no / 36524); /* 36524 = 365*100 + 100/4 - 100/100 */
			g_day_no = g_day_no % 36524;

			if (g_day_no >= 365)
				g_day_no++;
			else
				leap = false;
		}

		gy += 4 * (g_day_no / 1461); /* 1461 = 365*4 + 4/4 */
		g_day_no %= 1461;

		if (g_day_no >= 366) {
			leap = false;

			g_day_no--;
			gy += g_day_no / 365;
			g_day_no = g_day_no % 365;
		}

		for (i = 0; g_day_no >= gDaysInMonth[i] + ((i == 1 && leap) ? 1 : 0); i++)
			g_day_no -= gDaysInMonth[i] + ((i == 1 && leap) ? 1 : 0);
		gm = i + 1;
		gd = new Long(g_day_no + 1).intValue();
		
		Calendar calendar = Calendar.getInstance();
		calendar.set(gy, gm - 1, gd);
		return calendar;
	}
	
	public static boolean isJalaliLeapYear(int year) {
		int mod = (year + 11) % 33;
		if ((mod != 32) && ((mod % 4) == 0)) {
			return true;
		} else {
			return false;
		}
	}
	
	public static int getMaxMonthDay(int year, int month) {
		if (month < 7) {
			return 31; // months 1..6
		} if (month < 12) {
			return 30; // months 7..11
		} if (isJalaliLeapYear(year)) {
			return 30; // month 12, but leap year
		}
		return 29; // month 12 and not a leap year
	}

	public static String getLongMonthName(int month) {
		switch(month) {
		case 1:
			return Resources.getSystem().getString(com.android.internal.R.string.month_long_farvardin);
		case 2:
			return Resources.getSystem().getString(com.android.internal.R.string.month_long_ordibehesht);
		case 3:
			return Resources.getSystem().getString(com.android.internal.R.string.month_long_khordad);
		case 4:
			return Resources.getSystem().getString(com.android.internal.R.string.month_long_tir);
		case 5:
			return Resources.getSystem().getString(com.android.internal.R.string.month_long_mordad);
		case 6:
			return Resources.getSystem().getString(com.android.internal.R.string.month_long_shahrivar);
		case 7:
			return Resources.getSystem().getString(com.android.internal.R.string.month_long_mehr);
		case 8:
			return Resources.getSystem().getString(com.android.internal.R.string.month_long_aban);
		case 9:
			return Resources.getSystem().getString(com.android.internal.R.string.month_long_azar);
		case 10:
			return Resources.getSystem().getString(com.android.internal.R.string.month_long_dey);
		case 11:
			return Resources.getSystem().getString(com.android.internal.R.string.month_long_bahman);
		case 12:
			return Resources.getSystem().getString(com.android.internal.R.string.month_long_esfand);
		}
		return "";
	}

	public static String getShortestMonthName(int month) {
		switch(month) {
		case 1:
			return Resources.getSystem().getString(com.android.internal.R.string.month_shortest_farvardin);
		case 2:
			return Resources.getSystem().getString(com.android.internal.R.string.month_shortest_ordibehesht);
		case 3:
			return Resources.getSystem().getString(com.android.internal.R.string.month_shortest_khordad);
		case 4:
			return Resources.getSystem().getString(com.android.internal.R.string.month_shortest_tir);
		case 5:
			return Resources.getSystem().getString(com.android.internal.R.string.month_shortest_mordad);
		case 6:
			return Resources.getSystem().getString(com.android.internal.R.string.month_shortest_shahrivar);
		case 7:
			return Resources.getSystem().getString(com.android.internal.R.string.month_shortest_mehr);
		case 8:
			return Resources.getSystem().getString(com.android.internal.R.string.month_shortest_aban);
		case 9:
			return Resources.getSystem().getString(com.android.internal.R.string.month_shortest_azar);
		case 10:
			return Resources.getSystem().getString(com.android.internal.R.string.month_shortest_dey);
		case 11:
			return Resources.getSystem().getString(com.android.internal.R.string.month_shortest_bahman);
		case 12:
			return Resources.getSystem().getString(com.android.internal.R.string.month_shortest_esfand);
		}
		return "";
	}
}
