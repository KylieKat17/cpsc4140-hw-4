package funshapes.controller;

import funshapes.model.AppModel;
import funshapes.shapes.KidsTargetSpawner;
import funshapes.shapes.SpawnContext;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
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
 * KidsExperimentController drives the Fitts' Law trial loop for the kids' game tab.
 *
 * <p>
 * Functionally identical to {@link ExperimentController} but uses
 * {@link KidsTargetSpawner} (star targets) and writes to a separate CSV file
 * ({@code fitts_results_kids.csv}).  Targets also get a brief pop-in scale
 * animation on spawn to keep the experience lively.
 * </p>
 *
 * <b>Invariants:</b><br>
 *   TOTAL_TRIALS >= 50<br>
 *   trialNumber in [0, TOTAL_TRIALS] during a session<br>
 *   activeShape == null OR fully inside drawingPane
 *
 * @author Kylie Gilbert
 * @version HW-4 Extra Credit – CPSC 4140 – Spring 2026
 */
public class KidsExperimentController {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** Number of trials per session. Must be >= 50. */
    public static final int TOTAL_TRIALS = 50;

    private static final double EDGE_PADDING  = 10.0;
    private static final String CSV_FILENAME  = "fitts_results_kids.csv";

    // -----------------------------------------------------------------------
    // Dependencies
    // -----------------------------------------------------------------------

    private final AppModel          model;
    private final Pane              drawingPane;
    private final KidsTargetSpawner spawner = new KidsTargetSpawner();

    // -----------------------------------------------------------------------
    // Observable state
    // -----------------------------------------------------------------------

    private final StringProperty statusProperty = new SimpleStringProperty("Ready — press Go! to begin.");

    // -----------------------------------------------------------------------
    // Internal experiment state
    // -----------------------------------------------------------------------

