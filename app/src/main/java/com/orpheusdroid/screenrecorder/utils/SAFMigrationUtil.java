package com.orpheusdroid.screenrecorder.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.ProgressIndicator;
import com.orpheusdroid.crashreporter.CrashReporter;
import com.orpheusdroid.screenrecorder.Const;
import com.orpheusdroid.screenrecorder.R;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

public class SAFMigrationUtil extends AppCompatActivity {
    private TextView infoHeader;
    private TextView progressText;
    private TextView moveCount;
    private TextView copyText;

    private ProgressIndicator progress;

    private ArrayList<File> videos = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_safmigration);

        infoHeader = findViewById(R.id.information_header);
        progressText = findViewById(R.id.saf_progress_tv);
        moveCount = findViewById(R.id.saf_file_count);
        copyText = findViewById(R.id.saf_current_copy_file);
        progress = findViewById(R.id.saf_progress_indicator);

        progress.setIndeterminate(false);
        progressText.setText(getString(R.string.saf_util_progress_text, 0));

        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + Const.APPDIR + File.separator);

        if (dir.isDirectory() && dir.canRead() && dir.listFiles() != null) {
            videos.addAll(Arrays.asList(dir.listFiles(file -> file.getName().endsWith(".mp4"))));
            moveCount.setText(getString(R.string.saf_util_move_count, 0, videos.size()));
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Start Migration?")
                    .setMessage("There are in total " + videos.size() + " files to move and it might take a while. Proceed?")
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> moveVideos())
                    .setCancelable(false)
                    .create().show();
        }

    }

    private void moveVideos() {
        infoHeader.setText(R.string.saf_util_info_header_copying);
        progress.setMax(videos.size());
        for (int i = 0; i < videos.size(); i++) {
            Log.d(Const.TAG, "File Name: " + videos.get(i).getAbsolutePath());
            moveCount.setText(getString(R.string.saf_util_move_count, (i + 1), videos.size()));
            progressText.setText(getString(R.string.saf_util_progress_text, (i + 1)));
            copyText.setText(getString(R.string.saf_util_copying_text, videos.get(i).getAbsolutePath()));
            saveToMediaStore(videos.get(i));
            progress.setProgress((i + 1));
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Migration Complete")
                .setMessage("All files have been moved to new storage location. Please verify if all files have moved manually and delete the folder manually.")
                .setPositiveButton(android.R.string.ok, ((dialogInterface, i) -> this.finish()))
                .setCancelable(false)
                .create().show();
    }

    private void saveToMediaStore(File video) {
        Log.d(Const.TAG, "Saving video from: " + video.getAbsolutePath());
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, video.getName());
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
            Log.d(Const.TAG, "save path: " + PATH + File.separator + video.getName());
            collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            values.put(MediaStore.Video.Media.DATA, PATH + File.separator + video.getName());
        }

        try {
            Uri itemUri = resolver.insert(collectionUri, values);
            // Add to the mediastore
            OutputStream os = resolver.openOutputStream(itemUri, "w");
            Files.copy(video.toPath(), os);
            os.close();

            video.delete();

        } catch (Exception e) {
            Log.d(Const.TAG, "Error saving screen recording: " + e);
            CrashReporter.logException(e);
        }
    }
}
