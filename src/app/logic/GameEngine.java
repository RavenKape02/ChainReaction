package app.logic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.time.Duration;

import app.model.BoardCell;
import app.model.BoardSnapshot;
import app.model.GameSettings;
import app.model.PlayerProfile;

public final class GameEngine {

    private final GameSettings settings;
    private final ArrayList<PlayerProfile> players;
    private final BoardCell[][] board;
    private final boolean[] eliminatedPlayers;

    private int currentPlayerIndex;
    private int movesPlayed;
    private Integer winnerIndex;
    
    // timer fields
    private Instant turnStartTime;
    private boolean timerExpired;

    public GameEngine(GameSettings settings, List<PlayerProfile> players) {
        if (players.size() != settings.getPlayerCount()) {
            throw new IllegalArgumentException("The number of players must match the selected settings.");
        }

        this.settings = settings;
        this.players = new ArrayList<>(players);
        this.board = new BoardCell[settings.getRows()][settings.getColumns()];
        this.eliminatedPlayers = new boolean[settings.getPlayerCount()];
        
        // initialize timer fields
        this.turnStartTime = null;
        this.timerExpired = false;

        for (int row = 0; row < settings.getRows(); row++) {
            for (int column = 0; column < settings.getColumns(); column++) {
                board[row][column] = new BoardCell();
            }
        }
    }

    // timer methods
    public void startTurnTimer() {
        if (settings.getTimeLimitSeconds() > 0) {
            this.turnStartTime = Instant.now();
            this.timerExpired = false;
        }
    }

    public boolean isTimerExpired() {
        if ((settings.getTimeLimitSeconds() <= 0) || (turnStartTime == null)) {
            return false;
        }
        Duration elapsed = Duration.between(turnStartTime, Instant.now());
        return elapsed.getSeconds() >= settings.getTimeLimitSeconds();
    }

    public int getRemainingTimeSeconds() {
        if (settings.getTimeLimitSeconds() <= 0) {
            return 0; // if no timer 
        }
        if (turnStartTime == null) {
            return settings.getTimeLimitSeconds();
        }
        Duration elapsed = Duration.between(turnStartTime, Instant.now());
        int remaining = settings.getTimeLimitSeconds() - (int) elapsed.getSeconds();
        return Math.max(0, remaining);
    }

    public void resetTimer() {
        this.turnStartTime = null;
        this.timerExpired = false;
    }
    
    // if the timer has expired advance to next player
    public boolean handleTimerExpiration() {
        if (isTimerExpired() && !timerExpired) {
            this.timerExpired = true;
            // advance to next player
            if (winnerIndex == null) {
                currentPlayerIndex = nextPlayerIndex(currentPlayerIndex);
            }
            // reset timer for next player
            startTurnTimer();
            return true;
        }
        return false;
    }
    
    // pauses timer by given duration.
    public void pauseTimer(Duration pauseDuration) {
        if (turnStartTime != null && settings.getTimeLimitSeconds() > 0) {
            this.turnStartTime = turnStartTime.plus(pauseDuration);
        }
    }

    public GameSettings getSettings() {
        return settings;
    }

    public ArrayList<PlayerProfile> getPlayers() {
        return new ArrayList<>(players);
    }

    public BoardSnapshot getSnapshot() {
        return createSnapshot();
    }

    public int criticalMass(int row, int column) {
        int mass = 4;
        if (row == 0 || row == settings.getRows() - 1) {
            mass--;
        }
        if (column == 0 || column == settings.getColumns() - 1) {
            mass--;
        }
        return mass;
    }

