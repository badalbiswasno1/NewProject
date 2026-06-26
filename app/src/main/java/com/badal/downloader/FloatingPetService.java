package com.badal.downloader;
import android.app.Service;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
public class FloatingPetService extends Service {
    private WindowManager windowManager;
    private View floatingView;
    private ImageView petImage;
    private DatabaseHelper db;
    private Handler clipboardHandler;
    private String lastDetectedLink = "";
    private WindowManager.LayoutParams params;
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    @Override
    public void onCreate() {
        super.onCreate();
        db = new DatabaseHelper(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        clipboardHandler = new Handler(Looper.getMainLooper());
        createFloatingWindow();
        startClipboardMonitor();
    }
    private void createFloatingWindow() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_pet, null);
        petImage = floatingView.findViewById(R.id.petImage);
        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }
        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;
        petImage.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    params.x = initialX + (int)(event.getRawX() - initialTouchX);
                    params.y = initialY + (int)(event.getRawY() - initialTouchY);
                    windowManager.updateViewLayout(floatingView, params);
                    return true;
                case MotionEvent.ACTION_UP:
                    float diffX = event.getRawX() - initialTouchX;
                    float diffY = event.getRawY() - initialTouchY;
                    if (Math.abs(diffX) < 10 && Math.abs(diffY) < 10) {
                        onPetClick();
                    }
                    return true;
            }
            return false;
        });
        windowManager.addView(floatingView, params);
    }
    private void startClipboardMonitor() {
        clipboardHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkClipboard();
                clipboardHandler.postDelayed(this, 1500);
            }
        }, 1500);
    }
    private void checkClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                String text = clip.getItemAt(0).getText().toString();
                if (LinkDetector.isValidLink(text) && !text.equals(lastDetectedLink) && !db.linkExists(text)) {
                    lastDetectedLink = text;
                    showLinkDetected(text);
                }
            }
        }
    }
    private void showLinkDetected(String link) {
        petImage.setImageResource(R.drawable.pet_alert);
        Toast.makeText(this, "Link detected! Tap pet to add", Toast.LENGTH_SHORT).show();
    }
    private void onPetClick() {
        if (lastDetectedLink != null && !lastDetectedLink.isEmpty() && LinkDetector.isValidLink(lastDetectedLink)) {
            String platform = LinkDetector.detect(lastDetectedLink);
            DownloadItem item = new DownloadItem(lastDetectedLink, platform, "PENDING");
            db.addLink(item);
            petImage.setImageResource(R.drawable.pet_normal);
            Toast.makeText(this, platform + " link added to queue!", Toast.LENGTH_SHORT).show();
            lastDetectedLink = "";
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Toast.makeText(this, "No new link detected!", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
        clipboardHandler.removeCallbacksAndMessages(null);
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
