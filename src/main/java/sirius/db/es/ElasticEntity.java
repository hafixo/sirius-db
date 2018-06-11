/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.db.es;

import sirius.db.es.query.ElasticQuery;
import sirius.db.es.query.FieldNotEqual;
import sirius.db.mixing.BaseEntity;
import sirius.db.mixing.BaseMapper;
import sirius.db.mixing.Mapping;
import sirius.db.mixing.Query;
import sirius.db.mixing.annotations.NullAllowed;
import sirius.kernel.di.std.Part;
import sirius.kernel.health.Exceptions;
import sirius.kernel.nls.NLS;

public abstract class ElasticEntity extends BaseEntity<String> {

    @Part
    protected static Elastic elastic;

    public static final Mapping ID = Mapping.named("id");
    @NullAllowed
    private String id;


    @Override
    protected void assertUnique(Mapping field, Object value, Mapping... within) {
        ElasticQuery<? extends ElasticEntity> qry = elastic.select(getClass()).eq(field, value);
        for (Mapping withinField : within) {
            qry.eq(withinField, getDescriptor().getProperty(withinField).getValue(this));
        }
        if (!isNew()) {
            qry.filter(new FieldNotEqual(ID, getId()));
        }
        if (qry.exists()) {
            throw Exceptions.createHandled()
                            .withNLSKey("Property.fieldNotUnique")
                            .set("field", getDescriptor().getProperty(field).getLabel())
                            .set("value", NLS.toUserString(value))
                            .handle();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E extends BaseEntity<?>, Q extends Query<Q, E>> BaseMapper<E, Q> getMapper() {
        return (BaseMapper<E, Q>)elastic;
    }

    @Override
    public String getId() {
        return id;
    }

    protected void setId(String id) {
        this.id = id;
    }
}
