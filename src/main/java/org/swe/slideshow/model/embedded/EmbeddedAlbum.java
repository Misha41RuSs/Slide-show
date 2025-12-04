package org.swe.slideshow.model.embedded;

import java.nio.file.Path;

public record EmbeddedAlbum(String id, String displayName, Path path) {
}

