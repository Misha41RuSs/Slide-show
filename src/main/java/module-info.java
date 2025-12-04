module org.swe.slideshow {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens org.swe.slideshow to javafx.fxml;
    exports org.swe.slideshow;
}