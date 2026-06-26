package com.badal.downloader;
public class DownloadedItem {
    private int id;
    private String link;
    private String platform;
    private String title;
    private String filepath;
    private String downloadedAt;
    public DownloadedItem(int id, String link, String platform, String title, String filepath, String downloadedAt) {
        this.id = id;
        this.link = link;
        this.platform = platform;
        this.title = title;
        this.filepath = filepath;
        this.downloadedAt = downloadedAt;
    }
    public int getId() { return id; }
    public String getLink() { return link; }
    public String getPlatform() { return platform; }
    public String getTitle() { return title; }
    public String getFilepath() { return filepath; }
    public String getDownloadedAt() { return downloadedAt; }
}
