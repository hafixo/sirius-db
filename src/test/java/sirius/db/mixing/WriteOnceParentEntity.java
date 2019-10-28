/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.db.mixing;

import sirius.db.jdbc.SQLEntity;
import sirius.db.mixing.annotations.ComplexDelete;

@ComplexDelete(false)
public class WriteOnceParentEntity extends SQLEntity {
}
