package com.badal.downloader;

public class DownloadItem {
    private int id;
    private String link;
    private String platform;
    private String status;

    public DownloadItem(int id, String link, String platform, String status) {
        this.id = id;
        this.link = link;
        this.platform = platform;
        this.status = status;
    }

    public DownloadItem(String link, String platform, String status) {
        this.link = link;
        this.platform = platform;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getLink() { return link; }
    public String getPlatform() { return platform; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
