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

package com.android.server.status;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.format.Jalali;
import android.util.AttributeSet;
import android.util.Slog;
import android.widget.TextView;
import android.view.MotionEvent;
import android.view.RemotableViewMethod;

import com.android.internal.R;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public final class DateView extends TextView {
    private static final String TAG = "DateView";

    private boolean mUpdating = false;
    private boolean mJalali = false;
    private CharSequence mJalaliLongDate;
    
    private Context mContext;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_TIME_TICK)
                    || action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                updateClock();
            }
        }
    };

    public DateView(Context context, AttributeSet attrs) {
        super(context, attrs);
       	mContext = context;
       	mJalali = Jalali.isJalali(mContext);
       	mJalaliLongDate = mContext.getText(R.string.jalali_long_date);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        setUpdates(false);
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        // makes the large background bitmap not force us to full width
        return 0;
    }

    private final void updateClock() {
        Date now = new Date();
        if (mJalali) {
        	setText(android.text.format.DateFormat.format(mJalaliLongDate, now, true));
        } else {
       		setText(String.format("%Ls", DateFormat.getDateInstance(DateFormat.LONG).format(now)));
        }
    }

    void setUpdates(boolean update) {
        if (update != mUpdating) {
           	mJalali = Jalali.isJalali(mContext);
           	mJalaliLongDate = mContext.getText(R.string.jalali_long_date);
            mUpdating = update;
            if (update) {
                // Register for Intent broadcasts for the clock and battery
                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_TIME_TICK);
                filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
                mContext.registerReceiver(mIntentReceiver, filter, null, null);
                updateClock();
            } else {
                mContext.unregisterReceiver(mIntentReceiver);
            }
        }
    }
}

