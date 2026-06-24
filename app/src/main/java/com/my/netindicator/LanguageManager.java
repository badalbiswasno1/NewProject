package com.my.netindicator;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;

public class LanguageManager {
    private static final String PREFS_NAME = "LanguagePrefs";
    private static final String KEY_LANGUAGE = "selected_language";
    
    public static final String LANG_BENGALI = "bn";
    public static final String LANG_ENGLISH = "en";
    public static final String LANG_HINDI = "hi";
    public static final String LANG_TAMIL = "ta";
    public static final String LANG_TELUGU = "te";
    public static final String LANG_MARATHI = "mr";
    public static final String LANG_GUJARATI = "gu";
    public static final String LANG_MALAYALAM = "ml";
    
    private Context context;
    private SharedPreferences prefs;
    private HashMap<String, HashMap<String, String>> translations;
    
    public LanguageManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        initTranslations();
    }
    
    public String getCurrentLanguage() {
        return prefs.getString(KEY_LANGUAGE, LANG_BENGALI);
    }
    
    public void setLanguage(String language) {
        prefs.edit().putString(KEY_LANGUAGE, language).apply();
    }
    
    public String get(String key) {
        String lang = getCurrentLanguage();
        HashMap<String, String> langMap = translations.get(lang);
        if (langMap != null && langMap.containsKey(key)) {
            return langMap.get(key);
        }
        HashMap<String, String> enMap = translations.get(LANG_ENGLISH);
        return enMap != null ? enMap.getOrDefault(key, key) : key;
    }
    
    private void initTranslations() {
        translations = new HashMap<>();
        
        HashMap<String, String> en = new HashMap<>();
        en.put("app_name", "True Network");
        en.put("true_network", "TRUE NETWORK");
        en.put("ping", "Ping");
        en.put("signal", "Signal");
        en.put("operator", "Operator");
        en.put("running", "Running");
        en.put("minutes", "minutes");
        en.put("seconds", "seconds");
        en.put("data_used", "Data used since app start");
        en.put("network_history", "Network History");
        en.put("time", "Time");
        en.put("total_records", "Total records");
        en.put("best", "Best");
        en.put("worst", "Worst");
        en.put("clear_history", "Clear History");
        en.put("settings", "Settings");
        en.put("language", "Language");
        en.put("floating_window", "Floating Window");
        en.put("window_color", "Window Color");
        en.put("window_transparency", "Transparency");
        en.put("window_size", "Size");
        en.put("show_window", "Show Window");
        en.put("hide_window", "Hide Window");
        en.put("data_analytics", "Data Analytics");
        en.put("select_date", "Select Date");
        en.put("timeframe", "Timeframe");
        en.put("per_app_data", "Per App Data");
        en.put("top_speed", "Top Speed");
        en.put("lowest_speed", "Lowest Speed");
        en.put("data_usage", "Data Usage");
        en.put("no_data", "No data available");
        en.put("good", "Good");
        en.put("moderate", "Moderate");
        en.put("weak", "Weak");
        en.put("excellent", "Excellent");
        en.put("very_good", "Very Good");
        en.put("fair", "Fair");
        en.put("poor", "Poor");
        translations.put(LANG_ENGLISH, en);
        
        HashMap<String, String> bn = new HashMap<>();
        bn.put("app_name", "ট্রু নেটওয়ার্ক");
        bn.put("true_network", "ট্রু নেটওয়ার্ক");
        bn.put("ping", "পিং");
        bn.put("signal", "সিগন্যাল");
        bn.put("operator", "অপারেটর");
        bn.put("running", "চলছে");
        bn.put("minutes", "মিনিট");
        bn.put("seconds", "সেকেন্ড");
        bn.put("data_used", "অ্যাপ চলার পর");
        bn.put("network_history", "নেটওয়ার্ক ইতিহাস");
        bn.put("time", "সময়");
        bn.put("total_records", "মোট");
        bn.put("best", "সেরা");
        bn.put("worst", "খারাপ");
        bn.put("clear_history", "ইতিহাস মুছো");
        bn.put("settings", "সেটিংস");
        bn.put("language", "ভাষা");
        bn.put("floating_window", "ফ্লোটিং উইন্ডো");
        bn.put("window_color", "উইন্ডো রং");
        bn.put("window_transparency", "স্বচ্ছতা");
        bn.put("window_size", "আকার");
        bn.put("show_window", "উইন্ডো দেখাও");
        bn.put("hide_window", "উইন্ডো লুকাও");
        bn.put("data_analytics", "ডেটা অ্যানালিটিক্স");
        bn.put("select_date", "তারিখ নির্বাচন");
        bn.put("timeframe", "সময়সীমা");
        bn.put("per_app_data", "অ্যাপ অনুযায়ী ডেটা");
        bn.put("top_speed", "সর্বোচ্চ গতি");
        bn.put("lowest_speed", "সর্বনিম্ন গতি");
        bn.put("data_usage", "ডেটা ব্যবহার");
        bn.put("no_data", "কোনো ডেটা নেই");
        bn.put("good", "ভালো");
        bn.put("moderate", "মাঝারি");
        bn.put("weak", "দুর্বল");
        bn.put("excellent", "চমৎকার");
        bn.put("very_good", "খুব ভালো");
        bn.put("fair", "মোটামুটি");
        bn.put("poor", "খারাপ");
        translations.put(LANG_BENGALI, bn);
        
        HashMap<String, String> hi = new HashMap<>();
        hi.put("app_name", "ट्रू नेटवर्क");
        hi.put("true_network", "ट्रू नेटवर्क");
        hi.put("ping", "पिंग");
        hi.put("signal", "सिग्नल");
        hi.put("operator", "ऑपरेटर");
        hi.put("running", "चल रहा है");
        hi.put("minutes", "मिनट");
        hi.put("seconds", "सेकंड");
        hi.put("data_used", "ऐप चलने के बाद");
        hi.put("network_history", "नेटवर्क इतिहास");
        hi.put("time", "समय");
        hi.put("total_records", "कुल रिकॉर्ड");
        hi.put("best", "सबसे अच्छा");
        hi.put("worst", "सबसे खराब");
        hi.put("clear_history", "इतिहास मिटाएं");
        hi.put("settings", "सेटिंग्स");
        hi.put("language", "भाषा");
        hi.put("floating_window", "फ्लोटिंग विंडो");
        hi.put("window_color", "विंडो रंग");
        hi.put("window_transparency", "पारदर्शिता");
        hi.put("window_size", "आकार");
        hi.put("show_window", "विंडो दिखाएं");
        hi.put("hide_window", "विंडो छुपाएं");
        hi.put("data_analytics", "डेटा एनालिटिक्स");
        hi.put("select_date", "तारीख चुनें");
        hi.put("timeframe", "समय सीमा");
        hi.put("per_app_data", "ऐप अनुसार डेटा");
        hi.put("top_speed", "सबसे तेज़ गति");
        hi.put("lowest_speed", "सबसे धीमी गति");
        hi.put("data_usage", "डेटा उपयोग");
        hi.put("no_data", "कोई डेटा नहीं");
        hi.put("good", "अच्छा");
        hi.put("moderate", "मध्यम");
        hi.put("weak", "कमजोर");
        hi.put("excellent", "उत्कृष्ट");
        hi.put("very_good", "बहुत अच्छा");
        hi.put("fair", "ठीकठाक");
        hi.put("poor", "खराब");
        translations.put(LANG_HINDI, hi);
        
        HashMap<String, String> ta = new HashMap<>();
        ta.put("app_name", "ட்ரூ நெட்வொர்க்");
        ta.put("true_network", "ட்ரூ நெட்வொர்க்");
        ta.put("ping", "பிங்");
        ta.put("signal", "சிக்னல்");
        ta.put("operator", "ஆபரேட்டர்");
        ta.put("running", "இயங்குகிறது");
        ta.put("minutes", "நிமிடங்கள்");
        ta.put("seconds", "விநாடிகள்");
        ta.put("data_used", "பயன்பாட்டிற்குப் பிறகு");
        ta.put("network_history", "நெட்வொர்க் வரலாறு");
        ta.put("time", "நேரம்");
        ta.put("total_records", "மொத்த பதிவுகள்");
        ta.put("best", "சிறந்த");
        ta.put("worst", "மோசமான");
        ta.put("clear_history", "வரலாற்றை அழி");
        ta.put("settings", "அமைப்புகள்");
        ta.put("language", "மொழி");
        ta.put("floating_window", "மிதக்கும் சாளரம்");
        ta.put("window_color", "சாளர நிறம்");
        ta.put("window_transparency", "பளிங்குத்தன்மை");
        ta.put("window_size", "அளவு");
        ta.put("show_window", "சாளரத்தைக் காட்டு");
        ta.put("hide_window", "சாளரத்தை மறை");
        ta.put("data_analytics", "தரவு பகுப்பாய்வு");
        ta.put("select_date", "தேதியைத் தேர்வு செய்");
        ta.put("timeframe", "காலக்கெடு");
        ta.put("per_app_data", "பயன்பாடு வாரியாக தரவு");
        ta.put("top_speed", "அதிகபட்ச வேகம்");
        ta.put("lowest_speed", "ஏற்றுக்குறைந்த வேகம்");
        ta.put("data_usage", "தரவு பயன்பாடு");
        ta.put("no_data", "தரவு இல்லை");
        ta.put("good", "நல்ல");
        ta.put("moderate", "மிதமான");
        ta.put("weak", "பலவீனமான");
        ta.put("excellent", "சிறந்த");
        ta.put("very_good", "மிக நல்ல");
        ta.put("fair", "சரியான");
        ta.put("poor", "மோசமான");
        translations.put(LANG_TAMIL, ta);
        
        HashMap<String, String> te = new HashMap<>();
        te.put("app_name", "ట్రూ నెట్‌వర్క్");
        te.put("true_network", "ట్రూ నెట్‌వర్క్");
        te.put("ping", "పింగ్");
        te.put("signal", "సిగ్నల్");
        te.put("operator", "ఆపరేటర్");
        te.put("running", "నడుస్తోంది");
        te.put("minutes", "నిమిషాలు");
        te.put("seconds", "సెకన్లు");
        te.put("data_used", "యాప్ ప్రారంభించిన తర్వాత");
        te.put("network_history", "నెట్‌వర్క్ చరిత్ర");
        te.put("time", "సమయం");
        te.put("total_records", "మొత్తం రికార్డులు");
        te.put("best", "ఉత్తమం");
        te.put("worst", "చెత్త");
        te.put("clear_history", "చరిత్రను తొలగించు");
        te.put("settings", "సెట్టింగ్స్");
        te.put("language", "భాష");
        te.put("floating_window", "ఫ్లోటింగ్ విండో");
        te.put("window_color", "విండో రంగు");
        te.put("window_transparency", "పారదర్శకత");
        te.put("window_size", "పరిమాణం");
        te.put("show_window", "విండో చూపు");
        te.put("hide_window", "విండో దాచు");
        te.put("data_analytics", "డేటా ఎనలిటిక్స్");
        te.put("select_date", "తేదీ ఎంచుకోండి");
        te.put("timeframe", "సమయ వ్యవధి");
        te.put("per_app_data", "యాప్ వారీగా డేటా");
        te.put("top_speed", "అత్యధిక వేగం");
        te.put("lowest_speed", "కనిష్ట వేగం");
        te.put("data_usage", "డేటా వినియోగం");
        te.put("no_data", "డేటా లేదు");
        te.put("good", "మంచిది");
        te.put("moderate", "మధ్యస్థం");
        te.put("weak", "బలహీనమైన");
        te.put("excellent", "అద్భుతమైన");
        te.put("very_good", "చాలా మంచిది");
        te.put("fair", "సరిపోయే");
        te.put("poor", "చెత్త");
        translations.put(LANG_TELUGU, te);
        
        HashMap<String, String> mr = new HashMap<>();
        mr.put("app_name", "ट्रू नेटवर्क");
        mr.put("true_network", "ट्रू नेटवर्क");
        mr.put("ping", "पिंग");
        mr.put("signal", "सिग्नल");
        mr.put("operator", "ऑपरेटर");
        mr.put("running", "चालू आहे");
        mr.put("minutes", "मिनिटे");
        mr.put("seconds", "सेकंद");
        mr.put("data_used", "अ‍ॅप सुरू झाल्यावर");
        mr.put("network_history", "नेटवर्क इतिहास");
        mr.put("time", "वेळ");
        mr.put("total_records", "एकूण रेकॉर्ड्स");
        mr.put("best", "सर्वोत्तम");
