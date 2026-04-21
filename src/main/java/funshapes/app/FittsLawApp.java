package funshapes.app;

import funshapes.controller.ExperimentController;
import funshapes.model.AppModel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * FittsLawApp is the JavaFX entry point for the HW-4 Fitts' Law Simulator.
 *
 * <p>
 * UI layout (mirrors the dare edition structure):
 * <ul>
 *   <li>Top HBox: title label (left), Go! and Quit! buttons (right)</li>
 *   <li>Center: drawing pane where targets appear</li>
 *   <li>Right: status panel showing trial progress (bound to controller)</li>
 * </ul>
 * </p>
 *
 * <p>
 * Window decorations (X button, minimize, resize) are intentionally left at
 * JavaFX defaults so the user can minimize or close at will. Closing mid-session
 * triggers forceClose() on the controller so the CSV is always flushed first.
 * </p>
 *
 * <b>Correspondences:</b><br>
 *   model      = AppModel (RNG, palette)<br>
 *   controller = ExperimentController (trial loop, CSV, timing)<br>
 *   goButton   = disabled while a session runs; re-enabled on completion
 *
 * @author Kylie Gilbert
 * @version HW-4 – CPSC 4140 – Spring 2026
 */
public class FittsLawApp extends Application {

    private ExperimentController controller;
    private Button goButton;

    // -----------------------------------------------------------------------
    // JavaFX lifecycle
    // -----------------------------------------------------------------------

    /**
     * Builds the window and wires the controller.
     *
     * @param primaryStage the primary application stage
     *
     * @pre primaryStage != null
     * @post Window is visible with top bar, drawing pane, and status panel.
     */
    @Override
    public void start(Stage primaryStage) {

        AppModel model = new AppModel();

        // ----- Root layout -----
        BorderPane root = new BorderPane();

        // ----- Center drawing pane -----
        Pane drawingPane = new Pane();
        drawingPane.setStyle("-fx-background-color: #f0f0f0;");
        root.setCenter(drawingPane);

        // ----- Controller -----
        controller = new ExperimentController(model, drawingPane);
        controller.setOnExperimentComplete(() -> goButton.setDisable(false));

        // ----- Right status panel (mirrors dare edition's config panel) -----
        VBox statusPanel = buildStatusPanel();
        root.setRight(statusPanel);

        // Bind the live status label to the controller's observable property.
        Label liveStatus = (Label) statusPanel.lookup("#liveStatus");
        if (liveStatus != null) {
            liveStatus.textProperty().bind(controller.statusProperty());
        }

        // ----- Top bar (HBox — same pattern as dare edition) -----
        root.setTop(buildTopBar(primaryStage));

        // ----- Scene -----
        Scene scene = new Scene(root, 950, 680);

        // Keyboard shortcuts: Ctrl+G = Go!, Ctrl+Q = Quit
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.G, KeyCombination.CONTROL_DOWN),
                () -> { if (!goButton.isDisabled()) triggerGo(); }
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN),
                () -> requestExit(primaryStage, "Ctrl+Q shortcut used.")
        );

        primaryStage.setTitle("HW-4: Fitts' Law Simulator – Kylie Gilbert");
        primaryStage.setScene(scene);

        /*
         * Window close (X button) is left at the JavaFX default so the user can
         * minimize and use OS controls freely, matching the dare edition behavior.
         * We add a handler solely to flush the CSV before the process exits.
         */
        primaryStage.setOnCloseRequest(e -> controller.forceClose());

        // Launch maximized so the window fits the screen without needing to resize.
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    // -----------------------------------------------------------------------
    // UI builders
    // -----------------------------------------------------------------------

    /**
     * Builds the top HBox bar containing the app title and action buttons.
     * Mirrors the dare edition's HBox top bar pattern exactly.
     *
     * @param stage the primary stage (used for the Quit! action)
     *
     * @pre stage != null
     * @post Returns a styled HBox with title label, spacer, Go!, and Quit! buttons.
     */
    private HBox buildTopBar(Stage stage) {
        Label titleLabel = new Label("Fitts' Law Simulator");
        titleLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 15));

        goButton = new Button("Go!");
        goButton.setStyle("-fx-font-weight: bold;");
        goButton.setOnAction(e -> triggerGo());

        Button quitButton = new Button("Quit!");
        quitButton.setOnAction(e -> requestExit(stage, "Quit! button pressed."));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(10, titleLabel, spacer, goButton, quitButton);
        topBar.setStyle(
                "-fx-padding: 8;" +
                "-fx-alignment: center-left;" +
                "-fx-border-color: #dddddd;" +
                "-fx-border-width: 0 0 1 0;"
        );
        return topBar;
    }

    /**
     * Builds the right-side status panel that shows experiment info.
     * Uses the same width and border style as the dare edition's config host.
     *
     * @pre none
     * @post Returns a VBox with a heading and a live status label (id="liveStatus").
     */
    private VBox buildStatusPanel() {
        Label heading = new Label("Experiment Status");
        heading.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));

        Label liveStatus = new Label("Ready — press Go! to begin.");
        liveStatus.setId("liveStatus");
        liveStatus.setWrapText(true);

        Separator sep = new Separator();

        Label hint1 = new Label("• Click targets as fast as you can.");
        Label hint2 = new Label("• Only the target responds to clicks.");
        Label hint3 = new Label("• Results saved to fitts_results.csv");
        Label trialInfo = new Label("Trials: " + ExperimentController.TOTAL_TRIALS);

        for (Label l : new Label[]{hint1, hint2, hint3, trialInfo}) {
            l.setWrapText(true);
            l.setStyle("-fx-text-fill: #555555;");
        }

        VBox panel = new VBox(10,
                heading, liveStatus, sep, hint1, hint2, hint3, trialInfo);
        panel.setPadding(new Insets(12));
        panel.setMinWidth(240);
        panel.setPrefWidth(260);
        panel.setStyle(
                "-fx-border-color: #bbbbbb;" +
                "-fx-border-width: 0 0 0 1;" +
                "-fx-background-color: #fafafa;"
        );
        panel.setAlignment(Pos.TOP_LEFT);
        return panel;
    }

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------

    /**
     * Disables Go! and starts the experiment via the controller.
     *
     * @pre goButton is not disabled
     * @post goButton is disabled; controller.beginExperiment() is called on next pulse.
     */
    private void triggerGo() {
        goButton.setDisable(true);
        Platform.runLater(controller::beginExperiment);
    }

    /**
     * Flushes the CSV writer and exits the application.
     *
     * @param stage  the primary stage (unused but kept for clarity)
     * @param reason brief log message
     *
     * @pre reason != null
     * @post controller.forceClose() called; Platform.exit() terminates the app.
     */
    private void requestExit(Stage stage, String reason) {
        System.out.println("Exiting: " + reason);
        controller.forceClose();
        Platform.exit();
    }

    // -----------------------------------------------------------------------
    // Entry point
    // -----------------------------------------------------------------------

    /**
     * @pre none
     * @post JavaFX runtime launched; start(Stage) invoked by framework.
     */
    public static void main(String[] args) { launch(args); }
}
