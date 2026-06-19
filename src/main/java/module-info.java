module de.andreas.cadas {
    requires javafx.controls;
    requires javafx.swing;
    requires javafx.web;
    requires java.desktop;
    requires jdk.httpserver;
    requires org.commonmark;
    requires org.commonmark.ext.gfm.tables;
    // pdfbox und commons-logging sind automatic modules und können nicht per jlink gelinkt werden.
    // Sie werden zur Laufzeit über den Classpath bereitgestellt, daher nur statisch angebunden.
    requires static org.apache.pdfbox;
    requires static org.apache.commons.logging;

    exports de.andreas.cadas;
    opens de.andreas.cadas to javafx.graphics;
}
