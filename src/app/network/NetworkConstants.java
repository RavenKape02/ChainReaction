package app.network;

public final class NetworkConstants {
    public static final int PORT = 4444;
    public static final int SOCKET_TIMEOUT_MS = 100;
    public static final int BUFFER_SIZE = 1024;
    
    public static final String MSG_CONNECT = "CONNECT";
    public static final String MSG_CONNECTED = "CONNECTED";
    public static final String MSG_START = "START";
    public static final String MSG_MOVE = "MOVE";
    public static final String MSG_STATE = "STATE";
    public static final String MSG_WAITING = "WAITING";
    public static final String MSG_ERROR = "ERROR";
    public static final String MSG_SETTINGS = "SETTINGS";
    
    private NetworkConstants() {}
}