package funshapes.shapes;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * CircleSpawner creates a single filled circle for Fitts' Law trials.
 *
 * <p>
 * For the experiment, radius is supplied externally by the trial runner
 * (random per trial). computeRadius() is kept as a sane fallback but
 * the controller overrides it for each trial.
 * </p>
 *
 * <b>Invariants:</b><br>
 *   MIN_TRIAL_RADIUS > 0<br>
 *   MAX_TRIAL_RADIUS > MIN_TRIAL_RADIUS
 *
 * @author Kylie Gilbert
 * @version HW-4 – CPSC 4140 – Spring 2026
 */
public class CircleSpawner implements ShapeSpawner {

    /** Minimum target radius for a Fitts' Law trial (pixels). */
    public static final double MIN_TRIAL_RADIUS = 15.0;

    /** Maximum target radius for a Fitts' Law trial (pixels). */
    public static final double MAX_TRIAL_RADIUS = 80.0;

    @Override
    public String getDisplayName() {
        return "Circle";
    }

    /**
     * Fallback radius calculation based on window size.
     * The experiment trial runner ignores this and uses a random radius instead.
     *
     * @pre paneWidth > 0 AND paneHeight > 0
     * @post result is in (0, min(paneWidth, paneHeight)/2.5]
     */
    @Override
    public double computeRadius(double paneWidth, double paneHeight) {
        double avg = (paneWidth + paneHeight) / 2.0;
        double radius = avg / 8.0;
        double max = Math.min(paneWidth, paneHeight) / 2.5;
        return Math.min(radius, max);
    }

    @Override public double getTopExtent(double radius)    { return radius; }
    @Override public double getBottomExtent(double radius) { return radius; }
    @Override public double getLeftExtent(double radius)   { return radius; }
    @Override public double getRightExtent(double radius)  { return radius; }

    /**
     * Builds a circle node at the given position with a random palette color.
     *
     * @param context  contains RNG and circle color palette
     * @param centerX  circle center X
     * @param centerY  circle center Y
     * @param radius   circle radius
     *
     * @pre context != null AND radius > 0
     * @post Returns a Circle fully configured with fill and white stroke.
     */
    @Override
    public Node build(SpawnContext context, double centerX, double centerY, double radius) {
        Color[] palette = context.getCirclePalette();
        Color fill = palette[context.getRandomGenerator().nextInt(palette.length)];

        Circle circle = new Circle(centerX, centerY, radius, fill);
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(3);
        return circle;
    }
}
