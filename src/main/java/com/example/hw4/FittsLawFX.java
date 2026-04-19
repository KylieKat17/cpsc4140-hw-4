package com.example.hw4;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

/**
 * FittsLawFX implements a Fitts' Law experiment simulator using JavaFX.
 *
 * <p>
 * The application presents the user with a series of circular targets at random
 * positions and sizes. The time to click each target is recorded along with
 * the target size and distance from the previous target. Results are written to
 * a CSV file for analysis.
 * </p>
 *
 * <b>Correspondences:</b><br>
 *   drawingPane    = the region of the window used for rendering targets<br>
 *   activeTarget   = the currently displayed target circle, or null if none<br>
 *   trialNumber    = 1-based index of the current trial<br>
 *   lastTargetX/Y  = center coordinates of the previous target (for distance calc)<br>
 *   trialStartTime = System.currentTimeMillis() snapshot when target appears<br>
 *   csvWriter      = PrintWriter to the output CSV file<br>
 *   countdownText  = Text node used for the 5-to-0 countdown display
 *
 * <p>
 * <b>Invariants:</b><br>
 *   TOTAL_TRIALS >= 50<br>
 *   MIN_RADIUS > 0 AND MAX_RADIUS > MIN_RADIUS<br>
 *   trialNumber is in [0, TOTAL_TRIALS] during experiment<br>
 *   activeTarget == null OR activeTarget is fully contained within drawingPane
 * </p>
 *
 * @author Kylie Gilbert
 * @version HW-4 – CPSC 4140 – Spring 2026
 */
public class FittsLawFX extends Application {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** Total number of trials to run per experiment session. */
    private static final int TOTAL_TRIALS = 50;

    /** Minimum target radius in pixels. */
    private static final double MIN_RADIUS = 15.0;

    /** Maximum target radius in pixels. */
    private static final double MAX_RADIUS = 80.0;

    /** Path (relative to working directory) for the generated CSV file. */
    private static final String CSV_FILENAME = "fitts_results.csv";

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /** Source of randomness for target placement, size, and color. */
    private final Random rng = new Random();

    /** Root layout that holds the menu bar and the drawing pane. */
    private BorderPane rootLayout;

    /** Pane on which targets are drawn. */
    private Pane drawingPane;

    /** The currently visible target circle. Null when no target is shown. */
    private Circle activeTarget;

    /** Current 1-based trial number. Zero means experiment has not started. */
    private int trialNumber = 0;

    /** X-coordinate of the center of the previous target (pixels). */
    private double lastTargetX = 0.0;

    /** Y-coordinate of the center of the previous target (pixels). */
    private double lastTargetY = 0.0;

    /** Millisecond timestamp captured when the current target is displayed. */
    private long trialStartTime = 0L;

    /** Writer for the output CSV file. Null until the experiment starts. */
    private PrintWriter csvWriter;

    /** Menu item reference so it can be disabled once the experiment starts. */
    private MenuItem goMenuItem;

    // -----------------------------------------------------------------------
    // JavaFX lifecycle
    // -----------------------------------------------------------------------

