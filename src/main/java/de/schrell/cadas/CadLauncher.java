package de.schrell.cadas;

/**
 * Schlanker, modulpfadgeeigneter Prozesseinstieg, der ausschließlich den JavaFX-Start delegiert.
 */
public final class CadLauncher {

    private CadLauncher() {
    }

    public static void main(String[] args) {
        CadApplication.launch(CadApplication.class, args);
    }
}
