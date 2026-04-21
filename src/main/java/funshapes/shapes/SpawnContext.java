package funshapes.shapes;

import javafx.scene.paint.Color;

import java.util.Random;

/**
 * SpawnContext bundles the shared resources that spawners need to build shapes:
 * a random generator and the application color palette.
 *
 * <b>Correspondences:</b><br>
 *   randomGenerator = source of randomness passed from AppModel<br>
 *   circlePalette   = fixed set of colors available to CircleSpawner
 *
 * @pre All constructor arguments are non-null.
 * @post Immutable container; fields never change after construction.
 */
public class SpawnContext {

    private final Random randomGenerator;
    private final Color[] circlePalette;

    /**
     * Constructs a SpawnContext with the given RNG and palette.
     *
     * @param randomGenerator source of randomness
     * @param circlePalette   palette of colors for circle targets
     *
     * @pre randomGenerator != null AND circlePalette != null
     * @post this.randomGenerator == randomGenerator AND this.circlePalette == circlePalette
     */
    public SpawnContext(Random randomGenerator, Color[] circlePalette) {
        this.randomGenerator = randomGenerator;
        this.circlePalette   = circlePalette;
    }

    /**
     * @pre none
     * @post Returns the shared Random instance; never null.
     */
    public Random getRandomGenerator() {
        return randomGenerator;
    }

    /**
     * @pre none
     * @post Returns the circle color palette array; never null.
     */
    public Color[] getCirclePalette() {
        return circlePalette;
    }
}
