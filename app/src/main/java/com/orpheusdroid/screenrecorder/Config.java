package com.orpheusdroid.screenrecorder;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;
import java.util.Locale;

public class Config {
    private static Config config;
    private SharedPreferences preferences;
    private Context mContext;

    private String saveLocation;
    private String fileFormat;
    private String prefix;

    private String resolution;
    private String fps;
    private String videoBitrate;
    private String orientation;

    private String audioSource;
    private String audioBitrate;
    private String audioChannel;
    private String audioSamplingRate;

    private String themePreference;

    private boolean floatingControls;
    private boolean showTouches;
    private boolean cameraOverlay;
    private boolean targetApp;
    private boolean isMagiskMode;
    private boolean isRootMode;
    private String targetAppPackage;

    private String codecOverride;

    private String language;

    private boolean crashlyticsEnabled;
    private boolean crashReportsEnabled;
    private boolean usageStatsEnabled;

    private Config(Context mContext) {
        this.mContext = mContext;
    }

    private Config() {

    }

    public static Config getInstance(Context mContext) {
        if (config == null) {
            config = new Config(mContext);
        }
        return config;
    }

    public String getSaveLocation() {
        return saveLocation;
    }

    public void setSaveLocation(String saveLocation) {
        this.saveLocation = saveLocation;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getFps() {
        return fps;
    }

    public void setFps(String fps) {
        this.fps = fps;
    }

    public String getVideoBitrate() {
        return videoBitrate;
    }

    public void setVideoBitrate(String videoBitrate) {
        this.videoBitrate = videoBitrate;
    }

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

    public String getAudioSource() {
        return audioSource;
    }

    public void setAudioSource(String audioSource) {
        this.audioSource = audioSource;
    }

    public String getAudioBitrate() {
        return audioBitrate;
    }

    public void setAudioBitrate(String audioBitrate) {
        this.audioBitrate = audioBitrate;
    }

    public String getAudioChannel() {
        return audioChannel;
    }

    public void setAudioChannel(String audioChannel) {
        this.audioChannel = audioChannel;
    }

    public String getAudioSamplingRate() {
        return audioSamplingRate;
    }

    public void setAudioSamplingRate(String audioSamplingRate) {
        this.audioSamplingRate = audioSamplingRate;
    }

    public boolean isFloatingControls() {
        return floatingControls;
    }

    public void setFloatingControls(boolean floatingControls) {
        this.floatingControls = floatingControls;
    }

    public boolean isShowTouches() {
        return showTouches;
    }

    public void setShowTouches(boolean showTouches) {
        this.showTouches = showTouches;
    }

    public boolean isCameraOverlay() {
        return cameraOverlay;
    }

    public void setCameraOverlay(boolean cameraOverlay) {
        this.cameraOverlay = cameraOverlay;
    }

    public boolean isTargetApp() {
        return targetApp;
    }

    public void setTargetApp(boolean targetApp) {
        this.targetApp = targetApp;
    }

    public String getTargetAppPackage() {
        return targetAppPackage;
    }

    public void setTargetAppPackage(String targetAppPackage) {
        this.targetAppPackage = targetAppPackage;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCodecOverride() {
        return codecOverride;
    }

    public void setCodecOverride(String codecOverride) {
        this.codecOverride = codecOverride;
    }

    public boolean isMagiskMode() {
        return isMagiskMode;
    }

    public void setMagiskMode(boolean magiskMode) {
        isMagiskMode = magiskMode;
    }

    public String getThemePreference() {
        return themePreference;
    }

    public void setThemePreference(String themePreference) {
        this.themePreference = themePreference;
    }

    public boolean isRootMode() {
        return isRootMode;
    }

    public void setRootMode(boolean rootMode) {
        isRootMode = rootMode;
    }

    public boolean isCrashlyticsEnabled() {
        return crashlyticsEnabled;
    }

    public void setCrashlyticsEnabled(boolean crashlyticsEnabled) {
        this.crashlyticsEnabled = crashlyticsEnabled;
    }

    public boolean isCrashReportsEnabled() {
        return crashReportsEnabled;
    }

    public void setCrashReportsEnabled(boolean crashReportsEnabled) {
        this.crashReportsEnabled = crashReportsEnabled;
    }

    public boolean isUsageStatsEnabled() {
        return usageStatsEnabled;
    }

    public void setUsageStatsEnabled(boolean usageStatsEnabled) {
        this.usageStatsEnabled = usageStatsEnabled;
    }

    public boolean shouldSetupAnalytics() {
        return isCrashlyticsEnabled() && (isCrashReportsEnabled() || isUsageStatsEnabled());
    }

    private SharedPreferences getPreferences() {
        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        }
        return preferences;
    }

    public void buildThemeConfig() {
        setThemePreference(getPreferences().getString(getString(R.string.preference_theme_key), "-1"));
    }

    public void buildConfig() {
        SharedPreferences preferences = getPreferences();

        saveLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + Const.APPDIR).getAbsolutePath();
        fileFormat = preferences.getString(getString(R.string.filename_key), "yyyyMMdd_HHmmss");
        prefix = preferences.getString(getString(R.string.fileprefix_key), "recording");

        resolution = preferences.getString(getString(R.string.res_key), "");
        fps = preferences.getString(getString(R.string.fps_key), "30");
        videoBitrate = preferences.getString(getString(R.string.bitrate_key), "7130317");
        orientation = preferences.getString(getString(R.string.orientation_key), "auto");

        audioSource = preferences.getString(getString(R.string.audiorec_key), "0");
        audioBitrate = preferences.getString(getString(R.string.bitrate_key), "192000");
        audioChannel = preferences.getString(getString(R.string.audiochannels_key), "1");
        audioSamplingRate = preferences.getString(getString(R.string.audiosamplingrate_key), "48000");

        floatingControls = preferences.getBoolean(getString(R.string.preference_floating_control_key), false);
        showTouches = preferences.getBoolean(getString(R.string.preference_show_touch_key), false);
        cameraOverlay = preferences.getBoolean(getString(R.string.preference_camera_overlay_key), false);
        targetApp = preferences.getBoolean(getString(R.string.preference_enable_target_app_key), false);
        targetAppPackage = preferences.getString(getString(R.string.preference_app_chooser_key), "");

        codecOverride = preferences.getString(getString(R.string.preference_advanced_settings_video_encoder_key), "0");

        language = preferences.getString(getString(R.string.preference_language_key), Locale.getDefault().getISO3Language());

        crashlyticsEnabled = preferences.getBoolean(getString(R.string.preference_crashlytics_master_key), false);
        crashReportsEnabled = preferences.getBoolean(getString(R.string.preference_crashlytics_crash_report_key), false);
        usageStatsEnabled = preferences.getBoolean(getString(R.string.preference_crashlytics_usage_stats_key), false);

        buildThemeConfig();
    }

