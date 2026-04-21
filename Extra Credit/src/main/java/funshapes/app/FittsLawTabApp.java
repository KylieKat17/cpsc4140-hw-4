package funshapes.app;

import funshapes.controller.ExperimentController;
import funshapes.controller.KidsExperimentController;
import funshapes.model.AppModel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * FittsLawTabApp is the Extra Credit entry point, extending the Fitts' Law simulator
 * with a TabPane that contains two tabs:
 *
 * <ol>
 *   <li><b>Experiment</b> — the standard Fitts' Law trial (circles, clinical UI)</li>
 *   <li><b>Kids' Game ⭐</b> — a fun star-clicking game for young children</li>
 * </ol>
 *
 * <p>
 * Both tabs share the same dare-edition-style HBox top bar pattern (Go!/Quit! buttons,
 * no menu bar, window decorations left at JavaFX defaults). Each tab has its own
 * drawing pane, controller, and CSV output file so sessions are fully independent.
 * </p>
 *
 * <p>
 * The Kids tab uses a colorful background, larger fonts, star-shaped targets with
 * emoji labels, a pop-in bounce animation on each spawn, and celebratory language
 * throughout, making it engaging and readable for young children.
 * </p>
 *
 * <b>Correspondences:</b><br>
 *   expController  = ExperimentController (standard tab)<br>
 *   kidsController = KidsExperimentController (kids tab)<br>
 *   goButton       = active Go! button for whichever tab is selected
 *
 * @author Kylie Gilbert
 * @version HW-4 Extra Credit – CPSC 4140 – Spring 2026
 */
public class FittsLawTabApp extends Application {

    // Controllers for each tab.
    private ExperimentController     expController;
    private KidsExperimentController kidsController;

    // The Go! button lives in the shared top bar and wires to the active tab.
    private Button goButton;

    // Track which tab is currently selected.
    private boolean kidsTabActive = false;

    // -----------------------------------------------------------------------
    // JavaFX lifecycle
    // -----------------------------------------------------------------------

