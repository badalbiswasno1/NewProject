package com.badal.downloader;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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

        db.updateStatus(id, "DOWNLOADING");

        try {
            String termuxCmd = "am startservice -n com.termux/com.termux.app.TermuxService -a com.termux.RUN_COMMAND --es com.termux.RUN_COMMAND_PATH /data/data/com.termux/files/usr/bin/yt-dlp --esa com.termux.RUN_COMMAND_ARGUMENTS '-o,/storage/emulated/0/Download/%(title)s.%(ext)s," + link + "'";
            Runtime.getRuntime().exec(termuxCmd);

            Thread.sleep(3000);

            db.updateStatus(id, "DONE");

            Intent updateIntent = new Intent("DOWNLOAD_UPDATE");
            updateIntent.putExtra("id", id);
            updateIntent.putExtra("status", "DONE");
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
