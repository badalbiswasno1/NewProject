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
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private RecyclerView recyclerView;
    private QueueAdapter adapter;
    private List<DownloadItem> queueList;
    private List<Object> displayList = new ArrayList<>();
    private EditText linkInput;
    private DatabaseHelper db;
    private Handler clipboardHandler;
    private boolean isPetActive = false;
    private SharedPreferences prefs;
    private String lastClipboardText = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = new DatabaseHelper(this);
        prefs = getSharedPreferences("PetSettings", MODE_PRIVATE);
        queueList = db.getAllLinks();
        rebuildDisplayList();
        linkInput = findViewById(R.id.linkInput);
        recyclerView = findViewById(R.id.recyclerView);
        Button addBtn = findViewById(R.id.addBtn);
        Button downloadAllBtn = findViewById(R.id.downloadAllBtn);
        Button clearBtn = findViewById(R.id.clearBtn);
        Button downloadedBtn = findViewById(R.id.downloadedBtn);
        Button petToggleBtn = findViewById(R.id.petToggleBtn);
        Button petSizeBtn = findViewById(R.id.petSizeBtn);
        adapter = new QueueAdapter(displayList, this);
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
                CharSequence csText = clip.getItemAt(0).getText();
                if (csText == null) return;
                String text = csText.toString().trim();
                if (text.equals(lastClipboardText)) return;
                lastClipboardText = text;
                String platform = LinkDetector.detect(text);
                if (platform != null && !db.linkExists(text)) {
                    DownloadItem item = new DownloadItem(text, platform, "PENDING");
                    db.addLink(item);
                    queueList.add(0, item);
                    rebuildDisplayList();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, platform + " link added!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    private void rebuildDisplayList() {
        displayList.clear();
        String[] order = {"FACEBOOK", "INSTAGRAM", "YOUTUBE", "TWITTER", "TIKTOK", "THREADS"};
        for (String platform : order) {
            List<DownloadItem> group = new ArrayList<>();
            for (DownloadItem item : queueList) {
                if (item.getPlatform().equals(platform)) {
                    group.add(item);
                }
            }
            if (!group.isEmpty()) {
                displayList.add(platform + " (" + group.size() + ")");
                displayList.addAll(group);
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
        if (db.linkExists(link)) {
            Toast.makeText(this, "Already in queue!", Toast.LENGTH_SHORT).show();
            return;
        }
        DownloadItem item = new DownloadItem(link, platform, "PENDING");
        db.addLink(item);
        queueList.add(0, item);
        rebuildDisplayList();
        adapter.notifyDataSetChanged();
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
        builder.setItems(new String[]{"Download Now", "Download Later (WiFi)"}, (dialog, which) -> {
            if (which == 0) {
                startBatchDownload(false);
            } else {
                startBatchDownload(true);
            }
        });
        builder.show();
    }
    private void startBatchDownload(boolean wifiOnly) {
        int pendingCount = 0;
        for (DownloadItem item : queueList) {
            if (item.getStatus().equals("PENDING") || item.getStatus().equals("WAITING_WIFI")) {
                if (wifiOnly) {
                    db.updateStatus(item.getId(), "WAITING_WIFI");
                    item.setStatus("WAITING_WIFI");
                } else {
                    db.updateStatus(item.getId(), "DOWNLOADING");
                    item.setStatus("DOWNLOADING");
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
        rebuildDisplayList();
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
            Toast.makeText(this, "Pet activated!", Toast.LENGTH_SHORT).show();
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
    public void showItemMenu(DownloadItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(item.getPlatform());
        builder.setItems(new String[]{"Copy Link", "Edit Link", "Delete", "Download Now"}, (dialog, which) -> {
            switch (which) {
                case 0:
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("link", item.getLink());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Link copied!", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    showEditDialog(item);
                    break;
                case 2:
                    db.deleteQueueItem(item.getId());
                    queueList.remove(item);
                    rebuildDisplayList();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Deleted!", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    Intent intent = new Intent(this, DownloadService.class);
                    intent.putExtra("link", item.getLink());
                    intent.putExtra("platform", item.getPlatform());
                    intent.putExtra("id", item.getId());
                    startService(intent);
                    break;
            }
        });
        builder.show();
    }
    private void showEditDialog(DownloadItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Link");
        EditText input = new EditText(this);
        input.setText(item.getLink());
        builder.setView(input);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newLink = input.getText().toString().trim();
            if (LinkDetector.isValidLink(newLink) && !db.linkExists(newLink)) {
                db.updateLink(item.getId(), newLink);
                item.setLink(newLink);
                rebuildDisplayList();
                adapter.notifyDataSetChanged();
                Toast.makeText(this, "Updated!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Invalid or duplicate!", Toast.LENGTH_SHORT).show();
            }
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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        clipboardHandler.removeCallbacks(clipboardRunnable);
    }
    class QueueAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<Object> items;
        private MainActivity activity;
        QueueAdapter(List<Object> items, MainActivity activity) {
            this.items = items;
            this.activity = activity;
        }
        @Override
        public int getItemViewType(int position) {
            return (items.get(position) instanceof String) ? TYPE_HEADER : TYPE_ITEM;
        }
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_header, parent, false);
                return new HeaderViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_queue, parent, false);
                return new ItemViewHolder(view);
            }
        }
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Object obj = items.get(position);
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).headerText.setText((String) obj);
            } else if (holder instanceof ItemViewHolder) {
                DownloadItem item = (DownloadItem) obj;
                ItemViewHolder ih = (ItemViewHolder) holder;
                ih.platformText.setText(item.getPlatform());
                ih.linkText.setText(item.getLink());
                ih.statusText.setText(item.getStatus());
                int color;
                switch (item.getStatus()) {
                    case "PENDING": color = 0xFF9E9E9E; break;
                    case "DOWNLOADING": color = 0xFF2196F3; break;
                    case "DONE": color = 0xFF4CAF50; break;
                    case "FAILED": color = 0xFFF44336; break;
                    case "WAITING_WIFI": color = 0xFFFF9800; break;
                    default: color = 0xFF9E9E9E;
                }
                ih.statusText.setTextColor(color);
                ih.itemView.setOnClickListener(v -> activity.showItemMenu(item));
            }
        }
        @Override
        public int getItemCount() {
            return items.size();
        }
        class HeaderViewHolder extends RecyclerView.ViewHolder {
            TextView headerText;
            HeaderViewHolder(View itemView) {
                super(itemView);
                headerText = (TextView) itemView;
            }
        }
        class ItemViewHolder extends RecyclerView.ViewHolder {
            TextView platformText, linkText, statusText;
            ItemViewHolder(View itemView) {
                super(itemView);
                platformText = itemView.findViewById(R.id.platformText);
                linkText = itemView.findViewById(R.id.linkText);
                statusText = itemView.findViewById(R.id.statusText);
            }
        }
    }
}
