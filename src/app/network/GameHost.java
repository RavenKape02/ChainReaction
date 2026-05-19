package app.network;

import java.util.concurrent.CountDownLatch;

import app.model.BoardSnapshot;
import app.model.GameSettings;
import app.model.PlayerProfile;


public final class GameHost {

    private final GameServer server;
    private final GameClient client;

    private final CountDownLatch gameStartLatch = new CountDownLatch(1);

    private BoardSnapshot latestSnapshot;
    private int selfPlayerId = -1;

    public GameHost(
            int playerCount, int rows, int columns, int timeLimit,
            String[] playerNames, javafx.scene.paint.Color[] playerColors
    ) throws Exception {
        // server
        this.server = new GameServer(
            playerCount, rows, columns, timeLimit, playerNames, playerColors
        );

        // client (host player 0)
        Thread.sleep(200);
        this.client = new GameClient("127.0.0.1", NetworkConstants.PORT);
        this.client.setCallback(new ClientCallback());
        Thread.sleep(50);

        this.client.connect(playerNames[0], colorToHex(playerColors[0]));
    }

    private static String colorToHex(javafx.scene.paint.Color c) {
        return String.format("#%02X%02X%02X",
            (int) Math.round(c.getRed() * 255),
            (int) Math.round(c.getGreen() * 255),
            (int) Math.round(c.getBlue() * 255));
    }

    public void waitForOpponent() throws InterruptedException {
        gameStartLatch.await();
    }

    public void sendMove(int row, int col) {
        if (selfPlayerId >= 0) {
            client.sendMove(row, col);
        }
    }

    public BoardSnapshot getLatestSnapshot() {
        return latestSnapshot;
    }

    public int getSelfPlayerId() {
        return selfPlayerId;
    }

    // Called by GameSessionView when the game ends
    public void stop() {
        client.stop();
        server.stop();
    }

    
    private class ClientCallback implements GameClient.StateCallback {
        @Override
        public void onStateReceived(BoardSnapshot snapshot) {
            latestSnapshot = snapshot;
        }

        @Override
        public void onConnected(int id) {
            selfPlayerId = id;
        }

        @Override
        public void onGameStart() {
            gameStartLatch.countDown();
        }

        @Override
        public void onError(String error) {
            System.err.println("[host] Server error: " + error);
        }
    }
}
