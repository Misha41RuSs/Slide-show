package org.swe.slideshow.model.factory;

import org.swe.slideshow.model.ConcreteAggregate;
import org.swe.slideshow.model.SlideNavigator;

public class DirectoryAggregateFactory implements AggregateComponentsFactory {
    private final String directory;
    private final String format;

    public DirectoryAggregateFactory(String directory, String format) {
        this.directory = directory;
        this.format = format;
    }

    @Override
    public ConcreteAggregate createAggregate() {
        return new ConcreteAggregate(directory, format);
    }

    @Override
    public SlideNavigator createNavigator(ConcreteAggregate aggregate) {
        return new SlideNavigator(aggregate);
    }

    @Override
    public String description() {
        return "Каталог: " + directory;
    }
}

