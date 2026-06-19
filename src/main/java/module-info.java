module de.andreas.cadas {
    requires javafx.controls;
    requires javafx.swing;
    requires javafx.web;
    requires java.desktop;
    requires jdk.httpserver;
    requires org.commonmark;
    requires org.commonmark.ext.gfm.tables;
    requires org.apache.pdfbox;
    requires org.apache.commons.logging;

    exports de.andreas.cadas;
    opens de.andreas.cadas to javafx.graphics;
}
