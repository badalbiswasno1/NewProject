package com.badal.downloader;
import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import androidx.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import org.json.JSONArray;
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
            String downloadUrl = getFastDlUrl(link);
            if (downloadUrl == null) {
                downloadUrl = getSnapUrl(link);
            }
            if (downloadUrl == null) {
                downloadUrl = getSaveFromUrl(link);
            }
            if (downloadUrl == null) {
                db.updateStatus(id, "FAILED");
                sendUpdate(id, "FAILED");
                return;
            }
            String ext = downloadUrl.contains(".mp4") ? ".mp4" : ".jpg";
            String filename = platform + "_" + System.currentTimeMillis() + ext;
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "QueueDownloader");
            dir.mkdirs();
            File file = new File(dir, filename);
            downloadFile(downloadUrl, file);
            db.addDownloaded(link, platform, filename, file.getAbsolutePath());
            db.updateStatus(id, "DONE");
            sendUpdate(id, "DONE");
        } catch (Exception e) {
            db.updateStatus(id, "FAILED");
            sendUpdate(id, "FAILED");
        }
    }
    private String getFastDlUrl(String videoUrl) {
        try {
            String api = "https://fastdl.app/api/convert?url=" + URLEncoder.encode(videoUrl, "UTF-8");
            URL url = new URL(api);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject json = new JSONObject(sb.toString());
            if (json.has("url")) return json.getString("url");
            if (json.has("download_url")) return json.getString("download_url");
            if (json.has("medias")) {
                JSONArray medias = json.getJSONArray("medias");
                if (medias.length() > 0) {
                    JSONObject media = medias.getJSONObject(0);
                    if (media.has("url")) return media.getString("url");
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    private String getSnapUrl(String videoUrl) {
        try {
            String api = "https://snapxapi.com/v1/download?url=" + URLEncoder.encode(videoUrl, "UTF-8");
            URL url = new URL(api);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject json = new JSONObject(sb.toString());
            if (json.has("data")) {
                JSONObject data = json.getJSONObject("data");
                if (data.has("url")) return data.getString("url");
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    private String getSaveFromUrl(String videoUrl) {
        try {
            URL url = new URL("https://save-from.net/api/convert");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String postData = "url=" + URLEncoder.encode(videoUrl, "UTF-8");
            OutputStream os = conn.getOutputStream();
            os.write(postData.getBytes());
            os.flush();
            os.close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
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
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
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
    private void sendUpdate(int id, String status) {
        Intent updateIntent = new Intent("DOWNLOAD_UPDATE");
        updateIntent.putExtra("id", id);
        updateIntent.putExtra("status", status);
        sendBroadcast(updateIntent);
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
