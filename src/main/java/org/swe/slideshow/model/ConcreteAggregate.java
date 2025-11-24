package org.swe.slideshow.model;

import javafx.scene.image.Image;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class ConcreteAggregate implements Aggregate {
    private String filetop;
    private String imageFormat;
    private final SlideCollection slideCollection;
    
    @Override
    public Iterator getIterator() {
        return new ImageIterator(slideCollection);
    }
    
    public ConcreteAggregate(String filetop, String imageFormat) {
        this.filetop = filetop;
        this.imageFormat = imageFormat;
        this.slideCollection = new SlideCollection();
        loadImageFiles();
    }
    
    private void loadImageFiles() {
        slideCollection.clear();
        if (filetop == null || filetop.isEmpty()) {
            return;
        }
        
        Path rootPath = Paths.get(filetop);
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            return;
        }
        
        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isSupportedImage)
                    .filter(this::matchesSelectedFormat)
                    .forEach(path -> slideCollection.add(path.toAbsolutePath().toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isSupportedImage(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".png") ||
                fileName.endsWith(".jpg") ||
                fileName.endsWith(".jpeg") ||
                fileName.endsWith(".gif") ||
                fileName.endsWith(".bmp");
    }

    private boolean matchesSelectedFormat(Path path) {
        if (imageFormat == null || imageFormat.isEmpty() || "*".equals(imageFormat)) {
            return true;
        }
        String fileName = path.getFileName().toString().toLowerCase();
        String format = imageFormat.toLowerCase().replace("*", "").replace(".", "");
        return fileName.endsWith("." + format);
    }
    
    public void setFiletop(String filetop) {
        this.filetop = filetop;
        loadImageFiles();
    }
    
    public void setImageFormat(String imageFormat) {
        this.imageFormat = imageFormat;
        loadImageFiles();
    }
    
    public int getImageCount() {
        return slideCollection.size();
    }
    
    private class ImageIterator implements Iterator {
        private final SlideCollection collection;
        private int current = -1;

        private ImageIterator(SlideCollection collection) {
            this.collection = collection;
        }
        
        private Image getImage(int index) {
            String imagePath = collection.getPath(index);
            if (imagePath == null) {
                return null;
            }
            
            Path path = Paths.get(imagePath);
            try (InputStream stream = Files.newInputStream(path)) {
                return new Image(stream);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        
        @Override
        public boolean hasNext(int x) {
            return collection.size() > 0 && current + x < collection.size();
        }
        
        @Override
        public Object next() {
            if (collection.size() == 0) {
                return null;
            }
            if (hasNext(1)) {
                current++;
            } else {
                current = 0;
            }
            return getImage(current);
        }
        
        @Override
        public Object preview() {
            if (collection.size() == 0) {
                return null;
            }
            if (current > 0) {
                current--;
            } else {
                current = collection.size() - 1;
            }
            return getImage(current);
        }
        
        @Override
        public int getCurrentIndex() {
            if (current < 0) return 1;
            if (current >= collection.size()) return collection.size();
            return current + 1;
        }

        @Override
        public String getCurrentItemId() {
            if (current < 0) {
                return null;
            }
            return collection.getPath(current);
        }
    }
}



