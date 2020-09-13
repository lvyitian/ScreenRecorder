package com.orpheusdroid.screenrecorder.ui;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.behavior.HideBottomViewOnScrollBehavior;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.orpheusdroid.crashreporter.ui.CrashReporterActivity;
import com.orpheusdroid.screenrecorder.Config;
import com.orpheusdroid.screenrecorder.Const;
import com.orpheusdroid.screenrecorder.DonateActivity;
import com.orpheusdroid.screenrecorder.R;
import com.orpheusdroid.screenrecorder.ScreenCamApp;
import com.orpheusdroid.screenrecorder.interfaces.IPermissionListener;
import com.orpheusdroid.screenrecorder.interfaces.VideoFragmentListener;
import com.orpheusdroid.screenrecorder.services.RecordingService;
import com.orpheusdroid.screenrecorder.ui.settings.fragments.RootSettingsFragment;
import com.orpheusdroid.screenrecorder.utils.Log;
import com.orpheusdroid.screenrecorder.utils.PermissionHelper;

import java.util.ArrayList;

import ly.count.android.sdk.Countly;

public class MainActivity extends BaseActivity {
    private PermissionHelper permissionHelper;
    private IPermissionListener permissionListener;
    private FloatingActionButton fab;
    private BottomNavigationView navView;
    private BottomAppBar bottomAppBar;
    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mediaProjection;
    private VideoFragmentListener videoFragmentListener;

    private ArrayList<String> files = new ArrayList<>();

    private RootSettingsFragment settingsFragment = new RootSettingsFragment();
    private VideoListFragment videoListFragment;
    private Fragment currentFragment = settingsFragment;
    private FragmentManager fm;

    private boolean isServiceRunning;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    if (currentFragment == settingsFragment)
                        return false;
                    getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                            .hide(currentFragment)
                            .show(settingsFragment)
                            .commit();
                    currentFragment = settingsFragment;
                    fab.show();
                    return true;
                case R.id.videos:
                    if (currentFragment == videoListFragment)
                        return false;

