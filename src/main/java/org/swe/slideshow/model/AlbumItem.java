package org.swe.slideshow.model;

public class AlbumItem {
    private final String imagePath;
    private String impressionText;
    private String emotion;

    public AlbumItem(String imagePath) {
        this.imagePath = imagePath;
        this.impressionText = "";
        this.emotion = "";
    }

    public AlbumItem(String imagePath, String impressionText, String emotion) {
        this.imagePath = imagePath;
        this.impressionText = impressionText != null ? impressionText : "";
        this.emotion = emotion != null ? emotion : "";
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getImpressionText() {
        return impressionText;
    }

    public void setImpressionText(String impressionText) {
        this.impressionText = impressionText != null ? impressionText : "";
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion != null ? emotion : "";
    }

    public boolean hasImpression() {
        return (impressionText != null && !impressionText.isBlank()) ||
               (emotion != null && !emotion.isBlank());
    }
}



