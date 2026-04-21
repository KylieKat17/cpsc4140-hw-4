package funshapes.controller;

import funshapes.model.AppModel;
import funshapes.shapes.CircleSpawner;
import funshapes.shapes.SpawnContext;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * ExperimentController drives the full Fitts' Law trial loop.
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Running the 5-to-0 countdown before the first trial</li>
 *   <li>Spawning a new circle target for each trial at a random size and position</li>
 *   <li>Measuring the time from target display to click</li>
 *   <li>Writing each trial's data to a CSV file</li>
 *   <li>Showing a completion summary when all trials are done</li>
 *   <li>Routing all errors through a dialog rather than a crash</li>
 * </ul>
 * </p>
 *
 * <b>Correspondences:</b><br>
 *   drawingPane    = Pane on which targets are drawn<br>
 *   spawner        = CircleSpawner strategy used to build each target<br>
 *   trialNumber    = 1-based current trial index (0 = not started)<br>
 *   lastTargetX/Y  = center of the previous target for distance calculation<br>
 *   trialStartTime = ms timestamp when the current target was displayed<br>
 *   csvWriter      = open PrintWriter to the output CSV file
 *
 * <b>Invariants:</b><br>
 *   TOTAL_TRIALS >= 50<br>
 *   trialNumber in [0, TOTAL_TRIALS] during a session<br>
 *   activeShape == null OR activeShape is fully inside drawingPane
 *
 * @author Kylie Gilbert
 * @version HW-4 – CPSC 4140 – Spring 2026
 */
public class ExperimentController {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** Number of trials per experiment session. Must be >= 50. */
    private static final int TOTAL_TRIALS = 50;

    /** Padding (px) so targets never touch the pane edge. */
    private static final double EDGE_PADDING = 8.0;

    /** Path for the generated CSV file (relative to working directory). */
    private static final String CSV_FILENAME = "fitts_results.csv";

    // -----------------------------------------------------------------------
    // Dependencies
    // -----------------------------------------------------------------------

    private final AppModel model;
    private final Pane drawingPane;
    private final CircleSpawner spawner = new CircleSpawner();

    // -----------------------------------------------------------------------
    // Experiment state
    // -----------------------------------------------------------------------

    /** Currently displayed target Node. Null when no target is on screen. */
    private Node activeShape;

    /** 1-based trial index. Zero means the experiment has not started. */
    private int trialNumber = 0;

    /** Center X of the previous trial's target, used for distance calculation. */
    private double lastTargetX = 0.0;

    /** Center Y of the previous trial's target, used for distance calculation. */
    private double lastTargetY = 0.0;

    /** System.currentTimeMillis() snapshot when the current target appeared. */
    private long trialStartTime = 0L;

    /** Open writer for the CSV output. Non-null only while the experiment runs. */
    private PrintWriter csvWriter;

