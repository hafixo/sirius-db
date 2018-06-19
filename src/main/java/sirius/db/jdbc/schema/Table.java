/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.db.jdbc.schema;

import sirius.db.mixing.EntityDescriptor;
import sirius.kernel.commons.Strings;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a database table.
 */
public class Table {

    private EntityDescriptor source;
    private String name;
    private List<String> primaryKey = new ArrayList<>();
    private List<TableColumn> columns = new ArrayList<>();
    private List<Key> keys = new ArrayList<>();
    private List<ForeignKey> foreignKeys = new ArrayList<>();

    public Table(EntityDescriptor source) {
        this.source = source;
    }

    public Table() {
    }

    public EntityDescriptor getSource() {
        return source;
    }

    /**
     * Returns the columns which make up the primary key
     *
     * @return the columns of the primary key
     */
    public List<String> getPrimaryKey() {
        return primaryKey;
    }

    /**
     * Returns the name of the table.
     *
     * @return the name of the table
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the table.
     *
     * @param name the name of the table
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns a mutable list of columns of this table.
     *
     * @return the list of columns of this table
     */
    public List<TableColumn> getColumns() {
        return columns;
    }

    /**
     * Returns a mutable list of keys of this table.
     *
     * @return the list of key of this table
     */
    public List<Key> getKeys() {
        return keys;
    }

    /**
     * Returns a mutable list of foreign keys of this table.
     *
     * @return the list of foreign keys of this table
     */
    public List<ForeignKey> getForeignKeys() {
        return foreignKeys;
    }

    /**
     * Returns the key with the given name.
     *
     * @param indexName the name of the key or index
     * @return the key with the given name or <tt>null</tt> if no such key exists
     */
    public Key getKey(String indexName) {
        for (Key key : keys) {
            if (Strings.areEqual(indexName, key.getName())) {
                return key;
            }
        }
        return null;
    }

    /**
     * Returns the foreign key with the given name.
     *
     * @param name the name of the foreign key
     * @return the foreign key with the given name or <tt>null</tt> if no such key exists
     */
    public ForeignKey getForeignKey(String name) {
        for (ForeignKey key : foreignKeys) {
            if (Strings.areEqual(name, key.getName())) {
                return key;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Provides a full report of all elements of this table.
     *
     * @return a string containing all elements of this table
     */
    public String dump() {
        StringBuilder sb = new StringBuilder("TABLE ");
        sb.append(name);
        sb.append("{\n");
        for (TableColumn col : columns) {
            sb.append("   COLUMN: ");
            sb.append(col);
            sb.append("\n");
        }
        sb.append("   PK: ");
        sb.append(Strings.join(primaryKey, ", "));
        sb.append("\n");
        for (Key key : keys) {
            sb.append("   INDEX: ");
            sb.append(key);
            sb.append("\n");
        }
        for (ForeignKey fk : foreignKeys) {
            sb.append("   FK: ");
            sb.append(fk);
            sb.append("\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Table)) {
            return false;
        }
        return ((Table) obj).name.equalsIgnoreCase(name);
    }

    @Override
    public int hashCode() {
        return name == null ? 0 : name.hashCode();
    }
}