    private Node        activeShape;
    private int         trialNumber    = 0;
    private double      lastTargetX    = 0.0;
    private double      lastTargetY    = 0.0;
    private long        trialStartTime = 0L;
    private PrintWriter csvWriter;
    private Runnable    onExperimentComplete;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * @pre model != null AND drawingPane != null
     * @post Controller bound to model and pane; no experiment running.
     */
    public KidsExperimentController(AppModel model, Pane drawingPane) {
        this.model       = model;
        this.drawingPane = drawingPane;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** @pre none  @post Returns non-null observable status string. */
    public StringProperty statusProperty() { return statusProperty; }

    /** @pre none  @post Completion callback registered. */
    public void setOnExperimentComplete(Runnable callback) {
        this.onExperimentComplete = callback;
    }

    /**
     * Opens CSV and begins the countdown. No-op if already running.
     *
     * @pre drawingPane has valid dimensions by the time countdown ends
     * @post csvWriter is open; countdown animating.
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
     * Flushes and closes the CSV if interrupted mid-session.
     *
     * @pre none
     * @post csvWriter closed and null.
     */
    public void forceClose() {
        if (csvWriter != null) { csvWriter.close(); csvWriter = null; }
    }

    // -----------------------------------------------------------------------
    // Countdown
    // -----------------------------------------------------------------------

    /**
     * Animates a fun 5-to-0 countdown with large colorful text.
     *
     * @pre drawingPane != null
     * @post Timeline plays; first trial begins on completion.
     */
    private void showCountdown() {
        Text countdownText = new Text("5");
        countdownText.setFont(Font.font("SansSerif", FontWeight.BOLD, 140));
        countdownText.setFill(Color.web("#FF6B6B"));

        StackPane overlay = new StackPane(countdownText);
        overlay.prefWidthProperty().bind(drawingPane.widthProperty());
        overlay.prefHeightProperty().bind(drawingPane.heightProperty());
        overlay.setAlignment(Pos.CENTER);
        drawingPane.getChildren().add(overlay);

        // Cycling countdown colors to add visual interest for kids.
        Color[] countColors = {
            Color.web("#FF6B6B"), Color.web("#FFD93D"),
            Color.web("#6BCB77"), Color.web("#4D96FF"),
            Color.web("#FF9FF3")
        };

        final int[] count = {5};
        Timeline timer = new Timeline();
        timer.setCycleCount(6);
        timer.getKeyFrames().add(new KeyFrame(Duration.seconds(1), e -> {
            count[0]--;
            if (count[0] > 0) {
                countdownText.setText(String.valueOf(count[0]));
                countdownText.setFill(countColors[count[0] % countColors.length]);
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
     * Advances trial counter; shows next target or concludes.
     *
     * @pre csvWriter != null
     * @post trialNumber incremented; showTarget() or concludeExperiment() called.
     */
    private void startNextTrial() {
        trialNumber++;
        if (trialNumber > TOTAL_TRIALS) {
            concludeExperiment();
        } else {
            statusProperty.set("⭐ Trial " + trialNumber + " / " + TOTAL_TRIALS + " — click the star!");
            showTarget();
        }
    }

    /**
     * Builds and shows a star target at a random size and position.
     * A pop-in scale animation makes the spawn feel bouncy and fun.
     *
     * @pre trialNumber in [1, TOTAL_TRIALS]
     * @pre drawingPane has valid positive dimensions
     * @post activeShape != null and inside drawingPane; trialStartTime set after pop-in.
     */
    private void showTarget() {
        removeActiveShapeIfPresent();

        double paneW = drawingPane.getWidth();
        double paneH = drawingPane.getHeight();
        if (paneW <= 0 || paneH <= 0) {
            showErrorDialog("Layout Error", "Drawing pane has no dimensions.", "Try resizing the window.");
            return;
        }

        double radius = KidsTargetSpawner.MIN_TRIAL_RADIUS
                + model.getRandomGenerator().nextDouble()
                * (KidsTargetSpawner.MAX_TRIAL_RADIUS - KidsTargetSpawner.MIN_TRIAL_RADIUS);

        double minX = radius + EDGE_PADDING, maxX = paneW - radius - EDGE_PADDING;
        double minY = radius + EDGE_PADDING, maxY = paneH - radius - EDGE_PADDING;

        if (maxX <= minX || maxY <= minY) {
            radius = KidsTargetSpawner.MIN_TRIAL_RADIUS;
            minX = radius + EDGE_PADDING; maxX = paneW - radius - EDGE_PADDING;
            minY = radius + EDGE_PADDING; maxY = paneH - radius - EDGE_PADDING;
            if (maxX <= minX || maxY <= minY) return;
        }

        final double centerX    = minX + model.getRandomGenerator().nextDouble() * (maxX - minX);
        final double centerY    = minY + model.getRandomGenerator().nextDouble() * (maxY - minY);
        final double finalRadius = radius;

        SpawnContext ctx = new SpawnContext(model.getRandomGenerator(), model.getCirclePalette());
        activeShape = spawner.build(ctx, centerX, centerY, finalRadius);

        drawingPane.getChildren().add(activeShape);

        /*
         * Pop-in animation: scale from 0 → 1 over 180ms.
         * The star appears to "bounce" into existence, which is engaging for children
         * and also gives a clear visual cue that a new target has appeared.
         * We capture trialStartTime AFTER the animation completes so timing is fair.
         */
        activeShape.setScaleX(0);
        activeShape.setScaleY(0);

        ScaleTransition popIn = new ScaleTransition(Duration.millis(180), activeShape);
        popIn.setFromX(0); popIn.setFromY(0);
        popIn.setToX(1);   popIn.setToY(1);
        popIn.setOnFinished(e -> trialStartTime = System.currentTimeMillis());
        popIn.play();

        /*
         * CLICK HANDLER:
         * The star Group consumes the event so the pane background never fires.
         * Only the target responds to clicks.
         */
        activeShape.setOnMouseClicked(mouseEvent -> {
            // Ignore clicks during the pop-in animation (trialStartTime == 0).
            if (trialStartTime == 0) { mouseEvent.consume(); return; }

            long elapsed = System.currentTimeMillis() - trialStartTime;
            trialStartTime = 0;

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
     * Writes one CSV row and flushes.
     *
     * @pre csvWriter != null AND trial >= 1
     * @post Row written and flushed.
     */
    private void recordTrial(int trial, double size, double distance, long timeMs) {
        csvWriter.printf("%d, %.1f, %.1f, %d%n", trial, size, distance, timeMs);
        csvWriter.flush();
    }

    // -----------------------------------------------------------------------
    // Conclusion
    // -----------------------------------------------------------------------

    /**
     * Closes CSV, shows fun completion UI and dialog, resets state.
     *
     * @pre trialNumber > TOTAL_TRIALS
     * @post csvWriter closed; completion shown; trialNumber reset; callback fired.
     */
    private void concludeExperiment() {
        if (csvWriter != null) { csvWriter.close(); csvWriter = null; }

        drawingPane.getChildren().clear();
        statusProperty.set("🎉 All done! Results in: " + CSV_FILENAME);

        Text doneText = new Text("🎉 Great job! 🎉\nAll " + TOTAL_TRIALS + " stars clicked!\nResults saved to:\n" + CSV_FILENAME);
        doneText.setFont(Font.font("SansSerif", FontWeight.BOLD, 26));
        doneText.setFill(Color.web("#FF6B6B"));
        doneText.setTextAlignment(TextAlignment.CENTER);

        StackPane overlay = new StackPane(doneText);
        overlay.prefWidthProperty().bind(drawingPane.widthProperty());
        overlay.prefHeightProperty().bind(drawingPane.heightProperty());
        overlay.setAlignment(Pos.CENTER);
        drawingPane.getChildren().add(overlay);

        Alert done = new Alert(Alert.AlertType.INFORMATION);
        done.setTitle("🎉 You Did It!");
        done.setHeaderText("All " + TOTAL_TRIALS + " stars clicked!");
        done.setContentText("Results saved to: " + CSV_FILENAME
                + "\n\nPress Go! to play again!");
        done.getButtonTypes().setAll(ButtonType.OK);
        done.showAndWait();

        trialNumber = 0;
        if (onExperimentComplete != null) onExperimentComplete.run();
    }

    // -----------------------------------------------------------------------
    // Utilities
    // -----------------------------------------------------------------------

    /** @pre drawingPane != null  @post activeShape removed from pane. */
    private void removeActiveShapeIfPresent() {
        if (activeShape != null) {
            drawingPane.getChildren().remove(activeShape);
            activeShape = null;
        }
    }

    /** @pre all args non-null  @post Modal error dialog shown; execution continues. */
    private void showErrorDialog(String title, String header, String details) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title); alert.setHeaderText(header); alert.setContentText(details);
        alert.showAndWait();
    }
}
