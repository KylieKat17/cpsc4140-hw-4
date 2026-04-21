package funshapes.shapes;

import javafx.scene.Node;

/**
 * ShapeSpawner defines a strategy for producing a JavaFX Node target
 * that can be safely positioned within a Pane.
 *
 * <p>Extent methods describe how far the shape reaches from its anchor
 * (centerX, centerY); the controller uses these to keep the shape on-screen.</p>
 */
public interface ShapeSpawner {

    /** @post result != null */
    String getDisplayName();

    /**
     * @pre paneWidth > 0 AND paneHeight > 0
     * @post result > 0
     */
    double computeRadius(double paneWidth, double paneHeight);

    /** @pre radius > 0  @post result >= 0 */
    double getTopExtent(double radius);
    /** @pre radius > 0  @post result >= 0 */
    double getBottomExtent(double radius);
    /** @pre radius > 0  @post result >= 0 */
    double getLeftExtent(double radius);
    /** @pre radius > 0  @post result >= 0 */
    double getRightExtent(double radius);

    /**
     * @pre context != null AND radius > 0
     * @post result != null
     */
    Node build(SpawnContext context, double centerX, double centerY, double radius);
}
