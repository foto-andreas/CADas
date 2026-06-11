module de.andreas.cadas {
    requires javafx.controls;
    requires javafx.swing;
    requires java.desktop;
    requires jdk.httpserver;

    exports de.andreas.cadas;
    opens de.andreas.cadas to javafx.graphics;
}
