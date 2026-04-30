package app.ui;

import java.util.ArrayList;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

final class BoardCellView extends StackPane {

    private final int criticalMass;
    private final double size;
    private final Pane orbPane = new Pane();
    private final Group orbGroup = new Group();
    private final Label criticalMassLabel = new Label();
    private final Label overflowLabel = new Label();
    private final ArrayList<Animation> runningAnimations = new ArrayList<>();

    // State tracking for animation decisions
    private int previousOrbCount = 0;
    private Color previousOwnerColor = null;
    private boolean hovered = false;
    private Color currentOwnerColor = null;
    private Color currentAccentColor = Color.web("#65c6ff");
    private boolean currentNearCritical = false;
    private boolean currentUnstable = false;

    BoardCellView(int criticalMass, double size, Runnable onClick) {
        this.criticalMass = criticalMass;
        this.size = size;

        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);
        setAlignment(Pos.CENTER);
        setPickOnBounds(true);

        orbPane.setMinSize(size, size);
        orbPane.setPrefSize(size, size);
        orbPane.setMaxSize(size, size);
        orbPane.setPickOnBounds(false);
        orbPane.setMouseTransparent(true);
        orbGroup.setLayoutX(size / 2.0);
        orbGroup.setLayoutY(size / 2.0);
        orbPane.getChildren().add(orbGroup);

        criticalMassLabel.setText(Integer.toString(criticalMass));
        criticalMassLabel.setMouseTransparent(true);
        criticalMassLabel.setStyle(
            "-fx-text-fill: rgba(231,241,255,0.45);"
                + "-fx-font-size: 11px;"
                + "-fx-font-weight: 700;"
        );

        overflowLabel.setVisible(false);
        overflowLabel.setMouseTransparent(true);
        overflowLabel.setStyle(
            "-fx-background-color: rgba(7,12,19,0.86);"
                + "-fx-background-radius: 999px;"
                + "-fx-padding: 2 7 2 7;"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 11px;"
                + "-fx-font-weight: 800;"
        );

        updateCellChrome(null, Color.web("#65c6ff"), false, false);

        getChildren().addAll(orbPane, criticalMassLabel, overflowLabel);
        StackPane.setAlignment(criticalMassLabel, Pos.TOP_RIGHT);
        StackPane.setAlignment(overflowLabel, Pos.BOTTOM_RIGHT);

        setOnMouseClicked(event -> onClick.run());

        // Hover effects
        setOnMouseEntered(event -> {
            hovered = true;
            setCursor(Cursor.HAND);
            updateCellChrome(currentOwnerColor, currentAccentColor, currentNearCritical, currentUnstable);
            ScaleTransition st = new ScaleTransition(Duration.millis(100), this);
            st.setToX(1.06);
            st.setToY(1.06);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        });

