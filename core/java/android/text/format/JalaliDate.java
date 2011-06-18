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

public class JalaliDate {
	public int year;
	public int month;
	public int day;
	
	public JalaliDate(int year, int month, int day) {
		this.year = year;
		this.month = month;
		this.day = day;
	}
	
	public JalaliDate(JalaliDate that) {
		this(that.year, that.month, that.day);
	}

	public void set(int year, int month, int day) {
		this.year = year;
		this.month = month;
		this.day = day;
	}
	
	public void set(JalaliDate that) {
		set(that.year, that.month, that.day);
	}

	public void increaseMonth(int num) {
		if (num < 0) {
			decreaseMonth(-num);
		}
		if (num > 12) {
			year += num / 12;
			num %= 12;
		}
		month += num;
		if (month > 12) {
			year++;
			month -= 12;
		}

		checkMonthDay();
	}
	
	public void decreaseMonth(int num) {
		if (num < 0) {
			increaseMonth(-num);
		}
		if (num > 12) {
			year -= num / 12;
			num %= 12;
		}
		month -= num;
		if (month < 1) {
			year--;
			month += 12;
		}
		
		checkMonthDay();
	}
	
	public void checkMonthDay() {
		if (day < 29)
			return;
		int max = Jalali.getMaxMonthDay(year, month);
		if (day > max) {
			day = max;
		}
	}
}