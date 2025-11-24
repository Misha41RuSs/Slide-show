package org.swe.slideshow.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImpressionStore {
    private static final Pattern ENTRY_PATTERN = Pattern.compile(
            "\"((?:\\\\.|[^\\\\\"])*)\"\\s*:\\s*\\{\\s*\"text\"\\s*:\\s*\"((?:\\\\.|[^\\\\\"])*)\"\\s*,\\s*\"emotion\"\\s*:\\s*\"((?:\\\\.|[^\\\\\"])*)\"\\s*}\\s*(?:,|$)",
            Pattern.DOTALL);

    public record ImpressionRecord(String text, String emotion) {}

    private final Path storageFile;
    private final Map<String, ImpressionRecord> impressions;
    private boolean initialized;

    public ImpressionStore() {
        this(Paths.get(System.getProperty("user.home"), ".slideshow-impressions.json"));
    }

    public ImpressionStore(Path storageFile) {
        this.storageFile = storageFile;
        this.impressions = new HashMap<>();
    }

    public synchronized ImpressionRecord getImpression(String imagePath) {
        ensureLoaded();
        if (imagePath == null) {
            return null;
        }
        return impressions.get(imagePath);
    }

    public synchronized void saveImpression(String imagePath, String text, String emotion) {
        ensureLoaded();
        if (imagePath == null || imagePath.isBlank()) {
            return;
        }

        boolean isTextEmpty = text == null || text.isBlank();
        boolean isEmotionEmpty = emotion == null || emotion.isBlank();

        if (isTextEmpty && isEmotionEmpty) {
            impressions.remove(imagePath);
        } else {
            impressions.put(imagePath, new ImpressionRecord(
                    isTextEmpty ? "" : text,
                    isEmotionEmpty ? "" : emotion));
        }
        persist();
    }

    private void ensureLoaded() {
        if (initialized) {
            return;
        }
        impressions.clear();
        if (Files.exists(storageFile)) {
            loadFromDisk();
        }
        initialized = true;
    }

    private void loadFromDisk() {
        try {
            String content = Files.readString(storageFile, StandardCharsets.UTF_8).trim();
            if (content.length() < 2 || content.charAt(0) != '{' ||
                    content.charAt(content.length() - 1) != '}') {
                return;
            }
            String body = content.substring(1, content.length() - 1);
            Matcher matcher = ENTRY_PATTERN.matcher(body);
            while (matcher.find()) {
                String key = unescape(matcher.group(1));
                String text = unescape(matcher.group(2));
                String emotion = unescape(matcher.group(3));
                impressions.put(key, new ImpressionRecord(text, emotion));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void persist() {
        try {
            Path parent = storageFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            StringBuilder builder = new StringBuilder();
            builder.append("{\n");
            int index = 0;
            for (Map.Entry<String, ImpressionRecord> entry : impressions.entrySet()) {
                builder.append("  \"")
                        .append(escape(entry.getKey()))
                        .append("\": {\n")
                        .append("    \"text\": \"")
                        .append(escape(coalesce(entry.getValue().text())))
                        .append("\",\n")
                        .append("    \"emotion\": \"")
                        .append(escape(coalesce(entry.getValue().emotion())))
                        .append("\"\n")
                        .append("  }");
                if (index < impressions.size() - 1) {
                    builder.append(",\n");
                } else {
                    builder.append('\n');
                }
                index++;
            }
            builder.append("}");

            Files.writeString(storageFile,
                    builder.toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String coalesce(String value) {
        return value == null ? "" : value;
    }

    private String escape(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
                }
            }
        }
        return builder.toString();
    }

    private String unescape(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\\' && i + 1 < value.length()) {
                char next = value.charAt(i + 1);
                switch (next) {
                    case '\\' -> builder.append('\\');
                    case '"' -> builder.append('"');
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case 'u' -> {
                        if (i + 5 < value.length()) {
                            String hex = value.substring(i + 2, i + 6);
                            try {
                                builder.append((char) Integer.parseInt(hex, 16));
                            } catch (NumberFormatException e) {
                                // Skip invalid sequence
                            }
                            i += 4;
                        }
                    }
                    default -> builder.append(next);
                }
                i++;
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }
}