        setOnMouseExited(event -> {
            hovered = false;
            setCursor(Cursor.DEFAULT);
            updateCellChrome(currentOwnerColor, currentAccentColor, currentNearCritical, currentUnstable);
            ScaleTransition st = new ScaleTransition(Duration.millis(100), this);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        });
    }

    void render(int orbCount, Color ownerColor, Color accentColor) {
        boolean nearCritical = orbCount > 0 && orbCount == criticalMass - 1;
        boolean unstable = orbCount >= criticalMass;

        boolean orbAdded = orbCount > previousOrbCount;
        boolean orbExploded = orbCount < previousOrbCount && previousOrbCount > 0;
        boolean colorChanged = ownerColor != null && previousOwnerColor != null
            && !ownerColor.equals(previousOwnerColor);

        currentOwnerColor = ownerColor;
        currentAccentColor = accentColor;
        currentNearCritical = nearCritical;
        currentUnstable = unstable;

        drawOrbs(orbCount, ownerColor == null ? Color.web("#d6e9ff") : ownerColor);
        updateCellChrome(ownerColor, accentColor, nearCritical, unstable);
        updateAnimations(nearCritical, unstable);

        // Orb placement pop animation
        if (orbAdded && orbCount > 0) {
            playPlacementPop();
        }

        // Explosion burst effect
        if (orbExploded) {
            playExplosionBurst(previousOwnerColor != null ? previousOwnerColor : accentColor);
        }

        // Color takeover flash
        if (colorChanged && orbCount > 0) {
            playTakeoverFlash(ownerColor);
        }

        overflowLabel.setVisible(orbCount > 4);
        if (orbCount > 4) {
            overflowLabel.setText(Integer.toString(orbCount));
        }

        previousOrbCount = orbCount;
        previousOwnerColor = ownerColor;
    }

    /** Flash a golden border to indicate this was the last move. */
    void flashLastMove() {
        DropShadow glow = new DropShadow(20, Color.web("#ffd166"));
        glow.setSpread(0.4);
        setEffect(glow);

        Timeline timeline = new Timeline(
            new KeyFrame(Duration.millis(350), event -> {
                setEffect(null);
                updateCellChrome(currentOwnerColor, currentAccentColor, currentNearCritical, currentUnstable);
            })
        );
        timeline.play();
    }

    private void playPlacementPop() {
        orbGroup.setScaleX(0.3);
        orbGroup.setScaleY(0.3);

        ScaleTransition pop = new ScaleTransition(Duration.millis(100), orbGroup);
        pop.setToX(1.0);
        pop.setToY(1.0);
        pop.setInterpolator(Interpolator.SPLINE(0.175, 0.885, 0.32, 1.275));
        pop.play();
    }

    private void playExplosionBurst(Color burstColor) {
        Circle ring = new Circle(0, 0, size * 0.1);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(burstColor.brighter());
        ring.setStrokeWidth(3);
        ring.setOpacity(0.9);
        ring.setMouseTransparent(true);
        ring.setLayoutX(size / 2.0);
        ring.setLayoutY(size / 2.0);

        orbPane.getChildren().add(ring);

        ScaleTransition expand = new ScaleTransition(Duration.millis(120), ring);
        expand.setFromX(0.5);
        expand.setFromY(0.5);
        expand.setToX(3.5);
        expand.setToY(3.5);
        expand.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(Duration.millis(120), ring);
        fade.setFromValue(0.9);
        fade.setToValue(0.0);

        ParallelTransition burst = new ParallelTransition(expand, fade);
        burst.setOnFinished(event -> orbPane.getChildren().remove(ring));
        burst.play();
    }

    private void playTakeoverFlash(Color newColor) {
        Circle flash = new Circle(size / 2.0, size / 2.0, size * 0.4);
        flash.setFill(newColor.deriveColor(0, 1, 1.5, 0.3));
        flash.setMouseTransparent(true);

        orbPane.getChildren().addFirst(flash);

        FadeTransition ft = new FadeTransition(Duration.millis(120), flash);
        ft.setFromValue(0.4);
        ft.setToValue(0.0);
        ft.setOnFinished(event -> orbPane.getChildren().remove(flash));
        ft.play();
    }

    private void drawOrbs(int orbCount, Color color) {
        orbGroup.getChildren().clear();
        if (orbCount == 0) {
            return;
        }

        int visibleOrbCount = Math.min(orbCount, 4);
        double radius = size * (visibleOrbCount >= 4 ? 0.105 : 0.12);
        double offset = size * 0.17;

        switch (visibleOrbCount) {
            case 1 -> orbGroup.getChildren().add(createOrb(0, 0, radius, color));
            case 2 -> {
                orbGroup.getChildren().add(createOrb(-offset, 0, radius, color));
                orbGroup.getChildren().add(createOrb(offset, 0, radius, color));
            }
            case 3 -> {
                orbGroup.getChildren().add(createOrb(0, -offset * 0.75, radius, color));
                orbGroup.getChildren().add(createOrb(-offset, offset * 0.8, radius, color));
                orbGroup.getChildren().add(createOrb(offset, offset * 0.8, radius, color));
            }
            default -> {
                orbGroup.getChildren().add(createOrb(-offset, -offset, radius, color));
                orbGroup.getChildren().add(createOrb(offset, -offset, radius, color));
                orbGroup.getChildren().add(createOrb(-offset, offset, radius, color));
                orbGroup.getChildren().add(createOrb(offset, offset, radius, color));
            }
        }
    }

    private Circle createOrb(double x, double y, double radius, Color baseColor) {
        Circle orb = new Circle(x, y, radius);
        orb.setFill(
            new RadialGradient(
                0,
                0.15,
                0.34,
                0.34,
                0.88,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0, baseColor.brighter().brighter()),
                new Stop(0.45, baseColor.brighter()),
                new Stop(1, baseColor.darker())
            )
        );
        orb.setStroke(baseColor.brighter());
        orb.setStrokeWidth(Math.max(1.2, size * 0.016));
        orb.setEffect(new DropShadow(size * 0.05, baseColor.deriveColor(0, 1, 1, 0.65)));
        return orb;
    }

    private void updateCellChrome(Color ownerColor, Color accentColor, boolean nearCritical, boolean unstable) {
        Color fillColor = ownerColor == null
            ? Color.web("#0f1c2b")
            : ownerColor.interpolate(Color.web("#0b1622"), 0.72);
        Color borderColor = ownerColor == null ? accentColor : ownerColor;

        double glowOpacity = unstable ? 0.44 : nearCritical ? 0.24 : 0.12;
        double borderOpacity = nearCritical || unstable ? 0.95 : 0.55;
        double borderWidth = nearCritical || unstable ? 1.9 : 1.2;
        int glowRadius = nearCritical || unstable ? 16 : 6;

        if (hovered) {
            glowOpacity = Math.min(glowOpacity + 0.2, 0.8);
            borderOpacity = Math.min(borderOpacity + 0.3, 1.0);
            borderWidth = Math.max(borderWidth, 2.2);
            glowRadius = Math.max(glowRadius, 14);
        }

        String glowColor = toRgba(borderColor, glowOpacity);

        setStyle(
            "-fx-background-color: " + toRgba(fillColor, ownerColor == null ? 0.6 : 0.82) + ";"
                + "-fx-background-radius: 18px;"
                + "-fx-border-color: " + toRgba(borderColor, borderOpacity) + ";"
                + "-fx-border-width: " + borderWidth + ";"
                + "-fx-border-radius: 18px;"
                + "-fx-effect: dropshadow(gaussian, " + glowColor + ", "
                + glowRadius
                + ", 0.35, 0, 0);"
        );
    }

    private void updateAnimations(boolean nearCritical, boolean unstable) {
        runningAnimations.forEach(Animation::stop);
        runningAnimations.clear();

        orbGroup.setRotate(0);
        orbGroup.setScaleX(1);
        orbGroup.setScaleY(1);

        if (!nearCritical && !unstable) {
            return;
        }

        RotateTransition rotate = new RotateTransition(
            unstable ? Duration.millis(430) : Duration.millis(950),
            orbGroup
        );
        rotate.setByAngle(360);
        rotate.setCycleCount(Animation.INDEFINITE);
        rotate.setInterpolator(Interpolator.LINEAR);

        ScaleTransition scale = new ScaleTransition(
            unstable ? Duration.millis(260) : Duration.millis(420),
            orbGroup
        );
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(unstable ? 1.18 : 1.11);
        scale.setToY(unstable ? 1.18 : 1.11);
        scale.setAutoReverse(true);
        scale.setCycleCount(Animation.INDEFINITE);
        scale.setInterpolator(Interpolator.EASE_BOTH);

        runningAnimations.add(rotate);
        runningAnimations.add(scale);
        rotate.play();
        scale.play();
    }

    private String toRgba(Color color, double opacity) {
        int red = (int) Math.round(color.getRed() * 255);
        int green = (int) Math.round(color.getGreen() * 255);
        int blue = (int) Math.round(color.getBlue() * 255);
        return "rgba(" + red + "," + green + "," + blue + "," + opacity + ")";
    }
}
