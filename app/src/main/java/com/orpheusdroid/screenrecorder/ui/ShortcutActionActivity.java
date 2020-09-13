package com.orpheusdroid.screenrecorder.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.orpheusdroid.screenrecorder.Const;
import com.orpheusdroid.screenrecorder.R;
import com.orpheusdroid.screenrecorder.services.RecordingService;
import com.orpheusdroid.screenrecorder.utils.Log;

public class ShortcutActionActivity extends AppCompatActivity {


    /**
     * Instance of {@link MediaProjectionManager} system service
     */
    private MediaProjectionManager mProjectionManager;

    private boolean isServiceRunning;
    private BroadcastReceiver pong = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Const.RECORDING_STATUS status = (Const.RECORDING_STATUS) intent.getSerializableExtra(Const.SEVICE_STATUS_BROADCAST_STATUS_KEY);
            isServiceRunning = status == Const.RECORDING_STATUS.RECORDING || status == Const.RECORDING_STATUS.PAUSED;
            Log.d(Const.TAG, "PONG: " + isServiceRunning);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.storage_permission_request_title), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            this.finish();
            return;
        }

        PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.preference_theme_key), Const.PREFS_LIGHT_THEME);
        //Acquiring media projection service to start screen mirroring
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        //Respond to app shortcut
        if (getIntent().getAction() != null) {
            if (getIntent().getAction().equals(Const.SCREEN_RECORDER_START_RECORDING_INTENT)) {
                startActivityForResult(mProjectionManager.createScreenCaptureIntent(), Const.SCREEN_RECORD_REQUEST_CODE);
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String intentAction = getIntent().getAction();

        //The user has denied permission for screen mirroring. Let's notify the user
        if (resultCode == RESULT_CANCELED && requestCode == Const.SCREEN_RECORD_REQUEST_CODE) {
            Toast.makeText(this,
                    getString(R.string.screen_recording_permission_denied), Toast.LENGTH_SHORT).show();
            //Return to home screen if the app was started from app shortcut
            if (intentAction != null && intentAction.equals(Const.SCREEN_RECORDER_START_RECORDING_INTENT))
                this.finish();
            return;

        }

        /*If code reaches this point, congratulations! The user has granted screen mirroring permission
         * Let us set the recorderservice intent with relevant data and start service*/
        Intent recorderService = new Intent(this, RecordingService.class);
        if (isServiceRunning)
            recorderService.setAction(Const.SCREEN_RECORDING_STOP);
        else
            recorderService.setAction(Const.SCREEN_RECORDING_START);
        recorderService.putExtra(Const.RECORDER_INTENT_DATA, data);
        recorderService.putExtra(Const.RECORDER_INTENT_RESULT, resultCode);
        startService(recorderService);
        LocalBroadcastManager.getInstance(this).sendBroadcastSync(new Intent(Const.SEVICE_STATUS_BROADCAST_REQUEST_ACTION));

        if (intentAction != null && intentAction.equals(Const.SCREEN_RECORDER_START_RECORDING_INTENT))
            this.finish();
    }

    @Override
    protected void onResume() {
        isServiceRunning = false;
        Log.d(Const.TAG, "PONG is set to false");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(pong);
        LocalBroadcastManager.getInstance(this).registerReceiver(pong, new IntentFilter(Const.SEVICE_STATUS_BROADCAST_RESPONSE_ACTION));
        LocalBroadcastManager.getInstance(this).sendBroadcastSync(new Intent(Const.SEVICE_STATUS_BROADCAST_REQUEST_ACTION));
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(pong);
        super.onDestroy();
    }
}