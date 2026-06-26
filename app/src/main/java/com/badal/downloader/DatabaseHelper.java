package com.badal.downloader;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "downloader.db";
    private static final int DB_VERSION = 2;
    private static final String TABLE_QUEUE = "queue";
    private static final String TABLE_DOWNLOADED = "downloaded";
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_QUEUE + " (id INTEGER PRIMARY KEY AUTOINCREMENT, link TEXT UNIQUE, platform TEXT, status TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_DOWNLOADED + " (id INTEGER PRIMARY KEY AUTOINCREMENT, link TEXT UNIQUE, platform TEXT, title TEXT, filepath TEXT, downloaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE " + TABLE_DOWNLOADED + " (id INTEGER PRIMARY KEY AUTOINCREMENT, link TEXT UNIQUE, platform TEXT, title TEXT, filepath TEXT, downloaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }
    }
    public void addLink(DownloadItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("link", item.getLink());
        cv.put("platform", item.getPlatform());
        cv.put("status", item.getStatus());
        db.insertWithOnConflict(TABLE_QUEUE, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
    }
    public List<DownloadItem> getAllLinks() {
        List<DownloadItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_QUEUE + " ORDER BY id DESC", null);
        while (cursor.moveToNext()) {
            list.add(new DownloadItem(
                cursor.getInt(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3)
            ));
        }
        cursor.close();
        db.close();
        return list;
    }
    public boolean linkExists(String link) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_QUEUE + " WHERE link=?", new String[]{link});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }
    public boolean isDownloaded(String link) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_DOWNLOADED + " WHERE link=?", new String[]{link});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }
    public void updateStatus(int id, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("status", status);
        db.update(TABLE_QUEUE, cv, "id=?", new String[]{String.valueOf(id)});
        db.close();
    }
    public void updateLink(int id, String newLink) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("link", newLink);
        db.update(TABLE_QUEUE, cv, "id=?", new String[]{String.valueOf(id)});
        db.close();
    }
    public void deleteQueueItem(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_QUEUE, "id=?", new String[]{String.valueOf(id)});
        db.close();
    }
    public void clearAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_QUEUE, null, null);
        db.close();
    }
    public void addDownloaded(String link, String platform, String title, String filepath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("link", link);
        cv.put("platform", platform);
        cv.put("title", title);
        cv.put("filepath", filepath);
        db.insertWithOnConflict(TABLE_DOWNLOADED, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
    }
    public List<DownloadedItem> getDownloaded() {
        List<DownloadedItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_DOWNLOADED + " ORDER BY downloaded_at DESC LIMIT 500", null);
        while (cursor.moveToNext()) {
            list.add(new DownloadedItem(
                cursor.getInt(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getString(4),
                cursor.getString(5)
            ));
        }
        cursor.close();
        db.close();
        return list;
    }
    public void deleteDownloaded(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_DOWNLOADED, "id=?", new String[]{String.valueOf(id)});
        db.close();
    }
}
