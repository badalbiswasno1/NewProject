package com.badal.downloader;
import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
public class DownloadService extends IntentService {
    private DatabaseHelper db;
    public DownloadService() {
        super("DownloadService");
    }
    @Override
    public void onCreate() {
        super.onCreate();
        db = new DatabaseHelper(this);
    }
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) return;
        String link = intent.getStringExtra("link");
        String platform = intent.getStringExtra("platform");
        int id = intent.getIntExtra("id", -1);
        if (id == -1) return;
        if (db.isDownloaded(link)) {
            db.updateStatus(id, "DONE");
            return;
        }
        db.updateStatus(id, "DOWNLOADING");
        try {
            String filename = platform + "_" + System.currentTimeMillis();
            String outputPath = "/storage/emulated/0/Download/QueueDownloader/" + filename + ".%(ext)s";
            java.io.File dir = new java.io.File("/storage/emulated/0/Download/QueueDownloader/");
            dir.mkdirs();
            ProcessBuilder pb = new ProcessBuilder(
                "sh", "-c",
                "export PATH=$PATH:/data/data/com.termux/files/usr/bin && " +
                "yt-dlp -o '" + outputPath + "' '" + link + "' 2>&1"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                android.util.Log.d("DownloadService", line);
            }
            process.waitFor();
            if (process.exitValue() == 0) {
                db.addDownloaded(link, platform, filename, outputPath);
                db.updateStatus(id, "DONE");
            } else {
                db.updateStatus(id, "FAILED");
            }
            Intent updateIntent = new Intent("DOWNLOAD_UPDATE");
            updateIntent.putExtra("id", id);
            updateIntent.putExtra("status", process.exitValue() == 0 ? "DONE" : "FAILED");
            sendBroadcast(updateIntent);
        } catch (Exception e) {
            db.updateStatus(id, "FAILED");
            Intent updateIntent = new Intent("DOWNLOAD_UPDATE");
            updateIntent.putExtra("id", id);
            updateIntent.putExtra("status", "FAILED");
            sendBroadcast(updateIntent);
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
