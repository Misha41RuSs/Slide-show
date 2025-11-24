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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import org.swe.slideshow.model.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Controller {
    private static final String DEFAULT_EMOTION = "🙂 Нейтрально";
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
    
    private ConcreteAggregate slides;
    private Iterator iter;
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
    private ImpressionStore impressionStore;
    private String currentImageId;
    private boolean impressionUpdatingInternally;
    
    @FXML
    public void initialize() {
        formatComboBox.getItems().addAll("*", "*.png",
                "*.jpg", "*.jpeg", "*.gif", "*.bmp");
        formatComboBox.setValue("*");

        delayTextField.setText("2000");

        slides = new ConcreteAggregate("", "*");
        iter = slides.getIterator();
        
        director = new Director();
        impressionStore = new ImpressionStore(resolveImpressionsFile());
        setupImpressionControls();

        stopButton.setDisable(true);
        nextButton.setDisable(true);
        prevButton.setDisable(true);
        
        updateStatus("Выберите каталог с изображениями");
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
            
            slides = new ConcreteAggregate(selectedDirectory, selectedFormat);
            iter = slides.getIterator();
            
            int imageCount = slides.getImageCount();
            if (imageCount > 0) {
                updateStatus("Найдено изображений: " + imageCount);
                nextButton.setDisable(false);
                prevButton.setDisable(false);
                startButton.setDisable(false);

                removeProgressIndicator();

                showNextImage();
            } else {
                updateStatus("Изображения не найдены в выбранном каталоге");
                nextButton.setDisable(true);
                prevButton.setDisable(true);
                startButton.setDisable(true);
                removeProgressIndicator();
                disableImpressionControls();
            }
        }
    }
    
    @FXML
    protected void onFormatChange() {
        String format = formatComboBox.getValue();
        if (format != null && !selectedDirectory.isEmpty()) {
            selectedFormat = format;
            slides.setImageFormat(selectedFormat);
            iter = slides.getIterator();
            
            int imageCount = slides.getImageCount();
            updateStatus("Найдено изображений: " + imageCount);
            if (imageCount > 0) {
                removeProgressIndicator();
                showNextImage();
            } else {
                removeProgressIndicator();
                disableImpressionControls();
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
        
        startButton.setDisable(false);
        stopButton.setDisable(true);
        nextButton.setDisable(false);
        prevButton.setDisable(false);
        selectDirectoryButton.setDisable(false);
        formatComboBox.setDisable(false);
        delayTextField.setDisable(false);
        
        updateStatus("Слайд-шоу остановлено");
    }
    
    @FXML
    protected void onNextClick() {
        showNextImage();
    }
    
    @FXML
    protected void onPrevClick() {
        if (iter != null && slides.getImageCount() > 0) {
            Image image = (Image) iter.preview();
            if (image != null) {
                screen.setImage(image);
                updateStatus("Предыдущее изображение");
                updateImpressionField();
            }
        }
    }
    
    private void handleSlideShow(ActionEvent event) {
        if (iter.hasNext(1)) {
            Image image = (Image) iter.next();
            if (image != null) {
                screen.setImage(image);
                updateProgressIndicator();
                updateImpressionField();
            }
        } else {
            iter = slides.getIterator();
            Image image = (Image) iter.next();
            if (image != null) {
                screen.setImage(image);
                updateProgressIndicator();
                updateImpressionField();
            }
        }
    }
    
    private void showNextImage() {
        if (iter != null && slides.getImageCount() > 0) {
            Image image = (Image) iter.next();
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
        if (progressIndicator != null && iter != null) {
            int currentIndex = iter.getCurrentIndex();
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
        if (currentImageId == null || impressionTextArea == null) {
            return;
        }
        String impression = impressionTextArea.getText();
        String selectedEmotion = emotionComboBox != null ? emotionComboBox.getValue() : DEFAULT_EMOTION;
        impressionStore.saveImpression(currentImageId, impression, selectedEmotion);
        if (impressionStatusLabel != null) {
            boolean textEmpty = impression == null || impression.isBlank();
            boolean emotionDefault = selectedEmotion == null || selectedEmotion.isBlank() || DEFAULT_EMOTION.equals(selectedEmotion);
            if (textEmpty && emotionDefault) {
                impressionStatusLabel.setText("Впечатление очищено");
            } else {
                impressionStatusLabel.setText("Впечатление сохранено");
            }
        }
    }

    private void setupImpressionControls() {
        if (impressionTextArea != null) {
            impressionTextArea.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!impressionUpdatingInternally && currentImageId != null && impressionStatusLabel != null) {
                    impressionStatusLabel.setText("Изменения не сохранены");
                }
            });
        }
        if (emotionComboBox != null) {
            emotionComboBox.getItems().setAll(EMOTION_OPTIONS);
            emotionComboBox.setValue(DEFAULT_EMOTION);
            emotionComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!impressionUpdatingInternally && currentImageId != null && impressionStatusLabel != null) {
                    impressionStatusLabel.setText("Изменения не сохранены");
                }
            });
        }
        disableImpressionControls();
    }

    private void updateImpressionField() {
        if (impressionTextArea == null) {
            return;
        }
        if (iter == null) {
            disableImpressionControls();
            return;
        }
        String imageId = iter.getCurrentItemId();
        currentImageId = imageId;
        if (imageId == null || imageId.isEmpty()) {
            disableImpressionControls();
            return;
        }
        ImpressionStore.ImpressionRecord stored = impressionStore.getImpression(imageId);
        String storedText = stored != null ? stored.text() : "";
        String storedEmotion = stored != null && stored.emotion() != null && !stored.emotion().isBlank()
                ? stored.emotion()
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
    }

    private void disableImpressionControls() {
        currentImageId = null;
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
    }

    private Path resolveImpressionsFile() {
        return Paths.get(System.getProperty("user.dir"),
                "src", "main", "resources", "org", "swe", "slideshow", "impressions.json");
    }
}
