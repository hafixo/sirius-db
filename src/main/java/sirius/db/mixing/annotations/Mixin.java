/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.db.mixing.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as mixin for another taret class (which is either an {@link sirius.db.mixing.Entity} or a {@link
 * sirius.db.mixing.Composite}).
 * <p>
 * A mixin can add properties to an entity or composite, which are not defined in the original class. This can be used
 * to defined customer extensions without modifying the standard classes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mixin {
    /**
     * The target class which will inherit all properties defined by this mixing.
     *
     * @return the target class to extend
     */
    Class<?> value();
}