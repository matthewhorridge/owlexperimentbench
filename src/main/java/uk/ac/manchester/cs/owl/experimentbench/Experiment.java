package uk.ac.manchester.cs.owl.experimentbench;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Author: Matthew Horridge<br>
 * The University of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 06/02/2011
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Experiment {

    /**
     * Provides a name for the experiment.
     * @return The name.  The default name is the empty string.
     */
    String name() default "";

    /**
     * The time after which an experiment should be killed.
     * @return The time in milliseconds
     */
    long kill() default Long.MAX_VALUE;
}
