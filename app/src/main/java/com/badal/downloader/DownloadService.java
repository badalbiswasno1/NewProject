package com.badal.downloader;
import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import androidx.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import org.json.JSONObject;
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
            String downloadUrl = getDownloadUrl(link);
            if (downloadUrl == null) {
                db.updateStatus(id, "FAILED");
                return;
            }
            String filename = platform + "_" + System.currentTimeMillis() + ".mp4";
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "QueueDownloader");
            dir.mkdirs();
            File file = new File(dir, filename);
            downloadFile(downloadUrl, file);
            db.addDownloaded(link, platform, filename, file.getAbsolutePath());
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
    private String getDownloadUrl(String videoUrl) {
        try {
            String apiUrl = "https://api.snapx.com/download?url=" + URLEncoder.encode(videoUrl, "UTF-8");
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            InputStream is = new BufferedInputStream(conn.getInputStream());
            StringBuilder sb = new StringBuilder();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, len));
            }
            is.close();
            JSONObject json = new JSONObject(sb.toString());
            if (json.has("url")) return json.getString("url");
            if (json.has("download_url")) return json.getString("download_url");
            if (json.has("video_url")) return json.getString("video_url");
            return null;
        } catch (Exception e) {
            return tryBackupApi(videoUrl);
        }
    }
    private String tryBackupApi(String videoUrl) {
        try {
            String apiUrl = "https://save-from.net/api/convert?url=" + URLEncoder.encode(videoUrl, "UTF-8");
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            InputStream is = new BufferedInputStream(conn.getInputStream());
            StringBuilder sb = new StringBuilder();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, len));
            }
            is.close();
            JSONObject json = new JSONObject(sb.toString());
            if (json.has("url")) return json.getString("url");
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    private void downloadFile(String fileUrl, File outputFile) {
        try {
            URL url = new URL(fileUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            InputStream is = new BufferedInputStream(conn.getInputStream());
            FileOutputStream fos = new FileOutputStream(outputFile);
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.flush();
            fos.close();
            is.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
