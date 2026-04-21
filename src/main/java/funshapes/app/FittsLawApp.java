package funshapes.app;

import funshapes.controller.ExperimentController;
import funshapes.model.AppModel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * FittsLawApp is the JavaFX application entry point for the HW-4 Fitts' Law simulator.
 *
 * <p>
 * UI layout:
 * <ul>
 *   <li>Top: menu bar with File → Go! and Quit</li>
 *   <li>Center: drawing pane where targets appear</li>
 * </ul>
 * </p>
 *
 * <p>
 * Selecting Go! hands control to {@link ExperimentController}, which runs the
 * full 50-trial session, records data to a CSV, and re-enables Go! on completion.
 * All exits are routed through {@code requestProgramExit()} so the app never
 * terminates without closing the CSV writer first.
 * </p>
 *
 * <b>Correspondences:</b><br>
 *   model      = shared AppModel (RNG, palette)<br>
 *   controller = ExperimentController driving the trial loop<br>
 *   goMenuItem = disabled while an experiment is running
 *
 * @author Kylie Gilbert
 * @version HW-4 – CPSC 4140 – Spring 2026
 */
public class FittsLawApp extends Application {

    /** Shared application model. */
    private AppModel model;

    /** Drives the experiment: countdown, trial loop, CSV output. */
    private ExperimentController controller;

    /** Kept so it can be disabled during a session and re-enabled after. */
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
     * @post A window with a menu bar and blank drawing pane is visible.
     *       All window-close events are routed through requestProgramExit().
     */
    @Override
    public void start(Stage primaryStage) {

        // ----- Model -----
        model = new AppModel();

        // ----- Root layout -----
        BorderPane root = new BorderPane();

        // ----- Drawing pane -----
        Pane drawingPane = new Pane();
        drawingPane.setStyle("-fx-background-color: #f0f0f0;");
        root.setCenter(drawingPane);

        // ----- Controller -----
        controller = new ExperimentController(model, drawingPane);

        // Re-enable Go! when a session finishes so the user can run another.
        controller.setOnExperimentComplete(() -> goMenuItem.setDisable(false));

        // ----- Menu bar -----
        MenuBar menuBar = new MenuBar();
        menuBar.setUseSystemMenuBar(false);

        Menu fileMenu = new Menu("File");
        goMenuItem = new MenuItem("Go!");
        MenuItem quitMenuItem = new MenuItem("Quit");

        goMenuItem.setOnAction(event -> {
            goMenuItem.setDisable(true); // disable until session ends
            // Defer by one pulse so layout dimensions are finalized.
            Platform.runLater(controller::beginExperiment);
        });

        quitMenuItem.setOnAction(event ->
                requestProgramExit("Quit selected from File menu.")
        );

        fileMenu.getItems().addAll(goMenuItem, new SeparatorMenuItem(), quitMenuItem);
        menuBar.getMenus().add(fileMenu);
        root.setTop(menuBar);

        // ----- Scene / Stage -----
        Scene scene = new Scene(root, 900, 700);
        primaryStage.setTitle("HW-4: Fitts' Law Simulator – Kylie Gilbert");
        primaryStage.setScene(scene);

        // Intercept OS close button for controlled termination.
        primaryStage.setOnCloseRequest(this::handleWindowCloseRequest);
        primaryStage.show();
    }

    // -----------------------------------------------------------------------
    // Event handlers
    // -----------------------------------------------------------------------

    /**
     * Intercepts the OS window close button and routes termination through
     * the controlled exit method so the CSV writer is always closed first.
     *
     * @param event the close request event from the OS
     *
     * @pre event != null
     * @post Default close behavior is suppressed; requestProgramExit() is called.
     */
    private void handleWindowCloseRequest(WindowEvent event) {
        event.consume();
        requestProgramExit("Window close button (X) was used.");
    }

    // -----------------------------------------------------------------------
    // Exit
    // -----------------------------------------------------------------------

    /**
     * Centralized, controlled exit point for the application.
     * Always closes the CSV writer before terminating so data is not lost.
     *
     * @param reason brief description of the termination trigger (for logging)
     *
     * @pre reason != null
     * @post controller.forceClose() is called; Platform.exit() terminates the app.
     */
    private void requestProgramExit(String reason) {
        System.out.println("Exiting: " + reason);
        if (controller != null) {
            controller.forceClose(); // flush and close CSV if mid-experiment
        }
        Platform.exit();
    }

    // -----------------------------------------------------------------------
    // Entry point
    // -----------------------------------------------------------------------

    /**
     * Program entry point.
     *
     * @param args command-line arguments (unused)
     *
     * @pre none
     * @post JavaFX runtime is launched; start(Stage) is invoked by the framework.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
