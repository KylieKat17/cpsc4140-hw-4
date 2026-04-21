package funshapes.shapes;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * KidsTargetSpawner builds a cheerful star target for the kids' game tab.
 *
 * <p>
 * Each target is a 5-pointed star with a large emoji face in the center,
 * drawn entirely from JavaFX primitives. Colors are chosen from a bright,
 * child-friendly palette. The star has a thick contrasting stroke so it
 * stands out against any background color.
 * </p>
 *
 * <b>Invariants:</b><br>
 *   MIN_TRIAL_RADIUS > 0<br>
 *   MAX_TRIAL_RADIUS > MIN_TRIAL_RADIUS<br>
 *   EMOJIS is non-empty
 *
 * @author Kylie Gilbert
 * @version HW-4 Extra Credit – CPSC 4140 – Spring 2026
 */
public class KidsTargetSpawner implements ShapeSpawner {

    /** Minimum target outer radius (pixels) for kids' trials. */
    public static final double MIN_TRIAL_RADIUS = 30.0;

    /** Maximum target outer radius (pixels) for kids' trials. */
    public static final double MAX_TRIAL_RADIUS = 90.0;

    /** Fun emoji characters displayed inside each star target. */
    private static final String[] EMOJIS = {"⭐", "🌟", "💫", "🎯", "🌈", "🦄", "🍭", "🎪"};

    /** Bright, child-friendly fill colors. */
    private static final Color[] KIDS_PALETTE = {
            Color.web("#FF6B6B"), // coral red
            Color.web("#FFD93D"), // sunny yellow
            Color.web("#6BCB77"), // grass green
            Color.web("#4D96FF"), // sky blue
            Color.web("#FF9FF3"), // bubblegum pink
            Color.web("#FFA94D"), // orange
            Color.web("#A29BFE"), // lavender
            Color.web("#55EFC4")  // mint
    };

    @Override public String getDisplayName() { return "Star"; }

    /**
     * Fallback radius — not used during experiment trials.
     *
     * @pre paneWidth > 0 AND paneHeight > 0
     * @post result > 0
     */
    @Override
    public double computeRadius(double paneWidth, double paneHeight) {
        return Math.min((paneWidth + paneHeight) / 14.0,
                        Math.min(paneWidth, paneHeight) / 3.0);
    }

    /*
     * Extent methods: a star's bounding box is approximately the outer radius
     * in every direction (the points don't exceed outerRadius from center).
     */
    @Override public double getTopExtent(double radius)    { return radius; }
    @Override public double getBottomExtent(double radius) { return radius; }
    @Override public double getLeftExtent(double radius)   { return radius; }
    @Override public double getRightExtent(double radius)  { return radius; }

    /**
     * Builds a 5-pointed star Group with an emoji label centered inside it.
     *
     * @param context  shared RNG and palette (RNG used for color/emoji selection)
     * @param cx       center X of the star
     * @param cy       center Y of the star
     * @param radius   outer radius of the star
     *
     * @pre context != null AND radius > 0
     * @post Returns a Group containing the star polygon and emoji text; never null.
     */
    @Override
    public Node build(SpawnContext context, double cx, double cy, double radius) {
        Color fill = KIDS_PALETTE[context.getRandomGenerator().nextInt(KIDS_PALETTE.length)];
        String emoji = EMOJIS[context.getRandomGenerator().nextInt(EMOJIS.length)];

        Polygon star = buildStar(cx, cy, radius, radius * 0.42);
        star.setFill(fill);
        star.setStroke(Color.WHITE);
        star.setStrokeWidth(Math.max(3, radius * 0.08));

        // Emoji label centered on the star.
        Text label = new Text(emoji);
        label.setFont(Font.font("SansSerif", FontWeight.BOLD, radius * 0.55));
        // Offset so the emoji visually centers on (cx, cy).
        label.setX(cx - radius * 0.28);
        label.setY(cy + radius * 0.20);

        return new Group(star, label);
    }

    // -----------------------------------------------------------------------
    // Star geometry helper
    // -----------------------------------------------------------------------

    /**
     * Computes the points of a 5-pointed star polygon.
     *
     * @param cx          center X
     * @param cy          center Y
     * @param outerRadius distance from center to outer points
     * @param innerRadius distance from center to inner notch points
     *
     * @pre outerRadius > innerRadius > 0
     * @post Returns a Polygon with 10 vertices alternating outer and inner radii.
     */
    private Polygon buildStar(double cx, double cy, double outerRadius, double innerRadius) {
        Polygon star = new Polygon();
        int points = 5;
        // Stars start pointing up, so offset by -90°.
        double startAngle = -Math.PI / 2.0;
        double angleStep  = Math.PI / points; // alternates outer/inner every half-step

        for (int i = 0; i < points * 2; i++) {
            double angle = startAngle + i * angleStep;
            double r     = (i % 2 == 0) ? outerRadius : innerRadius;
            star.getPoints().add(cx + r * Math.cos(angle));
            star.getPoints().add(cy + r * Math.sin(angle));
        }

        return star;
    }
}
