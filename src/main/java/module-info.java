module de.andreas.cadas {
    requires javafx.controls;
    requires javafx.swing;
    requires javafx.web;
    requires java.desktop;
    requires jdk.httpserver;
    requires org.commonmark;
    requires org.commonmark.ext.gfm.tables;
    // PDFBox ist ein automatisches Modul und kann nicht per jlink gelinkt werden.
    // Es wird zur Laufzeit über den Modulpfad bereitgestellt, daher nur statisch angebunden.
    requires static org.apache.pdfbox;

    opens de.andreas.cadas to javafx.graphics;
}
