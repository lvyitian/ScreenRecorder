/*
 * Copyright (c) 2016-2018. Vijai Chandra Prasad R.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package com.orpheusdroid.screenrecorder;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import com.orpheusdroid.crashreporter.CrashReporter;
import com.orpheusdroid.screenrecorder.utils.LocaleHelper;

import java.util.ArrayList;

import ly.count.android.sdk.Countly;
import ly.count.android.sdk.CountlyConfig;
import ly.count.android.sdk.DeviceId;

/**
 * Todo: Add class description here
 *
 * @author Vijai Chandra Prasad .R
 */
public class ScreenCamBaseApp extends Application {
    public static void setDarkLightTheme(int selectedDarkLightTheme) {
        AppCompatDelegate.setDefaultNightMode(selectedDarkLightTheme);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        CrashReporter.initialize(this);
        Config.getInstance(this).buildThemeConfig();

        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(Integer.parseInt(Config.getInstance(this).getThemePreference()));
        checkMagiskMode();
        checkRootMode();

    }

    private void checkRootMode() {
        Config.getInstance(this).setRootMode(false);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base, "en"));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public boolean checkMagiskMode() {
        int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
        boolean isMagiskMode = (getApplicationInfo().flags & mask) != 0;
        Config.getInstance(this).setMagiskMode(isMagiskMode);
        return isMagiskMode;
    }

    public void setupAnalytics() {
        Config.getInstance(this).buildConfig();
        if (Config.getInstance(this).shouldSetupAnalytics()) {
            ArrayList<String> features = new ArrayList<String>();
            CountlyConfig countlyConfig = new CountlyConfig(this, "07273a5c91f8a932685be1e3ad0d160d3de6d4ba", "https://analytics.orpheusdroid.com")
                    .setIdMode(DeviceId.Type.OPEN_UDID)
                    .setHttpPostForced(true)
                    .setRecordAllThreadsWithCrash()
                    .setLoggingEnabled(BuildConfig.DEBUG)
                    .setViewTracking(true)
                    .enableCrashReporting()
                    .setParameterTamperingProtectionSalt(getPackageName())
                    .setRequiresConsent(true);

            if (Config.getInstance(this).isCrashReportsEnabled()) {
                features.add(Countly.CountlyFeatureNames.crashes);
            }
            if (Config.getInstance(this).isUsageStatsEnabled()) {
                features.add(Countly.CountlyFeatureNames.sessions);
                features.add(Countly.CountlyFeatureNames.views);
                features.add(Countly.CountlyFeatureNames.events);
                features.add(Countly.CountlyFeatureNames.users);
            }

            countlyConfig.setConsentEnabled(features.toArray(new String[0]));
            Countly.sharedInstance().init(countlyConfig);

            Log.d(Const.TAG, "Crashlytics enabled");
        } else {
            Log.d(Const.TAG, "Crashlytics disabled");
        }
    }
}