    /**
     * Initializes and displays the primary application window.
     *
     * @param primaryStage the primary application stage provided by JavaFX
     *
     * @pre primaryStage != null
     *
     * @post A window with a menu bar and blank drawing pane is displayed.
     *       Window close events are routed through requestProgramExit().
     */
    @Override
    public void start(Stage primaryStage) {
        rootLayout = new BorderPane();

        // ----- Menu Bar -----
        MenuBar menuBar = new MenuBar();
        menuBar.setUseSystemMenuBar(false);

        Menu fileMenu = new Menu("File");
        goMenuItem = new MenuItem("Go!");
        MenuItem quitMenuItem = new MenuItem("Quit");

        // Defer launch by one pulse so layout dimensions are finalized.
        goMenuItem.setOnAction(event -> Platform.runLater(this::beginExperiment));
        quitMenuItem.setOnAction(event -> requestProgramExit("Quit selected from File menu."));

        fileMenu.getItems().addAll(goMenuItem, new SeparatorMenuItem(), quitMenuItem);
        menuBar.getMenus().add(fileMenu);

        // ----- Drawing Pane -----
        // Intentionally blank on startup; content appears only after Go!
        drawingPane = new Pane();
        drawingPane.setStyle("-fx-background-color: #f0f0f0;");

        rootLayout.setTop(menuBar);
        rootLayout.setCenter(drawingPane);

        Scene scene = new Scene(rootLayout, 900, 700);
        primaryStage.setTitle("HW-4: Fitts' Law Simulator – Kylie Gilbert");
        primaryStage.setScene(scene);

        // Intercept the OS-level close button for controlled termination.
        primaryStage.setOnCloseRequest(this::handleWindowCloseRequest);
        primaryStage.show();
    }

    // -----------------------------------------------------------------------
    // Experiment lifecycle
    // -----------------------------------------------------------------------

    /**
     * Prepares the CSV writer and starts the 5-to-0 countdown.
     * Called once when the user selects File → Go!
     *
     * @pre trialNumber == 0 (experiment not yet started)
     * @pre drawingPane width and height > 0
     *
     * @post csvWriter is open, goMenuItem is disabled, countdown is running.
     */
    private void beginExperiment() {
        // Prevent re-entry if the experiment is already running.
        if (trialNumber > 0) {
            return;
        }

        // Disable Go! to prevent duplicate sessions.
        goMenuItem.setDisable(true);

        // Open the CSV output file.
        try {
            csvWriter = new PrintWriter(new FileWriter(CSV_FILENAME));
            // Write CSV header row.
            csvWriter.println("Trial Number,Target Size (pixels),Distance to Target (pixels),Time to Click (ms)");
        } catch (IOException e) {
            showErrorDialog(
                    "File Error",
                    "Could not create output file: " + CSV_FILENAME,
                    e.getMessage()
            );
            goMenuItem.setDisable(false);
            return;
        }

        // Clear the pane and begin countdown.
        drawingPane.getChildren().clear();
        showCountdown();
    }

    /**
     * Displays a 5-to-0 countdown in the center of the drawing pane.
     * When the countdown reaches 0, the first trial begins automatically.
     *
     * @pre drawingPane is visible and has valid dimensions
     *
     * @post A Timeline animates the countdown; the first target is shown on completion.
     */
    private void showCountdown() {
        // Large centered countdown label.
        Text countdownText = new Text("5");
        countdownText.setFont(Font.font("SansSerif", FontWeight.BOLD, 120));
        countdownText.setFill(Color.STEELBLUE);

        StackPane overlay = new StackPane(countdownText);
        overlay.prefWidthProperty().bind(drawingPane.widthProperty());
        overlay.prefHeightProperty().bind(drawingPane.heightProperty());
        overlay.setAlignment(Pos.CENTER);

        drawingPane.getChildren().add(overlay);

        // Timeline fires once per second to decrement the counter.
        final int[] countdown = {5};
        Timeline timer = new Timeline();
        timer.setCycleCount(6); // ticks for: 4, 3, 2, 1, 0

        KeyFrame tick = new KeyFrame(Duration.seconds(1), event -> {
            countdown[0]--;
            if (countdown[0] > 0) {
                countdownText.setText(String.valueOf(countdown[0]));
            } else {
                // Countdown finished – clear overlay and start trial 1.
                drawingPane.getChildren().remove(overlay);
                startNextTrial();
            }
        });

        timer.getKeyFrames().add(tick);
        timer.play();
    }

    /**
     * Advances to the next trial or concludes the experiment if all trials are done.
     *
     * @pre csvWriter != null
     *
     * @post Either a new target is shown (trialNumber incremented),
     *       or concludeExperiment() is called.
     */
    private void startNextTrial() {
        trialNumber++;

        if (trialNumber > TOTAL_TRIALS) {
            concludeExperiment();
            return;
        }

        showTarget();
    }