    /**
     * Builds the full tabbed UI.
     *
     * @param primaryStage the primary application stage
     *
     * @pre primaryStage != null
     * @post Window is visible; both tabs are ready; controllers are wired.
     */
    @Override
    public void start(Stage primaryStage) {

        // ----- Models (separate so RNG/state are independent) -----
        AppModel expModel  = new AppModel();
        AppModel kidsModel = new AppModel();

        // ----- Drawing panes (one per tab) -----
        Pane expPane  = new Pane();
        expPane.setStyle("-fx-background-color: #f0f0f0;");

        Pane kidsPane = new Pane();
        kidsPane.setStyle("-fx-background-color: #FFF9C4;"); // warm yellow for kids

        // ----- Controllers -----
        expController  = new ExperimentController(expModel, expPane);
        kidsController = new KidsExperimentController(kidsModel, kidsPane);

        // Re-enable Go! when each session completes.
        expController.setOnExperimentComplete(()  -> goButton.setDisable(false));
        kidsController.setOnExperimentComplete(() -> goButton.setDisable(false));

        // ----- Status labels (bound to each controller's observable) -----
        Label expStatus  = new Label();
        expStatus.setWrapText(true);
        expStatus.textProperty().bind(expController.statusProperty());

        Label kidsStatus = new Label();
        kidsStatus.setWrapText(true);
        kidsStatus.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));
        kidsStatus.setTextFill(Color.web("#E84393"));
        kidsStatus.textProperty().bind(kidsController.statusProperty());

        // ----- Experiment tab content -----
        Tab expTab = new Tab("Experiment");
        expTab.setClosable(false);
        expTab.setContent(buildExpTabContent(expPane, expStatus));

        // ----- Kids tab content -----
        Tab kidsTab = new Tab("Kids' Game ⭐");
        kidsTab.setClosable(false);
        kidsTab.setContent(buildKidsTabContent(kidsPane, kidsStatus));

        // ----- TabPane -----
        TabPane tabPane = new TabPane(expTab, kidsTab);
        tabPane.setTabMinWidth(130);

        // Track which tab is selected so Go! routes to the right controller.
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            kidsTabActive = (newTab == kidsTab);
            // Re-enable Go! on tab switch in case the other tab is mid-session
            // (the current tab's session is still running — we just reset the button
            // for the newly visible tab if it's idle).
            goButton.setDisable(false);
        });

        // ----- Root layout -----
        BorderPane root = new BorderPane();
        root.setTop(buildTopBar(primaryStage));
        root.setCenter(tabPane);

        // ----- Scene -----
        Scene scene = new Scene(root, 1000, 720);
        primaryStage.setTitle("HW-4 Extra Credit: Fitts' Law Simulator – Kylie Gilbert");
        primaryStage.setScene(scene);

        // Flush both CSV writers if user closes mid-session.
        primaryStage.setOnCloseRequest(e -> {
            expController.forceClose();
            kidsController.forceClose();
        });

        primaryStage.show();
    }

    // -----------------------------------------------------------------------
    // Tab content builders
    // -----------------------------------------------------------------------

    /**
     * Builds the content node for the standard Experiment tab.
     * Layout: drawing pane (center) + right status panel.
     *
     * @param drawingPane the pane controlled by expController
     * @param statusLabel bound to expController.statusProperty()
     *
     * @pre drawingPane != null AND statusLabel != null
     * @post Returns a BorderPane with drawing area and status panel.
     */
    private BorderPane buildExpTabContent(Pane drawingPane, Label statusLabel) {
        // Right status panel.
        VBox statusPanel = new VBox(10,
                bold("Experiment Status", 13),
                statusLabel,
                new Separator(),
                hint("• Click each circle as fast as you can."),
                hint("• Only the target circle responds to clicks."),
                hint("• Results → fitts_results.csv"),
                hint("Trials: " + ExperimentController.TOTAL_TRIALS)
        );
        statusPanel.setPadding(new Insets(12));
        statusPanel.setMinWidth(230);
        statusPanel.setPrefWidth(250);
        statusPanel.setStyle(
                "-fx-border-color: #bbbbbb; -fx-border-width: 0 0 0 1;" +
                "-fx-background-color: #fafafa;"
        );
        statusPanel.setAlignment(Pos.TOP_LEFT);

        BorderPane content = new BorderPane();
        content.setCenter(drawingPane);
        content.setRight(statusPanel);
        return content;
    }

    /**
     * Builds the content node for the Kids' Game tab.
     * Uses a warm yellow background, large fonts, and kid-friendly labels.
     *
     * @param drawingPane the pane controlled by kidsController
     * @param statusLabel bound to kidsController.statusProperty()
     *
     * @pre drawingPane != null AND statusLabel != null
     * @post Returns a BorderPane with colorful drawing area and fun status panel.
     */
    private BorderPane buildKidsTabContent(Pane drawingPane, Label statusLabel) {
        // Fun right panel for kids.
        Label heading = new Label("⭐ Click the Stars! ⭐");
        heading.setFont(Font.font("SansSerif", FontWeight.BOLD, 16));
        heading.setTextFill(Color.web("#E84393"));

        Label h1 = new Label("🎯 Click each star!");
        Label h2 = new Label("⚡ Be as fast as you can!");
        Label h3 = new Label("📊 Results → fitts_results_kids.csv");
        Label h4 = new Label("⭐ Stars: " + KidsExperimentController.TOTAL_TRIALS);

        for (Label l : new Label[]{h1, h2, h3, h4}) {
            l.setFont(Font.font("SansSerif", 13));
            l.setWrapText(true);
        }

        VBox statusPanel = new VBox(12, heading, statusLabel, new Separator(), h1, h2, h3, h4);
        statusPanel.setPadding(new Insets(14));
        statusPanel.setMinWidth(230);
        statusPanel.setPrefWidth(260);
        statusPanel.setStyle(
                "-fx-border-color: #FFD93D; -fx-border-width: 0 0 0 4;" +
                "-fx-background-color: #FFFDE7;"
        );
        statusPanel.setAlignment(Pos.TOP_LEFT);

        BorderPane content = new BorderPane();
        content.setCenter(drawingPane);
        content.setRight(statusPanel);
        return content;
    }

    // -----------------------------------------------------------------------
    // Shared top bar (same dare edition HBox pattern)
    // -----------------------------------------------------------------------

    /**
     * Builds the shared top HBox bar with title label, Go!, and Quit! buttons.
     * Mirrors the dare edition's top bar exactly — no menu bar.
     *
     * @param stage the primary stage (used by Quit!)
     *
     * @pre stage != null
     * @post Returns styled HBox with title, spacer, Go!, Quit!.
     *       goButton field is set.
     */
    private HBox buildTopBar(Stage stage) {
        Label titleLabel = new Label("Fitts' Law Simulator");
        titleLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 15));

        goButton = new Button("Go!");
        goButton.setStyle("-fx-font-weight: bold;");
        goButton.setOnAction(e -> triggerGo());

        Button quitButton = new Button("Quit!");
        quitButton.setOnAction(e -> {
            System.out.println("Exiting: Quit! button pressed.");
            expController.forceClose();
            kidsController.forceClose();
            Platform.exit();
        });

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

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------

    /**
     * Disables Go! and starts the experiment for whichever tab is currently active.
     *
     * @pre goButton is not disabled
     * @post goButton disabled; appropriate controller.beginExperiment() called next pulse.
     */
    private void triggerGo() {
        goButton.setDisable(true);
        if (kidsTabActive) {
            Platform.runLater(kidsController::beginExperiment);
        } else {
            Platform.runLater(expController::beginExperiment);
        }
    }

    // -----------------------------------------------------------------------
    // UI helpers
    // -----------------------------------------------------------------------

    /**
     * Creates a bold label with the given text and font size.
     *
     * @pre text != null AND size > 0
     * @post Returns a Label with bold SansSerif font applied.
     */
    private Label bold(String text, double size) {
        Label l = new Label(text);
        l.setFont(Font.font("SansSerif", FontWeight.BOLD, size));
        return l;
    }

    /**
     * Creates a small gray hint label with word wrap.
     *
     * @pre text != null
     * @post Returns a Label styled as a muted hint.
     */
    private Label hint(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setStyle("-fx-text-fill: #555555;");
        return l;
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
