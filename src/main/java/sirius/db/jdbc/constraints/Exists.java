/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.db.jdbc.constraints;

import com.google.common.collect.Lists;
import sirius.db.jdbc.SQLEntity;
import sirius.db.mixing.Mixing;
import sirius.db.mixing.Mapping;
import sirius.db.mixing.EntityDescriptor;
import sirius.db.jdbc.Constraint;
import sirius.db.jdbc.SmartQuery;
import sirius.db.jdbc.TranslationState;
import sirius.kernel.di.std.Part;

import java.util.List;

/**
 * Generates an EXISTS subquery mapping the given field to the field of the queried entity.
 */
public class Exists extends Constraint {

    private Mapping outerColumn;
    private Mapping innerColumn;
    private List<Constraint> constraints = Lists.newArrayList();
    private Class<? extends SQLEntity> other;
    private boolean not = false;

    @Part
    private static Mixing mixing;

    private Exists() {
    }

    /**
     * Generates an NOT EXISTS clause: <tt>NOT EXISTS(SELECT * FROM other WHERE e.outerColumn = other.innerColumn</tt>.
     *
     * @param outerColumn the column of the entities being queried to match
     * @param other       the entity type to search in (which must not exist)
     * @param innerColumn the column within that inner entity type which must match the outer column
     * @return an exists constraint which can be filtered with additional constraints
     */
    public static Exists notMatchingIn(Mapping outerColumn, Class<? extends SQLEntity> other, Mapping innerColumn) {
        Exists result = new Exists();
        result.outerColumn = outerColumn;
        result.innerColumn = innerColumn;
        result.other = other;
        result.not = true;
        return result;
    }

    /**
     * Generates an EXISTS clause: <tt>EXISTS(SELECT * FROM other WHERE e.outerColumn = other.innerColumn</tt>.
     *
     * @param outerColumn the column of the entities being queried to match
     * @param other       the entity type to search in (which must exist)
     * @param innerColumn the column within that inner entity type which must match the outer column
     * @return an exists constraint which can be filtered with additional constraints
     */
    public static Exists matchingIn(Mapping outerColumn, Class<? extends SQLEntity> other, Mapping innerColumn) {
        Exists result = new Exists();
        result.outerColumn = outerColumn;
        result.innerColumn = innerColumn;
        result.other = other;
        return result;
    }

    /**
     * Adds an additional constraint to further filter the entities which must or must not exist.
     *
     * @param constraint the constaint on the inner entity type
     * @return the exists itself for fluent method calls
     */
    public Exists where(Constraint constraint) {
        constraints.add(constraint);

        return this;
    }

    @Override
    public boolean addsConstraint() {
        return true;
    }

    @Override
    public void appendSQL(SmartQuery.Compiler compiler) {
        // Determines the target descriptor and a new alias
        EntityDescriptor ed = mixing.getDescriptor(other);
        String newAlias = compiler.generateTableAlias();

        // Creates a backup of the current WHERE string builder
        StringBuilder originalWHEREBuilder = compiler.getWHEREBuilder();
        compiler.setWHEREBuilder(new StringBuilder());

        // Applies the main constraint and switches the underlying JOIN state just in time.
        compiler.getWHEREBuilder().append(" WHERE ").append(compiler.translateColumnName(outerColumn)).append(" = ");
        TranslationState state = compiler.captureAndReplaceTranslationState(newAlias, ed);
        compiler.getWHEREBuilder().append(compiler.translateColumnName(innerColumn));

        // Applies additional constraints...
        for (Constraint c : constraints) {
            if (c.addsConstraint()) {
                compiler.getWHEREBuilder().append(" AND ");
                c.appendSQL(compiler);
            }
        }

        // Generates the effective EXISTS constraint, restores the original WHERE builder
        // and appends the EXISTS to it...
        StringBuilder existsBuilder = new StringBuilder();
        if (not) {
            existsBuilder.append("NOT ");
        }
        existsBuilder.append("EXISTS(SELECT * FROM ").append(ed.getRelationName()).append(" ").append(newAlias);
        existsBuilder.append(compiler.getJoins());

        compiler.restoreTranslationState(state);
        StringBuilder existsWHEREBuilder = compiler.getWHEREBuilder();
        compiler.setWHEREBuilder(originalWHEREBuilder);
        compiler.getWHEREBuilder().append(existsBuilder).append(existsWHEREBuilder).append(")");
    }
}