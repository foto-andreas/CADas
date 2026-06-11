module de.andreas.cadas {
    requires javafx.controls;

    exports de.andreas.cadas;
    opens de.andreas.cadas to javafx.graphics;
}
