package org.swe.slideshow.model;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.Font;

public class Indicator {
    private VBox panel = new VBox(8);
    private Rectangle progressBar;
    private Rectangle backgroundBar;
    private Arc progressArc;
    private Circle backgroundCircle;
    private Text titleText;
    private Text percentText;
    private Text fractionText;
    private static final double FIXED_BAR_WIDTH = 500.0;
    private static final double BAR_HEIGHT = 25.0;
    private static final double CIRCLE_RADIUS = 80.0;
    private static final double CIRCLE_STROKE_WIDTH = 15.0;
    private float startValue = 0;
    private float stopValue = 100;
    private float currentValue = 0;
    private Color normalColor = Color.LIGHTGRAY;
    private Color selectedColor = Color.BLUE;
    private Pane progressPane;
    private Pane circularPane;
    private HBox textBox;
    private boolean isCircular = false;
    private Text centerTimeText;
    
    public Indicator() {
        panel.setPadding(new Insets(10));
        panel.setAlignment(Pos.CENTER);
        panel.setPrefWidth(FIXED_BAR_WIDTH + 40);
        panel.setPrefHeight(180);
        panel.setMinHeight(180);
    }
    
    public void setCircularSize(double width, double height) {
        panel.setPrefWidth(width);
        panel.setPrefHeight(height);
        panel.setMinWidth(width);
        panel.setMinHeight(height);
    }
    
    public void setPaint(Color normalColor) {
        this.normalColor = normalColor;
    }
    
    public void setSelectedColor(Color selectedColor) {
        this.selectedColor = selectedColor;
    }
    
    public void setBounds(float start, float stop) {
        this.startValue = start;
        this.stopValue = stop;
    }
    
    public void setCurrentValue(float value) {
        this.currentValue = value;
        updateProgress();
    }
    
    public void setTitle(String title) {
        if (titleText != null) {
            titleText.setText(title);
        } else {
            addTitle(title);
        }
    }
    
    public void addTitle(String title) {
        titleText = new Text(title);
        titleText.setFont(new Font(14));
        panel.getChildren().add(0, titleText);
    }
    
    public void addBoundsLine(float start, float stop) {
        this.startValue = start;
        this.stopValue = stop;
    }
    
    public void addProgressBar(float measure) {
        this.currentValue = measure;

        if (progressPane == null) {
            progressPane = new Pane();
            progressPane.setPrefWidth(FIXED_BAR_WIDTH);
            progressPane.setPrefHeight(BAR_HEIGHT + 10);
            progressPane.setMinWidth(FIXED_BAR_WIDTH);
            progressPane.setMinHeight(BAR_HEIGHT + 10);
            progressPane.setMaxWidth(FIXED_BAR_WIDTH);

            backgroundBar = new Rectangle(0, 0, FIXED_BAR_WIDTH, BAR_HEIGHT);
            backgroundBar.setFill(normalColor);
            backgroundBar.setLayoutX(0);
            backgroundBar.setLayoutY(5);
            backgroundBar.setArcWidth(5);
            backgroundBar.setArcHeight(5);
            backgroundBar.setStroke(Color.DARKGRAY);
            backgroundBar.setStrokeWidth(1);

            double initialWidth = calculateProgressWidth();
            progressBar = new Rectangle(0, 0, initialWidth, BAR_HEIGHT);
            progressBar.setFill(selectedColor);
            progressBar.setLayoutX(0);
            progressBar.setLayoutY(5);
            progressBar.setArcWidth(5);
            progressBar.setArcHeight(5);

            progressPane.getChildren().addAll(backgroundBar, progressBar);

            int insertIndex = 1;
            if (panel.getChildren().size() < insertIndex) {
                insertIndex = panel.getChildren().size();
            }
            panel.getChildren().add(insertIndex, progressPane);
        } else {
            double progressWidth = calculateProgressWidth();
            if (progressWidth >= 0 && progressWidth <= FIXED_BAR_WIDTH) {
                progressBar.setWidth(Math.max(progressWidth, 2.0));
            }
        }
    }
    
    private double calculateProgressWidth() {
        if (stopValue == startValue) return 0;
        double progress = ((currentValue - startValue) / (stopValue - startValue));
        if (progress < 0) progress = 0;
        if (progress > 1) progress = 1;
        double width = progress * FIXED_BAR_WIDTH;
        if (width == 0 && currentValue >= startValue) {
            width = 2.0;
        }
        return width;
    }
    
    public void addMark(String measure) {

    }
    