    public MoveResult playMove(int row, int column) {
        if (winnerIndex != null) {
            return MoveResult.rejected("This match is already over. Start a new round from the header.");
        }

        if (isTimerExpired()) {
            return MoveResult.rejected("Time's up! Turn skipped due to timer expiration.");
        }

        if (!isInsideBoard(row, column)) {
            return MoveResult.rejected("That move is outside the board.");
        }

        BoardCell selectedCell = board[row][column];
        if (!selectedCell.isEmpty() && selectedCell.getOwnerIndex() != currentPlayerIndex) {
            return MoveResult.rejected("You can only place an orb in an empty cell or one you already control.");
        }

        // reset timer at start of turn
        startTurnTimer();

        int actingPlayerIndex = currentPlayerIndex;
        ArrayList<BoardSnapshot> animationFrames = new ArrayList<>();

        addOrb(row, column, actingPlayerIndex);
        animationFrames.add(createSnapshot());

        ArrayDeque<int[]> pendingExplosions = new ArrayDeque<>();
        if (board[row][column].getOrbCount() >= criticalMass(row, column)) {
            pendingExplosions.add(new int[] { row, column });
        }

        while (!pendingExplosions.isEmpty()) {
            int[] cellPosition = pendingExplosions.removeFirst();
            int currentRow = cellPosition[0];
            int currentColumn = cellPosition[1];
            BoardCell explodingCell = board[currentRow][currentColumn];

            while (!explodingCell.isEmpty() && explodingCell.getOrbCount() >= criticalMass(currentRow, currentColumn)) {
                int ownerIndex = explodingCell.getOwnerIndex();
                int requiredMass = criticalMass(currentRow, currentColumn);

                explodingCell.setOrbCount(explodingCell.getOrbCount() - requiredMass);

                // Each emitted orb recolors the destination cell to the exploding player.
                for (int[] neighbor : collectNeighbors(currentRow, currentColumn)) {
                    int neighborRow = neighbor[0];
                    int neighborColumn = neighbor[1];
                    BoardCell neighborCell = board[neighborRow][neighborColumn];

                    neighborCell.setOwnerIndex(ownerIndex);
                    neighborCell.setOrbCount(neighborCell.getOrbCount() + 1);

                    if (neighborCell.getOrbCount() >= criticalMass(neighborRow, neighborColumn)) {
                        pendingExplosions.addLast(new int[] { neighborRow, neighborColumn });
                    }
                }

                animationFrames.add(createSnapshot());
                explodingCell = board[currentRow][currentColumn];
            }
        }

        movesPlayed++;
        if (movesPlayed > players.size()) {
            updateEliminatedPlayers();
        }

        winnerIndex = findWinner();
        if (winnerIndex == null) {
            currentPlayerIndex = nextPlayerIndex(currentPlayerIndex);
        } else {
            currentPlayerIndex = winnerIndex.intValue();
        }

        BoardSnapshot finalSnapshot = createSnapshot();
        String message = winnerIndex == null
            ? players.get(currentPlayerIndex).getName() + " is up next."
            : players.get(winnerIndex.intValue()).getName() + " wins the match.";

        return MoveResult.accepted(actingPlayerIndex, message, animationFrames, finalSnapshot);
    }

    private void addOrb(int row, int column, int playerIndex) {
        BoardCell cell = board[row][column];
        cell.setOwnerIndex(playerIndex);
        cell.setOrbCount(cell.getOrbCount() + 1);
    }

    private boolean isInsideBoard(int row, int column) {
        return row >= 0 && row < settings.getRows() && column >= 0 && column < settings.getColumns();
    }

    private ArrayList<int[]> collectNeighbors(int row, int column) {
        ArrayList<int[]> neighbors = new ArrayList<>(4);

        if (row > 0) {
            neighbors.add(new int[] { row - 1, column });
        }
        if (row < settings.getRows() - 1) {
            neighbors.add(new int[] { row + 1, column });
        }
        if (column > 0) {
            neighbors.add(new int[] { row, column - 1 });
        }
        if (column < settings.getColumns() - 1) {
            neighbors.add(new int[] { row, column + 1 });
        }

        return neighbors;
    }

    private void updateEliminatedPlayers() {
        boolean[] playerHasOrbs = new boolean[players.size()];

        for (int row = 0; row < settings.getRows(); row++) {
            for (int column = 0; column < settings.getColumns(); column++) {
                int ownerIndex = board[row][column].getOwnerIndex();
                if (ownerIndex >= 0) {
                    playerHasOrbs[ownerIndex] = true;
                }
            }
        }

        for (int playerIndex = 0; playerIndex < players.size(); playerIndex++) {
            if (!playerHasOrbs[playerIndex]) {
                eliminatedPlayers[playerIndex] = true;
            }
        }
    }

    private Integer findWinner() {
        if (movesPlayed < players.size()) {
            return null;
        }

        int ownerIndex = -1;

        for (int row = 0; row < settings.getRows(); row++) {
            for (int column = 0; column < settings.getColumns(); column++) {
                BoardCell cell = board[row][column];
                if (cell.isEmpty()) {
                    continue;
                }

                if (ownerIndex == -1) {
                    ownerIndex = cell.getOwnerIndex();
                } else if (ownerIndex != cell.getOwnerIndex()) {
                    return null;
                }
            }
        }

        return ownerIndex == -1 ? null : Integer.valueOf(ownerIndex);
    }

    private int nextPlayerIndex(int startIndex) {
        int playerIndex = startIndex;

        do {
            playerIndex = (playerIndex + 1) % players.size();
            if (!eliminatedPlayers[playerIndex]) {
                return playerIndex;
            }
        } while (playerIndex != startIndex);

        return startIndex;
    }

    private BoardSnapshot createSnapshot() {
        int[][] orbCounts = new int[settings.getRows()][settings.getColumns()];
        int[][] ownerIndexes = new int[settings.getRows()][settings.getColumns()];

        for (int row = 0; row < settings.getRows(); row++) {
            for (int column = 0; column < settings.getColumns(); column++) {
                orbCounts[row][column] = board[row][column].getOrbCount();
                ownerIndexes[row][column] = board[row][column].getOwnerIndex();
            }
        }

        return new BoardSnapshot(
            orbCounts,
            ownerIndexes,
            eliminatedPlayers,
            currentPlayerIndex,
            movesPlayed,
            winnerIndex
        );
    }
}
