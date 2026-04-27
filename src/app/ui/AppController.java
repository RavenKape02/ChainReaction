package app.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.logic.GameEngine;
import app.logic.MoveResult;
import app.model.BoardSnapshot;
import app.model.GameSettings;
import app.model.PlayerProfile;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;

public final class AppController {

    private static final double WINDOW_WIDTH = 1200;
    private static final double WINDOW_HEIGHT = 900;
    private static final String ROOT_STYLE =
        "-fx-background-color: linear-gradient(to bottom right, #07111d, #102238 45%, #0f1724 100%);";
    private static final String GLASS_CARD_STYLE =
        "-fx-background-color: rgba(12,22,35,0.8);"
            + "-fx-background-radius: 28px;"
            + "-fx-border-color: rgba(255,255,255,0.08);"
            + "-fx-border-radius: 28px;"
            + "-fx-border-width: 1px;"
            + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.36), 30, 0.18, 0, 12);";
    private static final String PRIMARY_BUTTON_STYLE =
        "-fx-background-color: linear-gradient(to right, #63d7ff, #7c8bff);"
            + "-fx-text-fill: #071019;"
            + "-fx-font-size: 15px;"
            + "-fx-font-weight: 800;"
            + "-fx-padding: 14 24 14 24;"
            + "-fx-background-radius: 16px;";
    private static final String SECONDARY_BUTTON_STYLE =
        "-fx-background-color: rgba(255,255,255,0.08);"
            + "-fx-text-fill: #edf5ff;"
            + "-fx-font-size: 14px;"
            + "-fx-font-weight: 700;"
            + "-fx-padding: 14 22 14 22;"
            + "-fx-background-radius: 16px;"
            + "-fx-border-color: rgba(255,255,255,0.11);"
            + "-fx-border-radius: 16px;";
    private static final String TITLE_STYLE =
        "-fx-text-fill: white;"
            + "-fx-font-size: 52px;"
            + "-fx-font-weight: 900;";
    private static final String SUBTITLE_STYLE =
        "-fx-text-fill: rgba(232,242,255,0.7);"
            + "-fx-font-size: 16px;"
            + "-fx-font-weight: 500;";

    private final Stage stage;
    private final ArrayList<PlayerProfile> allPlayers;
    private final ArrayList<BoardOption> boardOptions;

    private Scene scene;
    private GameSettings currentSettings;

    public AppController(Stage stage) {
        this.stage = stage;
        this.allPlayers = createDefaultPlayers();
        this.boardOptions = new ArrayList<>();
        boardOptions.add(new BoardOption("9 x 6", 9, 6));
        boardOptions.add(new BoardOption("15 x 10", 15, 10));
        this.currentSettings = new GameSettings(2, 9, 6, 15);
    }

    public void show() {
        scene = new Scene(buildMainMenu(), WINDOW_WIDTH, WINDOW_HEIGHT);
        stage.setTitle("Chain Reaction");
        stage.setMinWidth(980);
        stage.setMinHeight(760);
        stage.setScene(scene);
        stage.show();
    }

    private Parent buildMainMenu() {
        BorderPane root = new BorderPane();
        root.setStyle(ROOT_STYLE);
        root.setPadding(new Insets(36));

        VBox content = new VBox(26);
        content.setMaxWidth(760);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(34, 38, 34, 38));
        content.setStyle(GLASS_CARD_STYLE);

        Label title = new Label("Chain Reaction");
        title.setStyle(TITLE_STYLE);

        Label subtitle = new Label(
            "Modern JavaFX 26 recreation with the original critical-mass explosion rules."
        );
        subtitle.setWrapText(true);
        subtitle.setStyle(SUBTITLE_STYLE);

        ComboBox<Integer> playerCountBox = new ComboBox<>();
        for (int playerCount = 2; playerCount <= 8; playerCount++) {
            playerCountBox.getItems().add(playerCount);
        }
        playerCountBox.setValue(currentSettings.getPlayerCount());
        styleComboBox(playerCountBox);

        ComboBox<BoardOption> boardSizeBox = new ComboBox<>();
        boardSizeBox.getItems().addAll(boardOptions);
        boardSizeBox.setValue(findBoardOption(currentSettings.getRows(), currentSettings.getColumns()));
        styleComboBox(boardSizeBox);

        // timer settings
        ComboBox<Integer> timerComboBox = new ComboBox<>();
        timerComboBox.getItems().addAll(0, 10, 15, 20, 30, 45, 60);
        timerComboBox.setValue(currentSettings.getTimeLimitSeconds());
        styleComboBox(timerComboBox);
        
        // display "No Timer" for 0 value on dropdown
        timerComboBox.setCellFactory(lv -> new javafx.scene.control.ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item == 0) {
                    setText("No Timer");
                } else {
                    setText(item + "s");
                }
            }
        });
        
        // display "No Timer" for 0 value on main menu
        timerComboBox.setButtonCell(new javafx.scene.control.ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item == 0) {
                    setText("No Timer");
                } else {
                    setText(item + "s");
                }
            }
        });

        FlowPane playerPreview = new FlowPane();
        playerPreview.setHgap(10);
        playerPreview.setVgap(10);

        Runnable refreshPreview = () -> {
            currentSettings = new GameSettings(
                playerCountBox.getValue(),
                boardSizeBox.getValue().rows(),
                boardSizeBox.getValue().columns(),
                timerComboBox.getValue()
            );

            playerPreview.getChildren().clear();
            for (int index = 0; index < currentSettings.getPlayerCount(); index++) {
                playerPreview.getChildren().add(createPlayerChip(allPlayers.get(index), false, false));
            }
        };

        playerCountBox.setOnAction(event -> refreshPreview.run());
        boardSizeBox.setOnAction(event -> refreshPreview.run());
        timerComboBox.setOnAction(event -> refreshPreview.run());
        refreshPreview.run();

        GridPane configurationGrid = new GridPane();
        configurationGrid.setHgap(18);
        configurationGrid.setVgap(12);

        Label playersLabel = createSectionLabel("Players");
        Label boardLabel = createSectionLabel("Board");
        Label timerLabel = createSectionLabel("Turn Timer");
        configurationGrid.add(playersLabel, 0, 0);
        configurationGrid.add(boardLabel, 1, 0);
        configurationGrid.add(playerCountBox, 0, 1);
        configurationGrid.add(boardSizeBox, 1, 1);
        configurationGrid.add(timerLabel, 2, 0);
        configurationGrid.add(timerComboBox, 2, 1);

        VBox infoCard = new VBox(10);
        infoCard.setPadding(new Insets(18));
        infoCard.setStyle(
            "-fx-background-color: rgba(255,255,255,0.05);"
                + "-fx-background-radius: 20px;"
                + "-fx-border-color: rgba(255,255,255,0.07);"
                + "-fx-border-radius: 20px;"
        );
        infoCard.getChildren().addAll(
            createHintLabel("Corners explode at 2 orbs."),
            createHintLabel("Edges explode at 3 orbs."),
            createHintLabel("Inner cells explode at 4 orbs."),
            createHintLabel("Any exploding orb recolors the destination cell.")
        );

        HBox actions = new HBox(14);
        Button startButton = createPrimaryButton("Start Match");
        Button settingsButton = createSecondaryButton("Player Colors");
        startButton.setOnAction(event -> showGameScreen());
        settingsButton.setOnAction(event -> showSettingsScreen());
        actions.getChildren().addAll(startButton, settingsButton);

        Label previewLabel = createSectionLabel("Active Player Palette");

        content.getChildren().addAll(
            title,
            subtitle,
            configurationGrid,
            previewLabel,
            playerPreview,
            infoCard,
            actions
        );

        root.setCenter(wrapCentered(content));
        return root;
    }

    private Parent buildSettingsScreen() {
        BorderPane root = new BorderPane();
        root.setStyle(ROOT_STYLE);
        root.setPadding(new Insets(36));

        VBox card = new VBox(20);
        card.setMaxWidth(860);
        card.setPadding(new Insets(32, 34, 32, 34));
        card.setStyle(GLASS_CARD_STYLE);

        Label title = new Label("Player Colors");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 34px; -fx-font-weight: 900;");

        Label subtitle = new Label(
            "Choose eight distinct colors. The current match uses the first N players from the main menu."
        );
        subtitle.setWrapText(true);
        subtitle.setStyle(SUBTITLE_STYLE);

        VBox rows = new VBox(12);
        ArrayList<ColorPicker> pickers = new ArrayList<>();

        for (int index = 0; index < allPlayers.size(); index++) {
            PlayerProfile player = allPlayers.get(index);
            HBox row = new HBox(16);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(14, 16, 14, 16));
            row.setStyle(
                "-fx-background-color: rgba(255,255,255,0.04);"
                    + "-fx-background-radius: 18px;"
                    + "-fx-border-color: rgba(255,255,255,0.06);"
                    + "-fx-border-radius: 18px;"
            );

            Circle swatch = new Circle(14, player.getColor());
            Label name = new Label(player.getName());
            name.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: 800;");

            Label hexLabel = new Label(colorToHex(player.getColor()));
            hexLabel.setStyle("-fx-text-fill: rgba(235,244,255,0.55); -fx-font-size: 13px; -fx-font-weight: 700;");

            ColorPicker picker = new ColorPicker(player.getColor());
            picker.setStyle("-fx-color-label-visible: false;");
            picker.valueProperty().addListener((observable, oldColor, newColor) -> {
                Color opaqueColor = Color.color(newColor.getRed(), newColor.getGreen(), newColor.getBlue());
                swatch.setFill(opaqueColor);
                hexLabel.setText(colorToHex(opaqueColor));
            });

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            row.getChildren().addAll(swatch, name, spacer, hexLabel, picker);
            rows.getChildren().add(row);
            pickers.add(picker);
        }

        ScrollPane scrollPane = new ScrollPane(rows);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
            "-fx-background: transparent;"
                + "-fx-background-color: transparent;"
                + "-fx-border-color: transparent;"
        );
        scrollPane.setMaxHeight(520);

        Label messageLabel = new Label("Settings apply to new matches.");
        messageLabel.setStyle("-fx-text-fill: rgba(232,242,255,0.7); -fx-font-size: 14px; -fx-font-weight: 700;");

        HBox actions = new HBox(14);
        Button saveButton = createPrimaryButton("Save Colors");
        Button backButton = createSecondaryButton("Back");
        saveButton.setOnAction(event -> {
            Set<String> uniqueColors = new HashSet<>();
            ArrayList<PlayerProfile> updatedProfiles = new ArrayList<>();

            for (int index = 0; index < pickers.size(); index++) {
                Color selectedColor = pickers.get(index).getValue();
                Color opaqueColor = Color.color(selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue());
                String hex = colorToHex(opaqueColor);
                if (!uniqueColors.add(hex)) {
                    messageLabel.setText("Each player must use a different color.");
                    messageLabel.setTextFill(Color.web("#ff9aa7"));
                    return;
                }

                updatedProfiles.add(new PlayerProfile(allPlayers.get(index).getName(), opaqueColor));
            }

            allPlayers.clear();
            allPlayers.addAll(updatedProfiles);
            messageLabel.setText("Colors saved.");
            messageLabel.setTextFill(Color.web("#89f0c2"));
        });
        backButton.setOnAction(event -> showMainMenu());
        actions.getChildren().addAll(saveButton, backButton);

        card.getChildren().addAll(title, subtitle, scrollPane, messageLabel, actions);
        root.setCenter(wrapCentered(card));
        return root;
    }

    private void showGameScreen() {
        ArrayList<PlayerProfile> activePlayers = new ArrayList<>(
            allPlayers.subList(0, currentSettings.getPlayerCount())
        );
        GameEngine engine = new GameEngine(currentSettings, activePlayers);
        transitionTo(new GameSessionView(engine, activePlayers).root());
    }

    private void showMainMenu() {
        transitionTo(buildMainMenu());
    }

    private void showSettingsScreen() {
        transitionTo(buildSettingsScreen());
    }

    private void transitionTo(Parent newRoot) {
        Parent currentRoot = scene.getRoot();
        FadeTransition fadeOut = new FadeTransition(Duration.millis(120), currentRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> {
            newRoot.setOpacity(0);
            scene.setRoot(newRoot);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(150), newRoot);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: rgba(240,247,255,0.85); -fx-font-size: 13px; -fx-font-weight: 800;");
        return label;
    }

    private Label createHintLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: rgba(236,244,255,0.8); -fx-font-size: 14px; -fx-font-weight: 600;");
        return label;
    }

    private Button createPrimaryButton(String text) {
        Button button = new Button(text);
        button.setStyle(PRIMARY_BUTTON_STYLE);
        button.setOnMouseEntered(event -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), button);
            st.setToX(1.05);
            st.setToY(1.05);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
            button.setStyle(PRIMARY_BUTTON_STYLE
                + "-fx-effect: dropshadow(gaussian, rgba(99,215,255,0.35), 14, 0.3, 0, 0);");
        });
        button.setOnMouseExited(event -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
            button.setStyle(PRIMARY_BUTTON_STYLE);
        });
        return button;
    }

    private Button createSecondaryButton(String text) {
        Button button = new Button(text);
        button.setStyle(SECONDARY_BUTTON_STYLE);
        button.setOnMouseEntered(event -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), button);
            st.setToX(1.05);
            st.setToY(1.05);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
            button.setStyle(SECONDARY_BUTTON_STYLE
                + "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.15), 10, 0.2, 0, 0);");
        });
        button.setOnMouseExited(event -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
            button.setStyle(SECONDARY_BUTTON_STYLE);
        });
        return button;
    }

    private <T> void styleComboBox(ComboBox<T> comboBox) {
        comboBox.setMinWidth(180);
        comboBox.setStyle(
            "-fx-background-color: rgba(255,255,255,0.08);"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: 700;"
                + "-fx-background-radius: 14px;"
                + "-fx-border-color: rgba(255,255,255,0.1);"
                + "-fx-border-radius: 14px;"
                + "-fx-padding: 6 10 6 10;"
        );
    }

    private Parent wrapCentered(Region region) {
        StackPane pane = new StackPane(region);
        pane.setAlignment(Pos.CENTER);
        return pane;
    }

    private HBox createPlayerChip(PlayerProfile player, boolean active, boolean eliminated) {
        return createPlayerChip(player, active, eliminated, -1);
    }

    private HBox createPlayerChip(PlayerProfile player, boolean active, boolean eliminated, int orbCount) {
        HBox chip = new HBox(10);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setPadding(new Insets(10, 14, 10, 14));

        Circle dot = new Circle(8, player.getColor());
        Label nameLabel = new Label(player.getName());
        nameLabel.setStyle(
            "-fx-text-fill: " + (eliminated ? "rgba(255,255,255,0.4)" : "white") + ";"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: 800;"
        );

        chip.setStyle(
            "-fx-background-color: rgba(255,255,255," + (active ? "0.12" : "0.05") + ");"
                + "-fx-background-radius: 999px;"
                + "-fx-border-color: " + toRgba(player.getColor(), active ? 0.95 : 0.28) + ";"
                + "-fx-border-radius: 999px;"
                + "-fx-border-width: " + (active ? 1.8 : 1.0) + ";"
                + (eliminated ? "-fx-opacity: 0.45;" : "")
        );

        chip.getChildren().addAll(dot, nameLabel);

        if (orbCount >= 0) {
            Label countLabel = new Label(Integer.toString(orbCount));
            countLabel.setStyle(
                "-fx-text-fill: rgba(235,244,255,0.6);"
                    + "-fx-font-size: 12px;"
                    + "-fx-font-weight: 800;"
                    + "-fx-background-color: rgba(255,255,255,0.08);"
                    + "-fx-background-radius: 999px;"
                    + "-fx-padding: 2 8 2 8;"
            );
            chip.getChildren().add(countLabel);
        }

        if (active && !eliminated) {
            ScaleTransition pulse = new ScaleTransition(Duration.millis(600), chip);
            pulse.setFromX(1.0);
            pulse.setFromY(1.0);
            pulse.setToX(1.06);
            pulse.setToY(1.06);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.setInterpolator(Interpolator.EASE_BOTH);
            pulse.play();
        }

        return chip;
    }

    private BoardOption findBoardOption(int rows, int columns) {
        for (BoardOption option : boardOptions) {
            if (option.rows() == rows && option.columns() == columns) {
                return option;
            }
        }
        return boardOptions.get(0);
    }

    private ArrayList<PlayerProfile> createDefaultPlayers() {
        ArrayList<PlayerProfile> defaults = new ArrayList<>();
        defaults.add(new PlayerProfile("Player 1", Color.web("#ff5f70")));
        defaults.add(new PlayerProfile("Player 2", Color.web("#4de1ff")));
        defaults.add(new PlayerProfile("Player 3", Color.web("#ffd166")));
        defaults.add(new PlayerProfile("Player 4", Color.web("#9b8cff")));
        defaults.add(new PlayerProfile("Player 5", Color.web("#7ef7b8")));
        defaults.add(new PlayerProfile("Player 6", Color.web("#ff9f68")));
        defaults.add(new PlayerProfile("Player 7", Color.web("#f28dff")));
        defaults.add(new PlayerProfile("Player 8", Color.web("#b1ff67")));
        return defaults;
    }

    private String colorToHex(Color color) {
        int red = (int) Math.round(color.getRed() * 255);
        int green = (int) Math.round(color.getGreen() * 255);
        int blue = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02X%02X%02X", red, green, blue);
    }

    private String toRgba(Color color, double opacity) {
        int red = (int) Math.round(color.getRed() * 255);
        int green = (int) Math.round(color.getGreen() * 255);
        int blue = (int) Math.round(color.getBlue() * 255);
        return "rgba(" + red + "," + green + "," + blue + "," + opacity + ")";
    }

    private record BoardOption(String label, int rows, int columns) {
        @Override
        public String toString() {
            return label;
        }
    }

    private final class GameSessionView {

        private final GameEngine engine;
        private final ArrayList<PlayerProfile> players;
        private final StackPane rootStack;
        private final BorderPane gamePane;
        private final BoardCellView[][] cellViews;
        private final Label turnLabel = new Label();
        private final Label moveCountLabel = new Label();
        private final Label statusLabel = new Label();
        private final Label winnerBanner = new Label();
        private final FlowPane playerStrip = new FlowPane();
        private final GridPane boardGrid = new GridPane();
        private final Label timerLabel = new Label();

        // timer update mechanism
        private javafx.animation.AnimationTimer timerDisplayUpdater;

        private boolean animationRunning;
        private int lastMoveRow = -1;
        private int lastMoveCol = -1;

        private GameSessionView(GameEngine engine, ArrayList<PlayerProfile> players) {
            this.engine = engine;
            this.players = players;
            this.rootStack = new StackPane();
            this.gamePane = new BorderPane();
            this.cellViews = new BoardCellView[engine.getSettings().getRows()][engine.getSettings().getColumns()];

            gamePane.setStyle(ROOT_STYLE);
            gamePane.setPadding(new Insets(28));

            gamePane.setTop(buildHeader());
            gamePane.setCenter(buildBoardArea());
            gamePane.setBottom(buildFooter());

            rootStack.getChildren().add(gamePane);

            renderSnapshot(engine.getSnapshot());
            statusLabel.setText("Select a cell that is empty or already yours.");
            
            // setup timer display updater
            setupTimerDisplayUpdater();
        }

        private Parent root() {
            return rootStack;
        }

        private Parent buildHeader() {
            VBox header = new VBox(16);

            HBox topRow = new HBox(14);
            topRow.setAlignment(Pos.CENTER_LEFT);

            Label title = new Label("Chain Reaction");
            title.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: 900;");

            Label boardLabel = new Label(engine.getSettings().getBoardLabel());
            boardLabel.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);"
                    + "-fx-background-radius: 999px;"
                    + "-fx-padding: 8 14 8 14;"
                    + "-fx-text-fill: rgba(240,247,255,0.82);"
                    + "-fx-font-size: 13px;"
                    + "-fx-font-weight: 800;"
            );

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button restartButton = createPrimaryButton("New Match");
            Button menuButton = createSecondaryButton("Main Menu");
            restartButton.setOnAction(event -> showGameScreen());
            menuButton.setOnAction(event -> showMainMenu());

            topRow.getChildren().addAll(title, boardLabel, spacer, restartButton, menuButton);

            HBox statusRow = new HBox(16);
            statusRow.setAlignment(Pos.CENTER_LEFT);

            turnLabel.setStyle(
                "-fx-background-color: rgba(255,255,255,0.07);"
                    + "-fx-background-radius: 999px;"
                    + "-fx-padding: 10 16 10 16;"
                    + "-fx-text-fill: white;"
                    + "-fx-font-size: 14px;"
                    + "-fx-font-weight: 800;"
            );

            // timer label
            timerLabel.setStyle(
                "-fx-background-color: rgba(255,255,255,0.07);"
                    + "-fx-background-radius: 999px;"
                    + "-fx-padding: 10 16 10 16;"
                    + "-fx-text-fill: white;"
                    + "-fx-font-size: 14px;"
                    + "-fx-font-weight: 800;"
            );
            timerLabel.setText(engine.getSettings().getTimeLimitSeconds() <= 0 ? "∞" : engine.getSettings().getTimeLimitSeconds() + "s");

            winnerBanner.setVisible(false);
            winnerBanner.setStyle(
                "-fx-background-color: linear-gradient(to right, #89f0c2, #63d7ff);"
                    + "-fx-background-radius: 999px;"
                    + "-fx-padding: 10 16 10 16;"
                    + "-fx-text-fill: #05101a;"
                    + "-fx-font-size: 14px;"
                    + "-fx-font-weight: 900;"
            );

            moveCountLabel.setText("Move 0");
            moveCountLabel.setStyle(
                "-fx-background-color: rgba(255,255,255,0.07);"
                    + "-fx-background-radius: 999px;"
                    + "-fx-padding: 10 16 10 16;"
                    + "-fx-text-fill: rgba(240,247,255,0.65);"
                    + "-fx-font-size: 13px;"
                    + "-fx-font-weight: 800;"
            );

            playerStrip.setHgap(10);
            playerStrip.setVgap(10);

            statusRow.getChildren().addAll(turnLabel, moveCountLabel, timerLabel, winnerBanner);
            header.getChildren().addAll(topRow, statusRow, playerStrip);
            return header;
        }

        private Parent buildBoardArea() {
            double cellSize = engine.getSettings().getRows() >= 15 ? 44 : 68;

            boardGrid.setHgap(8);
            boardGrid.setVgap(8);
            boardGrid.setAlignment(Pos.CENTER);
            boardGrid.setPadding(new Insets(18));

            for (int row = 0; row < engine.getSettings().getRows(); row++) {
                for (int column = 0; column < engine.getSettings().getColumns(); column++) {
                    int currentRow = row;
                    int currentColumn = column;

                    BoardCellView cellView = new BoardCellView(
                        engine.criticalMass(row, column),
                        cellSize,
                        () -> handleMove(currentRow, currentColumn)
                    );
                    cellViews[row][column] = cellView;
                    boardGrid.add(cellView, column, row);
                }
            }

            StackPane boardCard = new StackPane(boardGrid);
            boardCard.setPadding(new Insets(20));
            boardCard.setStyle(GLASS_CARD_STYLE);
            return wrapCentered(boardCard);
        }

        private Parent buildFooter() {
            HBox footer = new HBox();
            footer.setAlignment(Pos.CENTER_LEFT);
            footer.setPadding(new Insets(18, 8, 0, 8));

            statusLabel.setStyle(
                "-fx-text-fill: rgba(236,244,255,0.78);"
                    + "-fx-font-size: 14px;"
                    + "-fx-font-weight: 700;"
            );

            footer.getChildren().add(statusLabel);
            return footer;
        }

        private void handleMove(int row, int column) {
            if (animationRunning) {
                return;
            }

            MoveResult result = engine.playMove(row, column);
            if (!result.isAccepted()) {
                statusLabel.setText(result.getMessage());
                return;
            }

            lastMoveRow = row;
            lastMoveCol = column;

            animationRunning = true;
            boardGrid.setDisable(true);
            statusLabel.setText("Resolving chain reaction...");

            long animationStartNanos = System.nanoTime();

            List<BoardSnapshot> frames = result.getAnimationFrames();
            renderSnapshot(frames.get(0));

            if (frames.size() == 1) {
                // still pause timer briefly even for single frame animation
                long animationEndNanos = System.nanoTime();
                javafx.util.Duration animationDuration = javafx.util.Duration.millis((animationEndNanos - animationStartNanos) / 1_000_000);
                pauseTimerForAnimation(animationDuration);
                finishMove(result);
                return;
            }

            SequentialTransition sequence = new SequentialTransition();
            for (int index = 1; index < frames.size(); index++) {
                BoardSnapshot frame = frames.get(index);
                PauseTransition pause = new PauseTransition(Duration.millis(150));
                pause.setOnFinished(event -> renderSnapshot(frame));
                sequence.getChildren().add(pause);
            }

            sequence.setOnFinished(event -> {
                // calculate duration and pause timer after animation ended
                long animationEndNanos = System.nanoTime();
                javafx.util.Duration animationDuration = javafx.util.Duration.millis((animationEndNanos - animationStartNanos) / 1_000_000);
                pauseTimerForAnimation(animationDuration);
                finishMove(result);
            });
            sequence.play();
        }

        private void finishMove(MoveResult result) {
            renderSnapshot(result.getFinalSnapshot());
            statusLabel.setText(result.getMessage());
            animationRunning = false;
            boardGrid.setDisable(false);

            // Flash the last-played cell
            if (lastMoveRow >= 0 && lastMoveCol >= 0) {
                cellViews[lastMoveRow][lastMoveCol].flashLastMove();
            }

            if (result.getFinalSnapshot().hasWinner()) {
                boardGrid.setDisable(true);
                showWinnerOverlay(result.getFinalSnapshot());
            }
        }

        private void showWinnerOverlay(BoardSnapshot snapshot) {
            int winIdx = snapshot.getWinnerIndex().intValue();
            PlayerProfile winner = players.get(winIdx);

            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-background-color: rgba(5,10,20,0.82);");

            VBox card = new VBox(22);
            card.setAlignment(Pos.CENTER);
            card.setMaxWidth(420);
            card.setPadding(new Insets(44, 40, 44, 40));
            card.setStyle(GLASS_CARD_STYLE);

            Label trophy = new Label("\uD83C\uDFC6");
            trophy.setStyle("-fx-font-size: 56px;");

            Label winnerName = new Label(winner.getName() + " Wins!");
            winnerName.setStyle(
                "-fx-text-fill: white; -fx-font-size: 34px; -fx-font-weight: 900;"
            );

            Label subtitle = new Label("Dominated the board in " + snapshot.getMovesPlayed() + " moves");
            subtitle.setStyle(
                "-fx-text-fill: rgba(232,242,255,0.65); -fx-font-size: 14px; -fx-font-weight: 700;"
            );

            HBox buttons = new HBox(14);
            buttons.setAlignment(Pos.CENTER);
            Button playAgain = createPrimaryButton("Play Again");
            Button mainMenu = createSecondaryButton("Main Menu");
            playAgain.setOnAction(event -> showGameScreen());
            mainMenu.setOnAction(event -> showMainMenu());
            buttons.getChildren().addAll(playAgain, mainMenu);

            card.getChildren().addAll(trophy, winnerName, subtitle, buttons);
            overlay.getChildren().add(card);

            overlay.setOpacity(0);
            rootStack.getChildren().add(overlay);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(350), overlay);
            fadeIn.setToValue(1.0);
            fadeIn.play();
            
            // stop timer updates when game ends
            if (timerDisplayUpdater != null) {
                timerDisplayUpdater.stop();
            }
        }

        private void renderSnapshot(BoardSnapshot snapshot) {
            PlayerProfile currentPlayer = players.get(snapshot.getCurrentPlayerIndex());

            turnLabel.setText(snapshot.hasWinner()
                ? players.get(snapshot.getWinnerIndex().intValue()).getName() + " controls the board"
                : currentPlayer.getName() + " to play");

            moveCountLabel.setText("Move " + snapshot.getMovesPlayed());

            winnerBanner.setVisible(snapshot.hasWinner());
            if (snapshot.hasWinner()) {
                winnerBanner.setText(players.get(snapshot.getWinnerIndex().intValue()).getName() + " wins");
            }

            playerStrip.getChildren().clear();
            for (int playerIndex = 0; playerIndex < players.size(); playerIndex++) {
                playerStrip.getChildren().add(
                    createPlayerChip(
                        players.get(playerIndex),
                        playerIndex == snapshot.getCurrentPlayerIndex() && !snapshot.hasWinner(),
                        snapshot.isPlayerEliminated(playerIndex),
                        countPlayerOrbs(snapshot, playerIndex)
                    )
                );
            }

            for (int row = 0; row < snapshot.getRows(); row++) {
                for (int column = 0; column < snapshot.getColumns(); column++) {
                    int ownerIndex = snapshot.getOwnerIndex(row, column);
                    Color ownerColor = ownerIndex >= 0 ? players.get(ownerIndex).getColor() : null;
                    cellViews[row][column].render(
                        snapshot.getOrbCount(row, column),
                        ownerColor,
                        currentPlayer.getColor()
                    );
                }
            }
        }

        private int countPlayerOrbs(BoardSnapshot snapshot, int playerIndex) {
            int count = 0;
            for (int row = 0; row < snapshot.getRows(); row++) {
                for (int col = 0; col < snapshot.getColumns(); col++) {
                    if (snapshot.getOwnerIndex(row, col) == playerIndex) {
                        count += snapshot.getOrbCount(row, col);
                    }
                }
            }
            return count;
        }

        // timer display updater
        private void setupTimerDisplayUpdater() {
            // stop any existing timer updater
            if (timerDisplayUpdater != null) {
                timerDisplayUpdater.stop();
            }
            
            // do not start timer updater if timer is disabled
            if (engine.getSettings().getTimeLimitSeconds() <= 0) {
                return;
            }
            
            timerDisplayUpdater = new AnimationTimer() {
                private long lastUpdate = 0;
                
                @Override
                public void handle(long now) {
                    // update at most once per second
                    if (now - lastUpdate >= 1_000_000_000) {
                        lastUpdate = now;
                        updateTimerDisplay();
                    }
                }
            };
            timerDisplayUpdater.start();
        }
        
        private void updateTimerDisplay() {
            // show ∞ and neutral style if timer is no time limit 
            if (engine.getSettings().getTimeLimitSeconds() <= 0) {
                timerLabel.setText("∞");
                timerLabel.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.03);"
                        + "-fx-background-radius: 999px;"
                        + "-fx-padding: 10 16 10 16;"
                        + "-fx-text-fill: rgba(255,255,255,0.35);"
                        + "-fx-font-size: 14px;"
                        + "-fx-font-weight: 800;"
                );
                return;
            }
            
            int remainingSeconds = engine.getRemainingTimeSeconds();
            
            // update timer display
            timerLabel.setText(remainingSeconds + "s");
            
            // change color based on remaining time
            if (remainingSeconds <= 3) {
                timerLabel.setStyle(
                    "-fx-background-color: rgba(255,0,0,0.2);"
                        + "-fx-background-radius: 999px;"
                        + "-fx-padding: 10 16 10 16;"
                        + "-fx-text-fill: #ff0000;"
                        + "-fx-font-size: 14px;"
                        + "-fx-font-weight: 800;"
                );
            } else if (remainingSeconds <= 5) {
                timerLabel.setStyle(
                    "-fx-background-color: rgba(255,165,0,0.2);"
                        + "-fx-background-radius: 999px;"
                        + "-fx-padding: 10 16 10 16;"
                        + "-fx-text-fill: #ffa500;"
                        + "-fx-font-size: 14px;"
                        + "-fx-font-weight: 800;"
                );
            } else {
                timerLabel.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.07);"
                        + "-fx-background-radius: 999px;"
                        + "-fx-padding: 10 16 10 16;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-size: 14px;"
                        + "-fx-font-weight: 800;"
                );
            }
            
            // check for timer expiration and handle turn skipping
            if (engine.handleTimerExpiration()) {
                statusLabel.setText("Time's up! Turn skipped.");
                renderSnapshot(engine.getSnapshot());
            }
        }
        
        // call to pause timer during animations
        private void pauseTimerForAnimation(javafx.util.Duration duration) {
            // only pause if timer is enabled
            if (engine.getSettings().getTimeLimitSeconds() <= 0) {
                return;
            }
            // convert javafx Duration to java Duration
            java.time.Duration javaDuration = java.time.Duration.ofMillis((long) duration.toMillis());
            engine.pauseTimer(javaDuration);
        }
    }
}
