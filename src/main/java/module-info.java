module de.andreas.cadas {
    requires javafx.controls;
    requires jdk.httpserver;

    exports de.andreas.cadas;
    opens de.andreas.cadas to javafx.graphics;
}
