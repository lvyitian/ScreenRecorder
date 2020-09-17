package com.orpheusdroid.screenrecorder.services;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.orpheusdroid.crashreporter.CrashReporter;
import com.orpheusdroid.screenrecorder.Config;
import com.orpheusdroid.screenrecorder.Const;
import com.orpheusdroid.screenrecorder.R;
import com.orpheusdroid.screenrecorder.utils.ConfigHelper;
import com.orpheusdroid.screenrecorder.utils.Log;
import com.orpheusdroid.screenrecorder.utils.NotificationHelper;
import com.orpheusdroid.screenrecorder.utils.Resolution;
import com.orpheusdroid.screenrecorder.utils.ResolutionHelper;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;

public class RecordingService extends Service {
    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            Toast.makeText(RecordingService.this, R.string.screen_recording_stopped_toast, Toast.LENGTH_SHORT).show();
        }
    };
    private MediaProjectionCallback mMediaProjectionCallback;
    private Config config;
    private ConfigHelper configHelper;
    private ResolutionHelper resolutionHelper;
    private Resolution resolution;
    private String SAVEPATH;
    private long startTime, elapsedTime = 0;
    private Intent data;
    private int result;
    private boolean isBound = false;
    private NotificationHelper notificationHelper;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaRecorder mMediaRecorder;
    private AudioManager mAudioManager;
    private boolean isRecording = false;
    private int nextFileCount = 1;
    private String currentFilePath;
    private String nextFilePath;
    private File currentFile;
    private File nextFile;
    private BroadcastReceiver serviceBroadcastReceiver;

    //private File saveFile;

    private ServiceConnection floatingControlsConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //Get the service instance
            FloatingControlService.ServiceBinder binder = (FloatingControlService.ServiceBinder) service;
            Log.d(Const.TAG, "Floating service bound to recorder service");
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            Log.d(Const.TAG, "Floating service unbound to recorder service");
        }
    };

    private ServiceConnection floatingCameraConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //Get the service instance
            FloatingCameraViewService.ServiceBinder binder = (FloatingCameraViewService.ServiceBinder) service;
            FloatingCameraViewService floatingCameraViewService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    public RecordingService() {
    }

    //Start service as a foreground service. We dont want the service to be killed in case of low memory
    private void startNotificationForeGround(Notification notification, int ID) {
        startForeground(ID, notification);
    }

    @Override
    public void onCreate() {
        serviceBroadcastReceiver = new serviceStatusBroadcast();
        LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(serviceBroadcastReceiver, new IntentFilter(Const.SEVICE_STATUS_BROADCAST_REQUEST_ACTION));
    }

    public Config getConfig() {
        return config == null ? Config.getInstance(this) : config;
    }

    public ConfigHelper getConfigHelper() {
        return configHelper == null ? ConfigHelper.getInstance(this) : configHelper;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Const.TAG, "Starting Service. isBound: " + isBound + ", isRecording: " + isRecording);

        if (intent != null) {
            data = intent.getParcelableExtra(Const.RECORDER_INTENT_DATA);
            result = intent.getIntExtra(Const.RECORDER_INTENT_RESULT, Activity.RESULT_OK);
        }

        if (config == null) {
            config = Config.getInstance(this);
        }

        if (notificationHelper == null) {
            notificationHelper = NotificationHelper.getInstance(this);
        }

        if (resolutionHelper == null) {
            resolutionHelper = ResolutionHelper.getInstance(this);
        }

        if (configHelper == null) {
            configHelper = ConfigHelper.getInstance(this);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            notificationHelper.createNotificationChannels();

        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                //case Const.SCREEN_RECORDING_SHOW_FLOATING_ACTIONS:
                //break;
                case Const.SCREEN_RECORDING_START:
                    //SAVEPATH = //configHelper.getFileSaveName(config.getSaveLocation());
                    SAVEPATH = getConfigHelper().getFileName();
                    //currentFilePath = SAVEPATH;
                    /*notificationHelper.setSAVEPATH(SAVEPATH);
                    File savelocation = new File(SAVEPATH);
                    if (!savelocation.exists() || !savelocation.isDirectory()) {
                        if (!savelocation.mkdir())
                            stopRecording();
                    }*/
                    Log.d(Const.TAG, "Save path: " + SAVEPATH + ".mp4");
                    if (!isRecording) {
                        startRecording();
                        isRecording = true;
                    }
                    break;
                case Const.SCREEN_RECORDING_STOP:
                    stopRecording();
                    break;
                case Const.SCREEN_RECORDING_PAUSE:
                    pauseRecording();
                    break;
                case Const.SCREEN_RECORDING_RESUME:
                    resumeRecording();
                    break;
            }
        }
        return START_STICKY;
    }

    private void startRecording() {
        showNotification();
        getConfig().buildConfig();
        resolution = resolutionHelper.getWidthHeight();

        mMediaRecorder = new MediaRecorder();

        if (getConfig().isFloatingControls() && Settings.canDrawOverlays(this)) {
            if (!isBound) {
                showFloatingControls();
            }
        }

        mMediaRecorder.setOnErrorListener((mr, what, extra) -> {
            android.util.Log.e(Const.TAG, "Screencam Error: " + what + ", Extra: " + extra);
            Toast.makeText(this, R.string.recording_failed_toast, Toast.LENGTH_SHORT).show();
            CrashReporter.logException(new RuntimeException("Screen Recording failed: \nWhat:" + what + "\nExtra: " + extra));
            destroyMediaProjection();
        });

        mMediaRecorder.setOnInfoListener((mr, what, extra) -> {
            android.util.Log.d(Const.TAG, "Screencam Info: " + what + ", Extra: " + extra);
        });

        initRecorder();

        mMediaProjectionCallback = new MediaProjectionCallback();
        MediaProjectionManager mProjectionManager = (MediaProjectionManager) getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        //Initialize MediaProjection using data received from Intent
        if (mProjectionManager != null) {
            mMediaProjection = mProjectionManager.getMediaProjection(result, data);
        } else {
            Log.d(Const.TAG, "Creating media projection failed");
            destroyMediaProjection();
            stopSelf();
        }
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);

        mVirtualDisplay = createVirtualDisplay();

        /*if (floatingControlService != null)
            floatingControlService.onRecordingStarted();*/

        if (getConfig().isTargetApp() && !getConfig().getTargetAppPackage().equals("")) {
            startActivity(getPackageManager().getLaunchIntentForPackage(getConfig().getTargetAppPackage()));
        }

        try {
            mMediaRecorder.start();
            if (getConfig().isCameraOverlay()) {
                Intent floatingCameraIntent = new Intent(this, FloatingCameraViewService.class);
                startService(floatingCameraIntent);
                bindService(floatingCameraIntent,
                        floatingCameraConnection, BIND_AUTO_CREATE);
            }
            Intent result = new Intent(Const.SEVICE_STATUS_BROADCAST_RESPONSE_ACTION).putExtra(Const.SEVICE_STATUS_BROADCAST_STATUS_KEY, Const.RECORDING_STATUS.RECORDING);
            LocalBroadcastManager.getInstance(this).sendBroadcast(result);
        } catch (IllegalStateException ise) {
            Log.d(Const.TAG, "188: Media recorder start failed");
            CrashReporter.logException(ise);
            ise.printStackTrace();
            mMediaProjection.stop();
            stopSelf();
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private void pauseRecording() {
        mMediaRecorder.pause();
        //calculate total elapsed time until pause
        elapsedTime += (System.currentTimeMillis() - startTime);

        //Set Resume action to Notification and update the current notification
        Intent recordResumeIntent = new Intent(this, RecordingService.class);
        recordResumeIntent.setAction(Const.SCREEN_RECORDING_RESUME);
        PendingIntent precordResumeIntent = PendingIntent.getService(this, 0, recordResumeIntent, 0);
        NotificationCompat.Action action = new NotificationCompat.Action(R.drawable.ic_record_white,
                getString(R.string.screen_recording_notification_action_resume), precordResumeIntent);
        notificationHelper.updateNotification(
                notificationHelper.createRecordingNotification(action)
                        .setUsesChronometer(false).build(),
                Const.SCREEN_RECORDER_NOTIFICATION_ID
        );
        Toast.makeText(this, R.string.screen_recording_paused_toast, Toast.LENGTH_SHORT).show();

        //Send a broadcast receiver to the plugin app to disable show touches since the recording is paused
        /*if (showTouches) {
            Intent TouchIntent = new Intent();
            TouchIntent.setAction("com.orpheusdroid.screenrecorder.DISABLETOUCH");
            TouchIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            sendBroadcast(TouchIntent);
        }
        if (floatingControlService != null)
            floatingControlService.onRecordingPaused(); */

        Intent result = new Intent(Const.SEVICE_STATUS_BROADCAST_RESPONSE_ACTION).putExtra(Const.SEVICE_STATUS_BROADCAST_STATUS_KEY, Const.RECORDING_STATUS.PAUSED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(result);
    }

    @TargetApi(Build.VERSION_CODES.N)
    private void resumeRecording() {
        mMediaRecorder.resume();

        startTime = System.currentTimeMillis();

        Intent recordPauseIntent = new Intent(this, RecordingService.class);
        recordPauseIntent.setAction(Const.SCREEN_RECORDING_PAUSE);
        PendingIntent precordPauseIntent = PendingIntent.getService(this, 0, recordPauseIntent, 0);
        NotificationCompat.Action action = new NotificationCompat.Action(R.drawable.ic_pause_white,
                getString(R.string.screen_recording_notification_action_pause), precordPauseIntent);
        notificationHelper.updateNotification(
                notificationHelper.createRecordingNotification(action).setUsesChronometer(true)
                        .setWhen((System.currentTimeMillis() - elapsedTime)).build(), Const.SCREEN_RECORDER_NOTIFICATION_ID);
        Toast.makeText(this, R.string.screen_recording_resumed_toast, Toast.LENGTH_SHORT).show();


        /*if (floatingControlService != null)
            floatingControlService.onRecordingResumed();*/

        Intent result = new Intent(Const.SEVICE_STATUS_BROADCAST_RESPONSE_ACTION).putExtra(Const.SEVICE_STATUS_BROADCAST_STATUS_KEY, Const.RECORDING_STATUS.RECORDING);
        LocalBroadcastManager.getInstance(this).sendBroadcast(result);
    }

    private void stopRecording() {
        if (isBound) {
            unbindService(floatingControlsConnection);
            android.util.Log.d(Const.TAG, "Unbinding connection service");
        }

        /*if (floatingControlService != null)
            floatingControlService.onRecordingStopped();*/
        stopScreenSharing();
    }

    private VirtualDisplay createVirtualDisplay() {
        Log.d(Const.TAG, resolution.toString());
        return mMediaProjection.createVirtualDisplay("MainActivity",
                resolution.getWIDTH(), resolution.getHEIGHT(), resolution.getDPI(),
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null
                /*Handler*/);
    }

    private void initRecorder() {
        boolean mustRecAudio = false;
        String audioBitRate = getConfig().getAudioBitrate();
        String audioSamplingRate = getConfig().getAudioSamplingRate();
        String audioChannel = getConfig().getAudioChannel();
        String audioRecSource = getConfig().getAudioSource();
        //String SAVEPATH = config.getSaveLocation();
        int FPS = Integer.parseInt(getConfig().getFps());
        int BITRATE = Integer.parseInt(getConfig().getVideoBitrate());

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        /*int bufferSize = AudioRecord.getMinBufferSize(32000,
                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT) * 2;

        AudioPlaybackCaptureConfiguration audioConfig =
                new AudioPlaybackCaptureConfiguration.Builder(mMediaProjection)
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                        .addMatchingUsage(AudioAttributes.USAGE_GAME)
                        .build();

        AudioFormat audioFormat = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(32000)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build();


        AudioRecord recorder = new AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(audioConfig)
                //.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .build();
        recorder.startRecording();*/

        try {
            switch (audioRecSource) {
                case "1":
                    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mustRecAudio = true;
                    break;
                case "2":
                    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
                    mMediaRecorder.setAudioEncodingBitRate(Integer.parseInt(audioBitRate));
                    mMediaRecorder.setAudioSamplingRate(Integer.parseInt(audioSamplingRate));
                    mMediaRecorder.setAudioChannels(Integer.parseInt(audioChannel));
                    mustRecAudio = true;

                    android.util.Log.d(Const.TAG, "bit rate: " + audioBitRate + " sampling: " + audioSamplingRate + " channel" + audioChannel);
                    break;
                case "3":
                    mAudioManager.setParameters("screenRecordAudioSource=8");
                    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX);
                    mMediaRecorder.setAudioEncodingBitRate(Integer.parseInt(audioBitRate));
                    mMediaRecorder.setAudioSamplingRate(Integer.parseInt(audioSamplingRate));
                    mMediaRecorder.setAudioChannels(Integer.parseInt(audioChannel));
                    mustRecAudio = true;
                    break;
            }

            //currentFilePath = File.createTempFile(SAVEPATH, ".mp4");
            currentFilePath = SAVEPATH;
            currentFile = File.createTempFile(currentFilePath, ".mp4");

            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setOutputFile(currentFile);
            mMediaRecorder.setVideoSize(resolution.getWIDTH(), resolution.getHEIGHT());
            mMediaRecorder.setVideoEncoder(getConfigHelper().getBestVideoEncoder(resolution.getWIDTH(), resolution.getHEIGHT()));
            //mMediaRecorder.setMaxFileSize(configHelper.getFreeSpaceInBytes(config.getSaveLocation()));
            if (mustRecAudio)
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setVideoEncodingBitRate(BITRATE);
            mMediaRecorder.setVideoFrameRate(FPS);
            mMediaRecorder.setMaxFileSize(3221225472L); //3221225472L
            mMediaRecorder.setOnInfoListener((mediaRecorder, what, extra) -> {
                switch (what) {
                    case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_APPROACHING:
                        try {
                            nextFilePath = SAVEPATH + "_" + nextFileCount;
                            nextFile = File.createTempFile(SAVEPATH, "_" + nextFileCount + ".mp4");
                            mediaRecorder.setNextOutputFile(nextFile);
                            Toast.makeText(RecordingService.this, "Max File limit approaching. Next file name will be suffixed with " + nextFileCount, Toast.LENGTH_SHORT).show();
                            nextFileCount++;
                        } catch (IOException e) {
                            e.printStackTrace();
                            CrashReporter.logException(e);
                        }
                        break;
                    case MediaRecorder.MEDIA_RECORDER_INFO_NEXT_OUTPUT_FILE_STARTED:
                        Toast.makeText(RecordingService.this, "Next file started", Toast.LENGTH_SHORT).show();
                        Log.d(Const.TAG, "Next file started");
                        Log.d(Const.TAG, "Current Path:" + currentFilePath + "\n next path: " + nextFilePath);
                        //indexFile(currentFilePath + ".mp4", false);
                        saveToMediaStore(currentFile, false);
                        currentFilePath = nextFilePath;
                        currentFile = nextFile;
                }
            });
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            CrashReporter.logException(e);
            destroyMediaProjection();
        }
    }

    private void saveToMediaStore(File video, boolean isLast) {
        Log.d(Const.TAG, "Saving video from: " + video.getAbsolutePath());
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, currentFilePath + ".mp4");
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis());

        ContentResolver resolver = getContentResolver();
        Uri collectionUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String PATH = Environment.DIRECTORY_MOVIES + File.separator + Const.APPDIR;
            values.put(MediaStore.Video.Media.RELATIVE_PATH, PATH);
            values.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
            collectionUri = MediaStore.Video.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            String PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + File.separator + Const.APPDIR;
            new File(PATH).mkdirs();
            Log.d(Const.TAG, "save path: " + PATH + File.separator + currentFilePath + ".mp4");
            collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            values.put(MediaStore.Video.Media.DATA, PATH + File.separator + currentFilePath + ".mp4");
        }

        try {
            Uri itemUri = resolver.insert(collectionUri, values);
            // Add to the mediastore
            OutputStream os = resolver.openOutputStream(itemUri, "w");
            Files.copy(video.toPath(), os);
            os.close();

            video.delete();
            if (isLast)
                notificationHelper.showShareNotification(itemUri);
        } catch (Exception e) {
            Log.d(Const.TAG, "Error saving screen recording: " + e);
            CrashReporter.logException(e);
        }
    }

    private void destroyMediaProjection() {
        mAudioManager.setParameters("screenRecordAudioSource=0");
        try {
            mMediaRecorder.stop();
            //indexFile(currentFilePath, true);
            saveToMediaStore(currentFile, true);
            android.util.Log.i(Const.TAG, "MediaProjection Stopped");
        } catch (RuntimeException e) {
            Log.d(Const.TAG, "Fatal exception! Destroying media projection failed." + "\n" + e);
            if (new File(getConfig().getSaveLocation()).delete())
                Log.d(Const.TAG, "Corrupted file delete successful");
            Toast.makeText(this, getString(R.string.fatal_exception_message), Toast.LENGTH_SHORT).show();
            CrashReporter.logException(e);
        } finally {
            mMediaRecorder.reset();
            mVirtualDisplay.release();
            mMediaRecorder.release();
            if (mMediaProjection != null) {
                mMediaProjection.stop();
            }
            Intent result = new Intent(Const.SEVICE_STATUS_BROADCAST_RESPONSE_ACTION).putExtra(Const.SEVICE_STATUS_BROADCAST_STATUS_KEY, Const.RECORDING_STATUS.STOPPED);
            LocalBroadcastManager.getInstance(this).sendBroadcast(result);
            stopSelf();
        }
    }

    /* Its weird that android does not index the files immediately once its created and that causes
     * trouble for user in finding the video in gallery. Let's explicitly announce the file creation
     * to android and index it */
    private void indexFile(String filePath, boolean isLast) {
        //Create a new ArrayList and add the newly created video file path to it
        ArrayList<String> toBeScanned = new ArrayList<>();
        toBeScanned.add(filePath + ".mp4");
        String[] toBeScannedStr = new String[toBeScanned.size()];
        toBeScannedStr = toBeScanned.toArray(toBeScannedStr);

        //Request MediaScannerConnection to scan the new file and index it
        MediaScannerConnection.scanFile(getApplicationContext(), toBeScannedStr, null, (path, uri) -> {
            Log.d(Const.TAG, "SCAN COMPLETED: " + path + ", URI: " + uri);
            //Show toast on main thread

            if (isLast) {
                Message message = mHandler.obtainMessage();
                message.sendToTarget();
                //stopSelf();
                notificationHelper.showShareNotification(uri);
            }
        });
    }

    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            Log.d(Const.TAG, "Virtual display is null. Screen sharing already stopped");
            return;
        }
        destroyMediaProjection();
    }

    private void showFloatingControls() {
        Intent floatingControlsIntent = new Intent(this, FloatingControlService.class);
        startService(floatingControlsIntent);
        bindService(floatingControlsIntent,
                floatingControlsConnection, BIND_AUTO_CREATE);
    }

    private void showNotification() {
        //startTime is to calculate elapsed recording time to update notification during pause/resume
        startTime = System.currentTimeMillis();
        Intent recordPauseIntent = new Intent(this, RecordingService.class);
        recordPauseIntent.setAction(Const.SCREEN_RECORDING_PAUSE);
        PendingIntent precordPauseIntent = PendingIntent.getService(this, 0, recordPauseIntent, 0);
        NotificationCompat.Action action = new NotificationCompat.Action(R.drawable.ic_pause_white,
                getString(R.string.screen_recording_notification_action_pause), precordPauseIntent);

        //Start Notification as foreground
        startNotificationForeGround(notificationHelper.createRecordingNotification(action).build(), Const.SCREEN_RECORDER_NOTIFICATION_ID);
    }


    @Override
    public void onDestroy() {
        if (getConfig().isFloatingControls() && isBound) {
            //unbindService(floatingCameraConnection);
        }
        if (serviceBroadcastReceiver != null)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceBroadcastReceiver);
        super.onDestroy();
        Log.d(Const.TAG, "Recording service destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void unregisterCallback() {
        mMediaProjection.unregisterCallback(mMediaProjectionCallback);
        Log.d(Const.TAG, "Recording: Unregistering callback");
        mMediaProjection = null;
    }

    private class serviceStatusBroadcast extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            LocalBroadcastManager
                    .getInstance(getApplicationContext())
                    .sendBroadcastSync(new Intent(Const.SEVICE_STATUS_BROADCAST_RESPONSE_ACTION).putExtra(Const.SEVICE_STATUS_BROADCAST_STATUS_KEY, Const.RECORDING_STATUS.RECORDING));
        }
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            Log.d(Const.TAG, "Recording Stopped");
            unregisterCallback();
        }
    }
}
