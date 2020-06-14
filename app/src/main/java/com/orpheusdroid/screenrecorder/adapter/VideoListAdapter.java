package com.orpheusdroid.screenrecorder.adapter;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.orpheusdroid.crashreporter.reporter.FilesProvider;
import com.orpheusdroid.screenrecorder.Const;
import com.orpheusdroid.screenrecorder.R;
import com.orpheusdroid.screenrecorder.adapter.models.videolist.VideoHeader;
import com.orpheusdroid.screenrecorder.adapter.models.videolist.VideoItem;
import com.orpheusdroid.screenrecorder.adapter.models.videolist.VideoListItem;
import com.orpheusdroid.screenrecorder.utils.Log;

import java.util.ArrayList;

public class VideoListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ActionMode.Callback {
    private ArrayList<VideoListItem> videos = new ArrayList<>();
    private AppCompatActivity context;

    private boolean isMultiselect;
    private ArrayList<VideoItem> selectedVideoPositions = new ArrayList<>();
    private ActionMode actionMode;


    public VideoListAdapter(ArrayList<VideoListItem> videos, AppCompatActivity context) {
        this.videos = videos;
        this.context = context;
    }

    public void setVideos(ArrayList<VideoListItem> videos) {
        this.videos = videos;
    }

    @Override
    public int getItemViewType(int position) {
        return videos.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case VideoListItem.TYPE_HEADER:
                View v1 = inflater.inflate(R.layout.item_video_header, parent,
                        false);
                viewHolder = new VideoHeaderHolder(v1);
                break;

            case VideoListItem.TYPE_VIDEO:
                View v2 = inflater.inflate(R.layout.item_video_videolist, parent, false);
                viewHolder = new VideoListHolder(v2);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {

            case VideoListItem.TYPE_HEADER:

                VideoHeader headerItem = (VideoHeader) videos.get(position);
                VideoHeaderHolder generalViewHolder = (VideoHeaderHolder) holder;
                /*DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
                LocalDate timestamp = headerItem.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();*/
                java.text.DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(context);

                generalViewHolder.header.setText(dateFormat.format(headerItem.getDate()));
                break;

            case VideoListItem.TYPE_VIDEO:
                VideoItem videoItem = (VideoItem) videos.get(position);
                VideoListHolder videoViewHolder = (VideoListHolder) holder;

                videoViewHolder.fileName.setText(videoItem.getVideo().getFileName());

                Glide.with(context)
                        .load(videoItem.getVideo().getFile())
                        .centerCrop()
                        .into(videoViewHolder.thumbnail);

                videoViewHolder.videoOverflowMenu.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PopupMenu menu = new PopupMenu(context, videoViewHolder.videoOverflowMenu);
                        menu.getMenuInflater().inflate(R.menu.video_list_menu, menu.getMenu());
                        //Toast.makeText(context,menu.getMenu().getItem(0).getTitle(), Toast.LENGTH_SHORT).show();
                        menu.getMenu().getItem(0).setVisible(true);
                        menu.setOnMenuItemClickListener(menuItem -> handlePopupMenu(menuItem, videoItem, position));
                        menu.show();
                    }
                });

                videoViewHolder.videoThumbLayout.setOnClickListener(view -> {
                    if (isMultiselect) {
                        selectItem(videoViewHolder, videoItem);
                        return;
                    }
                    videoItem.getVideo().getFile();
                    videoItem.getType();
                    Log.d(Const.TAG, "Video clicked");
                    Intent videoPlayer = new Intent(Intent.ACTION_VIEW);
                    videoPlayer.setDataAndType(Uri.parse(videoItem.getVideo().getFile().getAbsolutePath()), "video/mp4");
                    context.startActivity(videoPlayer);
                });

                videoViewHolder.videoThumbLayout.setOnLongClickListener(view -> {
                    if (!isMultiselect) {
                        isMultiselect = true;
                        Log.d(Const.TAG, "Long clicked");
                        Log.d(Const.TAG, position + "");
                        actionMode = context.startSupportActionMode(this);
                        selectItem(videoViewHolder, videoItem);
                        return true;
                    }
                    return false;
                });
                break;
        }
    }

    private boolean handlePopupMenu(MenuItem menuItem, VideoItem videoItem, int position) {
        switch (menuItem.getItemId()) {
            case R.id.action_share:
                Uri uri = FilesProvider.getUriForFile(context.getApplicationContext(), "com.orpheusdroid.screenrecorder.fileprovider",
                        videoItem.getVideo().getFile());
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType(Intent.normalizeMimeType("video/mp4"));
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Intent chooserIntent = Intent.createChooser(intent, context.getString(com.orpheusdroid.crashreporter.R.string.intent_share_title));
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(chooserIntent);
                break;
            case R.id.action_delete:
                videoItem.getVideo().getFile().delete();
                videos.remove(videoItem);
                notifyItemRemoved(position);
                break;
        }
        return false;
    }


    private void selectItem(VideoListHolder holder, VideoItem videoItem) {
        // If the "selectedItems" list contains the item, remove it and set it's state to normal
        if (selectedVideoPositions.contains(videoItem)) {
            selectedVideoPositions.remove(videoItem);
            holder.videoItemParent.setAlpha(1.0f);
        } else {
            // Else, add it to the list and add a darker shade over the image, letting the user know that it was selected
            selectedVideoPositions.add(videoItem);
            holder.videoItemParent.setAlpha(0.3f);
        }
        if (selectedVideoPositions.size() == 0) {
            isMultiselect = false;
            if (actionMode != null)
                actionMode.finish();
        } else {
            actionMode.setTitle(context.getResources().getQuantityString(R.plurals.video_action_mode_title, selectedVideoPositions.size(), selectedVideoPositions.size()));
        }
    }

    @Override
    public int getItemCount() {
        return videos != null ? videos.size() : 0;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.video_list_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu.removeItem(1);
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            for (VideoItem selectedVideo :
                    selectedVideoPositions) {
                Log.d(Const.TAG, "Delete video at pos: " + selectedVideo.getVideo().getFileName());
                selectedVideo.getVideo().getFile().delete();
                videos.remove(selectedVideo);
            }
        }
        mode.finish();
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        isMultiselect = false;
        selectedVideoPositions.clear();
        notifyDataSetChanged();
        Log.d(Const.TAG, "Notified data set changed");
    }

    private static final class VideoHeaderHolder extends RecyclerView.ViewHolder {
        private AppCompatTextView header;

        VideoHeaderHolder(@NonNull View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.videoList_header);
        }
    }

    private static final class VideoListHolder extends RecyclerView.ViewHolder {
        private ConstraintLayout videoItemParent;
        private TextView fileName;
        private ImageView thumbnail;
        private ImageButton videoOverflowMenu;
        private RelativeLayout videoThumbLayout;

        VideoListHolder(@NonNull View itemView) {
            super(itemView);
            videoItemParent = itemView.findViewById(R.id.video_item_parent);
            videoThumbLayout = itemView.findViewById(R.id.videoThumb_layout);
            fileName = itemView.findViewById(R.id.fileName);
            thumbnail = itemView.findViewById(R.id.videoThumb);
            videoOverflowMenu = itemView.findViewById(R.id.thumbnailOverflow);
        }
    }
}
