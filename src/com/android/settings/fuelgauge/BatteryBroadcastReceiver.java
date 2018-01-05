/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.support.annotation.VisibleForTesting;

import com.android.settings.Utils;

/**
 * Use this broadcastReceiver to listen to the battery change, and it will invoke
 * {@link OnBatteryChangedListener} if any of the followings has been changed:
 *
 * 1. Battery level(e.g. 100%->99%)
 * 2. Battery status(e.g. plugged->unplugged)
 * 3. Battery saver(e.g. off->on)
 */
public class BatteryBroadcastReceiver extends BroadcastReceiver {

    interface OnBatteryChangedListener {
        void onBatteryChanged();
    }

    @VisibleForTesting
    String mBatteryLevel;
    @VisibleForTesting
    String mBatteryStatus;
    private OnBatteryChangedListener mBatteryListener;
    private Context mContext;

    public BatteryBroadcastReceiver(Context context) {
        mContext = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        updateBatteryStatus(intent, false /* forceUpdate */);
    }

    public void setBatteryChangedListener(OnBatteryChangedListener lsn) {
        mBatteryListener = lsn;
    }

    public void register() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);

        final Intent intent = mContext.registerReceiver(this, intentFilter);
        updateBatteryStatus(intent, true /* forceUpdate */);
    }

    public void unRegister() {
        mContext.unregisterReceiver(this);
    }

    private void updateBatteryStatus(Intent intent, boolean forceUpdate) {
        if (intent != null && mBatteryListener != null) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                final String batteryLevel = Utils.getBatteryPercentage(intent);
                final String batteryStatus = Utils.getBatteryStatus(
                        mContext.getResources(), intent);
                if (forceUpdate || !batteryLevel.equals(mBatteryLevel) || !batteryStatus.equals(
                        mBatteryStatus)) {
                    mBatteryLevel = batteryLevel;
                    mBatteryStatus = batteryStatus;
                    mBatteryListener.onBatteryChanged();
                }
            } else if (PowerManager.ACTION_POWER_SAVE_MODE_CHANGED.equals(intent.getAction())) {
                mBatteryListener.onBatteryChanged();
            }
        }
    }

}