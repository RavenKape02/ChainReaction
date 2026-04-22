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

    private int[][] copyMatrix(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int row = 0; row < source.length; row++) {
            copy[row] = Arrays.copyOf(source[row], source[row].length);
        }
        return copy;
    }
}