    /**
     * Generates and displays a randomly sized target at a random valid position.
     * Records the start time for this trial.
     *
     * @pre trialNumber is in [1, TOTAL_TRIALS]
     * @pre drawingPane has valid (positive) dimensions
     *
     * @post activeTarget is non-null and fully visible in drawingPane.
     *       trialStartTime is set to the current system time in milliseconds.
     */
    private void showTarget() {
        removeTargetIfPresent();

        double paneW = drawingPane.getWidth();
        double paneH = drawingPane.getHeight();

        if (paneW <= 0 || paneH <= 0) {
            showErrorDialog("Layout Error",
                    "Drawing pane has no size.",
                    "Try resizing the window and pressing Go! again.");
            return;
        }

        // Choose a random radius within the allowed range.
        double radius = MIN_RADIUS + rng.nextDouble() * (MAX_RADIUS - MIN_RADIUS);

        // Constrain center so the circle stays fully inside the pane.
        double centerX = radius + rng.nextDouble() * (paneW - 2 * radius);
        double centerY = radius + rng.nextDouble() * (paneH - 2 * radius);

        // Pick a random color from a pleasant palette.
        Color[] palette = {
            Color.TOMATO, Color.DODGERBLUE, Color.MEDIUMSEAGREEN,
            Color.DARKORCHID, Color.DARKORANGE, Color.DEEPPINK
        };
        Color fill = palette[rng.nextInt(palette.length)];

        // Create the target circle.
        activeTarget = new Circle(centerX, centerY, radius, fill);
        activeTarget.setStroke(Color.WHITE);
        activeTarget.setStrokeWidth(3);

        // Small trial counter label in the bottom-left corner (non-interactive).
        Text label = new Text(10, paneH - 10,
                "Trial " + trialNumber + " / " + TOTAL_TRIALS);
        label.setFont(Font.font("SansSerif", 14));
        label.setFill(Color.DARKGRAY);

        drawingPane.getChildren().addAll(activeTarget, label);

        // Capture start time AFTER the target is rendered for measurement accuracy.
        trialStartTime = System.currentTimeMillis();

        /*
         * EVENT HANDLING:
         * Only the circle itself listens for clicks (mouseEvent.consume()
         * prevents the pane from also receiving the event), satisfying the
         * requirement that "only the target responds to the click."
         */
        final double capturedCenterX = centerX;
        final double capturedCenterY = centerY;
        final double capturedRadius  = radius;

        activeTarget.setOnMouseClicked(mouseEvent -> {
            long elapsed = System.currentTimeMillis() - trialStartTime;

            // Target width (W) for Fitts' Law is the diameter.
            double targetDiameter = 2.0 * capturedRadius;

            // Distance (A) is Euclidean distance from the previous target center.
            double distance = Math.hypot(
                    capturedCenterX - lastTargetX,
                    capturedCenterY - lastTargetY
            );

            // For trial 1 there is no previous target, so distance is reported as 0.
            double reportedDistance = (trialNumber == 1) ? 0.0 : distance;

            recordTrial(trialNumber, targetDiameter, reportedDistance, elapsed);

            // Update last-target position for the next trial's distance calculation.
            lastTargetX = capturedCenterX;
            lastTargetY = capturedCenterY;

            // Clear the pane (removes target and label) before the next trial.
            drawingPane.getChildren().clear();
            activeTarget = null;

            mouseEvent.consume();
            startNextTrial();
        });
    }

