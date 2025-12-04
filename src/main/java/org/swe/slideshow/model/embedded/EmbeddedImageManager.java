package org.swe.slideshow.model.embedded;

import org.swe.slideshow.Application;
import org.swe.slideshow.model.AlbumItem;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class EmbeddedImageManager {
    private static final String BASE_RESOURCE_FOLDER = "/images";
    private Path basePath;

    public EmbeddedImageManager() {
        resolveBasePath();
    }

    public List<EmbeddedAlbum> discoverAlbums() {
        List<EmbeddedAlbum> albums = new ArrayList<>();
        if (basePath == null || !Files.exists(basePath)) {
            return albums;
        }

        albums.add(new EmbeddedAlbum("base", "Базовая коллекция", basePath));
        try (Stream<Path> children = Files.list(basePath)) {
            children.filter(Files::isDirectory)
                    .sorted(Comparator.comparing(Path::getFileName))
                    .forEach(path -> albums.add(new EmbeddedAlbum(
                            path.getFileName().toString(),
                            prettifyName(path.getFileName().toString()),
                            path)));
        } catch (IOException ignored) {
        }
        return albums;
    }

    public AlbumItem[] loadAlbumItems(EmbeddedAlbum album) throws IOException {
        if (album == null || album.path() == null) {
            return new AlbumItem[0];
        }

        List<AlbumItem> items = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(album.path())) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isSupportedImage)
                    .forEach(path -> items.add(new AlbumItem(path.toAbsolutePath().toString())));
        }
        return items.toArray(new AlbumItem[0]);
    }

    private boolean isSupportedImage(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".png")
                || fileName.endsWith(".jpg")
                || fileName.endsWith(".jpeg")
                || fileName.endsWith(".gif")
                || fileName.endsWith(".bmp");
    }

    private String prettifyName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Альбом";
        }
        String clean = raw.replace('_', ' ').trim();
        if (clean.isEmpty()) {
            return "Альбом";
        }
        return clean.substring(0, 1).toUpperCase(Locale.ROOT) + clean.substring(1);
    }

    private void resolveBasePath() {
        URL resourceUrl = Application.class.getResource(BASE_RESOURCE_FOLDER);
        if (resourceUrl == null) {
            basePath = null;
            return;
        }
        try {
            basePath = Paths.get(resourceUrl.toURI());
        } catch (URISyntaxException | IllegalArgumentException e) {
            basePath = null;
        }
    }
}

