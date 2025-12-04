package org.swe.slideshow.model.factory;

import org.swe.slideshow.model.ConcreteAggregate;
import org.swe.slideshow.model.SlideNavigator;

import java.io.IOException;

public interface AggregateComponentsFactory {
    ConcreteAggregate createAggregate() throws IOException;
    SlideNavigator createNavigator(ConcreteAggregate aggregate);
    String description();
}

