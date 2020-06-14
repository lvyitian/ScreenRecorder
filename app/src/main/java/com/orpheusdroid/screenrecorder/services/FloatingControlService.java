package com.orpheusdroid.screenrecorder.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;
import com.orpheusdroid.screenrecorder.Const;
import com.orpheusdroid.screenrecorder.R;
import com.orpheusdroid.screenrecorder.utils.Log;
import com.orpheusdroid.screenrecorder.views.FloatingView;

import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

public class FloatingControlService extends Service implements FloatingView.FloatMoveCallback {
    private WindowManager mWindowManager;
    private DisplayMetrics mDisplayMetrics;
    private FloatingView mView;
    private SpeedDialView floatingMenu;
    private SpeedDialActionItem recordItem;
    private SpeedDialActionItem stopItem;
    private SpeedDialActionItem pauseItem;

    private boolean isServiceRunning;

    private WindowManager.LayoutParams mLayoutParams;
    private IBinder binder = new FloatingControlService.ServiceBinder();
    private SpeedDialView.OnActionSelectedListener itemListener = actionItem -> {
        Intent recorderService = new Intent(this, RecordingService.class);
        switch (actionItem.getId()) {
            case R.id.fm_record:
                recorderService.setAction(Const.SCREEN_RECORDING_RESUME);
                break;
            case R.id.fm_pause:
                recorderService.setAction(Const.SCREEN_RECORDING_PAUSE);
                break;
            case R.id.fm_stop:
                recorderService.setAction(Const.SCREEN_RECORDING_STOP);
                break;
        }
        startService(recorderService);
        return true;
    };
    private BroadcastReceiver status = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            Const.RECORDING_STATUS status = (Const.RECORDING_STATUS) intent.getSerializableExtra(Const.SEVICE_STATUS_BROADCAST_STATUS_KEY);
            Log.d(Const.TAG, "PONG: " + intent.getAction() + ", result: " + status);
            switch (status) {
                case RECORDING:
                    floatingMenu.removeActionItem(recordItem);
                    floatingMenu.addActionItem(pauseItem);
                    break;
                case PAUSED:
                    floatingMenu.removeActionItem(pauseItem);
                    floatingMenu.addActionItem(recordItem);
                    break;
                case STOPPED:
                    break;
            }
        }
    };

    public FloatingControlService() {
    }

    private static float pxFromDp(Context context, float dp) {
        Log.d(Const.TAG, context.getResources().getDisplayMetrics().density + " ");
        return context.getResources().getDisplayMetrics().density * dp;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Const.TAG, "Floating control started");
        this.mView = (FloatingView) LayoutInflater.from(this).inflate(R.layout.content_floating_view, null);
        floatingMenu = mView.findViewById(R.id.speedDial);

        //floatingMenu.inflate(R.menu.floating_menu);
        initViews();

        LocalBroadcastManager.getInstance(this).registerReceiver(status, new IntentFilter(Const.SEVICE_STATUS_BROADCAST_RESPONSE_ACTION));
        LocalBroadcastManager.getInstance(this).sendBroadcastSync(new Intent(Const.SEVICE_STATUS_BROADCAST_REQUEST_ACTION));

        floatingMenu.setOnActionSelectedListener(itemListener);

        this.mWindowManager = (WindowManager) this.getSystemService(WINDOW_SERVICE);
        this.mDisplayMetrics = new DisplayMetrics();
        this.mWindowManager.getDefaultDisplay().getRealMetrics(this.mDisplayMetrics);

        initLayoutParams();

        mWindowManager.addView(mView, mLayoutParams);

        return START_STICKY;
    }

    private void initViews() {
        PopupMenu popupMenu = new PopupMenu(this, new View(this));
        popupMenu.inflate(R.menu.floating_menu);
        Menu menu = popupMenu.getMenu();

        recordItem = new SpeedDialActionItem.Builder(menu.getItem(0).getItemId(), menu.getItem(0).getIcon())
                .setFabImageTintColor(getColor(R.color.famIconColor))
                .setFabBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.famBGColor, getTheme()))
                .create();

        pauseItem = new SpeedDialActionItem.Builder(menu.getItem(1).getItemId(), menu.getItem(1).getIcon())
                .setFabImageTintColor(getColor(R.color.famIconColor))
                .setFabBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.famBGColor, getTheme()))
                .create();

        stopItem = new SpeedDialActionItem.Builder(menu.getItem(2).getItemId(), menu.getItem(2).getIcon())
                .setFabImageTintColor(getColor(R.color.famIconColor))
                .setFabBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.famBGColor, getTheme()))
                .create();

        floatingMenu.addActionItem(stopItem);
        floatingMenu.addActionItem(pauseItem);
    }

    private void initLayoutParams() {
        Log.d("FloatWindowisLandscape init", " init" + isLandscape());
        boolean isLandscape = isLandscape();

        this.mLayoutParams = new WindowManager.LayoutParams();
        this.mLayoutParams.flags = 262184;
        this.mLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        this.mLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;

        mView.setCallback(this);

        this.mLayoutParams.setTitle("Screenrecorder");

        Log.d(Const.TAG, "mLayoutParams.height: " + this.mLayoutParams.height + "mLayoutParams.width: " + this.mLayoutParams.width);
        int mHeight = WindowManager.LayoutParams.WRAP_CONTENT;
        if (mHeight != WindowManager.LayoutParams.WRAP_CONTENT) {
            this.mLayoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        }
        int mWidth = WindowManager.LayoutParams.WRAP_CONTENT;
        if (mWidth != WindowManager.LayoutParams.WRAP_CONTENT) {
            this.mLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        }

        this.mLayoutParams.gravity = Gravity.TOP | Gravity.START;

        this.mLayoutParams.format = PixelFormat.RGBA_8888;

        if (Build.VERSION.SDK_INT >= 26) {
            this.mLayoutParams.type = TYPE_APPLICATION_OVERLAY;
        } else {
            this.mLayoutParams.type = TYPE_SYSTEM_ALERT;
        }

        if (isLandscape()) {
            this.mLayoutParams.y = (int) (((float) this.mDisplayMetrics.heightPixels) - (((float) getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin)) + pxFromDp(this, 51.0f)));
            this.mLayoutParams.x = (int) (((float) this.mDisplayMetrics.widthPixels) - (((float) getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin)) + pxFromDp(this, 183.0f)));
            Log.d("FloatWindowisLandscape", "mLayoutParams.x: " + this.mLayoutParams.x + " " + "mLayoutParams.y: " + this.mLayoutParams.y);
            return;
        }
        this.mLayoutParams.x = (int) (((float) this.mDisplayMetrics.widthPixels) - pxFromDp(this, 192.0f));
        this.mLayoutParams.y = this.mDisplayMetrics.heightPixels - (this.mDisplayMetrics.heightPixels / 3);
    }

    private boolean isLandscape() {
        return this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    @Override
    public void onActionDown(MotionEvent motionEvent) {
        Log.d(Const.TAG, "Motion Event: " + motionEvent.getAction());
    }

    @Override
    public void onActionMove(MotionEvent motionEvent, float offsetX, float offsetY) {
        mLayoutParams.x = (int) offsetX;
        mLayoutParams.y = (int) offsetY;
        mWindowManager.updateViewLayout(mView, mLayoutParams);
    }

    @Override
    public void onActionUp(MotionEvent motionEvent, boolean z) {

    }

    @Override
    public void onDestroy() {
        if (mView != null) mWindowManager.removeView(mView);
        Log.d(Const.TAG, "Unbinding successful!");
        super.onDestroy();
    }

    //Return ServiceBinder instance on successful binding
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(Const.TAG, "Binding successful!");
        return binder;
    }

    //Stop the service once the service is unbinded from recording service
    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(Const.TAG, "Unbinding and stopping service");
        stopSelf();
        return super.onUnbind(intent);
    }

    /**
     * Binder class for binding to recording service
     */
    class ServiceBinder extends Binder {
        FloatingControlService getService() {
            return FloatingControlService.this;
        }
    }
}
