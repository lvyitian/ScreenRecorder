package com.orpheusdroid.screenrecorder.adapter.models.videolist;

import java.util.Date;

public class VideoHeader extends VideoListItem {
    private Date Date;

    public Date getDate() {
        return Date;
    }

    public void setDate(java.util.Date date) {
        Date = date;
    }

    @Override
    public int getType() {
        return TYPE_HEADER;
    }

    @Override
    public String toString() {
        return "VideoHeader{" +
                "Date='" + Date + '\'' +
                '}';
    }
}
