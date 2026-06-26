package com.badal.downloader;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.List;
public class DownloadedActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private DownloadedAdapter adapter;
    private List<DownloadedItem> downloadedList;
    private DatabaseHelper db;
    private TextView countText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloaded);
        db = new DatabaseHelper(this);
        downloadedList = db.getDownloaded();
        recyclerView = findViewById(R.id.downloadedRecyclerView);
        countText = findViewById(R.id.countText);
        Button backBtn = findViewById(R.id.backBtn);
        Button clearHistoryBtn = findViewById(R.id.clearHistoryBtn);
        adapter = new DownloadedAdapter(downloadedList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        updateCount();
        backBtn.setOnClickListener(v -> finish());
        clearHistoryBtn.setOnClickListener(v -> {
            for (DownloadedItem item : downloadedList) {
                db.deleteDownloaded(item.getId());
            }
            downloadedList.clear();
            adapter.notifyDataSetChanged();
            updateCount();
            Toast.makeText(this, "History cleared!", Toast.LENGTH_SHORT).show();
        });
    }
    private void updateCount() {
        countText.setText("Total: " + downloadedList.size() + " / 500");
    }
    class DownloadedAdapter extends RecyclerView.Adapter<DownloadedAdapter.ViewHolder> {
        private List<DownloadedItem> items;
        DownloadedAdapter(List<DownloadedItem> items) {
            this.items = items;
        }
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_downloaded, parent, false);
            return new ViewHolder(view);
        }
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            DownloadedItem item = items.get(position);
            holder.platformText.setText(item.getPlatform());
            holder.titleText.setText(item.getTitle() != null ? item.getTitle() : "Untitled");
            holder.dateText.setText(item.getDownloadedAt());
            holder.linkText.setText(item.getLink());
            holder.itemView.setOnClickListener(v -> {
                File file = new File(item.getFilepath());
                if (file.exists()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri uri = Uri.fromFile(file);
                    intent.setDataAndType(uri, "video/*");
                    startActivity(intent);
                } else {
                    Toast.makeText(DownloadedActivity.this, "File not found!", Toast.LENGTH_SHORT).show();
                }
            });
            holder.deleteBtn.setOnClickListener(v -> {
                db.deleteDownloaded(item.getId());
                items.remove(position);
                notifyItemRemoved(position);
                updateCount();
            });
        }
        @Override
        public int getItemCount() {
            return items.size();
        }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView platformText, titleText, dateText, linkText;
            Button deleteBtn;
            ViewHolder(View itemView) {
                super(itemView);
                platformText = itemView.findViewById(R.id.platformText);
                titleText = itemView.findViewById(R.id.titleText);
                dateText = itemView.findViewById(R.id.dateText);
                linkText = itemView.findViewById(R.id.linkText);
                deleteBtn = itemView.findViewById(R.id.deleteBtn);
            }
        }
    }
}
