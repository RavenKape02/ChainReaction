package app.network;

import java.util.ArrayList;
import java.util.List;
import app.model.BoardSnapshot;

public class TestNetwork {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Host...");
        GameHost host = new GameHost(
            2, 3, 3, 0,
            new String[]{"Philip", "Ralph"},
            new javafx.scene.paint.Color[]{
                javafx.scene.paint.Color.RED,
                javafx.scene.paint.Color.BLUE
            }
        );
        System.out.println("  Host is player " + host.getSelfPlayerId() + " — waiting for opponent...");

        Thread.sleep(300);

        System.out.println("Starting Client Joiner...");
        TestClient clientJoiner = new TestClient("127.0.0.1", NetworkConstants.PORT, "Ralph", "#0000FF");

        Thread hostThread = new Thread(() -> {
            try {
                host.waitForOpponent();
                System.out.println("  [host] Players connected — game starting");
                Thread.sleep(200);
                System.out.println("  [host] Sending MOVE 0,0");
                host.sendMove(0, 0);
            } catch (Exception e) {
                System.err.println("  [host] Error: " + e.getMessage());
            }
        });
        hostThread.start();

        Thread.sleep(1500);
        System.out.println();

        if (clientJoiner.getLatestSnapshot() == null) {
            System.out.println("FAIL: Ralph never received a STATE.");
            clientJoiner.stop();
            host.stop();
            return;
        }

        // Print Ralph's current snapshot
        printSnapshot("Ralph's state after host move", clientJoiner.getLatestSnapshot());

        System.out.println("- Ralph's sending MOVE 1,1");
        clientJoiner.sendMove(1, 1);
        Thread.sleep(300);

        if (clientJoiner.getLatestSnapshot() == null) {
            System.out.println("FAIL: Ralph got no snapshot after own MOVE.");
            clientJoiner.stop();
            host.stop();
            return;
        }

        System.out.println();
        System.out.println("Ralph's Final Board State");
        BoardSnapshot snap = clientJoiner.getLatestSnapshot();
        if (snap != null && snap.getRows() > 0) {
            System.out.println("  Current player: " + snap.getCurrentPlayerIndex());
            System.out.println("  Moves played:   " + snap.getMovesPlayed());
            for (int r = 0; r < snap.getRows(); r++) {
                for (int c = 0; c < snap.getColumns(); c++) {
                    System.out.printf("  [%d,%d] owner=%d orbs=%d",
                        r, c, snap.getOwnerIndex(r, c), snap.getOrbCount(r, c));
                    if (c < snap.getColumns() - 1) System.out.print("  ");
                }
                System.out.println();
            }

            boolean hasOrbAt00 = snap.getOwnerIndex(0, 0) == host.getSelfPlayerId() && snap.getOrbCount(0, 0) >= 1;
            boolean hasOrbAt11 = snap.getOwnerIndex(1, 1) >= 0 && snap.getOrbCount(1, 1) >= 1;
            boolean twoMoves = snap.getMovesPlayed() >= 2;
            System.out.println();
            System.out.println("  Board 3x3 ?        " + (snap.getRows() == 3 && snap.getColumns() == 3));
            System.out.println("  Host orb at 0,0 ?  " + hasOrbAt00);
            System.out.println("  Ralph's orb at 1,1? " + hasOrbAt11);
            System.out.println("  Two moves played?  " + twoMoves);
            if (snap.getRows() == 3 && snap.getColumns() == 3 && hasOrbAt00 && twoMoves) {
                System.out.println();
            } else {
                System.out.println();
                System.out.println("SOME FAILED.");
            }
        }

        System.out.println();
        System.out.println("Stopping all...");
        clientJoiner.stop();
        host.stop();
        System.out.println("Done.");
    }

    private static void printSnapshot(String label, BoardSnapshot snap) {
        if (snap == null || snap.getRows() == 0) {
            System.out.println("  [snapshot] null / uninitialized");
            return;
        }
        System.out.println("  [" + label + "] rows=" + snap.getRows() + " cols=" + snap.getColumns()
            + " moves=" + snap.getMovesPlayed()
            + " turn=" + snap.getCurrentPlayerIndex());
        for (int r = 0; r < snap.getRows(); r++) {
            StringBuilder line = new StringBuilder();
            for (int c = 0; c < snap.getColumns(); c++) {
                line.append(String.format("  [%d,%d] o=%d",
                    r, c, snap.getOrbCount(r, c)));
            }
            System.out.println(line);
        }
    }


    static class TestClient implements GameClient.StateCallback {
        private final GameClient client;
        private BoardSnapshot latestSnapshot;

        TestClient(String serverIp, int port, String name, String colorHex) throws Exception {
            this.client = new GameClient(serverIp, port);
            client.setCallback(this);
            Thread.sleep(50);
            client.connect(name, colorHex);
        }

        void sendMove(int row, int col) { client.sendMove(row, col); }
        BoardSnapshot getLatestSnapshot() { return latestSnapshot; }
        void stop() { client.stop(); }

        @Override public void onStateReceived(BoardSnapshot snapshot) {
            System.out.println("  [Ralph] STATE — player " +
                snapshot.getCurrentPlayerIndex() + " to play, " +
                snapshot.getMovesPlayed() + " moves played");
            latestSnapshot = snapshot;
        }
        @Override public void onConnected(int id) {
            System.out.println("  [Ralph] CONNECTED as player " + id);
        }
        @Override public void onGameStart() {
            System.out.println("  [Ralph] START received");
        }
        @Override public void onSettingsReceived(int playerCount, int rows, int columns, int timeLimitSeconds) {
            System.out.println("  [Ralph] SETTINGS: players=" + playerCount + " rows=" + rows + " cols=" + columns + " timer=" + timeLimitSeconds);
        }
        @Override public void onError(String error) {
            System.out.println("  [Ralph] ERROR: " + error);
        }
    }
}
