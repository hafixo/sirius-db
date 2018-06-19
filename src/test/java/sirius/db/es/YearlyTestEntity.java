/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.db.es;

import sirius.db.es.annotations.StorePerYear;
import sirius.db.mixing.Mapping;

import java.time.LocalDateTime;

@StorePerYear("timestamp")
public class YearlyTestEntity extends VersionedEntity {

    public static final Mapping TIMESTAMP = Mapping.named("timestamp");
    private LocalDateTime timestamp;

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}