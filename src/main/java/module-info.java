module org.swe.slideshow {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.swe.slideshow to javafx.fxml;
    exports org.swe.slideshow;
}