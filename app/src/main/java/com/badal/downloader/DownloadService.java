package com.badal.downloader;
import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONObject;
public class DownloadService extends IntentService {
    private static final String TAG = "DownloadService";
    private DatabaseHelper db;
    private File debugLogFile;
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
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "QueueDownloader");
        dir.mkdirs();
        debugLogFile = new File(dir, "debug.log");
        if (db.isDownloaded(link)) {
            db.updateStatus(id, "DONE");
            return;
        }
        db.updateStatus(id, "DOWNLOADING");
        writeDebugLog("===== NEW DOWNLOAD ATTEMPT =====");
        writeDebugLog("Link: " + link + " Platform: " + platform);
        try {
            Log.d(TAG, "Starting download for: " + link);
            String downloadUrl = getDownloadUrl(link);
            if (downloadUrl == null) {
                Log.e(TAG, "No download URL found");
                writeDebugLog("RESULT: FAILED - no API returned a usable URL");
                db.updateStatus(id, "FAILED");
                sendUpdate(id, "FAILED");
                return;
            }
            Log.d(TAG, "Download URL: " + downloadUrl);
            String ext = downloadUrl.contains(".jpg") || downloadUrl.contains(".jpeg") ? ".jpg" : ".mp4";
            String filename = platform + "_" + System.currentTimeMillis() + ext;
            File file = new File(dir, filename);
            downloadFile(downloadUrl, file);
            db.addDownloaded(link, platform, filename, file.getAbsolutePath());
            db.updateStatus(id, "DONE");
            sendUpdate(id, "DONE");
            Log.d(TAG, "Download complete: " + file.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Download error: " + e.getMessage());
            writeDebugLog("RESULT: FAILED - exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            db.updateStatus(id, "FAILED");
            sendUpdate(id, "FAILED");
        }
    }
    private void writeDebugLog(String msg) {
        try {
            if (debugLogFile == null) return;
            FileWriter fw = new FileWriter(debugLogFile, true);
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            fw.write("[" + time + "] " + msg + "\n");
            fw.close();
        } catch (IOException e) {
        }
    }
    private String getDownloadUrl(String videoUrl) {
        String result = tryApi1(videoUrl);
        if (result != null) return result;
        result = tryApi2(videoUrl);
        if (result != null) return result;
        result = tryApi3(videoUrl);
        return result;
    }
    private String tryApi1(String videoUrl) {
        try {
            String api = "https://fastdl.app/api/convert?url=" + URLEncoder.encode(videoUrl, "UTF-8");
            Log.d(TAG, "Trying API1: " + api);
            writeDebugLog("API1 (fastdl): " + api);
            URL url = new URL(api);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10)");
            int code = conn.getResponseCode();
            Log.d(TAG, "API1 response: " + code);
            writeDebugLog("API1 response code: " + code);
            if (code != 200) return null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            String response = sb.toString();
            Log.d(TAG, "API1 body: " + response.substring(0, Math.min(200, response.length())));
            writeDebugLog("API1 body: " + response.substring(0, Math.min(300, response.length())));
            JSONObject json = new JSONObject(response);
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
            Log.e(TAG, "API1 error: " + e.getMessage());
            writeDebugLog("API1 error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return null;
        }
    }
    private String tryApi2(String videoUrl) {
        try {
            String api = "https://snapxapi.com/v1/download?url=" + URLEncoder.encode(videoUrl, "UTF-8");
            Log.d(TAG, "Trying API2: " + api);
            writeDebugLog("API2 (snapxapi): " + api);
            URL url = new URL(api);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            int code = conn.getResponseCode();
            Log.d(TAG, "API2 response: " + code);
            writeDebugLog("API2 response code: " + code);
            if (code != 200) return null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            String response = sb.toString();
            Log.d(TAG, "API2 body: " + response.substring(0, Math.min(200, response.length())));
            JSONObject json = new JSONObject(response);
            if (json.has("data")) {
                JSONObject data = json.getJSONObject("data");
                if (data.has("url")) return data.getString("url");
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "API2 error: " + e.getMessage());
            writeDebugLog("API2 error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return null;
        }
    }
    private String tryApi3(String videoUrl) {
        try {
            Log.d(TAG, "Trying API3");
            writeDebugLog("API3 (save-from): https://save-from.net/api/convert");
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
            int code = conn.getResponseCode();
            Log.d(TAG, "API3 response: " + code);
            writeDebugLog("API3 response code: " + code);
            if (code != 200) return null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            String response = sb.toString();
            Log.d(TAG, "API3 body: " + response.substring(0, Math.min(200, response.length())));
            JSONObject json = new JSONObject(response);
            if (json.has("url")) return json.getString("url");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "API3 error: " + e.getMessage());
            writeDebugLog("API3 error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return null;
        }
    }
    private void downloadFile(String fileUrl, File outputFile) {
        try {
            Log.d(TAG, "Downloading from: " + fileUrl);
            URL url = new URL(fileUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10)");
            InputStream is = conn.getInputStream();
            FileOutputStream fos = new FileOutputStream(outputFile);
            byte[] buffer = new byte[8192];
            int len;
            long total = 0;
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
                total += len;
            }
            fos.flush();
            fos.close();
            is.close();
            Log.d(TAG, "Downloaded bytes: " + total);
        } catch (Exception e) {
            Log.e(TAG, "Download file error: " + e.getMessage());
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
