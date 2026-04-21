package funshapes.shapes;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * CircleSpawner builds a single filled circle target for Fitts' Law trials.
 *
 * <p>Trial radius is supplied externally by the experiment runner per-trial.
 * {@code computeRadius()} is a sane fallback but is not used during experiments.</p>
 *
 * <b>Invariants:</b><br>
 *   MIN_TRIAL_RADIUS > 0<br>
 *   MAX_TRIAL_RADIUS > MIN_TRIAL_RADIUS
 *
 * @author Kylie Gilbert
 * @version HW-4 – CPSC 4140 – Spring 2026
 */
public class CircleSpawner implements ShapeSpawner {

    /** Minimum target radius (pixels) used during Fitts' Law trials. */
    public static final double MIN_TRIAL_RADIUS = 15.0;

    /** Maximum target radius (pixels) used during Fitts' Law trials. */
    public static final double MAX_TRIAL_RADIUS = 80.0;

    @Override public String getDisplayName() { return "Circle"; }

    /**
     * Fallback sizing (not used during experiments).
     *
     * @pre paneWidth > 0 AND paneHeight > 0
     * @post result in (0, min(paneWidth, paneHeight)/2.5]
     */
    @Override
    public double computeRadius(double paneWidth, double paneHeight) {
        double avg = (paneWidth + paneHeight) / 2.0;
        return Math.min(avg / 8.0, Math.min(paneWidth, paneHeight) / 2.5);
    }

    @Override public double getTopExtent(double r)    { return r; }
    @Override public double getBottomExtent(double r) { return r; }
    @Override public double getLeftExtent(double r)   { return r; }
    @Override public double getRightExtent(double r)  { return r; }

    /**
     * Builds a circle at the given position with a random palette color.
     *
     * @pre context != null AND radius > 0
     * @post Returns a Circle with fill and white stroke applied.
     */
    @Override
    public Node build(SpawnContext context, double cx, double cy, double radius) {
        Color[] palette = context.getCirclePalette();
        Color fill = palette[context.getRandomGenerator().nextInt(palette.length)];
        Circle circle = new Circle(cx, cy, radius, fill);
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(3);
        return circle;
    }
}
