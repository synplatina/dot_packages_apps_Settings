/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.wifi.savedaccesspoints;

import android.annotation.Nullable;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.wifi.WifiConfigUiBase;
import com.android.settings.wifi.WifiDialog;
import com.android.settings.wifi.WifiSettings;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.AccessPointPreference;

/**
 * UI to manage saved networks/access points.
 */
public class SavedAccessPointsWifiSettings extends DashboardFragment
        implements WifiDialog.WifiDialogListener, DialogInterface.OnCancelListener {

    private static final String TAG = "SavedAccessPoints";

    private WifiManager mWifiManager;
    private Bundle mAccessPointSavedState;
    private AccessPoint mSelectedAccessPoint;

    // Instance state key
    private static final String SAVE_DIALOG_ACCESS_POINT_STATE = "wifi_ap_state";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.WIFI_SAVED_ACCESS_POINTS;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.wifi_display_saved_access_points;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mWifiManager = (WifiManager) getContext()
                .getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        use(SavedAccessPointsPreferenceController.class)
                .setHost(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SAVE_DIALOG_ACCESS_POINT_STATE)) {
                mAccessPointSavedState =
                        savedInstanceState.getBundle(SAVE_DIALOG_ACCESS_POINT_STATE);
            }
        }
    }

    public void showWifiDialog(@Nullable AccessPointPreference accessPoint) {
        removeDialog(WifiSettings.WIFI_DIALOG_ID);

        if (accessPoint != null) {
            // Save the access point and edit mode
            mSelectedAccessPoint = accessPoint.getAccessPoint();
        } else {
            // No access point is selected. Clear saved state.
            mSelectedAccessPoint = null;
            mAccessPointSavedState = null;
        }

        showDialog(WifiSettings.WIFI_DIALOG_ID);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case WifiSettings.WIFI_DIALOG_ID:
                // Modify network
                if (mSelectedAccessPoint == null) {
                    // Restore AP from save state
                    mSelectedAccessPoint = new AccessPoint(getActivity(), mAccessPointSavedState);
                    // Reset the saved access point data
                    mAccessPointSavedState = null;
                }
                final WifiDialog dialog = WifiDialog.createModal(
                        getActivity(), this, mSelectedAccessPoint, WifiConfigUiBase.MODE_VIEW);
                dialog.setOnCancelListener(this);

                return dialog;
        }
        return super.onCreateDialog(dialogId);
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case WifiSettings.WIFI_DIALOG_ID:
                return SettingsEnums.DIALOG_WIFI_SAVED_AP_EDIT;
            default:
                return 0;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // If the dialog is showing (indicated by the existence of mSelectedAccessPoint), then we
        // save its state.
        if (mSelectedAccessPoint != null) {
            mAccessPointSavedState = new Bundle();
            mSelectedAccessPoint.saveWifiState(mAccessPointSavedState);
            outState.putBundle(SAVE_DIALOG_ACCESS_POINT_STATE, mAccessPointSavedState);
        }
    }

    @Override
    public void onForget(WifiDialog dialog) {
        if (mSelectedAccessPoint != null) {
            if (mSelectedAccessPoint.isPasspointConfig()) {
                try {
                    mWifiManager.removePasspointConfiguration(
                            mSelectedAccessPoint.getPasspointFqdn());
                } catch (RuntimeException e) {
                    Log.e(TAG, "Failed to remove Passpoint configuration for "
                            + mSelectedAccessPoint.getConfigName());
                }
                use(SavedAccessPointsPreferenceController.class)
                        .postRefreshSavedAccessPoints();
            } else {
                // mForgetListener will call initPreferences upon completion
                mWifiManager.forget(mSelectedAccessPoint.getConfig().networkId,
                        use(SavedAccessPointsPreferenceController.class));
            }
            mSelectedAccessPoint = null;
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        mSelectedAccessPoint = null;
    }
}