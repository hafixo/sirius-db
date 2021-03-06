/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.db.jdbc.constraints;

import sirius.db.jdbc.OMA;
import sirius.db.jdbc.SmartQuery;
import sirius.db.mixing.query.constraints.Constraint;

/**
 * Defines a constraint which is accepted by {@link SmartQuery} and most probably generated by {@link SQLFilterFactory}.
 *
 * @see OMA#FILTERS
 */
public abstract class SQLConstraint extends Constraint {

    /**
     * Generates the appropriate SQL into the given compiler
     *
     * @param compiler the current query compiler to apply the constraint to
     */
    public abstract void appendSQL(SmartQuery.Compiler compiler);
}
