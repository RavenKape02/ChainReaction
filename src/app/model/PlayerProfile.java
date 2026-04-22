package app.model;

import javafx.scene.paint.Color;

public final class PlayerProfile {

    private final String name;
    private final Color color;

    public PlayerProfile(String name, Color color) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Player name cannot be blank.");
        }
        if (color == null) {
            throw new IllegalArgumentException("Player color cannot be null.");
        }

        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }

    public PlayerProfile withColor(Color newColor) {
        return new PlayerProfile(name, newColor);
    }
}
