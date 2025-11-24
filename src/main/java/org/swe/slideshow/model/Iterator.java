package org.swe.slideshow.model;

public interface Iterator {
    boolean hasNext(int x);
    Object next();
    Object preview();
    int getCurrentIndex();
    String getCurrentItemId();
}



