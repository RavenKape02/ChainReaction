package app.ui;

import javafx.application.Platform;
import app.model.BoardSnapshot;

/**
 * Minimal callback that allows GameHost to trigger board render
 */
public interface SnapshotRenderCallback {
    void renderSnapshotFromNetwork(BoardSnapshot snapshot);
}
