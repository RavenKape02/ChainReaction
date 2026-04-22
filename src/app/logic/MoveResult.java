package app.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import app.model.BoardSnapshot;

public final class MoveResult {

    private final boolean accepted;
    private final String message;
    private final int actingPlayerIndex;
    private final ArrayList<BoardSnapshot> animationFrames;
    private final BoardSnapshot finalSnapshot;

    private MoveResult(
        boolean accepted,
        String message,
        int actingPlayerIndex,
        ArrayList<BoardSnapshot> animationFrames,
        BoardSnapshot finalSnapshot
    ) {
        this.accepted = accepted;
        this.message = message;
        this.actingPlayerIndex = actingPlayerIndex;
        this.animationFrames = new ArrayList<>(animationFrames);
        this.finalSnapshot = finalSnapshot;
    }

    public static MoveResult rejected(String message) {
        return new MoveResult(false, message, -1, new ArrayList<>(), null);
    }

    public static MoveResult accepted(
        int actingPlayerIndex,
        String message,
        ArrayList<BoardSnapshot> animationFrames,
        BoardSnapshot finalSnapshot
    ) {
        return new MoveResult(true, message, actingPlayerIndex, animationFrames, finalSnapshot);
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getMessage() {
        return message;
    }

    public int getActingPlayerIndex() {
        return actingPlayerIndex;
    }

    public List<BoardSnapshot> getAnimationFrames() {
        return Collections.unmodifiableList(animationFrames);
    }

    public BoardSnapshot getFinalSnapshot() {
        return finalSnapshot;
    }
}
