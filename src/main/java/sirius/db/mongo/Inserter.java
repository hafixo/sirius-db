/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.db.mongo;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import sirius.kernel.commons.Watch;
import sirius.kernel.health.Microtiming;

/**
 * Fluent builder to build an insert statement.
 */
public class Inserter {

    private BasicDBObject obj = new BasicDBObject();
    private Mongo mongo;

    protected Inserter(Mongo mongo) {
        this.mongo = mongo;
    }

    /**
     * Sets a field to the given value.
     *
     * @param key   the name of the field to set
     * @param value the value to set the field to
     * @return the builder itself for fluent method calls
     */
    public Inserter set(String key, Object value) {
        obj.put(key, QueryBuilder.transformValue(value));
        return this;
    }

    /**
     * Sets a field to the given list of values.
     *
     * @param key    the name of the field to set
     * @param values the values to set the field to
     * @return the builder itself for fluent method calls
     */
    public Inserter setList(String key, Object... values) {
        BasicDBList list = new BasicDBList();
        for (Object value : values) {
            list.add(QueryBuilder.transformValue(value));
        }
        obj.put(key, list);
        return this;
    }

    /**
     * Executes the insert statement into the given collection.
     *
     * @param collection the collection to insert the document into
     * @return the inserted document
     */
    public Document into(String collection) {
        Watch w = Watch.start();
        mongo.db().getCollection(collection).insert(obj);
        mongo.callDuration.addValue(w.elapsedMillis());
        if (Microtiming.isEnabled()) {
            w.submitMicroTiming("mongo", "INSERT - " + collection + ": " + obj);
        }
        return new Document(obj);
    }
}