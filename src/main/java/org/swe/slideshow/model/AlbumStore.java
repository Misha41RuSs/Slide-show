package org.swe.slideshow.model;

import org.swe.slideshow.visual.AlbumImageRenderer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AlbumStore {
    private static final Pattern ITEM_PATTERN = Pattern.compile(
            "\\{\\s*\"imagePath\"\\s*:\\s*\"((?:\\\\.|[^\\\\\"])*)\"\\s*,\\s*\"text\"\\s*:\\s*\"((?:\\\\.|[^\\\\\"])*)\"\\s*,\\s*\"emotion\"\\s*:\\s*\"((?:\\\\.|[^\\\\\"])*)\"\\s*}\\s*(?:,|$)",
            Pattern.DOTALL);

    public static void saveAlbum(Path albumDirectory, AlbumItem[] items) throws IOException {
        if (albumDirectory == null || items == null) {
            throw new IllegalArgumentException("Album directory and items cannot be null");
        }

        Files.createDirectories(albumDirectory);
        Path imagesDir = albumDirectory.resolve("images");
        Files.createDirectories(imagesDir);

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\n  \"items\": [\n");

        java.util.List<String> itemJsonList = new java.util.ArrayList<>();

        for (AlbumItem item : items) {
            if (item == null || item.getImagePath() == null) {
                continue;
            }

            Path sourceImage = Paths.get(item.getImagePath());
            if (!Files.exists(sourceImage)) {
                continue;
            }

            String originalName = sourceImage.getFileName().toString();
            String baseName = originalName.contains(".")
                    ? originalName.substring(0, originalName.lastIndexOf('.'))
                    : originalName;
            Path destImage = imagesDir.resolve(baseName + ".png");

            int copyIndex = 1;
            while (Files.exists(destImage)) {
                destImage = imagesDir.resolve(baseName + "_" + copyIndex + ".png");
                copyIndex++;
            }

            AlbumImageRenderer.renderWithEmotion(sourceImage, destImage, item);

            String relativePath = "images/" + destImage.getFileName().toString();
            StringBuilder itemBuilder = new StringBuilder();
            itemBuilder.append("    {\n")
                    .append("      \"imagePath\": \"")
                    .append(escape(relativePath))
                    .append("\",\n")
                    .append("      \"text\": \"")
                    .append(escape(item.getImpressionText() != null ? item.getImpressionText() : ""))
                    .append("\",\n")
                    .append("      \"emotion\": \"")
                    .append(escape(item.getEmotion() != null ? item.getEmotion() : ""))
                    .append("\"\n")
                    .append("    }");
            itemJsonList.add(itemBuilder.toString());
        }

        for (int i = 0; i < itemJsonList.size(); i++) {
            jsonBuilder.append(itemJsonList.get(i));
            if (i < itemJsonList.size() - 1) {
                jsonBuilder.append(",");
            }
            jsonBuilder.append("\n");
        }

        jsonBuilder.append("  ]\n}");

        Path jsonFile = albumDirectory.resolve("album.json");
        Files.writeString(jsonFile, jsonBuilder.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    public static AlbumItem[] loadAlbum(Path albumDirectory) throws IOException {
        if (albumDirectory == null || !Files.exists(albumDirectory)) {
            throw new IllegalArgumentException("Album directory does not exist");
        }

        Path jsonFile = albumDirectory.resolve("album.json");
        if (!Files.exists(jsonFile)) {
            throw new IOException("album.json not found in album directory");
        }

        String content = Files.readString(jsonFile, StandardCharsets.UTF_8).trim();
        if (content.length() < 10 || !content.contains("\"items\"")) {
            throw new IOException("Invalid album.json format");
        }

        int itemsStart = content.indexOf('[');
        int itemsEnd = content.lastIndexOf(']');
        if (itemsStart < 0 || itemsEnd < 0 || itemsEnd <= itemsStart) {
            throw new IOException("Invalid album.json format: items array not found");
        }

        String itemsBody = content.substring(itemsStart + 1, itemsEnd);
        java.util.List<AlbumItem> items = new java.util.ArrayList<>();

        Matcher matcher = ITEM_PATTERN.matcher(itemsBody);
        while (matcher.find()) {
            String relativeImagePath = unescape(matcher.group(1));
            String text = unescape(matcher.group(2));
            String emotion = unescape(matcher.group(3));

            Path imagePath = albumDirectory.resolve(relativeImagePath);
            if (Files.exists(imagePath)) {
                items.add(new AlbumItem(imagePath.toAbsolutePath().toString(), text, emotion));
            }
        }

        return items.toArray(new AlbumItem[0]);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
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

    private static String unescape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
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
                            if (isHexSequence(hex)) {
                                builder.append((char) Integer.parseInt(hex, 16));
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

    private static boolean isHexSequence(String value) {
        if (value == null || value.length() != 4) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), 16) == -1) {
                return false;
            }
        }
        return true;
    }
}

