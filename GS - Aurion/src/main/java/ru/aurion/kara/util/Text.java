package ru.aurion.kara.util;

import org.bukkit.ChatColor;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Text {
    private static final Pattern DURATION = Pattern.compile("(\\d+)([smhdw])", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.##");

    private Text() {}

    public static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }

    public static long parseDuration(String input) {
        Matcher matcher = DURATION.matcher(input);
        long seconds = 0;
        int end = 0;
        while (matcher.find()) {
            if (matcher.start() != end) return -1;
            long value = Long.parseLong(matcher.group(1));
            seconds = Math.addExact(seconds, switch (matcher.group(2).toLowerCase()) {
                case "s" -> value;
                case "m" -> Math.multiplyExact(value, 60);
                case "h" -> Math.multiplyExact(value, 3600);
                case "d" -> Math.multiplyExact(value, 86400);
                case "w" -> Math.multiplyExact(value, 604800);
                default -> 0;
            });
            end = matcher.end();
        }
        return end == input.length() && seconds > 0 ? seconds * 1000 : -1;
    }

    public static String date(long epochMillis) {
        return DATE.format(Instant.ofEpochMilli(epochMillis));
    }

    public static String money(double value) {
        return MONEY.format(value);
    }

    public static String plain(String value, int max) {
        if (value == null) return "";
        String normalized = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }
}
