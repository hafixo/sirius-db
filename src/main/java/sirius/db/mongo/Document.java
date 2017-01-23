/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.db.mongo;

import com.mongodb.DBObject;
import sirius.kernel.commons.Value;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * A simple wrapper of a document in Mongo DB.
 */
public class Document {

    private DBObject obj;

    /**
     * Wraps a result from Mongo DB
     *
     * @param obj the document or object to wrap
     */
    public Document(DBObject obj) {
        this.obj = obj;
    }

    /**
     * Returns the ID of the document
     *
     * @return the id as assigned by mongo db
     */
    public String id() {
        return String.valueOf(obj.get("_id"));
    }

    /**
     * Returns the value of the requested field wrapped as {@link Value}.
     *
     * @param field the field to fetch
     * @return the value for the given field
     */
    public Value get(String field) {
        return Value.of(obj.get(field));
    }

    /**
     * Returns the string contents of the given field
     *
     * @param field the field to fetch
     * @return the string contents or "" if the field is empty
     */
    @Nonnull
    public String getString(String field) {
        return get(field).asString();
    }

    /**
     * Returns the list contained in the given field
     *
     * @param field the field to fetch
     * @return the list contained in the given field or an empty list, if the field was empty
     */
    @SuppressWarnings("unchecked")
    public List<Object> getList(String field) {
        Object result = obj.get(field);
        return result == null ? Collections.emptyList() : (List<Object>) result;
    }

    /**
     * Returns the list of strings contained in the given field
     *
     * @param field the field to fetch
     * @return the list contained in the given field or an empty list, if the field was empty
     */
    @SuppressWarnings("unchecked")
    public List<String> getStringList(String field) {
        return (List<String>) (Object) getList(field);
    }

    /**
     * Updates the underlying object, without updating the database.
     * <p>
     * This can be used to enhance an existing object after an update for further consumers. This is not intended to
     * update or modify the database in any way. Use {@link Mongo#update()} instead.
     *
     * @param key   the field to update
     * @param value the new value for the field
     */
    public void put(String key, Object value) {
        obj.put(key, value);
    }

    @Override
    public String toString() {
        return obj == null ? "null" : obj.toString();
    }

    /**
     * Retruns the underlying Mongo DB Obect.
     *
     * @return the underlying object
     */
    public DBObject getUnderlyingObject() {
        return obj;
    }
}
