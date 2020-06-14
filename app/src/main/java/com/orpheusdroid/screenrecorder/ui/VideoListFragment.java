package com.orpheusdroid.screenrecorder.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.orpheusdroid.screenrecorder.Config;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class VideoListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, VideoFragmentListener {

    private static VideoListFragment fragment;
    private VideoListViewModel mViewModel;
    private TextView message;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ShimmerFrameLayout shimmer;
    private RecyclerView videoList;
    private ArrayList<VideoListItem> consolidatedList = new ArrayList<>();
    private VideoListAdapter videoListAdapter;

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

        //checkPermissionAndLoadVideos();

        return view;
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

    private void checkPermissionAndLoadVideos() {
        /*if (getActivity() instanceof MainActivity) {
            if (! ((MainActivity) getActivity()).isVideoFragmentListernerInitialized())
                ((MainActivity) getActivity()).setVideoFragmentListener(this);
            ((MainActivity) getActivity()).init(Const.EXTDIR_REQUEST_CODE);
        }*/
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // request the permission
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Const.EXTDIR_REQUEST_CODE);
        } else {
            // has the permission.
            listVideos();
        }
    }

    private void listVideos() {
        File directory = new File(Config.getInstance(getActivity()).getSaveLocation());
        //Remove directory pointers and other files from the list
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                Toast.makeText(getActivity(), "The directory creation failed.", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d(Const.TAG, "Directory missing! Creating dir");
        }

        if (!directory.canRead() && !directory.canWrite()) {
            Toast.makeText(getActivity(), "The directory is neither readable nor writable", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<File> filesList = new ArrayList<File>();
        if (directory.isDirectory() && directory.exists()) {
            Log.d(Const.TAG, "Getting videos for " + directory.getAbsolutePath());
            filesList.addAll(Arrays.asList(getVideos(directory.listFiles())));
        }

        new GetVideosAsync().execute(filesList.toArray(new File[0]));

        Log.d(Const.TAG, "Fetching data");
    }

    /**
     * Filter all video files from array of files
     *
     * @param files File[] containing files from a directory
     * @return File[] containing only video files
     */
    private File[] getVideos(File[] files) {
        List<File> newFiles = new ArrayList<>();
        for (File file : files) {
            if (!file.isDirectory() && isVideoFile(file.getPath()))
                newFiles.add(file);
        }
        return newFiles.toArray(new File[newFiles.size()]);
    }


    /*@Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Log.d(Const.TAG, "Video Activity created");

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(getString(R.string.title_videos));

        //mViewModel = ViewModelProviders.of(this).get(VideoListViewModel.class);
        // TODO: Use the ViewModel
    }*/

    @Override
    public void onRefresh() {
        videoList.setVisibility(View.GONE);
        shimmer.setVisibility(View.VISIBLE);
        shimmer.showShimmer(true);
        listVideos();
    }

    private void setAdapter(ArrayList<VideoListItem> videos) {
        shimmer.stopShimmer();
        shimmer.setVisibility(View.GONE);
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
        if (result)
            listVideos();
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
                    listVideos();
                }
        }
    }

    class GetVideosAsync extends AsyncTask<File[], Integer, ArrayList<VideoListItem>> {
        File[] files;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //Set refreshing to true
            consolidatedList = new ArrayList<>();
            //swipeRefreshLayout.setRefreshing(false);
        }

        @Override
        protected void onPostExecute(ArrayList<VideoListItem> videos) {
            //If the directory has no videos, remove recyclerview from rootview and show empty message.
            // Else set recyclerview and remove message textview
            if (videos.isEmpty()) {
                shimmer.stopShimmer();
                shimmer.setVisibility(View.GONE);
                videoList.setVisibility(View.GONE);
                message.setVisibility(View.VISIBLE);
            } else {
                //Sort the videos in a descending order
                //Collections.reverse(videos);
                setAdapter(videos);
            }
            //Finish refreshing
            swipeRefreshLayout.setRefreshing(false);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            Log.d(Const.TAG, "Progress is :" + values[0]);
        }

        @Override
        protected ArrayList<VideoListItem> doInBackground(File[]... arg) {
            //Get video file name, Uri and video thumbnail from mediastore
            files = arg[0];
            ArrayList<Video> videosList = new ArrayList<>();
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (!file.isDirectory() && isVideoFile(file.getPath())) {
                    Date lastModified = new Date(file.lastModified());
                    /*SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    try {
                        lastModified = sdf.parse(sdf.format(lastModified));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }*/
                    Video video = new Video(file.getName(),
                            file,
                            lastModified
                    );
                    //getBitmap(file, video);
                    videosList.add(video);
                    //Update progress dialog
                    publishProgress(i);
                }
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
                //consolidatedList.add(dateItem);
            }

            return consolidatedList;
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
}
