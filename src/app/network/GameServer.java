package app.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import app.logic.GameEngine;
import app.model.GameSettings;
import app.model.PlayerProfile;
import app.model.BoardSnapshot;

public class GameServer implements Runnable {
    private static final int WAITING_FOR_PLAYERS = 0;
    private static final int GAME_START = 1;
    private static final int IN_PROGRESS = 2;
    
    private DatagramSocket serverSocket;
    private Thread serverThread;
    private GameEngine engine;
    private GameSettings settings;
    private Map<Integer, NetworkPlayer> players;
    private int currentPlayerId = 0;
    private int gameStage = WAITING_FOR_PLAYERS;
    private boolean running = true;
    private int playerCount = 0;
    
    public GameServer(int numPlayers, int rows, int columns, int timeLimit, String[] playerNames, javafx.scene.paint.Color[] playerColors) {
        this.settings = new GameSettings(numPlayers, rows, columns, timeLimit);
        
        java.util.List<PlayerProfile> profiles = new java.util.ArrayList<>();
        for (int i = 0; i < playerNames.length; i++) {
            profiles.add(new PlayerProfile(playerNames[i], playerColors[i]));
        }
        
        this.engine = new GameEngine(settings, profiles);
        this.players = new HashMap<>();
        
        try {
            serverSocket = new DatagramSocket(NetworkConstants.PORT);
            serverSocket.setSoTimeout(NetworkConstants.SOCKET_TIMEOUT_MS);
            serverThread = new Thread(this);
            serverThread.start();
            System.out.println("Network Server started on port " + NetworkConstants.PORT);
        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        }
    }
    
    @Override
    public void run() {
        byte[] buffer = new byte[NetworkConstants.BUFFER_SIZE];
        
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                serverSocket.receive(packet);
                
                String message = new String(packet.getData(), 0, packet.getLength()).trim();
                handleMessage(message, packet.getAddress(), packet.getPort());
            } catch (IOException e) {
            }
        }
    }
    
    private void handleMessage(String message, InetAddress address, int port) {
        String[] parts = message.split("\t");
        
        if (parts.length == 0) return;
        
        String command = parts[0];
        switch (gameStage) {
            case WAITING_FOR_PLAYERS:
                if (command.equals(NetworkConstants.MSG_CONNECT) && parts.length >= 3) {
                    String name = parts[1];
                    String colorHex = parts[2];
                    javafx.scene.paint.Color color = javafx.scene.paint.Color.web(colorHex);
                    
                    NetworkPlayer player = new NetworkPlayer(name, color, address, port, currentPlayerId);
                    players.put(currentPlayerId, player);
                    playerCount++;
                    
                    System.out.println("Player connected: " + name + " (id=" + currentPlayerId + ")");
                    
                    sendToClient(address, port, NetworkConstants.MSG_CONNECTED + " " + currentPlayerId);
                    
                    if (playerCount >= settings.getPlayerCount()) {
                        gameStage = GAME_START;
                        // send host game settings to all players
                        broadcast(NetworkConstants.MSG_SETTINGS + " " + 
                                settings.getPlayerCount() + " " +
                                settings.getRows() + " " +
                                settings.getColumns() + " " +
                                settings.getTimeLimitSeconds());
                        // then start the game
                        broadcast(NetworkConstants.MSG_START);
                        gameStage = IN_PROGRESS;
                        broadcast(NetworkConstants.MSG_STATE + " " + engine.getSnapshot().encode());
                    }
                    
                    currentPlayerId++;
                }
                break;
                
            case IN_PROGRESS:
                if (command.equals(NetworkConstants.MSG_MOVE) && parts.length >= 4) {
                    int playerId = Integer.parseInt(parts[1]);
                    int row = Integer.parseInt(parts[2]);
                    int col = Integer.parseInt(parts[3]);
                    
                    if (playerId == engine.getSnapshot().getCurrentPlayerIndex()) {
                        var result = engine.playMove(row, col);
                        if (result.isAccepted()) {
                            broadcast(NetworkConstants.MSG_STATE + " " + engine.getSnapshot().encode());
                        } else {
                            sendToClient(address, port, NetworkConstants.MSG_ERROR + " " + result.getMessage());
                        }
                    }
                }
                break;
        }
    }
    
    private void broadcast(String message) {
        for (NetworkPlayer player : players.values()) {
            sendToClient(player.getAddress(), player.getPort(), message);
        }
    }
    
    private void sendToClient(InetAddress address, int port, String message) {
        try {
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            serverSocket.send(packet);
        } catch (IOException e) {
            System.err.println("Failed to send message: " + e.getMessage());
        }
    }
    
    public void stop() {
        running = false;
        if (serverSocket != null) {
            serverSocket.close();
        }
    }
}