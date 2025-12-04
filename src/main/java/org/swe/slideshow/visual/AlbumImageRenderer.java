package org.swe.slideshow.visual;

import org.swe.slideshow.model.AlbumItem;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public final class AlbumImageRenderer {
    private static final int BORDER = 28;
    private static final int FOOTER_HEIGHT = 90;
    private static final int FOOTER_PADDING = 20;
    private static final int MAX_LINES = 3;

    private AlbumImageRenderer() {
    }

    public static void renderWithEmotion(Path sourceImage,
                                         Path destinationImage,
                                         AlbumItem item) throws IOException {
        BufferedImage original = ImageIO.read(sourceImage.toFile());
        if (original == null) {
            throw new IOException("Unsupported image format: " + sourceImage);
        }

        EmotionPalette.EmotionStyle style = EmotionPalette.styleFor(item != null ? item.getEmotion() : null);
        int width = original.getWidth() + BORDER * 2;
        int height = original.getHeight() + BORDER * 2 + FOOTER_HEIGHT;

        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.setColor(style.awtColor());
            g.fillRoundRect(0, 0, width, height, 30, 30);

            g.drawImage(original, BORDER, BORDER, null);

            String caption = buildCaption(item);
            if (!caption.isBlank()) {
                drawCaption(g, caption, width, height);
            }
        } finally {
            g.dispose();
        }

        ImageIO.write(canvas, "png", destinationImage.toFile());
    }

    private static void drawCaption(Graphics2D g, String caption, int width, int height) {
        int footerWidth = width - BORDER * 2;
        int footerX = BORDER;
        int footerY = height - FOOTER_HEIGHT - BORDER / 2;

        g.setColor(new Color(0, 0, 0, 190));
        g.fillRoundRect(footerX, footerY, footerWidth, FOOTER_HEIGHT, 20, 20);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        FontMetrics metrics = g.getFontMetrics();
        int lineHeight = metrics.getHeight();
        int textY = footerY + FOOTER_PADDING + metrics.getAscent();
        int availableWidth = footerWidth - FOOTER_PADDING * 2;

        String[] words = caption.split("\\s+");
        StringBuilder line = new StringBuilder();
        int linesDrawn = 0;

        for (String word : words) {
            String prospective = line.length() == 0 ? word : line + " " + word;
            if (metrics.stringWidth(prospective) > availableWidth && line.length() > 0) {
                g.drawString(line.toString(), footerX + FOOTER_PADDING, textY);
                linesDrawn++;
                if (linesDrawn >= MAX_LINES) {
                    return;
                }
                line = new StringBuilder(word);
                textY += lineHeight;
            } else {
                if (line.length() > 0) {
                    line.append(" ");
                }
                line.append(word);
            }
        }

        if (line.length() > 0 && linesDrawn < MAX_LINES) {
            g.drawString(line.toString(), footerX + FOOTER_PADDING, textY);
        }
    }

    private static String buildCaption(AlbumItem item) {
        if (item == null) {
            return "";
        }
        String emotion = item.getEmotion();
        String impression = item.getImpressionText();

        StringBuilder builder = new StringBuilder();
        if (emotion != null && !emotion.isBlank()) {
            builder.append(emotion.trim());
        } else {
            builder.append(EmotionPalette.DEFAULT_EMOTION);
        }
        if (impression != null && !impression.isBlank()) {
            if (builder.length() > 0) {
                builder.append(" • ");
            }
            builder.append(impression.trim());
        }
        return builder.toString();
    }
}

