package org.swe.slideshow.model;

import javafx.scene.image.Image;

public class SlideNavigator {
    private final ConcreteAggregate aggregate;
    private Iterator iterator;

    public SlideNavigator(ConcreteAggregate aggregate) {
        this.aggregate = aggregate;
        refresh();
    }

    public Image nextImage() {
        if (iterator == null) {
            return null;
        }
        if (iterator.hasNext(1)) {
            return (Image) iterator.next();
        }
        refresh();
        return iterator != null ? (Image) iterator.next() : null;
    }

    public Image previousImage() {
        if (iterator == null) {
            return null;
        }
        return (Image) iterator.preview();
    }

    public void refresh() {
        iterator = aggregate != null ? aggregate.getIterator() : null;
    }

    public int currentIndex() {
        return iterator != null ? iterator.getCurrentIndex() : 0;
    }

    public AlbumItem currentItem() {
        return iterator != null ? iterator.getCurrentItem() : null;
    }

    public int totalSlides() {
        return aggregate != null ? aggregate.getImageCount() : 0;
    }

    public boolean hasSlides() {
        return aggregate != null && aggregate.getImageCount() > 0;
    }
}

