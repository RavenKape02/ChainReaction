package app.network;

import java.net.InetAddress;
import javafx.scene.paint.Color;
import app.model.PlayerProfile;

public final class NetworkPlayer {
    private final String name;
    private final Color color;
    private final InetAddress address;
    private final int port;
    private final int playerId;
    
    public NetworkPlayer(String name, Color color, InetAddress address, int port) {
        this(name, color, address, port, -1);
    }
    
    public NetworkPlayer(String name, Color color, InetAddress address, int port, int playerId) {
        this.name = name;
        this.color = color;
        this.address = address;
        this.port = port;
        this.playerId = playerId;
    }
    
    public String getName() {
        return name;
    }
    
    public Color getColor() {
        return color;
    }
    
    public InetAddress getAddress() {
        return address;
    }
    
    public int getPort() {
        return port;
    }
    
    public int getPlayerId() {
        return playerId;
    }
    
    @Override
    public String toString() {
        String colorHex = String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
        return playerId + ":" + name + ":" + colorHex;
    }
    
    public static NetworkPlayer fromString(String data, InetAddress address, int port) {
        String[] parts = data.split(":");
        int id = Integer.parseInt(parts[0]);
        String name = parts[1];
        Color color = Color.web(parts[2]);
        return new NetworkPlayer(name, color, address, port, id);
    }
    
    public PlayerProfile toProfile() {
        return new PlayerProfile(name, color);
    }
}