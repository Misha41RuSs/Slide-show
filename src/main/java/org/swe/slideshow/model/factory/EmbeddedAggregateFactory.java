package org.swe.slideshow.model.factory;

import org.swe.slideshow.model.ConcreteAggregate;
import org.swe.slideshow.model.SlideNavigator;
import org.swe.slideshow.model.embedded.EmbeddedAlbum;
import org.swe.slideshow.model.embedded.EmbeddedImageManager;

import java.io.IOException;

public class EmbeddedAggregateFactory implements AggregateComponentsFactory {
    private final EmbeddedImageManager manager;
    private final EmbeddedAlbum album;

    public EmbeddedAggregateFactory(EmbeddedImageManager manager, EmbeddedAlbum album) {
        this.manager = manager;
        this.album = album;
    }

    @Override
    public ConcreteAggregate createAggregate() throws IOException {
        ConcreteAggregate aggregate = new ConcreteAggregate("", "*");
        aggregate.loadFromAlbumItems(manager.loadAlbumItems(album));
        return aggregate;
    }

    @Override
    public SlideNavigator createNavigator(ConcreteAggregate aggregate) {
        return new SlideNavigator(aggregate);
    }

    @Override
    public String description() {
        return "Встроенный альбом: " + album.displayName();
    }
}

