package org.swe.slideshow;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import org.swe.slideshow.model.AlbumItem;
import org.swe.slideshow.model.AlbumStore;
import org.swe.slideshow.model.ConcreteAggregate;
import org.swe.slideshow.model.Builder;
import org.swe.slideshow.model.BuilderIndicator;
import org.swe.slideshow.model.Director;
import org.swe.slideshow.model.Indicator;
import org.swe.slideshow.model.SlideNavigator;
import org.swe.slideshow.model.embedded.EmbeddedAlbum;
import org.swe.slideshow.model.embedded.EmbeddedImageManager;
import org.swe.slideshow.model.factory.AggregateComponentsFactory;
import org.swe.slideshow.model.factory.DirectoryAggregateFactory;
import org.swe.slideshow.model.factory.EmbeddedAggregateFactory;
import org.swe.slideshow.visual.EmotionPalette;
import org.swe.slideshow.visual.EmotionPalette.EmotionStyle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class Controller {
    private static final String DEFAULT_EMOTION = EmotionPalette.DEFAULT_EMOTION;
    private static final String[] EMOTION_OPTIONS = new String[]{
            DEFAULT_EMOTION,
            "😊 Радость",
            "🤩 Восхищение",
            "😮 Удивление",
            "😢 Грусть",
            "😌 Спокойствие",
            "😎 Вдохновение"
    };
    @FXML
    private StackPane imageWrapper;

    @FXML
    private ImageView screen;
    
    @FXML
    private Button startButton;
    
    @FXML
    private Button stopButton;
    
    @FXML
    private Button selectDirectoryButton;
    
    @FXML
    private Button nextButton;
    
    @FXML
    private Button prevButton;
    
    @FXML
    private ComboBox<String> formatComboBox;
    
    @FXML
    private TextField delayTextField;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Pane indicatorPane;

    @FXML
    private TextArea impressionTextArea;

    @FXML
    private Button saveImpressionButton;

    @FXML
    private Label impressionStatusLabel;

    @FXML
    private ComboBox<String> emotionComboBox;

    @FXML
    private Button saveAlbumButton;

    @FXML
    private Button loadAlbumButton;

    @FXML
    private ComboBox<String> embeddedAlbumComboBox;

    @FXML
    private Button loadEmbeddedButton;

    private ConcreteAggregate slides;
    private SlideNavigator navigator;
    private Timeline timeline;
    private String selectedDirectory = "";
    private String selectedFormat = "*";
    private int slideDelay = 2000;
    private Indicator progressIndicator;
    private Indicator timerIndicator;
    private Director director;
    private Timeline timerTimeline;
    private long startTime;
    private float maxTime = 300.0f;
    private boolean impressionUpdatingInternally;
    private final EmbeddedImageManager embeddedImageManager = new EmbeddedImageManager();
    private final Map<String, EmbeddedAlbum> embeddedAlbums = new LinkedHashMap<>();
    
    @FXML
    public void initialize() {
        formatComboBox.getItems().addAll("*", "*.png",
                "*.jpg", "*.jpeg", "*.gif", "*.bmp");
        formatComboBox.setValue("*");

        delayTextField.setText("2000");

        slides = new ConcreteAggregate("", "*");
        navigator = new SlideNavigator(slides);
        
        director = new Director();
        setupImpressionControls();
        setupEmbeddedAlbums();

        stopButton.setDisable(true);
        nextButton.setDisable(true);
        prevButton.setDisable(true);
        saveAlbumButton.setDisable(true);
        resetEmotionVisuals();
        
        updateStatus("Выберите каталог с изображениями или загрузите альбом");
    }
    
    @FXML
    protected void onSelectDirectoryClick() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Выберите каталог с изображениями");
        
        File selectedDirectoryFile = directoryChooser.showDialog(screen.getScene().getWindow());
        if (selectedDirectoryFile != null) {
            selectedDirectory = selectedDirectoryFile.getAbsolutePath();
            String format = formatComboBox.getValue();
            if (format != null) {
                selectedFormat = format;
            }
            
            AggregateComponentsFactory factory = new DirectoryAggregateFactory(selectedDirectory, selectedFormat);
            loadFromFactory(factory,
                    count -> "Найдено изображений: " + count,
                    "Изображения не найдены в выбранном каталоге",
                    false);
        }
    }
    
    @FXML
    protected void onFormatChange() {
        String format = formatComboBox.getValue();
        if (format != null && !selectedDirectory.isEmpty()) {
            selectedFormat = format;
            slides.setImageFormat(selectedFormat);
            if (navigator != null) {
                navigator.refresh();
            }
            
            int imageCount = slides.getImageCount();
            updateStatus("Найдено изображений: " + imageCount);
            if (imageCount > 0) {
                removeProgressIndicator();
                updateControlsForImageCount(imageCount);
                showNextImage();
            } else {
                removeProgressIndicator();
                updateControlsForImageCount(0);
            }
        }
    }
    
    @FXML
    protected void onStartClick() {
        if (slides.getImageCount() == 0) {
            updateStatus("Нет изображений для показа");
            return;
        }
        
        try {
            slideDelay = Integer.parseInt(delayTextField.getText());
            if (slideDelay < 100) {
                slideDelay = 100;
                delayTextField.setText("100");
            }
        } catch (NumberFormatException e) {
            slideDelay = 2000;
            delayTextField.setText("2000");
        }

        if (timeline != null) {
            timeline.stop();
        }

        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        
        EventHandler<ActionEvent> eventHandler = this::handleSlideShow;
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(slideDelay), eventHandler));
        
        timeline.play();

        startTime = System.currentTimeMillis();

        int imageCount = slides.getImageCount();
        createProgressIndicator(imageCount);
        createTimerIndicator();
        updateIndicatorsDisplay();
        
        startButton.setDisable(true);
        stopButton.setDisable(false);
        nextButton.setDisable(true);
        prevButton.setDisable(true);
        selectDirectoryButton.setDisable(true);
        formatComboBox.setDisable(true);
        delayTextField.setDisable(true);
        
        updateStatus("Слайд-шоу запущено");
    }
    
    @FXML
    protected void onStopClick() {
        if (timeline != null) {
            timeline.stop();
        }

        if (timerTimeline != null) {
            timerTimeline.stop();
        }

        removeProgressIndicator();
        removeTimerIndicator();
        
        stopButton.setDisable(true);
        selectDirectoryButton.setDisable(false);
        formatComboBox.setDisable(false);
        delayTextField.setDisable(false);
        updateControlsForImageCount(slides.getImageCount());
        
        updateStatus("Слайд-шоу остановлено");
    }
    
    @FXML
    protected void onNextClick() {
        showNextImage();
    }
    
    @FXML
    protected void onPrevClick() {
        if (navigator != null && navigator.hasSlides()) {
            Image image = navigator.previousImage();
            if (image != null) {
                screen.setImage(image);
                updateStatus("Предыдущее изображение");
                updateImpressionField();
            }
        }
    }
    
    private void handleSlideShow(ActionEvent event) {
        if (navigator == null || !navigator.hasSlides()) {
            return;
        }
        Image image = navigator.nextImage();
        if (image != null) {
            screen.setImage(image);
            updateProgressIndicator();
            updateImpressionField();
        }
    }
    
    private void showNextImage() {
        if (navigator != null && navigator.hasSlides()) {
            Image image = navigator.nextImage();
            if (image != null) {
                screen.setImage(image);
                updateStatus("Изображение загружено");
                if (timeline != null && timeline.getStatus() == javafx.animation.Animation.Status.RUNNING) {
                    updateProgressIndicator();
                }
                updateImpressionField();
            }
        } else {
            disableImpressionControls();
        }
    }
    
    private void createProgressIndicator(int totalSlides) {
        if (progressIndicator == null) {
            Builder builder = new BuilderIndicator();
            progressIndicator = director.constructSlideShowIndicator(builder, totalSlides);
            progressIndicator.setPercentAndFraction(1, totalSlides);
            updateProgressIndicator();
        }
    }
    
    private void createTimerIndicator() {
        if (timerIndicator == null) {
            Builder builder = new BuilderIndicator();
            timerIndicator = director.constructTimeIndicator(builder, 0.0f, maxTime, 0.0f);
            timerIndicator.setTimeDisplay(0.0f);
            startTimer();
        }
    }
    
    private void startTimer() {
        if (timerTimeline != null) {
            timerTimeline.stop();
        }
        
        timerTimeline = new Timeline();
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        
        EventHandler<ActionEvent> timerHandler = e -> updateTimer();
        timerTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(100), timerHandler));
        timerTimeline.play();
    }
    
    private void updateTimer() {
        if (timerIndicator != null && startTime > 0) {
            long currentTime = System.currentTimeMillis();
            float elapsedSeconds = (currentTime - startTime) / 1000.0f;

            if (elapsedSeconds > maxTime) {
                elapsedSeconds = maxTime;
            }
            
            timerIndicator.setCurrentValue(elapsedSeconds);
            timerIndicator.setTimeDisplay(elapsedSeconds);
        }
    }
    
    private void removeProgressIndicator() {
        progressIndicator = null;
    }
    
    private void removeTimerIndicator() {
        if (timerTimeline != null) {
            timerTimeline.stop();
            timerTimeline = null;
        }
        timerIndicator = null;
        startTime = 0;
    }
    
    private void updateProgressIndicator() {
        if (progressIndicator != null && navigator != null) {
            int currentIndex = navigator.currentIndex();
            int totalSlides = slides.getImageCount();
            progressIndicator.updateProgress(currentIndex, totalSlides);
            progressIndicator.setTitle("Прогресс слайд-шоу");
        }
    }
    
    private void updateIndicatorsDisplay() {
        if (indicatorPane != null) {
            indicatorPane.getChildren().clear();
            
            HBox indicatorsBox = new HBox(30);
            indicatorsBox.setAlignment(javafx.geometry.Pos.CENTER);
            
            if (progressIndicator != null) {
                indicatorsBox.getChildren().add(progressIndicator.getPanel());
            }
            
            if (timerIndicator != null) {
                indicatorsBox.getChildren().add(timerIndicator.getPanel());
            }
            
            indicatorPane.getChildren().add(indicatorsBox);
        }
    }
    
    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    @FXML
    protected void onSaveImpressionClick() {
        AlbumItem currentItem = navigator != null ? navigator.currentItem() : null;
        if (currentItem == null || impressionTextArea == null) {
            return;
        }
        String impression = impressionTextArea.getText();
        String selectedEmotion = emotionComboBox != null ? emotionComboBox.getValue() : DEFAULT_EMOTION;
        
        currentItem.setImpressionText(impression);
        currentItem.setEmotion(selectedEmotion);
        
        if (impressionStatusLabel != null) {
            boolean textEmpty = impression == null || impression.isBlank();
            boolean emotionDefault = selectedEmotion == null || selectedEmotion.isBlank() || DEFAULT_EMOTION.equals(selectedEmotion);
            if (textEmpty && emotionDefault) {
                impressionStatusLabel.setText("Впечатление очищено");
            } else {
                impressionStatusLabel.setText("Впечатление сохранено");
            }
        }
        applyEmotionTheme(currentItem);
    }

    @FXML
    protected void onSaveAlbumClick() {
        if (slides.getImageCount() == 0) {
            updateStatus("Нет изображений для сохранения");
            return;
        }

        javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
        directoryChooser.setTitle("Выберите место для сохранения альбома");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File selectedDir = directoryChooser.showDialog(screen.getScene().getWindow());
        if (selectedDir != null) {
            Optional<String> albumNameResult = promptAlbumName();
            if (albumNameResult.isEmpty()) {
                updateStatus("Сохранение альбома отменено");
                return;
            }
            try {
                AlbumItem[] items = slides.getAllItems();
                Path albumPath = buildAlbumPath(selectedDir.toPath(), albumNameResult.get());
                AlbumStore.saveAlbum(albumPath, items);
                updateStatus("Альбом сохранен: " + albumPath.getFileName());
            } catch (IOException e) {
                e.printStackTrace();
                updateStatus("Ошибка при сохранении альбома: " + e.getMessage());
            }
        }
    }

    @FXML
    protected void onLoadAlbumClick() {
        javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
        directoryChooser.setTitle("Выберите папку альбома");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File selectedDir = directoryChooser.showDialog(screen.getScene().getWindow());
        if (selectedDir != null) {
            try {
                Path albumPath = selectedDir.toPath();
                AlbumItem[] items = AlbumStore.loadAlbum(albumPath);
                
                slides.loadFromAlbumItems(items);
                if (navigator != null) {
                    navigator.refresh();
                } else {
                    navigator = new SlideNavigator(slides);
                }
                
                int imageCount = slides.getImageCount();
                if (imageCount > 0) {
                    updateStatus("Альбом загружен: " + imageCount + " изображений");
                    updateControlsForImageCount(imageCount);
                    showNextImage();
                } else {
                    updateStatus("Альбом пуст или поврежден");
                    updateControlsForImageCount(0);
                }
            } catch (IOException e) {
                e.printStackTrace();
                updateStatus("Ошибка при загрузке альбома: " + e.getMessage());
            }
        }
    }

    @FXML
    protected void onLoadEmbeddedClick() {
        if (embeddedAlbumComboBox == null || embeddedAlbumComboBox.getValue() == null) {
            updateStatus("Нет доступных встроенных альбомов");
            return;
        }
        EmbeddedAlbum album = embeddedAlbums.get(embeddedAlbumComboBox.getValue());
        if (album == null) {
            updateStatus("Не удалось определить встроенный альбом");
            return;
        }
        AggregateComponentsFactory factory = new EmbeddedAggregateFactory(embeddedImageManager, album);
        loadFromFactory(factory,
                count -> "Загружен встроенный альбом («" + album.displayName() + "»): " + count + " изображений",
                "Встроенный альбом пуст",
                true);
    }

    private void setupImpressionControls() {
        if (impressionTextArea != null) {
            impressionTextArea.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!impressionUpdatingInternally && navigator != null && navigator.currentItem() != null && impressionStatusLabel != null) {
                    impressionStatusLabel.setText("Изменения не сохранены");
                }
            });
        }
        if (emotionComboBox != null) {
            emotionComboBox.getItems().setAll(EMOTION_OPTIONS);
            emotionComboBox.setValue(DEFAULT_EMOTION);
            emotionComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!impressionUpdatingInternally && navigator != null && navigator.currentItem() != null && impressionStatusLabel != null) {
                    impressionStatusLabel.setText("Изменения не сохранены");
                }
            });
        }
        disableImpressionControls();
    }

    private void setupEmbeddedAlbums() {
        if (embeddedAlbumComboBox == null) {
            return;
        }
        embeddedAlbums.clear();
        for (EmbeddedAlbum album : embeddedImageManager.discoverAlbums()) {
            embeddedAlbums.put(album.displayName(), album);
        }
        embeddedAlbumComboBox.getItems().setAll(embeddedAlbums.keySet());
        if (!embeddedAlbums.isEmpty()) {
            embeddedAlbumComboBox.setValue(embeddedAlbumComboBox.getItems().get(0));
            toggleEmbeddedButton(false);
        } else {
            toggleEmbeddedButton(true);
        }
        embeddedAlbumComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                toggleEmbeddedButton(false);
            }
        });
    }

    private void updateImpressionField() {
        if (impressionTextArea == null) {
            return;
        }
        if (navigator == null) {
            disableImpressionControls();
            return;
        }
        AlbumItem currentItem = navigator.currentItem();
        if (currentItem == null) {
            disableImpressionControls();
            return;
        }
        
        String storedText = currentItem.getImpressionText() != null ? currentItem.getImpressionText() : "";
        String storedEmotion = currentItem.getEmotion() != null && !currentItem.getEmotion().isBlank()
                ? currentItem.getEmotion()
                : DEFAULT_EMOTION;
        if (emotionComboBox != null && storedEmotion != null && !emotionComboBox.getItems().contains(storedEmotion)) {
            emotionComboBox.getItems().add(storedEmotion);
        }

        impressionUpdatingInternally = true;
        impressionTextArea.setDisable(false);
        impressionTextArea.setText(storedText);
        if (emotionComboBox != null) {
            emotionComboBox.setDisable(false);
            emotionComboBox.setValue(storedEmotion);
        }
        impressionUpdatingInternally = false;

        if (saveImpressionButton != null) {
            saveImpressionButton.setDisable(false);
        }
        if (impressionStatusLabel != null) {
            boolean textEmpty = storedText == null || storedText.isBlank();
            boolean emotionDefault = storedEmotion == null || storedEmotion.isBlank() || DEFAULT_EMOTION.equals(storedEmotion);
            if (textEmpty && emotionDefault) {
                impressionStatusLabel.setText("Добавьте впечатление");
            } else {
                impressionStatusLabel.setText("Впечатление загружено");
            }
        }
        applyEmotionTheme(currentItem);
    }

    private void disableImpressionControls() {
        if (impressionTextArea != null) {
            impressionUpdatingInternally = true;
            impressionTextArea.clear();
            impressionUpdatingInternally = false;
            impressionTextArea.setDisable(true);
        }
        if (emotionComboBox != null) {
            impressionUpdatingInternally = true;
            emotionComboBox.setValue(DEFAULT_EMOTION);
            impressionUpdatingInternally = false;
            emotionComboBox.setDisable(true);
        }
        if (saveImpressionButton != null) {
            saveImpressionButton.setDisable(true);
        }
        if (impressionStatusLabel != null) {
            impressionStatusLabel.setText("Нет изображения");
        }
        applyEmotionTheme(null);
    }

    private void resetEmotionVisuals() {
        applyEmotionTheme(null);
    }

    private void applyEmotionTheme(AlbumItem item) {
        EmotionStyle style = EmotionPalette.styleFor(item != null ? item.getEmotion() : null);
        if (imageWrapper != null) {
            imageWrapper.setStyle(String.format("-fx-border-color: %s; -fx-border-width: 6; -fx-border-radius: 18; -fx-background-radius: 18; -fx-padding: 6;", style.cssColor()));
        }
    }

    private Optional<String> promptAlbumName() {
        TextInputDialog dialog = new TextInputDialog("Мой альбом");
        dialog.setTitle("Сохранение альбома");
        dialog.setHeaderText("Введите имя папки для альбома");
        dialog.setContentText("Имя:");
        return dialog.showAndWait().map(this::sanitizeAlbumName);
    }

    private Path buildAlbumPath(Path baseDir, String rawName) throws IOException {
        String cleanedName = rawName == null || rawName.isBlank()
                ? "album_" + System.currentTimeMillis()
                : rawName.trim();
        Path candidate = baseDir.resolve(cleanedName);
        if (!Files.exists(candidate)) {
            return candidate;
        }
        return makeUniquePath(candidate);
    }

    private String sanitizeAlbumName(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\\\/:*?\"<>|]", "").trim();
    }

    private Path makeUniquePath(Path basePath) throws IOException {
        int counter = 1;
        Path current = basePath;
        while (Files.exists(current)) {
            String fileName = basePath.getFileName().toString();
            Path parent = basePath.getParent();
            String candidateName = fileName + "_" + counter;
            current = parent != null ? parent.resolve(candidateName) : Path.of(candidateName);
            counter++;
        }
        return current;
    }

    private void loadFromFactory(AggregateComponentsFactory factory,
                                 Function<Integer, String> successMessageSupplier,
                                 String emptyMessage,
                                 boolean disableEmbeddedButton) {
        try {
            ConcreteAggregate aggregate = factory.createAggregate();
            SlideNavigator newNavigator = factory.createNavigator(aggregate);
            slides = aggregate;
            navigator = newNavigator;
            handleDatasetChange(successMessageSupplier, emptyMessage, disableEmbeddedButton);
        } catch (IOException e) {
            updateStatus("Ошибка загрузки: " + e.getMessage());
            removeProgressIndicator();
            updateControlsForImageCount(0);
        }
    }

    private void handleDatasetChange(Function<Integer, String> successMessageSupplier,
                                     String emptyMessage,
                                     boolean disableEmbeddedButton) {
        int imageCount = slides != null ? slides.getImageCount() : 0;
        if (navigator != null) {
            navigator.refresh();
        }
        if (imageCount > 0) {
            removeProgressIndicator();
            updateControlsForImageCount(imageCount);
            showNextImage();
            if (successMessageSupplier != null) {
                updateStatus(successMessageSupplier.apply(imageCount));
            }
        } else {
            removeProgressIndicator();
            updateControlsForImageCount(0);
            if (emptyMessage != null) {
                updateStatus(emptyMessage);
            }
            disableImpressionControls();
        }
        if (disableEmbeddedButton) {
            toggleEmbeddedButton(true);
        }
    }

    private void toggleEmbeddedButton(boolean disable) {
        if (loadEmbeddedButton != null) {
            loadEmbeddedButton.setDisable(disable);
        }
    }

    private void updateControlsForImageCount(int count) {
        boolean hasImages = count > 0;
        boolean timelineActive = timeline != null && timeline.getStatus() == Timeline.Status.RUNNING;
        nextButton.setDisable(!hasImages || timelineActive);
        prevButton.setDisable(!hasImages || timelineActive);
        if (!timelineActive) {
            startButton.setDisable(!hasImages);
        }
        saveAlbumButton.setDisable(!hasImages || timelineActive);
        if (!hasImages) {
            disableImpressionControls();
            if (screen != null) {
                screen.setImage(null);
            }
        }
    }
}
