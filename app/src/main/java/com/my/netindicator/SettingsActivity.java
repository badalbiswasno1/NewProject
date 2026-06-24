package com.my.netindicator;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private LanguageManager langManager;
    private FloatingWindowPrefs windowPrefs;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        langManager = new LanguageManager(this);
        windowPrefs = new FloatingWindowPrefs(this);
        buildUI();
    }
    
    private void buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#111111"));
        
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(Color.parseColor("#111111"));
        main.setPadding(40, 80, 40, 40);
        
        TextView title = new TextView(this);
        title.setText("⚙ " + langManager.get("settings"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setPadding(0, 0, 0, 30);
        main.addView(title);
        
        addSectionTitle(main, langManager.get("language"));
        
        String[] names = langManager.getLanguageNames();
        String[] codes = langManager.getLanguageCodes();
        String currentLang = langManager.getCurrentLanguage();
        
        for (int i = 0; i < names.length; i++) {
            final String code = codes[i];
            Button langBtn = createButton(names[i], 
                code.equals(currentLang) ? Color.parseColor("#00CC44") : Color.parseColor("#333333"));
            langBtn.setOnClickListener(v -> {
                langManager.setLanguage(code);
                Toast.makeText(this, "Language changed to " + names[getIndex(codes, code)], Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
            main.addView(langBtn);
        }
        
        addDivider(main);
        addSectionTitle(main, langManager.get("floating_window"));
        
        Button visibilityBtn = createButton(
            windowPrefs.isVisible() ? langManager.get("hide_window") : langManager.get("show_window"),
            windowPrefs.isVisible() ? Color.parseColor("#E63329") : Color.parseColor("#00CC44"));
        visibilityBtn.setOnClickListener(v -> {
            windowPrefs.setVisible(!windowPrefs.isVisible());
            stopService(new Intent(this, FloatingService.class));
            if (windowPrefs.isVisible()) {
                startService(new Intent(this, FloatingService.class));
            }
            recreate();
        });
        main.addView(visibilityBtn);
        
        addSectionTitle(main, langManager.get("window_color"));
        LinearLayout colorRow = new LinearLayout(this);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        colorRow.setGravity(Gravity.CENTER);
        
        int[] colors = {
            Color.parseColor("#00CC44"),
            Color.parseColor("#FFD700"),
            Color.parseColor("#FF8800"),
            Color.parseColor("#E63329"),
            Color.parseColor("#0099FF"),
            Color.parseColor("#FF00FF"),
            Color.parseColor("#FFFFFF"),
            Color.parseColor("#00FFFF")
        };
        
        for (int c : colors) {
            View colorDot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(60, 60);
            params.setMargins(10, 10, 10, 10);
            colorDot.setLayoutParams(params);
            
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(c);
            drawable.setStroke(3, Color.WHITE);
            colorDot.setBackground(drawable);
            
            colorDot.setOnClickListener(v -> {
                windowPrefs.setTextColor(c);
                Toast.makeText(this, "Color updated!", Toast.LENGTH_SHORT).show();
                restartFloatingService();
            });
            colorRow.addView(colorDot);
        }
        main.addView(colorRow);
        
        addSectionTitle(main, "Background " + langManager.get("window_color"));
        LinearLayout bgColorRow = new LinearLayout(this);
        bgColorRow.setOrientation(LinearLayout.HORIZONTAL);
        bgColorRow.setGravity(Gravity.CENTER);
        
        int[] bgColors = {
            Color.parseColor("#CC000000"),
            Color.parseColor("#88000000"),
            Color.parseColor("#44000000"),
            Color.parseColor("#CC333333"),
            Color.parseColor("#CC111111"),
            Color.parseColor("#CC0000FF"),
            Color.parseColor("#CCFF0000"),
            Color.parseColor("#CC00FF00")
        };
        
        for (int c : bgColors) {
            View colorDot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(60, 60);
            params.setMargins(10, 10, 10, 10);
            colorDot.setLayoutParams(params);
            
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(c);
            drawable.setStroke(3, Color.WHITE);
            colorDot.setBackground(drawable);
            
            colorDot.setOnClickListener(v -> {
                windowPrefs.setBackgroundColor(c);
                Toast.makeText(this, "Background updated!", Toast.LENGTH_SHORT).show();
                restartFloatingService();
            });
            bgColorRow.addView(colorDot);
        }
        main.addView(bgColorRow);
        
        addSectionTitle(main, langManager.get("window_transparency"));
        TextView transLabel = new TextView(this);
        transLabel.setText("Transparency: " + windowPrefs.getTransparency() + "%");
        transLabel.setTextColor(Color.parseColor("#AAAAAA"));
        transLabel.setPadding(0, 10, 0, 5);
        main.addView(transLabel);
        
        SeekBar transSeek = new SeekBar(this);
        transSeek.setMax(100);
        transSeek.setProgress(windowPrefs.getTransparency());
        transSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                transLabel.setText("Transparency: " + p + "%");
                windowPrefs.setTransparency(p);
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {
                restartFloatingService();
            }
        });
        main.addView(transSeek);
        
        addSectionTitle(main, langManager.get("window_size"));
        TextView sizeLabel = new TextView(this);
        sizeLabel.setText("Size: " + windowPrefs.getSize() + "sp");
        sizeLabel.setTextColor(Color.parseColor("#AAAAAA"));
        sizeLabel.setPadding(0, 10, 0, 5);
        main.addView(sizeLabel);
        
        SeekBar sizeSeek = new SeekBar(this);
        sizeSeek.setMax(30);
        sizeSeek.setProgress(windowPrefs.getSize() - 10);
        sizeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                int size = p + 10;
                sizeLabel.setText("Size: " + size + "sp");
                windowPrefs.setSize(size);
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {
                restartFloatingService();
            }
        });
        main.addView(sizeSeek);
        
        addSectionTitle(main, "Position");
        LinearLayout posRow = new LinearLayout(this);
        posRow.setOrientation(LinearLayout.HORIZONTAL);
        posRow.setGravity(Gravity.CENTER);
        
        String[] positions = {"Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right", "Center"};
        for (String pos : positions) {
            Button posBtn = new Button(this);
            posBtn.setText(pos);
            posBtn.setTextSize(10);
            posBtn.setBackgroundColor(Color.parseColor("#333333"));
            posBtn.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT);
            p.setMargins(5, 5, 5, 5);
            posBtn.setLayoutParams(p);
            posBtn.setOnClickListener(v -> {
                windowPrefs.setPosition(pos);
                restartFloatingService();
                Toast.makeText(this, "Position: " + pos, Toast.LENGTH_SHORT).show();
            });
            posRow.addView(posBtn);
        }
        main.addView(posRow);
        
        addDivider(main);
        
        Button analyticsBtn = createButton("📊 " + langManager.get("data_analytics"), Color.parseColor("#0099FF"));
        analyticsBtn.setOnClickListener(v -> startActivity(new Intent(this, DataAnalyticsActivity.class)));
        main.addView(analyticsBtn);
        
        addDivider(main);
        
        Button backBtn = createButton("← Back", Color.parseColor("#333333"));
        backBtn.setOnClickListener(v -> finish());
        main.addView(backBtn);
        
        scroll.addView(main);
        setContentView(scroll);
    }
    
    private void addSectionTitle(LinearLayout parent, String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextColor(Color.parseColor("#FFD700"));
        title.setTextSize(16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 20, 0, 10);
        parent.addView(title);
    }
    
    private void addDivider(LinearLayout parent) {
        View line = new View(this);
        line.setBackgroundColor(Color.parseColor("#333333"));
        line.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 2));
        line.setPadding(0, 20, 0, 20);
        parent.addView(line);
    }
    
    private Button createButton(String text, int color) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setBackgroundColor(color);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(14);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 8, 0, 8);
        btn.setLayoutParams(params);
        return btn;
    }
    
    private int getIndex(String[] arr, String val) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(val)) return i;
        }
        return 0;
    }
    
    private void restartFloatingService() {
        stopService(new Intent(this, FloatingService.class));
        if (windowPrefs.isVisible()) {
            startService(new Intent(this, FloatingService.class));
        }
    }
}
