package com.orpheusdroid.screenrecorder.adapter.models.videolist;

import android.graphics.Bitmap;
import android.net.Uri;

import java.io.File;
import java.util.Date;

public class Video implements Comparable<Video> {
    private String FileName;
    private File file;
    private Uri fileUri;
    private Bitmap thumbnail;
    private Date lastModified;
    private boolean isSection = false;
    private boolean isSelected = false;

    public Video() {

    }

    public Video(boolean isSection, Date lastModified) {
        this.isSection = isSection;
        this.lastModified = lastModified;
    }

    public Video(String fileName, File file, Date lastModified) {
        this.FileName = fileName;
        this.file = file;
        this.lastModified = lastModified;
    }

    public String getFileName() {
        return FileName;
    }

    public void setFileName(String fileName) {
        FileName = fileName;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public Uri getFileUri() {
        return fileUri;
    }

    public void setFileUri(Uri fileUri) {
        this.fileUri = fileUri;
    }

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = new Date(lastModified * 1000);
    }

    public boolean isSection() {
        return isSection;
    }

    public void setSection(boolean section) {
        isSection = section;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    @Override
    public int compareTo(Video o) {
        return getLastModified().compareTo(o.getLastModified());
    }

    @Override
    public String toString() {
        return "Video{" +
                "FileName='" + FileName + '\'' +
                ", file=" + fileUri +
                ", filePath=" + fileUri.getPath() +
                ", thumbnail=" + thumbnail +
                ", lastModified=" + lastModified +
                ", isSection=" + isSection +
                ", isSelected=" + isSelected +
                '}';
    }
}
