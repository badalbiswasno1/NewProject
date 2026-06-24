package com.my.netindicator;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.graphics.Color;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] PERMISSIONS = {
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.READ_PHONE_STATE
    };

    private TextView tvNetworkType, tvPing, tvSignal, tvTime, tvDbm, tvData, tvHistory, tvGrade;
    private Handler handler = new Handler();
    private Runnable updater;
    private String lastNetwork = "";
    private long startTime;
    private NetworkLogger logger;
    private long lastPing = 0;
    private SeekBar timeSeek;
    private TextView tvTimeLabel;
    private int[] timeOptions = {15, 30, 60, 1440};
    private LanguageManager langManager;
    private FloatingWindowPrefs windowPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        langManager = new LanguageManager(this);
        windowPrefs = new FloatingWindowPrefs(this);
        startTime = System.currentTimeMillis();
        logger = new NetworkLogger(this);

        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);
        }

        buildUI();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 0);
            } else {
                if (windowPrefs.isVisible()) {
                    startService(new Intent(this, FloatingService.class));
                }
            }
        }

        updater = new Runnable() {
            public void run() {
                try {
                    updateUI();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                handler.postDelayed(this, 2000);
            }
        };
        handler.post(updater);
        updateHistory(0);
    }

    private boolean hasPermissions() {
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#111111"));

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(Color.parseColor("#111111"));
        main.setPadding(40, 80, 40, 40);
        main.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(main);

        // Title
        TextView title = new TextView(this);
        title.setText(langManager.get("true_network"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 10);
        main.addView(title);

        addLine(main, "#FFD700");

        // Precise Network Grade
        tvGrade = new TextView(this);
        tvGrade.setText("...");
        tvGrade.setTextSize(72);
        tvGrade.setTypeface(null, android.graphics.Typeface.BOLD);
        tvGrade.setGravity(Gravity.CENTER);
        tvGrade.setPadding(0, 20, 0, 0);
        main.addView(tvGrade);

        // Base Network Type
        tvNetworkType = new TextView(this);
        tvNetworkType.setText("...");
        tvNetworkType.setTextSize(18);
        tvNetworkType.setGravity(Gravity.CENTER);
        tvNetworkType.setPadding(0, 5, 0, 5);
        main.addView(tvNetworkType);

        // Ping
        tvPing = new TextView(this);
        tvPing.setText(langManager.get("ping") + ": -- ms");
        tvPing.setTextSize(20);
        tvPing.setTypeface(null, android.graphics.Typeface.BOLD);
        tvPing.setGravity(Gravity.CENTER);
        tvPing.setPadding(0, 0, 0, 5);
        main.addView(tvPing);

        // Signal
        tvDbm = new TextView(this);
        tvDbm.setText(langManager.get("signal") + ": -- dBm");
        tvDbm.setTextSize(16);
        tvDbm.setGravity(Gravity.CENTER);
        tvDbm.setTextColor(Color.parseColor("#AAAAAA"));
        tvDbm.setPadding(0, 0, 0, 20);
        main.addView(tvDbm);

        addLine(main, "#FFD700");

        // Operator
        tvSignal = new TextView(this);
        tvSignal.setText(langManager.get("operator") + ": --");
        tvSignal.setTextColor(Color.parseColor("#00CC44"));
        tvSignal.setTextSize(16);
        tvSignal.setGravity(Gravity.CENTER);
        tvSignal.setPadding(0, 20, 0, 8);
        main.addView(tvSignal);

        // Time running
        tvTime = new TextView(this);
        tvTime.setText(langManager.get("running") + ": 0 " + langManager.get("seconds"));
        tvTime.setTextColor(Color.parseColor("#AAAAAA"));
        tvTime.setTextSize(13);
        tvTime.setGravity(Gravity.CENTER);
        tvTime.setPadding(0, 0, 0, 10);
        main.addView(tvTime);

        // Data usage
        tvData = new TextView(this);
        tvData.setText("Data: --");
        tvData.setTextColor(Color.parseColor("#FFD700"));
        tvData.setTextSize(14);
        tvData.setGravity(Gravity.CENTER);
        tvData.setPadding(0, 0, 0, 20);
        main.addView(tvData);

        addLine(main, "#E63329");

        // History Title
        TextView histTitle = new TextView(this);
        histTitle.setText(langManager.get("network_history"));
        histTitle.setTextColor(Color.parseColor("#FFD700"));
        histTitle.setTextSize(15);
        histTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        histTitle.setPadding(0, 15, 0, 5);
        main.addView(histTitle);

        tvTimeLabel = new TextView(this);
        tvTimeLabel.setText(langManager.get("time") + ": " + getTimeLabel(0));
        tvTimeLabel.setTextColor(Color.parseColor("#AAAAAA"));
        tvTimeLabel.setTextSize(13);
        tvTimeLabel.setGravity(Gravity.CENTER);
        main.addView(tvTimeLabel);

        timeSeek = new SeekBar(this);
        timeSeek.setMax(3);
        timeSeek.setProgress(0);
        timeSeek.setPadding(0, 10, 0, 10);
        timeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                tvTimeLabel.setText(langManager.get("time") + ": " + getTimeLabel(p));
                updateHistory(p);
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });
        main.addView(timeSeek);

        tvHistory = new TextView(this);
        tvHistory.setTextColor(Color.parseColor("#CCCCCC"));
        tvHistory.setTextSize(11);
        tvHistory.setPadding(0, 5, 0, 20);
        tvHistory.setTypeface(android.graphics.Typeface.MONOSPACE);
        main.addView(tvHistory);

        Button clearBtn = new Button(this);
        clearBtn.setText(langManager.get("clear_history"));
        clearBtn.setBackgroundColor(Color.parseColor("#E63329"));
        clearBtn.setTextColor(Color.WHITE);
        clearBtn.setTextSize(12);
        clearBtn.setOnClickListener(v -> {
            logger.clear();
            tvHistory.setText(langManager.get("no_data"));
        });
        main.addView(clearBtn);

        addLine(main, "#333333");

        // Data Analytics Button
        Button analyticsBtn = new Button(this);
        analyticsBtn.setText("📊 " + langManager.get("data_analytics"));
        analyticsBtn.setBackgroundColor(Color.parseColor("#0099FF"));
        analyticsBtn.setTextColor(Color.WHITE);
        analyticsBtn.setTextSize(14);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        ap.setMargins(0, 10, 0, 10);
        analyticsBtn.setLayoutParams(ap);
        analyticsBtn.setOnClickListener(v -> startActivity(new Intent(this, DataAnalyticsActivity.class)));
        main.addView(analyticsBtn);

        // Settings Button
        Button settingsBtn = new Button(this);
        settingsBtn.setText("⚙ " + langManager.get("settings"));
        settingsBtn.setBackgroundColor(Color.parseColor("#333333"));
        settingsBtn.setTextColor(Color.WHITE);
        settingsBtn.setTextSize(14);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.setMargins(0, 10, 0, 0);
        settingsBtn.setLayoutParams(bp);
        settingsBtn.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        main.addView(settingsBtn);

        setContentView(scroll);
    }

    private String getTimeLabel(int index) {
        String[] labels = {"১৫ মিনিট", "৩০ মিনিট", "১ ঘন্টা", "সারাদিন"};
        if (langManager.getCurrentLanguage().equals(LanguageManager.LANG_ENGLISH)) {
            labels = new String[]{"15 min", "30 min", "1 hour", "All day"};
        } else if (langManager.getCurrentLanguage().equals(LanguageManager.LANG_HINDI)) {
            labels = new String[]{"15 मिनट", "30 मिनट", "1 घंटा", "पूरा दिन"};
        }
        return labels[index];
    }

    private void addLine(LinearLayout parent, String color) {
        View line = new View(this);
        line.setBackgroundColor(Color.parseColor(color));
        line.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 3));
        parent.addView(line);
    }

    private void updateHistory(int timeIndex) {
        try {
            JSONArray logs = logger.getLogs();
            long cutoff = System.currentTimeMillis() - ((long) timeOptions[timeIndex] * 60 * 1000);
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-8s %-6s %-6s %-8s\n", langManager.get("time"), "Grade", "Ping", "Data"));
            sb.append("----------------------------------\n");
            long totalData = 0;
            long minPing = Long.MAX_VALUE, maxPing = 0;
            int count = 0;

            for (int i = logs.length() - 1; i >= 0; i--) {
                JSONObject obj = logs.getJSONObject(i);
                long ping = obj.getLong("ping");
                long data = obj.getLong("data");
                totalData += data;
                if (ping > 0) {
                    if (ping < minPing) minPing = ping;
                    if (ping > maxPing) maxPing = ping;
                }
                count++;
                if (count <= 20) {
                    sb.append(String.format("%-8s %-6s %-6s %-8s\n",
                            obj.getString("time").substring(0, 8),
                            obj.getString("grade"),
                            ping > 0 ? ping + "ms" : "--",
                            data + "KB"));
                }
            }

            if (count > 0) {
                sb.append("----------------------------------\n");
                sb.append(langManager.get("total_records") + ": " + count + "\n");
                sb.append(langManager.get("data_usage") + ": " + (totalData / 1024) + " MB\n");
                if (minPing < Long.MAX_VALUE)
                    sb.append(langManager.get("best") + ": " + minPing + "ms | " + 
                             langManager.get("worst") + ": " + maxPing + "ms");
            } else {
                sb.append(langManager.get("no_data"));
            }
            tvHistory.setText(sb.toString());
        } catch (Exception e) {
            tvHistory.setText("Error loading history");
        }
    }

    private void updateUI() {
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        int type = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        try {
            if (hasPermissions()) {
                type = tm.getDataNetworkType();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        String baseNetwork = getNetworkName(type);
        int color = getNetworkColor(type);

        NetworkGrade grade = calculatePreciseGrade(type, tm);
        String preciseGrade = grade.grade;
        int gradeColor = grade.color;

        tvGrade.setText(preciseGrade);
        tvGrade.setTextColor(gradeColor);
        tvNetworkType.setText("Base: " + baseNetwork);
        tvNetworkType.setTextColor(color);
        tvPing.setTextColor(color);

        if (!baseNetwork.equals(lastNetwork)) {
            lastNetwork = baseNetwork;
        }

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        tvTime.setText(langManager.get("running") + ": " + (elapsed / 60) + " " + 
                      langManager.get("minutes") + " " + (elapsed % 60) + " " + 
                      langManager.get("seconds"));

        tvSignal.setText(langManager.get("operator") + ": " + tm.getNetworkOperatorName());

        updateSignalInfo(tm, grade);

        try {
            long rx = android.net.TrafficStats.getMobileRxBytes();
            long tx = android.net.TrafficStats.getMobileTxBytes();
            long totalKB = (rx + tx) / 1024;
            String dataText = totalKB > 1024
                    ? langManager.get("data_used") + ": " + (totalKB / 1024) + " MB"
                    : langManager.get("data_used") + ": " + totalKB + " KB";
            tvData.setText(dataText);
        } catch (Exception e) {
            e.printStackTrace();
        }

        final String finalGrade = preciseGrade;
        new Thread(() -> {
            final long ping = measurePing();
            lastPing = ping;
            try {
                long rx = android.net.TrafficStats.getMobileRxBytes();
                long tx = android.net.TrafficStats.getMobileTxBytes();
                long dataKB = (rx + tx) / 1024;
                logger.log(finalGrade, ping, dataKB);
            } catch (Exception e) {
                e.printStackTrace();
            }

            final String pingText = ping >= 0 ? langManager.get("ping") + ": " + ping + " ms" : langManager.get("ping") + ": timeout";
            final int pingColor = ping < 0 ? Color.RED :
                    ping < 100 ? Color.parseColor("#00CC44") :
                            ping < 300 ? Color.parseColor("#FFD700") : Color.parseColor("#E63329");

            runOnUiThread(() -> {
                tvPing.setText(pingText);
                tvPing.setTextColor(pingColor);
                updateHistory(timeSeek.getProgress());
            });
        }).start();
    }

    private NetworkGrade calculatePreciseGrade(int type, TelephonyManager tm) {
        try {
            if (!hasPermissions()) return new NetworkGrade("?G", Color.WHITE);

            List<CellInfo> cells = tm.getAllCellInfo();
            if (cells == null || cells.isEmpty()) {
                return new NetworkGrade(getNetworkName(type) + ".0", getNetworkColor(type));
            }

            for (CellInfo cell : cells) {
                if (!cell.isRegistered()) continue;

                if (cell instanceof CellInfoNr && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    CellSignalStrengthNr nr = (CellSignalStrengthNr) ((CellInfoNr) cell).getCellSignalStrength();
                    int ssRsrp = nr.getSsRsrp();
                    int ssRsrq = nr.getSsRsrq();
                    float score = calculate5GScore(ssRsrp, ssRsrq);
                    String grade = String.format("5.%dG", (int)(score * 9));
                    int color = interpolateColor(Color.parseColor("#00CC44"), Color.parseColor("#00FF88"), score);
                    return new NetworkGrade(grade, color);
                }

                if (cell instanceof CellInfoLte) {
                    CellSignalStrengthLte lte = ((CellInfoLte) cell).getCellSignalStrength();
                    int rsrp = lte.getRsrp();
                    int rsrq = lte.getRsrq();
                    int snr = lte.getRssnr();
                    float score = calculate4GScore(rsrp, rsrq, snr);
                    String grade = String.format("4.%dG", (int)(score * 9));
                    int color = interpolateColor(Color.parseColor("#FFD700"), Color.parseColor("#00CC44"), score);
                    return new NetworkGrade(grade, color);
                }

                if (cell instanceof CellInfoWcdma) {
                    CellSignalStrengthWcdma wcdma = ((CellInfoWcdma) cell).getCellSignalStrength();
                    int asu = wcdma.getAsuLevel();
                    int dbm = wcdma.getDbm();
                    float score = calculate3GScore(asu, dbm);
                    String grade = String.format("3.%dG", (int)(score * 9));
                    int color = interpolateColor(Color.parseColor("#FF8800"), Color.parseColor("#FFD700"), score);
                    return new NetworkGrade(grade, color);
                }

                if (cell instanceof CellInfoGsm) {
                    CellSignalStrengthGsm gsm = ((CellInfoGsm) cell).getCellSignalStrength();
                    int asu = gsm.getAsuLevel();
                    int dbm = gsm.getDbm();
                    float score = calculate2GScore(asu, dbm);
                    String grade = String.format("2.%dG", (int)(score * 9));
                    int color = interpolateColor(Color.parseColor("#E63329"), Color.parseColor("#FF8800"), score);
                    return new NetworkGrade(grade, color);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new NetworkGrade(getNetworkName(type) + ".0", getNetworkColor(type));
    }

    private float calculate5GScore(int ssRsrp, int ssRsrq) {
        float rsrpScore = 0;
        if (ssRsrp >= -80) rsrpScore = 1.0f;
        else if (ssRsrp >= -90) rsrpScore = 0.8f;
        else if (ssRsrp >= -100) rsrpScore = 0.6f;
        else if (ssRsrp >= -110) rsrpScore = 0.4f;
        else if (ssRsrp >= -120) rsrpScore = 0.2f;
        else rsrpScore = 0.0f;

        float rsrqScore = 0;
        if (ssRsrq >= -10) rsrqScore = 1.0f;
        else if (ssRsrq >= -15) rsrqScore = 0.7f;
        else if (ssRsrq >= -20) rsrqScore = 0.4f;
        else rsrqScore = 0.1f;

        return (rsrpScore * 0.6f + rsrqScore * 0.4f);
    }

    private float calculate4GScore(int rsrp, int rsrq, int snr) {
        float rsrpScore = 0;
        if (rsrp >= -80) rsrpScore = 1.0f;
        else if (rsrp >= -90) rsrpScore = 0.8f;
        else if (rsrp >= -100) rsrpScore = 0.6f;
        else if (rsrp >= -110) rsrpScore = 0.4f;
        else if (rsrp >= -120) rsrpScore = 0.2f;
        else rsrpScore = 0.0f;

        float rsrqScore = 0;
        if (rsrq >= -10) rsrqScore = 1.0f;
        else if (rsrq >= -15) rsrqScore = 0.7f;
        else if (rsrq >= -20) rsrqScore = 0.4f;
        else rsrqScore = 0.1f;

        float snrScore = 0;
        if (snr >= 20) snrScore = 1.0f;
        else if (snr >= 13) snrScore = 0.8f;
        else if (snr >= 0) snrScore = 0.5f;
        else snrScore = 0.1f;

        return (rsrpScore * 0.5f + rsrqScore * 0.3f + snrScore * 0.2f);
    }

    private float calculate3GScore(int asu, int dbm) {
        float asuScore = 0;
        if (asu >= 24) asuScore = 1.0f;
        else if (asu >= 16) asuScore = 0.7f;
        else if (asu >= 8) asuScore = 0.4f;
        else if (asu > 0) asuScore = 0.2f;
        else asuScore = 0.0f;

        float dbmScore = 0;
        if (dbm >= -70) dbmScore = 1.0f;
        else if (dbm >= -85) dbmScore = 0.7f;
        else if (dbm >= -100) dbmScore = 0.4f;
        else dbmScore = 0.1f;

        return (asuScore * 0.5f + dbmScore * 0.5f);
    }

    private float calculate2GScore(int asu, int dbm) {
        float asuScore = 0;
        if (asu >= 24) asuScore = 1.0f;
        else if (asu >= 16) asuScore = 0.7f;
        else if (asu >= 8) asuScore = 0.4f;
        else if (asu > 0) asuScore = 0.2f;
        else asuScore = 0.0f;

        float dbmScore = 0;
        if (dbm >= -70) dbmScore = 1.0f;
        else if (dbm >= -85) dbmScore = 0.7f;
        else if (dbm >= -100) dbmScore = 0.4f;
        else dbmScore = 0.1f;

        return (asuScore * 0.5f + dbmScore * 0.5f);
    }

    private int interpolateColor(int color1, int color2, float ratio) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);
        return Color.rgb(r, g, b);
    }

    private void updateSignalInfo(TelephonyManager tm, NetworkGrade grade) {
        try {
            if (!hasPermissions()) return;
            List<CellInfo> cells = tm.getAllCellInfo();
            if (cells == null || cells.isEmpty()) return;

            for (CellInfo cell : cells) {
                if (!cell.isRegistered()) continue;
                int dbm = 0;
                if (cell instanceof CellInfoLte) {
                    dbm = ((CellInfoLte) cell).getCellSignalStrength().getDbm();
                } else if (cell instanceof CellInfoWcdma) {
                    dbm = ((CellInfoWcdma) cell).getCellSignalStrength().getDbm();
                } else if (cell instanceof CellInfoGsm) {
                    dbm = ((CellInfoGsm) cell).getCellSignalStrength().getDbm();
                } else if (cell instanceof CellInfoNr && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    dbm = ((CellInfoNr) cell).getCellSignalStrength().getDbm();
                }

                String sigLevel;
                if (dbm > -80) sigLevel = langManager.get("excellent");
                else if (dbm > -90) sigLevel = langManager.get("very_good");
                else if (dbm > -100) sigLevel = langManager.get("good");
                else if (dbm > -110) sigLevel = langManager.get("fair");
                else if (dbm > -120) sigLevel = langManager.get("moderate");
                else sigLevel = langManager.get("weak");

                tvDbm.setText(langManager.get("signal") + ": " + dbm + " dBm (" + sigLevel + ") | " + grade.grade);
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long measurePing() {
        try {
            long start = System.currentTimeMillis();
            Process p = Runtime.getRuntime().exec("ping -c 1 -W 2 8.8.8.8");
            int result = p.waitFor();
            if (result == 0) return System.currentTimeMillis() - start;
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private String getNetworkName(int type) {
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_NR: return "5G";
            case TelephonyManager.NETWORK_TYPE_LTE: return "4G";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_UMTS: return "3G";
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_GPRS: return "2G";
            default: return "?G";
        }
    }

    private int getNetworkColor(int type) {
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_NR: return Color.parseColor("#00CC44");
            case TelephonyManager.NETWORK_TYPE_LTE: return Color.parseColor("#FFD700");
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_UMTS: return Color.parseColor("#FF8800");
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_GPRS: return Color.parseColor("#E63329");
            default: return Color.WHITE;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updater);
    }

    private static class NetworkGrade {
        String grade;
        int color;
        NetworkGrade(String grade, int color) {
            this.grade = grade;
            this.color = color;
        }
    }
}
