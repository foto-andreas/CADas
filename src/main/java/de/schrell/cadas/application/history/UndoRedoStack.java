package de.schrell.cadas.application.history;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;

/**
 * Verwaltet unveränderliche Zustandskopien für Rückgängig/Wiederherstellen. Ein neuer Bearbeitungsschritt löscht
 * den Redo-Zweig. Die feste Obergrenze entfernt ausschließlich die ältesten Undo-Snapshots und verhindert dadurch
 * einen vom Bearbeitungsumfang abhängigen, unbegrenzten Speicherverbrauch.
 */
public final class UndoRedoStack<T> {

    private static final int DEFAULT_MAXIMUM_SNAPSHOTS = 100;
    private final Deque<T> undoStack = new ArrayDeque<>();
    private final Deque<T> redoStack = new ArrayDeque<>();
    private final int maximumSnapshots;

    public UndoRedoStack() {
        this(DEFAULT_MAXIMUM_SNAPSHOTS);
    }

    public UndoRedoStack(int maximumSnapshots) {
        if (maximumSnapshots < 1) {
            throw new IllegalArgumentException("Die Historie muss mindestens einen Snapshot aufnehmen können.");
        }
        this.maximumSnapshots = maximumSnapshots;
    }

    public void remember(T snapshot) {
        undoStack.push(Objects.requireNonNull(snapshot, "snapshot darf nicht null sein."));
        trimOldest(undoStack);
        redoStack.clear();
    }

    public Optional<T> undo(T currentSnapshot) {
        if (undoStack.isEmpty()) {
            return Optional.empty();
        }
        redoStack.push(Objects.requireNonNull(currentSnapshot, "currentSnapshot darf nicht null sein."));
        return Optional.of(undoStack.pop());
    }

    public Optional<T> redo(T currentSnapshot) {
        if (redoStack.isEmpty()) {
            return Optional.empty();
        }
        undoStack.push(Objects.requireNonNull(currentSnapshot, "currentSnapshot darf nicht null sein."));
        return Optional.of(redoStack.pop());
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    private void trimOldest(Deque<T> snapshots) {
        while (snapshots.size() > maximumSnapshots) {
            snapshots.removeLast();
        }
    }
}
