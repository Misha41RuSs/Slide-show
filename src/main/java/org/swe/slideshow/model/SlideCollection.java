package org.swe.slideshow.model;

import java.util.Arrays;

class SlideCollection {
    private static final int DEFAULT_CAPACITY = 16;
    private String[] imagePaths;
    private int size;

    SlideCollection() {
        imagePaths = new String[DEFAULT_CAPACITY];
    }

    void add(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return;
        }
        ensureCapacity(size + 1);
        imagePaths[size++] = imagePath;
    }

    String getPath(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        return imagePaths[index];
    }

    int size() {
        return size;
    }

    void clear() {
        Arrays.fill(imagePaths, 0, size, null);
        size = 0;
    }

    private void ensureCapacity(int desiredCapacity) {
        if (desiredCapacity <= imagePaths.length) {
            return;
        }
        int newCapacity = imagePaths.length;
        while (newCapacity < desiredCapacity) {
            newCapacity *= 2;
        }
        imagePaths = Arrays.copyOf(imagePaths, newCapacity);
    }
}


