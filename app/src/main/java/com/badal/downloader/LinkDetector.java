package com.badal.downloader;
public class LinkDetector {
    public static String detect(String url) {
        String lower = url.toLowerCase();
        if (lower.contains("instagram.com") || lower.contains("instagr.am")) return "INSTAGRAM";
        if (lower.contains("facebook.com") || lower.contains("fb.watch") || lower.contains("fb.com")) return "FACEBOOK";
        if (lower.contains("youtube.com") || lower.contains("youtu.be")) return "YOUTUBE";
        if (lower.contains("twitter.com") || lower.contains("x.com")) return "TWITTER";
        if (lower.contains("tiktok.com")) return "TIKTOK";
        if (lower.contains("threads.net")) return "THREADS";
        return null;
    }
    public static boolean isValidLink(String url) {
        return detect(url) != null;
    }
}
