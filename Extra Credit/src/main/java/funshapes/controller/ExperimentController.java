package funshapes.controller;

import funshapes.model.AppModel;
import funshapes.shapes.CircleSpawner;
import funshapes.shapes.SpawnContext;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
 *   <li>5-to-0 countdown before the first trial</li>
 *   <li>Spawning circle targets at random size and position per trial</li>
 *   <li>Measuring time from target display to click</li>
 *   <li>Writing trial data to CSV</li>
 *   <li>Exposing a {@code statusProperty} so the UI can reflect live progress</li>
 *   <li>Notifying the app on completion so Go! can be re-enabled</li>
 * </ul>
 * </p>
 *
 * <b>Correspondences:</b><br>
 *   drawingPane    = Pane on which targets are rendered<br>
 *   spawner        = CircleSpawner strategy used to build targets<br>
 *   trialNumber    = 1-based current trial index (0 = not started)<br>
 *   lastTargetX/Y  = previous target center for distance calculation<br>
 *   trialStartTime = ms timestamp when current target appeared<br>
 *   csvWriter      = open writer to the output CSV file
 *
 * <b>Invariants:</b><br>
 *   TOTAL_TRIALS >= 50<br>
 *   trialNumber in [0, TOTAL_TRIALS] during a session<br>
 *   activeShape == null OR fully contained within drawingPane
 *
 * @author Kylie Gilbert
 * @version HW-4 – CPSC 4140 – Spring 2026
 */
public class ExperimentController {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** Number of trials per session. Must be >= 50. */
    public static final int TOTAL_TRIALS = 50;

    /** Padding (px) so targets never clip the pane edge. */
    private static final double EDGE_PADDING = 8.0;

    /** CSV output file path (relative to working directory). */
    private static final String CSV_FILENAME = "fitts_results.csv";

    // -----------------------------------------------------------------------
    // Dependencies
    // -----------------------------------------------------------------------

    private final AppModel      model;
    private final Pane          drawingPane;
    private final CircleSpawner spawner = new CircleSpawner();

    // -----------------------------------------------------------------------
    // Observable state (bound by the status panel)
    // -----------------------------------------------------------------------

    /**
     * Human-readable status string, e.g. "Trial 3 / 50".
     * The right-side status panel binds to this property.
     */
    private final StringProperty statusProperty = new SimpleStringProperty("Ready — press Go! to begin.");

    // -----------------------------------------------------------------------
    // Internal experiment state
    // -----------------------------------------------------------------------

    /** Currently displayed target Node. Null when no target is on screen. */
    private Node activeShape;

    /** 1-based trial index. Zero means experiment has not started. */
    private int trialNumber = 0;

    /** Center X of the previous trial's target. */
    private double lastTargetX = 0.0;

    /** Center Y of the previous trial's target. */
    private double lastTargetY = 0.0;

    /** System.currentTimeMillis() snapshot when the current target appeared. */
    private long trialStartTime = 0L;

    /** Open writer for CSV output. Non-null only while the experiment runs. */
    private PrintWriter csvWriter;