    /**
     * Writes one row to the CSV file for the completed trial.
     *
     * @param trial    trial number (1-based)
     * @param size     target diameter in pixels
     * @param distance Euclidean distance from previous target center in pixels
     * @param timeMs   elapsed time in milliseconds
     *
     * @pre csvWriter != null AND trial >= 1
     *
     * @post One comma-separated row is written and flushed to the CSV file.
     */
    private void recordTrial(int trial, double size, double distance, long timeMs) {
        csvWriter.printf("%d, %.1f, %.1f, %d%n", trial, size, distance, timeMs);
        // Flush after every row to guard against data loss if closed unexpectedly.
        csvWriter.flush();
    }

    /**
     * Cleans up after all trials are complete: closes the CSV file and shows
     * a summary message.
     *
     * @pre trialNumber > TOTAL_TRIALS
     * @pre csvWriter != null
     *
     * @post csvWriter is closed. Drawing pane shows a completion message.
     *       A dialog informs the user where the CSV was saved.
     */
    private void concludeExperiment() {
        if (csvWriter != null) {
            csvWriter.close();
            csvWriter = null;
        }

        drawingPane.getChildren().clear();

        // Display completion text in the pane.
        Text doneText = new Text("Experiment complete!\nResults saved to: " + CSV_FILENAME);
        doneText.setFont(Font.font("SansSerif", FontWeight.BOLD, 24));
        doneText.setFill(Color.STEELBLUE);
        doneText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        StackPane overlay = new StackPane(doneText);
        overlay.prefWidthProperty().bind(drawingPane.widthProperty());
        overlay.prefHeightProperty().bind(drawingPane.heightProperty());
        overlay.setAlignment(Pos.CENTER);
        drawingPane.getChildren().add(overlay);

        // Also show a dialog so the result is not missed.
        Alert done = new Alert(Alert.AlertType.INFORMATION);
        done.setTitle("Experiment Complete");
        done.setHeaderText("All " + TOTAL_TRIALS + " trials finished!");
        done.setContentText(
                "Results saved to: " + CSV_FILENAME +
                "\n\nYou may close the window or quit via File → Quit."
        );
        done.getButtonTypes().setAll(ButtonType.OK);
        done.showAndWait();
    }

    // -----------------------------------------------------------------------
    // Utility helpers
    // -----------------------------------------------------------------------

    /**
     * Removes the active target from the pane if one is present.
     *
     * @pre drawingPane != null
     *
     * @post activeTarget is removed from drawingPane and set to null.
     */
    private void removeTargetIfPresent() {
        if (activeTarget != null) {
            drawingPane.getChildren().remove(activeTarget);
            activeTarget = null;
        }
    }

    /**
     * Intercepts the OS window close button and routes termination through
     * the controlled exit method.
     *
     * @param event the close request event
     *
     * @pre event != null
     *
     * @post Default close behavior is suppressed; requestProgramExit() is called.
     *       If a CSV is open mid-experiment, it is flushed and closed first.
     */
    private void handleWindowCloseRequest(WindowEvent event) {
        event.consume();
        if (csvWriter != null) {
            csvWriter.close();
            csvWriter = null;
        }
        requestProgramExit("Window close button (X) was used.");
    }

    /**
     * Centralized controlled exit point for the application.
     *
     * @param reason brief description of why the program is terminating
     *
     * @pre reason != null
     *
     * @post Platform.exit() is called; program terminates cleanly.
     */
    private void requestProgramExit(String reason) {
        System.out.println("Exiting: " + reason);
        Platform.exit();
    }

    /**
     * Displays an error dialog without terminating the application.
     *
     * @param title   dialog window title
     * @param header  short summary displayed in the dialog header
     * @param details detailed description of the error
     *
     * @pre title != null AND header != null AND details != null
     *
     * @post An error dialog is shown; execution continues after the user dismisses it.
     */
    private void showErrorDialog(String title, String header, String details) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(details);
        alert.showAndWait();
    }

    /**
     * Program entry point. Launches the JavaFX runtime.
     *
     * @param args command-line arguments (unused)
     *
     * @pre none
     *
     * @post JavaFX runtime is launched; start(Stage) is invoked by the framework.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
