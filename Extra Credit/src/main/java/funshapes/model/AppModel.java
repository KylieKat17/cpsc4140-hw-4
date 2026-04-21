package funshapes.model;

import javafx.scene.paint.Color;
import java.util.Random;

/**
 * AppModel holds shared application state: RNG and the target color palette.
 *
 * <b>Invariants:</b><br>
 *   randomGenerator != null<br>
 *   circlePalette.length > 0
 *
 * @author Kylie Gilbert
 * @version HW-4 – CPSC 4140 – Spring 2026
 */
public class AppModel {

    /** Shared RNG for the entire experiment session. */
    private final Random randomGenerator = new Random();

    /** Color palette for circle targets. */
    private final Color[] circlePalette = {
            Color.TOMATO,
            Color.DODGERBLUE,
            Color.MEDIUMSEAGREEN,
            Color.DARKORCHID,
            Color.DARKORANGE,
            Color.DEEPPINK
    };

    /** @pre none  @post Returns shared RNG; never null. */
    public Random getRandomGenerator() { return randomGenerator; }

    /** @pre none  @post Returns palette array; never null, length > 0. */
    public Color[] getCirclePalette()  { return circlePalette; }
}