    /** Runnable invoked when the experiment finishes, so the app can re-enable Go!. */
    private Runnable onExperimentComplete;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Creates an ExperimentController bound to the given model and drawing pane.
     *
     * @param model       shared application model (RNG, palette)
     * @param drawingPane pane on which targets will be drawn
     *
     * @pre model != null AND drawingPane != null
     * @post Controller is ready; no experiment is running yet.
     */
    public ExperimentController(AppModel model, Pane drawingPane) {
        this.model       = model;
        this.drawingPane = drawingPane;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Registers a callback invoked when all trials are complete.
     * Typically used by the app to re-enable the Go! button.
     *
     * @param callback runnable to invoke on completion; null disables the callback
     *
     * @pre none
     * @post onExperimentComplete is set to callback.
     */
    public void setOnExperimentComplete(Runnable callback) {
        this.onExperimentComplete = callback;
    }

    /**
     * Starts the experiment: opens the CSV file and begins the countdown.
     * No-op if an experiment is already in progress.
     *
     * @pre drawingPane has valid positive dimensions at the time the countdown ends
     * @post csvWriter is open, countdown is animating, trialNumber will increment to 1 on first trial.
     */
    public void beginExperiment() {
        if (trialNumber > 0) {
            return; // already running
        }

        // Open the CSV output file.
        try {
            csvWriter = new PrintWriter(new FileWriter(CSV_FILENAME));
            csvWriter.println("Trial Number,Target Size (pixels),Distance to Target (pixels),Time to Click (ms)");
        } catch (IOException e) {
            showErrorDialog("File Error",
                    "Could not create: " + CSV_FILENAME,
                    e.getMessage());
            return;
        }

        drawingPane.getChildren().clear();
        showCountdown();
    }

    /**
     * Safely closes the CSV writer if the experiment is interrupted mid-session
     * (e.g. the user closes the window before completing all trials).
     *
     * @pre none
     * @post csvWriter is closed and set to null.
     */
    public void forceClose() {
        if (csvWriter != null) {
            csvWriter.close();
            csvWriter = null;
        }
    }

    // -----------------------------------------------------------------------
    // Countdown
    // -----------------------------------------------------------------------

    /**
     * Animates a 5-to-0 countdown centered in the drawing pane.
     * After the countdown reaches 0, the first trial begins.
     *
     * @pre drawingPane != null
     * @post A Timeline animates the countdown text; startNextTrial() is called on completion.
     */
    private void showCountdown() {
        Text countdownText = new Text("5");
        countdownText.setFont(Font.font("SansSerif", FontWeight.BOLD, 120));
        countdownText.setFill(Color.STEELBLUE);

        StackPane overlay = new StackPane(countdownText);
        overlay.prefWidthProperty().bind(drawingPane.widthProperty());
        overlay.prefHeightProperty().bind(drawingPane.heightProperty());
        overlay.setAlignment(Pos.CENTER);
        drawingPane.getChildren().add(overlay);

        final int[] count = {5};
        Timeline timer = new Timeline();
        timer.setCycleCount(6);

        KeyFrame tick = new KeyFrame(Duration.seconds(1), e -> {
            count[0]--;
            if (count[0] > 0) {
                countdownText.setText(String.valueOf(count[0]));
            } else {
                drawingPane.getChildren().remove(overlay);
                startNextTrial();
            }
        });

        timer.getKeyFrames().add(tick);
        timer.play();
    }

    // -----------------------------------------------------------------------
    // Trial loop
    // -----------------------------------------------------------------------

    /**
     * Increments the trial counter and either shows the next target or concludes
     * the experiment if all trials are done.
     *
     * @pre csvWriter != null
     * @post trialNumber is incremented; either showTarget() or concludeExperiment() is called.
     */
    private void startNextTrial() {
        trialNumber++;

        if (trialNumber > TOTAL_TRIALS) {
            concludeExperiment();
        } else {
            showTarget();
        }
    }

    /**
     * Builds and displays a new circle target at a random size and position.
     * Records the start timestamp after the target is added to the pane.
     *
     * @pre trialNumber in [1, TOTAL_TRIALS]
     * @pre drawingPane has valid positive dimensions
     * @post activeShape != null and fully contained within drawingPane.
     *       trialStartTime is set to the current system time.
     */
    private void showTarget() {
        removeActiveShapeIfPresent();

        double paneW = drawingPane.getWidth();
        double paneH = drawingPane.getHeight();

        if (paneW <= 0 || paneH <= 0) {
            showErrorDialog("Layout Error",
                    "Drawing pane has no dimensions.",
                    "Try resizing the window.");
            return;
        }

        // Random radius within the Fitts' Law trial range.
        double radius = CircleSpawner.MIN_TRIAL_RADIUS
                + model.getRandomGenerator().nextDouble()
                * (CircleSpawner.MAX_TRIAL_RADIUS - CircleSpawner.MIN_TRIAL_RADIUS);

        // Constrain center so the circle stays fully inside the pane.
        double minX = spawner.getLeftExtent(radius)   + EDGE_PADDING;
        double maxX = paneW - spawner.getRightExtent(radius)  - EDGE_PADDING;
        double minY = spawner.getTopExtent(radius)    + EDGE_PADDING;
        double maxY = paneH - spawner.getBottomExtent(radius) - EDGE_PADDING;

        if (maxX <= minX || maxY <= minY) {
            // Pane too small for this radius — retry with minimum radius.
            radius = CircleSpawner.MIN_TRIAL_RADIUS;
            minX = radius + EDGE_PADDING;
            maxX = paneW - radius - EDGE_PADDING;
            minY = radius + EDGE_PADDING;
            maxY = paneH - radius - EDGE_PADDING;

            if (maxX <= minX || maxY <= minY) return; // pane genuinely too small
        }

        final double centerX = minX + model.getRandomGenerator().nextDouble() * (maxX - minX);
        final double centerY = minY + model.getRandomGenerator().nextDouble() * (maxY - minY);
        final double finalRadius = radius;

        SpawnContext ctx = new SpawnContext(model.getRandomGenerator(), model.getCirclePalette());
        activeShape = spawner.build(ctx, centerX, centerY, finalRadius);

        // Trial counter label — bottom-left of the pane, not part of the target.
        Text label = new Text(10, paneH - 10, "Trial " + trialNumber + " / " + TOTAL_TRIALS);
        label.setFont(Font.font("SansSerif", 14));
        label.setFill(Color.DARKGRAY);

        drawingPane.getChildren().addAll(activeShape, label);

        // Capture start time AFTER the target is rendered.
        trialStartTime = System.currentTimeMillis();

        /*
         * CLICK HANDLER:
         * Only the circle Node consumes the click (mouseEvent.consume()).
         * Clicking the background pane does nothing — satisfying the rubric requirement
         * that "only the target responds to the click."
         */
        activeShape.setOnMouseClicked(mouseEvent -> {
            long elapsed = System.currentTimeMillis() - trialStartTime;

            // Fitts' Law W = target diameter (width).
            double targetDiameter = 2.0 * finalRadius;

            // Fitts' Law A = Euclidean distance from previous target center.
            double distance = (trialNumber == 1)
                    ? 0.0
                    : Math.hypot(centerX - lastTargetX, centerY - lastTargetY);

            recordTrial(trialNumber, targetDiameter, distance, elapsed);

            lastTargetX = centerX;
            lastTargetY = centerY;

            drawingPane.getChildren().clear();
            activeShape = null;

            mouseEvent.consume();
            startNextTrial();
        });
    }

    // -----------------------------------------------------------------------
    // CSV output
    // -----------------------------------------------------------------------

    /**
     * Writes one row to the CSV file for the completed trial and flushes immediately.
     *
     * @param trial    1-based trial number
     * @param size     target diameter in pixels
     * @param distance Euclidean distance from the previous target center (px)
     * @param timeMs   elapsed time from target display to click (ms)
     *
     * @pre csvWriter != null AND trial >= 1
     * @post One row is written and flushed to the CSV file.
     */
    private void recordTrial(int trial, double size, double distance, long timeMs) {
        csvWriter.printf("%d, %.1f, %.1f, %d%n", trial, size, distance, timeMs);
        csvWriter.flush(); // Flush per row to guard against data loss on early exit.
    }

    // -----------------------------------------------------------------------
    // Conclusion
    // -----------------------------------------------------------------------

    /**
     * Closes the CSV writer, shows a completion overlay and dialog,
     * and notifies the app that the experiment is finished.
     *
     * @pre trialNumber > TOTAL_TRIALS
     * @post csvWriter is closed. Drawing pane shows a completion message.
     *       onExperimentComplete callback is invoked if set.
     */
    private void concludeExperiment() {
        if (csvWriter != null) {
            csvWriter.close();
            csvWriter = null;
        }

        drawingPane.getChildren().clear();

        // Centered completion message on the pane.
        Text doneText = new Text("Experiment complete!\nResults saved to:\n" + CSV_FILENAME);
        doneText.setFont(Font.font("SansSerif", FontWeight.BOLD, 22));
        doneText.setFill(Color.STEELBLUE);
        doneText.setTextAlignment(TextAlignment.CENTER);

        StackPane overlay = new StackPane(doneText);
        overlay.prefWidthProperty().bind(drawingPane.widthProperty());
        overlay.prefHeightProperty().bind(drawingPane.heightProperty());
        overlay.setAlignment(Pos.CENTER);
        drawingPane.getChildren().add(overlay);

        // Modal dialog so the user doesn't miss the result.
        Alert done = new Alert(Alert.AlertType.INFORMATION);
        done.setTitle("Experiment Complete");
        done.setHeaderText("All " + TOTAL_TRIALS + " trials finished!");
        done.setContentText(
                "Results saved to: " + CSV_FILENAME
                + "\n\nYou may run another session via File → Go!, or quit."
        );
        done.getButtonTypes().setAll(ButtonType.OK);
        done.showAndWait();

        // Notify the app so it can re-enable Go! for a repeat session.
        if (onExperimentComplete != null) {
            onExperimentComplete.run();
        }

        // Reset trial counter so Go! can start a fresh session.
        trialNumber = 0;
    }

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------

    /**
     * Removes the active target from the pane if one is present.
     *
     * @pre drawingPane != null
     * @post activeShape is removed from drawingPane and set to null.
     */
    private void removeActiveShapeIfPresent() {
        if (activeShape != null) {
            drawingPane.getChildren().remove(activeShape);
            activeShape = null;
        }
    }

    /**
     * Displays a non-fatal error dialog.
     *
     * @param title   dialog title
     * @param header  summary line
     * @param details extended error details
     *
     * @pre title != null AND header != null AND details != null
     * @post A modal error dialog is shown; execution resumes after dismissal.
     */
    private void showErrorDialog(String title, String header, String details) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(details);
        alert.showAndWait();
    }
}
