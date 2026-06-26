package com.badal.downloader;

import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);
        queueList = db.getAllLinks();

        linkInput = findViewById(R.id.linkInput);
        recyclerView = findViewById(R.id.recyclerView);
        Button addBtn = findViewById(R.id.addBtn);
        Button downloadAllBtn = findViewById(R.id.downloadAllBtn);
        Button clearBtn = findViewById(R.id.clearBtn);

        adapter = new QueueAdapter(queueList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        addBtn.setOnClickListener(v -> addLink());
        downloadAllBtn.setOnClickListener(v -> startBatchDownload());
        clearBtn.setOnClickListener(v -> clearQueue());

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
                    Toast.makeText(this, "Link detected! Press ADD", Toast.LENGTH_SHORT).show();
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
        queueList.add(item);
        adapter.notifyItemInserted(queueList.size() - 1);
        linkInput.setText("");
        Toast.makeText(this, platform + " link added!", Toast.LENGTH_SHORT).show();
    }

    private void startBatchDownload() {
        if (queueList.isEmpty()) {
            Toast.makeText(this, "Queue is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        int pendingCount = 0;
        for (DownloadItem item : queueList) {
            if (item.getStatus().equals("PENDING")) {
                Intent intent = new Intent(this, DownloadService.class);
                intent.putExtra("link", item.getLink());
                intent.putExtra("platform", item.getPlatform());
                intent.putExtra("id", item.getId());
                startService(intent);
                pendingCount++;
            }
        }

        if (pendingCount > 0) {
            Toast.makeText(this, "Downloading " + pendingCount + " items...", Toast.LENGTH_LONG).show();
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