                    if (videoListFragment == null) {
                        videoListFragment = new VideoListFragment();
                        fm.beginTransaction().add(R.id.fragment, videoListFragment, "2").hide(videoListFragment).commit();
                    }
                    getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                            .hide(currentFragment)
                            .show(videoListFragment)
                            .commit();
                    currentFragment = videoListFragment;
                    fab.hide();
                    //permissionHelper.requestPermissionStorage(Const.VIDEO_FRAGMENT_EXTDIR_REQUEST_CODE);
                    return true;
            }
            return false;
        }
    };

    private BroadcastReceiver pong = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Const.RECORDING_STATUS status = (Const.RECORDING_STATUS) intent.getSerializableExtra(Const.SEVICE_STATUS_BROADCAST_STATUS_KEY);
            isServiceRunning = status == Const.RECORDING_STATUS.RECORDING || status == Const.RECORDING_STATUS.PAUSED;
            Log.d(Const.TAG, "PONG: " + isServiceRunning);
            if (isServiceRunning && fab != null) {
                fab.setImageResource(R.drawable.ic_stop);
                fab.setTooltipText(getString(R.string.tile_stop_recording));
            } else if (!isServiceRunning && fab != null) {
                fab.setImageResource(R.drawable.ic_record);
                fab.setTooltipText(getString(R.string.tile_start_recording));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fm = getSupportFragmentManager();

        //fm.beginTransaction().replace(R.id.fragment, settingsFragment).commit();
        fm.beginTransaction().add(R.id.fragment, settingsFragment, "1").commit();

        navView = findViewById(R.id.bottom_navigation);

        bottomAppBar = findViewById(R.id.bottom_bar);

        //Acquiring media projection service to start screen mirroring
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        permissionHelper = PermissionHelper.getInstance(this);
        init(Const.EXTDIR_REQUEST_CODE);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            if (mediaProjection == null && !isServiceRunning) {
                //Request Screen recording permission
                startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), Const.SCREEN_RECORD_REQUEST_CODE);
            } else if (isServiceRunning) {
                //stop recording if the service is already active and recording
                //Toast.makeText(MainActivity.this, "Screen already recording", Toast.LENGTH_SHORT).show();
                Intent recorderService = new Intent(this, RecordingService.class);
                recorderService.setAction(Const.SCREEN_RECORDING_STOP);
                startService(recorderService);
            }
        });

        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ((View) navView.getParent()).getLayoutParams();
        params.setBehavior(new HideBottomViewOnScrollBehavior());

        if (getIntent().getAction() != null && getIntent().getAction().equals(Const.SCREEN_RECORDER_START_RECORDING_INTENT)) {
            parseIntent(getIntent());
        }

        Config config = Config.getInstance(this);
        config.buildConfig();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Config.getInstance(this).shouldSetupAnalytics()) {
            if (!Countly.sharedInstance().isInitialized()) {
                ((ScreenCamApp) getApplication()).setupAnalytics();
            }
            Countly.sharedInstance().onStart(this);
        }
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
    protected void onPause() {
        Log.d(Const.TAG, "PONG destroyed");
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (Countly.sharedInstance().isInitialized()) {
            Countly.sharedInstance().onStop();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(pong);
        super.onDestroy();
    }

    public void init(int RequestCode) {
        permissionHelper.requestPermissionStorage(RequestCode);
    }

    public void setVideoFragmentListener(VideoFragmentListener videoFragmentListener) {
        this.videoFragmentListener = videoFragmentListener;
    }

    public boolean isVideoFragmentListernerInitialized() {
        return videoFragmentListener != null;
    }

    public void setPermissionListener(IPermissionListener permissionListener) {
        this.permissionListener = permissionListener;
    }

    public void setBottomBarVisibility(boolean isVisible) {
        if (isVisible) {
            navView.setVisibility(View.VISIBLE);
            bottomAppBar.setVisibility(View.VISIBLE);
            //bottomAppBar.animate().translationY(0).alpha(1.0f);
            fab.show();
        } else {
            navView.setVisibility(View.GONE);
            bottomAppBar.setVisibility(View.GONE);
            //bottomAppBar.animate().translationY(bottomAppBar.getHeight()).alpha(0.0f);
            fab.hide();
        }
    }

    private void handleStoragePermission(int[] grantResults) {
        if ((grantResults.length > 0) &&
                (grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            android.util.Log.d(Const.TAG, "write storage Permission Denied");
            /* Disable floating action Button in case write storage permission is denied.
             * There is no use in recording screen when the video is unable to be saved */
            fab.setEnabled(false);
            permissionHelper.showSnackbar();
            /*if (videoFragmentListener != null)
                videoFragmentListener.onStorageResult(false);*/
        } else if ((grantResults.length > 0) &&
                (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            /* Since we have write storage permission now, lets create the app directory
             * in external storage*/
            Log.d(Const.TAG, "write storage Permission granted");
            permissionHelper.createDir();
            permissionHelper.hideSnackbar();
            fab.setEnabled(true);
            /*if (videoFragmentListener != null)
                videoFragmentListener.onStorageResult(true);*/
        }
    }

    /**
     * onActivityResult method to handle the activity results for floating controls
     * and screen recording permission
     *
     * @param requestCode Unique request code for different startActivityForResult calls
     * @param resultCode  result code representing the user's choice
     * @param data        Extra intent data passed from calling intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String intentAction = getIntent().getAction();

        //Result for system windows permission required to show floating controls
        if (requestCode == Const.FLOATING_CONTROLS_SYSTEM_WINDOWS_CODE || requestCode == Const.CAMERA_SYSTEM_WINDOWS_CODE) {
            //setSystemWindowsPermissionResult(requestCode);
            return;
        }

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

        //if (intentAction != null && intentAction.equals(getString(R.string.app_shortcut_action)))
        //this.finish();
    }

    private void parseIntent(Intent intent) {

        if (mediaProjection == null && !isServiceRunning) {
            //Request Screen recording permission
            startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), Const.SCREEN_RECORD_REQUEST_CODE);
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction() != null && intent.getAction().equals(Const.SCREEN_RECORDER_START_RECORDING_INTENT)) {
            parseIntent(intent);
        }
    }

    @Override
    public void recreate() {
        Log.d(Const.TAG, "Theme triggered recreate");
        ViewGroup container = findViewById(R.id.fragment);
        container.removeAllViews();
        getSupportFragmentManager().beginTransaction().remove(currentFragment).commit();
        super.recreate();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d("Permission", requestCode + "");

        switch (requestCode) {
            case Const.EXTDIR_REQUEST_CODE:
                handleStoragePermission(grantResults);
                break;

            /*case Const.VIDEO_FRAGMENT_EXTDIR_REQUEST_CODE:
                if ((grantResults.length > 0) &&
                        (grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
                    if (videoFragmentListener != null)
                        videoFragmentListener.onStorageResult(false);
                } else {
                    if (videoFragmentListener != null)
                    videoFragmentListener.onStorageResult(true);
                }
                break;*/
        }

        if (permissionListener != null)
            permissionListener.onPermissionResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            case R.id.privacy_policy:
                startActivity(new Intent(this, PrivacyPolicy.class));
                return true;
            case R.id.menu_faq:
                startActivity(new Intent(this, FAQActivity.class));
                return true;
            case R.id.donate:
                startActivity(new Intent(this, DonateActivity.class));
                return true;
            case R.id.crashLog:
                startActivity(new Intent(this, CrashReporterActivity.class));
                return true;
            case R.id.help:
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/joinchat/C_ZSIUKiqUCI5NsPMAv0eA")));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, "No browser app installed!", Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