    private String getString(int ID) {
        return mContext.getString(ID);
    }

    @Override
    public String toString() {
        return "Config{" +
                "preferences=" + preferences +
                ", mContext=" + mContext +
                ", saveLocation='" + saveLocation + '\'' +
                ", fileFormat='" + fileFormat + '\'' +
                ", prefix='" + prefix + '\'' +
                ", resolution='" + resolution + '\'' +
                ", fps='" + fps + '\'' +
                ", videoBitrate='" + videoBitrate + '\'' +
                ", orientation='" + orientation + '\'' +
                ", audioSource='" + audioSource + '\'' +
                ", audioBitrate='" + audioBitrate + '\'' +
                ", audioChannel='" + audioChannel + '\'' +
                ", audioSamplingRate='" + audioSamplingRate + '\'' +
                ", themePreference='" + themePreference + '\'' +
                ", floatingControls=" + floatingControls +
                ", showTouches=" + showTouches +
                ", cameraOverlay=" + cameraOverlay +
                ", targetApp=" + targetApp +
                ", isMagiskMode=" + isMagiskMode +
                ", isRootMode=" + isRootMode +
                ", targetAppPackage='" + targetAppPackage + '\'' +
                ", codecOverride='" + codecOverride + '\'' +
                ", language='" + language + '\'' +
                ", crashlyticsEnabled=" + crashlyticsEnabled +
                ", crashReportsEnabled=" + crashReportsEnabled +
                ", usageStatsEnabled=" + usageStatsEnabled +
                '}';
    }
}
