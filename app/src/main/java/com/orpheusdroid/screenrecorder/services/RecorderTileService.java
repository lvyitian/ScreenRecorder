package com.orpheusdroid.screenrecorder.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.orpheusdroid.screenrecorder.Const;
import com.orpheusdroid.screenrecorder.R;
import com.orpheusdroid.screenrecorder.ui.MainActivity;
import com.orpheusdroid.screenrecorder.utils.Log;

public class RecorderTileService extends TileService {
    private boolean isServiceRunning;
    private BroadcastReceiver pong = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d(Const.TAG, "PONG: " + intent.getAction());
            Const.RECORDING_STATUS status = (Const.RECORDING_STATUS) intent.getSerializableExtra(Const.SEVICE_STATUS_BROADCAST_STATUS_KEY);
            isServiceRunning = status == Const.RECORDING_STATUS.RECORDING || status == Const.RECORDING_STATUS.PAUSED;
            updateTile(isServiceRunning);
        }
    };

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        Log.d(Const.TAG, "Tile Added");
        updateTile(false);
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
        Log.d(Const.TAG, "Tile Removed");
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        isServiceRunning = false;
        Log.d(Const.TAG, "PONG is set to false");
        LocalBroadcastManager.getInstance(this).registerReceiver(pong, new IntentFilter(Const.SEVICE_STATUS_BROADCAST_RESPONSE_ACTION));
        LocalBroadcastManager.getInstance(this).sendBroadcastSync(new Intent(Const.SEVICE_STATUS_BROADCAST_REQUEST_ACTION));
    }

    @Override
    public void onStopListening() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(pong);
        super.onStopListening();
    }

    @Override
    public void onClick() {
        if (isServiceRunning) {
            Intent recorderService = new Intent(this, RecordingService.class);
            recorderService.setAction(Const.SCREEN_RECORDING_STOP);
            startService(recorderService);

        } else {
            Intent recordingIntent = new Intent(this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setAction(Const.SCREEN_RECORDER_START_RECORDING_INTENT);
            startActivityAndCollapse(recordingIntent);
        }
    }

    private void updateTile(boolean isServiceRunning) {
        Tile tile = getQsTile();
        if (isServiceRunning) {
            tile.setLabel(getString(R.string.tile_stop_recording));
            tile.setState(Tile.STATE_ACTIVE);
        } else {
            tile.setLabel(getString(R.string.tile_start_recording));
            tile.setState(Tile.STATE_INACTIVE);
        }
        tile.updateTile();
    }
}
