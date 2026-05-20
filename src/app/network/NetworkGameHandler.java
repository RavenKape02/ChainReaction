package app.network;

import app.model.BoardSnapshot;

// Shared contract for any object that drives a remote game session.
public interface NetworkGameHandler {

    // Returns the latest server-authoritative board snapshot
    BoardSnapshot getLatestSnapshot();

    /**
     * Sends a move to the server
     * @param row    board row index
     * @param column board column index
     */
    void sendMove(int row, int column);

    // Player ID assigned by the server to this instance, or -1 if not yet connected
    int getPlayerId();

    // stops the client socket, the server thread, or both
    void stop();
}