    public void addCircularProgress(float measure) {
        this.currentValue = measure;
        this.isCircular = true;
        
        if (circularPane == null) {
            circularPane = new Pane();
            double size = (CIRCLE_RADIUS + CIRCLE_STROKE_WIDTH) * 2;
            circularPane.setPrefWidth(size);
            circularPane.setPrefHeight(size);
            circularPane.setMinWidth(size);
            circularPane.setMinHeight(size);
            
            double centerX = CIRCLE_RADIUS + CIRCLE_STROKE_WIDTH;
            double centerY = CIRCLE_RADIUS + CIRCLE_STROKE_WIDTH;

            backgroundCircle = new Circle(centerX, centerY, CIRCLE_RADIUS);
            backgroundCircle.setFill(Color.TRANSPARENT);
            backgroundCircle.setStroke(normalColor);
            backgroundCircle.setStrokeWidth(CIRCLE_STROKE_WIDTH);

            double initialAngle = calculateCircularAngle();
            progressArc = new Arc(centerX, centerY, CIRCLE_RADIUS, CIRCLE_RADIUS, 90, initialAngle);
            progressArc.setType(ArcType.OPEN);
            progressArc.setFill(Color.TRANSPARENT);
            progressArc.setStroke(selectedColor);
            progressArc.setStrokeWidth(CIRCLE_STROKE_WIDTH);
            progressArc.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

            centerTimeText = new Text();
            centerTimeText.setFont(new Font(18));
            centerTimeText.setFill(selectedColor);
            centerTimeText.setLayoutX(centerX - 30);
            centerTimeText.setLayoutY(centerY + 5);
            centerTimeText.setText("00:00");
            
            circularPane.getChildren().addAll(backgroundCircle, progressArc, centerTimeText);
            
            int insertIndex = 1;
            if (panel.getChildren().size() < insertIndex) {
                insertIndex = panel.getChildren().size();
            }
            panel.getChildren().add(insertIndex, circularPane);
        } else {
            double angle = calculateCircularAngle();
            progressArc.setLength(angle);
        }
    }
    
    private double calculateCircularAngle() {
        if (stopValue == startValue) return 0;
        double progress = ((currentValue - startValue) / (stopValue - startValue));
        if (progress < 0) progress = 0;
        if (progress > 1) progress = 1;
        return -progress * 360.0;
    }
    
    public void setPercentAndFraction(int current, int total) {
        if (textBox == null) {
            textBox = new HBox(15);
            textBox.setAlignment(Pos.CENTER);

            percentText = new Text();
            percentText.setFont(new Font(16));
            percentText.setFill(selectedColor);
            textBox.getChildren().add(percentText);

            fractionText = new Text();
            fractionText.setFont(new Font(16));
            fractionText.setFill(Color.BLACK);
            textBox.getChildren().add(fractionText);

            panel.getChildren().add(textBox);
        }

        updatePercentAndFraction(current, total);
    }
    
    private void updatePercentAndFraction(int current, int total) {
        if (percentText != null && fractionText != null) {
            double percent = total > 0 ? (current * 100.0 / total) : 0;
            percentText.setText(String.format("%.0f%%", percent));
            fractionText.setText(current + " / " + total);
        }
    }
    
    public void setTimeDisplay(float seconds) {
        if (isCircular && centerTimeText != null) {
            int minutes = (int)(seconds / 60);
            int secs = (int)(seconds % 60);
            centerTimeText.setText(String.format("%02d:%02d", minutes, secs));
        } else {
            if (textBox == null) {
                textBox = new HBox(15);
                textBox.setAlignment(Pos.CENTER);
                
                percentText = new Text();
                percentText.setFont(new Font(16));
                percentText.setFill(selectedColor);
                textBox.getChildren().add(percentText);
                
                fractionText = new Text();
                fractionText.setFont(new Font(16));
                fractionText.setFill(Color.BLACK);
                textBox.getChildren().add(fractionText);
                
                panel.getChildren().add(textBox);
            }
            
            if (percentText != null && fractionText != null) {
                int minutes = (int)(seconds / 60);
                int secs = (int)(seconds % 60);
                percentText.setText(String.format("%02d:%02d", minutes, secs));
                fractionText.setText(String.format("%.1f сек", seconds));
            }
        }
    }
    
    private void updateProgress() {
        if (isCircular) {
            if (progressArc != null && startValue != stopValue) {
                double angle = calculateCircularAngle();
                progressArc.setLength(angle);
            }
        } else {
            if (progressBar != null && startValue != stopValue) {
                double progressWidth = calculateProgressWidth();
                if (progressWidth >= 0 && progressWidth <= FIXED_BAR_WIDTH) {
                    progressBar.setWidth(progressWidth);
                }
            }
        }
    }
    
    public void updateProgress(int current, int total) {
        setCurrentValue(current);
        updatePercentAndFraction(current, total);
        updateProgress();
    }
    
    public void show(Pane parentPane) {
        if (parentPane != null && !parentPane.getChildren().contains(panel)) {
            parentPane.getChildren().add(panel);
        }
    }
    
    public VBox getPanel() {
        return panel;
    }
    
    public void setMaxWidth(double width) {

    }
    
    public void setMaxHeight(double height) {
        panel.setPrefHeight(height);
    }
}




