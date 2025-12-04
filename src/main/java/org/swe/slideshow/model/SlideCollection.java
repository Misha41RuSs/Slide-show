package org.swe.slideshow.model;

import java.util.Arrays;

class SlideCollection {
    private static final int DEFAULT_CAPACITY = 16;
    private AlbumItem[] items;
    private int size;

    SlideCollection() {
        items = new AlbumItem[DEFAULT_CAPACITY];
    }

    void add(AlbumItem item) {
        if (item == null || item.getImagePath() == null || item.getImagePath().isBlank()) {
            return;
        }
        ensureCapacity(size + 1);
        items[size++] = item;
    }

    AlbumItem getItem(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        return items[index];
    }

    int size() {
        return size;
    }

    void clear() {
        Arrays.fill(items, 0, size, null);
        size = 0;
    }

    private void ensureCapacity(int desiredCapacity) {
        if (desiredCapacity <= items.length) {
            return;
        }
        int newCapacity = items.length;
        while (newCapacity < desiredCapacity) {
            newCapacity *= 2;
        }
        items = Arrays.copyOf(items, newCapacity);
    }
}


