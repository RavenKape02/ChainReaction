package app.model;

import java.util.Arrays;

public final class BoardSnapshot {

    private final int rows;
    private final int columns;
    private final int[][] orbCounts;
    private final int[][] ownerIndexes;
    private final boolean[] eliminatedPlayers;
    private final int currentPlayerIndex;
    private final int movesPlayed;
    private final Integer winnerIndex;

    public BoardSnapshot(
        int[][] orbCounts,
        int[][] ownerIndexes,
        boolean[] eliminatedPlayers,
        int currentPlayerIndex,
        int movesPlayed,
        Integer winnerIndex
    ) {
        this.rows = orbCounts.length;
        this.columns = rows == 0 ? 0 : orbCounts[0].length;
        this.orbCounts = copyMatrix(orbCounts);
        this.ownerIndexes = copyMatrix(ownerIndexes);
        this.eliminatedPlayers = Arrays.copyOf(eliminatedPlayers, eliminatedPlayers.length);
        this.currentPlayerIndex = currentPlayerIndex;
        this.movesPlayed = movesPlayed;
        this.winnerIndex = winnerIndex;
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public int getOrbCount(int row, int column) {
        return orbCounts[row][column];
    }

    public int getOwnerIndex(int row, int column) {
        return ownerIndexes[row][column];
    }

    public boolean isPlayerEliminated(int playerIndex) {
        return eliminatedPlayers[playerIndex];
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public int getMovesPlayed() {
        return movesPlayed;
    }

    public Integer getWinnerIndex() {
        return winnerIndex;
    }

    public boolean hasWinner() {
        return winnerIndex != null;
    }
    
    public String encode() {
        StringBuilder sb = new StringBuilder();
        sb.append(rows).append(",").append(columns).append("|");
        
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                sb.append(ownerIndexes[row][col]).append(",").append(orbCounts[row][col]).append(";");
            }
        }
        sb.append("|");
        
        for (int i = 0; i < eliminatedPlayers.length; i++) {
            sb.append(eliminatedPlayers[i] ? "1" : "0");
        }
        sb.append("|").append(currentPlayerIndex).append("|").append(movesPlayed).append("|");
        
        if (winnerIndex != null) {
            sb.append(winnerIndex);
        }
        
        return sb.toString();
    }
    
    public static BoardSnapshot decode(String encoded) {
        String data = encoded;
        String[] parts = data.split("\\|");
        
        String[] dims = parts[0].split(",");
        int rows = Integer.parseInt(dims[0]);
        int columns = Integer.parseInt(dims[1]);
        
        int[][] orbCounts = new int[rows][columns];
        int[][] ownerIndexes = new int[rows][columns];
        
        String[] cells = parts[1].split(";");
        int idx = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                String[] cell = cells[idx++].split(",");
                ownerIndexes[row][col] = Integer.parseInt(cell[0]);
                orbCounts[row][col] = Integer.parseInt(cell[1]);
            }
        }
        
        boolean[] eliminatedPlayers = new boolean[parts[2].length()];
        for (int i = 0; i < eliminatedPlayers.length; i++) {
            eliminatedPlayers[i] = parts[2].charAt(i) == '1';
        }
        
        int currentPlayerIndex = Integer.parseInt(parts[3]);
        int movesPlayed = Integer.parseInt(parts[4]);
        Integer winnerIndex = parts.length > 5 && !parts[5].isEmpty() ? Integer.parseInt(parts[5]) : null;
        
        return new BoardSnapshot(orbCounts, ownerIndexes, eliminatedPlayers, currentPlayerIndex, movesPlayed, winnerIndex);
    }

    private int[][] copyMatrix(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int row = 0; row < source.length; row++) {
            copy[row] = Arrays.copyOf(source[row], source[row].length);
        }
        return copy;
    }
}
