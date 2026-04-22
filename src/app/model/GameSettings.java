package app.model;

public final class GameSettings {

    private final int playerCount;
    private final int rows;
    private final int columns;

    public GameSettings(int playerCount, int rows, int columns) {
        if (playerCount < 2 || playerCount > 8) {
            throw new IllegalArgumentException("Player count must be between 2 and 8.");
        }
        if (rows < 2 || columns < 2) {
            throw new IllegalArgumentException("Board dimensions must be at least 2 x 2.");
        }

        this.playerCount = playerCount;
        this.rows = rows;
        this.columns = columns;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public String getBoardLabel() {
        return rows + " x " + columns;
    }
}
