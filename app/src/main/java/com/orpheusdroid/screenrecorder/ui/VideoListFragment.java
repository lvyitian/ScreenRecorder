package com.orpheusdroid.screenrecorder.ui;

import android.Manifest;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.orpheusdroid.screenrecorder.Const;
import com.orpheusdroid.screenrecorder.R;
import com.orpheusdroid.screenrecorder.adapter.VideoListAdapter;
import com.orpheusdroid.screenrecorder.adapter.models.videolist.Video;
import com.orpheusdroid.screenrecorder.adapter.models.videolist.VideoHeader;
import com.orpheusdroid.screenrecorder.adapter.models.videolist.VideoItem;
import com.orpheusdroid.screenrecorder.adapter.models.videolist.VideoListItem;
import com.orpheusdroid.screenrecorder.interfaces.VideoFragmentListener;
import com.orpheusdroid.screenrecorder.utils.PermissionHelper;

import java.io.File;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class VideoListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, VideoFragmentListener {

    private static VideoListFragment fragment;
    private VideoListViewModel mViewModel;
    private TextView message;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ShimmerFrameLayout shimmer;
    private RecyclerView videoList;
    private ArrayList<VideoListItem> consolidatedList = new ArrayList<>();
    private VideoListAdapter videoListAdapter;
    private Uri collection;

    public static VideoListFragment getInstance() {
        if (fragment == null)
            fragment = new VideoListFragment();
        return fragment;
    }

    /**
     * Method to check if the file's meme type is video
     *
     * @param path String - path to the file
     * @return boolean
     */
    private static boolean isVideoFile(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("video");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.video_list_fragment, container, false);

        videoList = view.findViewById(R.id.videoList);
        videoList.setHasFixedSize(true);

        shimmer = view.findViewById(R.id.shimmer_view_container);

        message = view.findViewById(R.id.message_tv);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);

        return view;
    }

    private void getVideosAndList() {
        consolidatedList = new ArrayList<>();

        String selection;
        String PATH;
        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.ARTIST,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Video.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
            );
            selection = MediaStore.Video.Media.RELATIVE_PATH;
            PATH = Environment.DIRECTORY_MOVIES + File.separator + Const.APPDIR
                    + File.separator + "%";
        } else {
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            selection = MediaStore.Video.Media.DATA;
            PATH = Environment.getExternalStorageDirectory().getPath() + File.separator
                    + Environment.DIRECTORY_MOVIES + File.separator + Const.APPDIR
                    + File.separator + "%";
        }

        Log.d(Const.TAG, "PATH: " + PATH);

        Single.fromCallable(() -> {
            Cursor mCursor = VideoListFragment.this.getContext().getContentResolver().query(collection, projection, selection + " like ? ",
                    new String[]{PATH}, null);
            return mCursor;
        }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Throwable {
                        swipeRefreshLayout.setRefreshing(false);
                        throwable.printStackTrace();
                    }
                })
                .subscribe(new Consumer<Cursor>() {
                    @Override
                    public void accept(Cursor cursor) throws Throwable {
                        swipeRefreshLayout.setRefreshing(false);
                        shimmer.stopShimmer();
                        shimmer.setVisibility(View.GONE);
                        if (cursor != null) {
                            if (cursor.getCount() == 0) {
                                videoList.setVisibility(View.GONE);
                                message.setVisibility(View.VISIBLE);
                            } else {
                                list(cursor);
                            }
                        }
                    }
                });
    }

    private void list(Cursor mCursor) {
        ArrayList<Video> videosList = new ArrayList<>();
        for (mCursor.moveToFirst(); !mCursor.isAfterLast(); mCursor.moveToNext()) {
            // The Cursor is now set to the right position
            Video video = new Video();
            video.setFileName(mCursor.getString(mCursor.getColumnIndex(MediaStore.Video.Media.TITLE)));
            video.setFileUri(ContentUris.withAppendedId(collection,
                    mCursor.getLong(mCursor.getColumnIndex(MediaStore.Video.Media._ID))));
            video.setLastModified(mCursor.getLong(mCursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)));
            videosList.add(video);
        }
        Map<Date, List<Video>> groupedHashMap = groupDataIntoHashMap(videosList);


        for (Date date : groupedHashMap.keySet()) {
            VideoHeader dateItem = new VideoHeader();
            dateItem.setDate(date);
            consolidatedList.add(dateItem);

            Collections.sort(groupedHashMap.get(date), Comparator.reverseOrder());

            for (Video video : groupedHashMap.get(date)) {
                VideoItem generalItem = new VideoItem();
                generalItem.setVideo(video);
                consolidatedList.add(generalItem);
            }
        }
        setAdapter(consolidatedList);
    }

    private void checkPermissionAndLoadVideos() {
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // request the permission
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Const.EXTDIR_REQUEST_CODE);
        } else {
            // has the permission.
            getVideosAndList();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(Const.TAG, "On Video resume");
        if (!consolidatedList.isEmpty()) {
            Log.d(Const.TAG, "Video list is not empty: " + consolidatedList.size());
            setAdapter(consolidatedList);
        }
        {
            checkPermissionAndLoadVideos();
        }
    }

    @Override
    public void onRefresh() {
        videoList.setVisibility(View.GONE);
        shimmer.setVisibility(View.VISIBLE);
        shimmer.animate();
        shimmer.showShimmer(true);
        getVideosAndList();
    }

    private void setAdapter(ArrayList<VideoListItem> videos) {
        videoListAdapter = new VideoListAdapter(videos, (AppCompatActivity) getActivity());
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        //videoList.setLayoutManager(layoutManager);
        videoList.setLayoutManager(layoutManager);
        videoList.setAdapter(videoListAdapter);

        com.orpheusdroid.screenrecorder.utils.Log.d(Const.TAG, "Videos loaded into rv");

        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return videoListAdapter.getItemViewType(position) == VideoListItem.TYPE_HEADER ? layoutManager.getSpanCount() : 1;
            }
        });

        //Log.d(Const.TAG, videos.toString());

        videoList.setVisibility(View.VISIBLE);
        message.setVisibility(View.GONE);
    }

    @Override
    public void onStorageResult(boolean result) {
        Log.d(Const.TAG, "Loading videos after result" + result);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Const.EXTDIR_REQUEST_CODE:
                if ((grantResults.length > 0) &&
                        (grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
                    android.util.Log.d(Const.TAG, "write storage Permission Denied");
                    /* Disable floating action Button in case write storage permission is denied.
                     * There is no use in recording screen when the video is unable to be saved */
                    PermissionHelper.getInstance((AppCompatActivity) getActivity()).showSnackbar();
                } else if ((grantResults.length > 0) &&
                        (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    /* Since we have write storage permission now, lets create the app directory
                     * in external storage*/
                    com.orpheusdroid.screenrecorder.utils.Log.d(Const.TAG, "write storage Permission granted");
                    getVideosAndList();
                }
        }
    }

    private Map<Date, List<Video>> groupDataIntoHashMap(List<Video> videos) {

        Map<Date, List<Video>> groupedHashMap = new TreeMap<>(Collections.reverseOrder());

        for (Video video : videos) {

            Date hashMapKey = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            try {
                hashMapKey = sdf.parse(sdf.format(video.getLastModified()));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if (groupedHashMap.containsKey(hashMapKey)) {
                // The key is already in the HashMap; add the pojo object
                // against the existing key.
                groupedHashMap.get(hashMapKey).add(video);
            } else {
                // The key is not there in the HashMap; create a new key-value pair
                List<Video> list = new ArrayList<>();
                list.add(video);
                groupedHashMap.put(hashMapKey, list);
            }
        }
        return groupedHashMap;
    }
}
