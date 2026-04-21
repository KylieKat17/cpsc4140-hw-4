package funshapes.shapes;

import javafx.scene.Node;

/**
 * ShapeSpawner defines a strategy for producing a clickable JavaFX Node
 * that can be positioned safely within a Pane.
 *
 * <p>
 * Extent methods describe how far the shape extends from its anchor point
 * (centerX, centerY) in each direction. The controller uses these to keep
 * the shape fully visible regardless of window size.
 * </p>
 */
public interface ShapeSpawner {

    /**
     * @return Display name used in UI labels.
     * @post result != null
     */
    String getDisplayName();

    /**
     * Computes a "radius" size basis for the shape given the current pane dimensions.
     *
     * @param paneWidth  current pane width in pixels
     * @param paneHeight current pane height in pixels
     *
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
     * Builds the JavaFX Node for the shape.
     *
     * @param context  shared RNG and palette access
     * @param centerX  anchor X coordinate
     * @param centerY  anchor Y coordinate
     * @param radius   size basis
     *
     * @pre context != null AND radius > 0
     * @post result != null
     */
    Node build(SpawnContext context, double centerX, double centerY, double radius);
}
