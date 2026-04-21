package funshapes.shapes;

import javafx.scene.paint.Color;
import java.util.Random;

/**
 * SpawnContext bundles the shared resources spawners need to build shapes.
 *
 * @pre All constructor arguments are non-null.
 * @post Immutable after construction.
 */
public class SpawnContext {

    private final Random randomGenerator;
    private final Color[] circlePalette;

    /**
     * @pre randomGenerator != null AND circlePalette != null
     * @post Fields are set to the given arguments.
     */
    public SpawnContext(Random randomGenerator, Color[] circlePalette) {
        this.randomGenerator = randomGenerator;
        this.circlePalette   = circlePalette;
    }

    /** @pre none  @post Returns shared RNG; never null. */
    public Random getRandomGenerator() { return randomGenerator; }

    /** @pre none  @post Returns circle color palette; never null. */
    public Color[] getCirclePalette()  { return circlePalette; }
}
