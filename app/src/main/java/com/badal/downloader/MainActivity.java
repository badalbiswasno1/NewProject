package com.badal.downloader;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private QueueAdapter adapter;
    private List<DownloadItem> queueList;
    private EditText linkInput;
    private DatabaseHelper db;
    private Handler clipboardHandler;
    private boolean isPetActive = false;
    private SharedPreferences prefs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = new DatabaseHelper(this);
        prefs = getSharedPreferences("PetSettings", MODE_PRIVATE);
        queueList = db.getAllLinks();
        linkInput = findViewById(R.id.linkInput);
        recyclerView = findViewById(R.id.recyclerView);
        Button addBtn = findViewById(R.id.addBtn);
        Button downloadAllBtn = findViewById(R.id.downloadAllBtn);
        Button clearBtn = findViewById(R.id.clearBtn);
        Button downloadedBtn = findViewById(R.id.downloadedBtn);
        Button petToggleBtn = findViewById(R.id.petToggleBtn);
        Button petSizeBtn = findViewById(R.id.petSizeBtn);
        adapter = new QueueAdapter(queueList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        addBtn.setOnClickListener(v -> addLink());
        downloadAllBtn.setOnClickListener(v -> showDownloadOptions());
        clearBtn.setOnClickListener(v -> clearQueue());
        downloadedBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, DownloadedActivity.class);
            startActivity(intent);
        });
        petToggleBtn.setOnClickListener(v -> togglePet());
        petSizeBtn.setOnClickListener(v -> showPetSizeDialog());
        clipboardHandler = new Handler(Looper.getMainLooper());
        clipboardHandler.postDelayed(clipboardRunnable, 1000);
    }
    private Runnable clipboardRunnable = new Runnable() {
        @Override
        public void run() {
            checkClipboard();
            clipboardHandler.postDelayed(this, 2000);
        }
    };
    private void checkClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                String text = clip.getItemAt(0).getText().toString();
                if (LinkDetector.isValidLink(text) && !db.linkExists(text)) {
                    linkInput.setText(text);
                }
            }
        }
    }
    private void addLink() {
        String link = linkInput.getText().toString().trim();
        if (link.isEmpty()) {
            Toast.makeText(this, "Paste a link first!", Toast.LENGTH_SHORT).show();
            return;
        }
        String platform = LinkDetector.detect(link);
        if (platform == null) {
            Toast.makeText(this, "Unsupported link!", Toast.LENGTH_SHORT).show();
            return;
        }
        DownloadItem item = new DownloadItem(link, platform, "PENDING");
        db.addLink(item);
        queueList.add(0, item);
        adapter.notifyItemInserted(0);
        linkInput.setText("");
        Toast.makeText(this, platform + " link added!", Toast.LENGTH_SHORT).show();
    }
    private void showDownloadOptions() {
        if (queueList.isEmpty()) {
            Toast.makeText(this, "Queue is empty!", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Download Options");
        builder.setItems(new String[]{"Download Now", "Download Later (WiFi)", "Schedule..."}, (dialog, which) -> {
            switch (which) {
                case 0:
                    startBatchDownload(false);
                    break;
                case 1:
                    startBatchDownload(true);
                    break;
                case 2:
                    Toast.makeText(this, "Schedule feature coming soon!", Toast.LENGTH_SHORT).show();
                    break;
            }
        });
        builder.show();
    }
    private void startBatchDownload(boolean wifiOnly) {
        int pendingCount = 0;
        for (DownloadItem item : queueList) {
            if (item.getStatus().equals("PENDING")) {
                if (wifiOnly) {
                    db.updateStatus(item.getId(), "WAITING_WIFI");
                    item.setStatus("WAITING_WIFI");
                } else {
                    Intent intent = new Intent(this, DownloadService.class);
                    intent.putExtra("link", item.getLink());
                    intent.putExtra("platform", item.getPlatform());
                    intent.putExtra("id", item.getId());
                    startService(intent);
                }
                pendingCount++;
            }
        }
        adapter.notifyDataSetChanged();
        if (pendingCount > 0) {
            String msg = wifiOnly ? pendingCount + " items waiting for WiFi!" : "Downloading " + pendingCount + " items...";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "All done!", Toast.LENGTH_SHORT).show();
        }
    }
    private void clearQueue() {
        db.clearAll();
        queueList.clear();
        adapter.notifyDataSetChanged();
        Toast.makeText(this, "Queue cleared!", Toast.LENGTH_SHORT).show();
    }
    private void togglePet() {
        if (!isPetActive) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 1001);
                return;
            }
            startService(new Intent(this, FloatingPetService.class));
            isPetActive = true;
            Toast.makeText(this, "Pet activated! Copy any link to auto-add.", Toast.LENGTH_SHORT).show();
        } else {
            stopService(new Intent(this, FloatingPetService.class));
            isPetActive = false;
            Toast.makeText(this, "Pet deactivated!", Toast.LENGTH_SHORT).show();
        }
    }
    private void showPetSizeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pet Size");
        View view = getLayoutInflater().inflate(R.layout.dialog_pet_size, null);
        SeekBar seekBar = view.findViewById(R.id.sizeSeekBar);
        TextView sizeText = view.findViewById(R.id.sizeText);
        int currentSize = prefs.getInt("size", 120);
        seekBar.setProgress(currentSize - 60);
        sizeText.setText(currentSize + "px");
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int size = progress + 60;
                sizeText.setText(size + "px");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        builder.setView(view);
        builder.setPositiveButton("Apply", (dialog, which) -> {
            int newSize = seekBar.getProgress() + 60;
            prefs.edit().putInt("size", newSize).apply();
            if (isPetActive) {
                Intent intent = new Intent(this, FloatingPetService.class);
                intent.putExtra("size", newSize);
                startService(intent);
            }
            Toast.makeText(this, "Pet size: " + newSize + "px", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startService(new Intent(this, FloatingPetService.class));
                isPetActive = true;
            }
        }
    }
    public void updateItemStatus(int id, String status) {
        for (int i = 0; i < queueList.size(); i++) {
            if (queueList.get(i).getId() == id) {
                queueList.get(i).setStatus(status);
                adapter.notifyItemChanged(i);
                break;
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        clipboardHandler.removeCallbacks(clipboardRunnable);
    }
    class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.ViewHolder> {
        private List<DownloadItem> items;
        QueueAdapter(List<DownloadItem> items) {
            this.items = items;
        }
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_queue, parent, false);
            return new ViewHolder(view);
        }
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            DownloadItem item = items.get(position);
            holder.platformText.setText(item.getPlatform());
            holder.linkText.setText(item.getLink());
            holder.statusText.setText(item.getStatus());
            int color;
            switch (item.getStatus()) {
                case "PENDING": color = 0xFF9E9E9E; break;
                case "DOWNLOADING": color = 0xFF2196F3; break;
                case "DONE": color = 0xFF4CAF50; break;
                case "FAILED": color = 0xFFF44336; break;
                case "WAITING_WIFI": color = 0xFFFF9800; break;
                default: color = 0xFF9E9E9E;
            }
            holder.statusText.setTextColor(color);
        }
        @Override
        public int getItemCount() {
            return items.size();
        }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView platformText, linkText, statusText;
            ViewHolder(View itemView) {
                super(itemView);
                platformText = itemView.findViewById(R.id.platformText);
                linkText = itemView.findViewById(R.id.linkText);
                statusText = itemView.findViewById(R.id.statusText);
            }
        }
    }
}