    /** Invoked when the session finishes so the app can re-enable Go!. */
    private Runnable onExperimentComplete;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * @pre model != null AND drawingPane != null
     * @post Controller is bound to the model and pane; no experiment running.
     */
    public ExperimentController(AppModel model, Pane drawingPane) {
        this.model       = model;
        this.drawingPane = drawingPane;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Returns the observable status string for UI binding.
     *
     * @pre none
     * @post Returns non-null property.
     */
    public StringProperty statusProperty() { return statusProperty; }

    /**
     * Registers a callback invoked when all trials complete.
     *
     * @pre none
     * @post onExperimentComplete is set.
     */
    public void setOnExperimentComplete(Runnable callback) {
        this.onExperimentComplete = callback;
    }

    /**
     * Starts the experiment: opens the CSV and begins the 5-to-0 countdown.
     * No-op if already in progress.
     *
     * @pre drawingPane has valid dimensions when the countdown ends
     * @post csvWriter is open; countdown is running.
     */
    public void beginExperiment() {
        if (trialNumber > 0) return;

        try {
            csvWriter = new PrintWriter(new FileWriter(CSV_FILENAME));
            csvWriter.println("Trial Number,Target Size (pixels),Distance to Target (pixels),Time to Click (ms)");
        } catch (IOException e) {
            showErrorDialog("File Error", "Could not create: " + CSV_FILENAME, e.getMessage());
            return;
        }

        drawingPane.getChildren().clear();
        statusProperty.set("Get ready…");
        showCountdown();
    }

    /**
     * Closes the CSV writer if the session is interrupted (e.g. window closed).
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
     * Displays a 5-to-0 countdown, then calls startNextTrial().
     *
     * @pre drawingPane != null
     * @post A Timeline animates the countdown; first trial begins on completion.
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
        timer.getKeyFrames().add(new KeyFrame(Duration.seconds(1), e -> {
            count[0]--;
            if (count[0] > 0) {
                countdownText.setText(String.valueOf(count[0]));
                statusProperty.set("Starting in " + count[0] + "…");
            } else {
                drawingPane.getChildren().remove(overlay);
                startNextTrial();
            }
        }));
        timer.play();
    }

    // -----------------------------------------------------------------------
    // Trial loop
    // -----------------------------------------------------------------------

    /**
     * Increments trial counter and shows the next target or concludes.
     *
     * @pre csvWriter != null
     * @post trialNumber incremented; showTarget() or concludeExperiment() called.
     */
    private void startNextTrial() {
        trialNumber++;
        if (trialNumber > TOTAL_TRIALS) {
            concludeExperiment();
        } else {
            statusProperty.set("Trial " + trialNumber + " / " + TOTAL_TRIALS);
            showTarget();
        }
    }

    /**
     * Builds and displays a circle target at a random size and position.
     * Captures start time after the target is added to the pane.
     *
     * @pre trialNumber in [1, TOTAL_TRIALS]
     * @pre drawingPane has valid positive dimensions
     * @post activeShape != null and fully contained within drawingPane.
     *       trialStartTime reflects the time the target appeared.
     */
    private void showTarget() {
        removeActiveShapeIfPresent();

        double paneW = drawingPane.getWidth();
        double paneH = drawingPane.getHeight();
        if (paneW <= 0 || paneH <= 0) {
            showErrorDialog("Layout Error", "Drawing pane has no dimensions.", "Try resizing the window.");
            return;
        }

        // Random radius within Fitts' Law trial range.
        double radius = CircleSpawner.MIN_TRIAL_RADIUS
                + model.getRandomGenerator().nextDouble()
                * (CircleSpawner.MAX_TRIAL_RADIUS - CircleSpawner.MIN_TRIAL_RADIUS);

        // Constrain center so circle stays fully inside pane.
        double minX = radius + EDGE_PADDING;
        double maxX = paneW - radius - EDGE_PADDING;
        double minY = radius + EDGE_PADDING;
        double maxY = paneH - radius - EDGE_PADDING;

        // Fallback if pane is too small for chosen radius.
        if (maxX <= minX || maxY <= minY) {
            radius = CircleSpawner.MIN_TRIAL_RADIUS;
            minX = radius + EDGE_PADDING; maxX = paneW - radius - EDGE_PADDING;
            minY = radius + EDGE_PADDING; maxY = paneH - radius - EDGE_PADDING;
            if (maxX <= minX || maxY <= minY) return;
        }

        final double centerX   = minX + model.getRandomGenerator().nextDouble() * (maxX - minX);
        final double centerY   = minY + model.getRandomGenerator().nextDouble() * (maxY - minY);
        final double finalRadius = radius;

        SpawnContext ctx = new SpawnContext(model.getRandomGenerator(), model.getCirclePalette());
        activeShape = spawner.build(ctx, centerX, centerY, finalRadius);

        drawingPane.getChildren().add(activeShape);

        // Capture start time AFTER target is rendered.
        trialStartTime = System.currentTimeMillis();

        /*
         * CLICK HANDLER:
         * Only the circle Node consumes the event (mouseEvent.consume()).
         * Clicking the background pane does nothing — only the target responds.
         */
        activeShape.setOnMouseClicked(mouseEvent -> {
            long elapsed = System.currentTimeMillis() - trialStartTime;

            double targetDiameter = 2.0 * finalRadius;
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
     * Writes one CSV row and flushes immediately.
     *
     * @pre csvWriter != null AND trial >= 1
     * @post Row written and flushed to CSV file.
     */
    private void recordTrial(int trial, double size, double distance, long timeMs) {
        csvWriter.printf("%d, %.1f, %.1f, %d%n", trial, size, distance, timeMs);
        csvWriter.flush();
    }

    // -----------------------------------------------------------------------
    // Conclusion
    // -----------------------------------------------------------------------

    /**
     * Closes the CSV, shows completion UI and dialog, resets state.
     *
     * @pre trialNumber > TOTAL_TRIALS
     * @post csvWriter closed; completion shown; trialNumber reset to 0; callback fired.
     */
    private void concludeExperiment() {
        if (csvWriter != null) { csvWriter.close(); csvWriter = null; }

        drawingPane.getChildren().clear();
        statusProperty.set("Complete! Results saved to: " + CSV_FILENAME);

        Text doneText = new Text("Experiment complete!\nResults saved to:\n" + CSV_FILENAME);
        doneText.setFont(Font.font("SansSerif", FontWeight.BOLD, 22));
        doneText.setFill(Color.STEELBLUE);
        doneText.setTextAlignment(TextAlignment.CENTER);

        StackPane overlay = new StackPane(doneText);
        overlay.prefWidthProperty().bind(drawingPane.widthProperty());
        overlay.prefHeightProperty().bind(drawingPane.heightProperty());
        overlay.setAlignment(Pos.CENTER);
        drawingPane.getChildren().add(overlay);

        Alert done = new Alert(Alert.AlertType.INFORMATION);
        done.setTitle("Experiment Complete");
        done.setHeaderText("All " + TOTAL_TRIALS + " trials finished!");
        done.setContentText("Results saved to: " + CSV_FILENAME
                + "\n\nYou may run another session via Go!, or quit.");
        done.getButtonTypes().setAll(ButtonType.OK);
        done.showAndWait();

        trialNumber = 0; // allow repeat session
        if (onExperimentComplete != null) onExperimentComplete.run();
    }

    // -----------------------------------------------------------------------
    // Utilities
    // -----------------------------------------------------------------------

    /**
     * Removes the active target from the pane if present.
     *
     * @pre drawingPane != null
     * @post activeShape removed from pane and set to null.
     */
    private void removeActiveShapeIfPresent() {
        if (activeShape != null) {
            drawingPane.getChildren().remove(activeShape);
            activeShape = null;
        }
    }

    /**
     * Displays a non-fatal error dialog without crashing.
     *
     * @pre title != null AND header != null AND details != null
     * @post Modal error dialog shown; execution resumes after dismissal.
     */
    private void showErrorDialog(String title, String header, String details) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(details);
        alert.showAndWait();
    }
}
