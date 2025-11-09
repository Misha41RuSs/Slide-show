package org.swe.slideshow.model;

import javafx.scene.image.Image;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ConcreteAggregate implements Aggregate {
    private String filetop;
    private String imageFormat;
    private List<File> imageFiles;
    
    @Override
    public Iterator getIterator() {
        return new ImageIterator();
    }
    
    public ConcreteAggregate(String filetop, String imageFormat) {
        this.filetop = filetop;
        this.imageFormat = imageFormat;
        this.imageFiles = new ArrayList<>();
        loadImageFiles();
    }
    
    private void loadImageFiles() {
        imageFiles.clear();
        if (filetop == null || filetop.isEmpty()) {
            return;
        }
        
        Path rootPath = Paths.get(filetop);
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            return;
        }
        
        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> {
                     String fileName = path.getFileName().toString().toLowerCase();
                     return fileName.endsWith(".png") || 
                            fileName.endsWith(".jpg") || 
                            fileName.endsWith(".jpeg") ||
                            fileName.endsWith(".gif") ||
                            fileName.endsWith(".bmp");
                 })
                 .filter(path -> {
                     if (imageFormat == null || imageFormat.isEmpty() || imageFormat.equals("*")) {
                         return true;
                     }
                     String fileName = path.getFileName().toString().toLowerCase();
                     String format = imageFormat.toLowerCase().replace("*", "").replace(".", "");
                     return fileName.endsWith("." + format);
                 })
                 .forEach(path -> imageFiles.add(path.toFile()));
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        return imageFiles.size();
    }
    
    private class ImageIterator implements Iterator {
        private int current = -1;
        
        private Image getImage(int index) {
            if (index < 0 || index >= imageFiles.size()) {
                return null;
            }
            
            File file = imageFiles.get(index);
            try {
                InputStream stream = new FileInputStream(file);
                return new Image(stream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }
        
        @Override
        public boolean hasNext(int x) {
            return current + x < imageFiles.size();
        }
        
        @Override
        public Object next() {
            if (hasNext(1)) {
                current++;
                return getImage(current);
            } else {
                current = 0;
                return getImage(0);
            }
        }
        
        @Override
        public Object preview() {
            if (current > 0) {
                current--;
                return getImage(current);
            } else {
                current = imageFiles.size() - 1;
                return getImage(current);
            }
        }
        
        @Override
        public int getCurrentIndex() {
            if (current < 0) return 1;
            if (current >= imageFiles.size()) return imageFiles.size();
            return current + 1;
        }
    }
}

