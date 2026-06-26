package com.badal.downloader;
import android.app.Service;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
    private Handler colourHandler;
    private String lastDetectedLink = "";
    private WindowManager.LayoutParams params;
    private float initialX;
    private float initialY;
    private float initialTouchX;
    private float initialTouchY;
    private int petSize;
    @Override
    public void onCreate() {
        super.onCreate();
        db = new DatabaseHelper(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        SharedPreferences prefs = getSharedPreferences("PetSettings", MODE_PRIVATE);
        petSize = prefs.getInt("size", 120);
        clipboardHandler = new Handler(Looper.getMainLooper());
        colourHandler = new Handler(Looper.getMainLooper());
        createFloatingWindow();
        startClipboardMonitor();
    }
    private void createFloatingWindow() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_pet, null);
        petImage = floatingView.findViewById(R.id.petImage);
        petImage.getLayoutParams().width = petSize;
        petImage.getLayoutParams().height = petSize;
        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }
        params = new WindowManager.LayoutParams(
            petSize,
            petSize,
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
                    params.x = (int)(initialX + event.getRawX() - initialTouchX);
                    params.y = (int)(initialY + event.getRawY() - initialTouchY);
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
                if (text.equals(lastDetectedLink)) return;
                lastDetectedLink = text;
                if (LinkDetector.isValidLink(text)) {
                    if (!db.linkExists(text)) {
                        autoAddLink(text);
                    }
                } else if (isUrl(text)) {
                    showInvalidLink();
                }
            }
        }
    }
    private boolean isUrl(String text) {
        return text.startsWith("http://") || text.startsWith("https://") || text.startsWith("www.");
    }
    private void autoAddLink(String link) {
        petImage.setImageResource(R.drawable.pet_alert);
        String platform = LinkDetector.detect(link);
        DownloadItem item = new DownloadItem(link, platform, "PENDING");
        db.addLink(item);
        Toast.makeText(this, platform + " auto-added!", Toast.LENGTH_SHORT).show();
        petImage.setImageResource(R.drawable.pet_success);
        colourHandler.postDelayed(() -> {
            petImage.setImageResource(R.drawable.pet_normal);
        }, 2000);
    }
    private void showInvalidLink() {
        petImage.setImageResource(R.drawable.pet_invalid);
        Toast.makeText(this, "Not a video link!", Toast.LENGTH_SHORT).show();
        colourHandler.postDelayed(() -> {
            petImage.setImageResource(R.drawable.pet_normal);
        }, 1500);
    }
    private void onPetClick() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("size")) {
            petSize = intent.getIntExtra("size", 120);
            params.width = petSize;
            params.height = petSize;
            petImage.getLayoutParams().width = petSize;
            petImage.getLayoutParams().height = petSize;
            windowManager.updateViewLayout(floatingView, params);
        }
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
        clipboardHandler.removeCallbacksAndMessages(null);
        colourHandler.removeCallbacksAndMessages(null);
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
