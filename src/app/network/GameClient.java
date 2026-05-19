package app.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import app.model.BoardSnapshot;

public class GameClient implements Runnable {
    private DatagramSocket socket;
    private Thread receiveThread;
    private boolean running = true;
    private InetAddress serverAddress;
    private int serverPort;
    private int playerId = -1;
    
    public interface StateCallback {
        void onStateReceived(BoardSnapshot snapshot);
        void onConnected(int id);
        void onGameStart();
        void onError(String error);
    }
    
    private StateCallback callback;
    private BoardSnapshot latestSnapshot;
    
    public GameClient(String serverIp, int port) throws IOException {
        this.serverAddress = InetAddress.getByName(serverIp);
        this.serverPort = port;
        this.socket = new DatagramSocket();
        this.socket.setSoTimeout(NetworkConstants.SOCKET_TIMEOUT_MS);
        this.receiveThread = new Thread(this);
        this.receiveThread.start();
    }
    
    public void setCallback(StateCallback callback) {
        this.callback = callback;
    }
    
    public void connect(String playerName, String colorHex) {
        send(NetworkConstants.MSG_CONNECT + " " + playerName + " " + colorHex);
    }
    
    public void sendMove(int row, int column) {
        if (playerId >= 0) {
            send(NetworkConstants.MSG_MOVE + " " + playerId + " " + row + " " + column);
        }
    }
    
    private void send(String message) {
        try {
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Failed to send: " + e.getMessage());
        }
    }
    
    @Override
    public void run() {
        byte[] buffer = new byte[NetworkConstants.BUFFER_SIZE];
        
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String message = new String(packet.getData(), 0, packet.getLength()).trim();
                handleMessage(message);
            } catch (IOException e) {
            }
        }
    }
    
    private void handleMessage(String message) {
        String[] parts = message.split(" ", 2);
        String command = parts[0];
        String payload = parts.length > 1 ? parts[1] : "";
        
        switch (command) {
            case NetworkConstants.MSG_CONNECTED:
                playerId = Integer.parseInt(payload);
                if (callback != null) callback.onConnected(playerId);
                System.out.println("Connected as player " + playerId);
                break;
                
            case NetworkConstants.MSG_START:
                if (callback != null) callback.onGameStart();
                System.out.println("Game started.");
                break;
                
            case NetworkConstants.MSG_STATE:
                try {
                    latestSnapshot = BoardSnapshot.decode(payload);
                    if (callback != null) callback.onStateReceived(latestSnapshot);
                } catch (Exception e) {
                    System.err.println("Failed to decode state: " + e.getMessage());
                }
                break;
                
            case NetworkConstants.MSG_ERROR:
                if (callback != null) callback.onError(payload);
                System.out.println("Error: " + payload);
                break;
                
            case NetworkConstants.MSG_WAITING:
                System.out.println("Waiting for players...");
                break;
        }
    }
    
    public BoardSnapshot getLatestSnapshot() {
        return latestSnapshot;
    }
    
    public int getPlayerId() {
        return playerId;
    }
    
    public void stop() {
        running = false;
        if (socket != null) {
            socket.close();
        }
    }
}