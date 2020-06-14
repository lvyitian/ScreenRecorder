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
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import com.orpheusdroid.crashreporter.CrashReporter;
import com.orpheusdroid.screenrecorder.utils.LocaleHelper;

import ly.count.android.sdk.Countly;
import ly.count.android.sdk.DeviceId;

import static com.orpheusdroid.screenrecorder.Const.ANALYTICS_API_KEY;
import static com.orpheusdroid.screenrecorder.Const.ANALYTICS_URL;

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
        Countly.sharedInstance()
                .setRequiresConsent(true)
                .setLoggingEnabled(true)
                .setHttpPostForced(true)
                .enableParameterTamperingProtection(getPackageName())
                .setViewTracking(true)
                .setIfStarRatingShownAutomatically(true)
                .setAutomaticStarRatingSessionLimit(3)
                .setIfStarRatingDialogIsCancellable(true)
                .enableCrashReporting();

        String[] groupFeatures = new String[]{Countly.CountlyFeatureNames.sessions
                , Countly.CountlyFeatureNames.users, Countly.CountlyFeatureNames.events
                , Countly.CountlyFeatureNames.starRating};
        Countly.sharedInstance().createFeatureGroup(Const.COUNTLY_USAGE_STATS_GROUP_NAME, groupFeatures);

        boolean isUsageStatsEnabled = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.preference_anonymous_statistics_key), false);
        Countly.sharedInstance().setConsentFeatureGroup(Const.COUNTLY_USAGE_STATS_GROUP_NAME, isUsageStatsEnabled);

        boolean isCrashesEnabled = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.preference_crash_reporting_key), false);
        Countly.sharedInstance().setConsent(new String[]{Countly.CountlyFeatureNames.crashes}, isCrashesEnabled);

        Countly.sharedInstance().init(this, ANALYTICS_URL, ANALYTICS_API_KEY, null, DeviceId.Type.OPEN_UDID);
        Log.d(Const.TAG, "Countly setup");
    }
}
