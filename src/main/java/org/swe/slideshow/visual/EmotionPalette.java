package org.swe.slideshow.visual;

import javafx.scene.paint.Color;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EmotionPalette {
    public static final String DEFAULT_EMOTION = "🙂 Нейтрально";

    private static final EmotionStyle DEFAULT_STYLE = new EmotionStyle(DEFAULT_EMOTION, "#9E9E9E");

    private static final Map<String, EmotionStyle> STYLES = new LinkedHashMap<>();

    static {
        STYLES.put(DEFAULT_EMOTION, DEFAULT_STYLE);
        STYLES.put("😊 Радость", new EmotionStyle("😊 Радость", "#FFC107"));
        STYLES.put("🤩 Восхищение", new EmotionStyle("🤩 Восхищение", "#FF6F61"));
        STYLES.put("😮 Удивление", new EmotionStyle("😮 Удивление", "#03A9F4"));
        STYLES.put("😢 Грусть", new EmotionStyle("😢 Грусть", "#5C6BC0"));
        STYLES.put("😌 Спокойствие", new EmotionStyle("😌 Спокойствие", "#4DB6AC"));
        STYLES.put("😎 Вдохновение", new EmotionStyle("😎 Вдохновение", "#8BC34A"));
    }

    private EmotionPalette() {
    }

    public static EmotionStyle styleFor(String emotion) {
        if (emotion == null || emotion.isBlank()) {
            return DEFAULT_STYLE;
        }
        return STYLES.getOrDefault(emotion, DEFAULT_STYLE);
    }

    public record EmotionStyle(String label, String hexColor) {
        public Color fxColor() {
            return Color.web(hexColor);
        }

        public java.awt.Color awtColor() {
            return java.awt.Color.decode(hexColor);
        }

        public String cssColor() {
            return hexColor;
        }
    }
}

