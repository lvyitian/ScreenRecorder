package com.orpheusdroid.screenrecorder.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.orpheusdroid.screenrecorder.R;
import com.orpheusdroid.screenrecorder.adapter.models.Apps;

import java.util.ArrayList;

public class AppsListAdapter extends RecyclerView.Adapter<AppsListAdapter.SimpleViewHolder> {
    private ArrayList<Apps> apps;
    private OnItemClicked onClick;

    public AppsListAdapter(ArrayList<Apps> apps) {
        this.apps = apps;
    }

    @Override
    public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.content_apps_list_preference, parent, false);
        return new SimpleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final SimpleViewHolder holder, final int position) {
        Apps app = apps.get(holder.getAdapterPosition());
        holder.textView.setText("" + app.getAppName());
        holder.appIcon.setImageDrawable(app.getAppIcon());

        // Show a visible tick mark for the selected app
        if (app.isSelectedApp())
            holder.selectedApp.setVisibility(View.VISIBLE);
        else
            holder.selectedApp.setVisibility(View.INVISIBLE);
        holder.app.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClick.onItemClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    public void setOnClick(OnItemClicked onClick) {
        this.onClick = onClick;
    }

    // Interface to handle recycler view item click
    public interface OnItemClicked {
        void onItemClick(int position);
    }

    // A static view holder class to hold the view items used by the recycler view
    static class SimpleViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView appIcon;
        ImageView selectedApp;
        RelativeLayout app;

        SimpleViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.appName);
            appIcon = itemView.findViewById(R.id.appIcon);
            selectedApp = itemView.findViewById(R.id.appChecked);
            app = itemView.findViewById(R.id.app);
        }
    }
}
