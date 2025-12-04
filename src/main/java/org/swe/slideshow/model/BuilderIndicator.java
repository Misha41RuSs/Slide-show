package org.swe.slideshow.model;

import javafx.scene.paint.Color;

public class BuilderIndicator implements Builder {
    private final Indicator indicator = new Indicator();
    
    @Override
    public void setView(int N, char norm, char select) {
        Color normalColor = getColorFromChar(norm);
        Color selectedColor = getColorFromChar(select);
        
        indicator.setPaint(normalColor);
        indicator.setSelectedColor(selectedColor);
    }
    
    private Color getColorFromChar(char c) {
        switch (c) {
            case 'r': case 'R': return Color.RED;
            case 'g': case 'G': return Color.LIGHTGRAY;
            case 'b': case 'B': return Color.BLUE;
            case 'y': case 'Y': return Color.YELLOW;
            case 'o': case 'O': return Color.ORANGE;
            case 'p': case 'P': return Color.PURPLE;
            case 'c': case 'C': return Color.CYAN;
            default: return Color.LIGHTGRAY;
        }
    }
    
    @Override
    public void lineBounds(float start, float stop) {
        indicator.setBounds(start, stop);
        indicator.addBoundsLine(start, stop);
    }
    
    @Override
    public void linePaint(float measure) {
        indicator.addProgressBar(measure);
    }
    
    @Override
    public void circularPaint(float measure) {
        indicator.addCircularProgress(measure);
    }
    
    @Override
    public void lineMark(String measure) {
        indicator.addMark(measure);
    }
    
    @Override
    public void addTitle(String name) {
        indicator.addTitle(name);
    }
    
    @Override
    public Indicator build() {
        return indicator;
    }
}




