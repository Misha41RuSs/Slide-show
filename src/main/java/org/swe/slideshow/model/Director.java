package org.swe.slideshow.model;

public class Director {
    
    public Indicator constructSlideShowIndicator(Builder builder, int totalSlides) {
        builder.addTitle("Прогресс слайд-шоу");
        builder.setView(totalSlides, 'g', 'b');
        builder.lineBounds(1, totalSlides);
        builder.linePaint(1);
        builder.lineMark("1");
        return builder.build();
    }
    
    public Indicator constructTimeIndicator(Builder builder, float startTime, float endTime, float currentTime) {
        builder.addTitle("Таймер");
        builder.setView(10, 'g', 'p');
        builder.lineBounds(startTime, endTime);
        builder.circularPaint(currentTime);
        builder.lineMark(String.format("%.1f", currentTime));
        Indicator indicator = builder.build();
        double circleSize = (80.0 + 15.0) * 2 + 40;
        indicator.setCircularSize(circleSize, circleSize + 50);
        return indicator;
    }
    
    public Indicator constructCustomIndicator(Builder builder, String title, float start, float stop, float measure, char norm, char select) {
        builder.setView(10, norm, select);
        builder.lineBounds(start, stop);
        builder.linePaint(measure);
        builder.lineMark(String.format("%.1f", measure));
        builder.addTitle(title);
        return builder.build();
    }
}




