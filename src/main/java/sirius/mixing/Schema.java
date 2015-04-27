/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.mixing;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import sirius.db.jdbc.Database;
import sirius.db.jdbc.Databases;
import sirius.kernel.di.Initializable;
import sirius.kernel.di.Injector;
import sirius.kernel.di.PartCollection;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Parts;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.mixing.properties.PropertyFactory;
import sirius.mixing.schema.DatabaseDialect;
import sirius.mixing.schema.SchemaTool;
import sirius.mixing.schema.SchemaUpdateAction;
import sirius.mixing.schema.Table;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Register(classes = {Schema.class, Initializable.class})
public class Schema implements Initializable {

    private Map<Class<?>, EntityDescriptor> descriptorsByType = Maps.newHashMap();

    public EntityDescriptor getDescriptor(Class<? extends Entity> aClass) {
        return descriptorsByType.get(aClass);
    }

    @Part
    private Databases dbs;
    private Database db;

    public Database getDatabase() {
        if (db == null) {
            db = dbs.get("mixing");
        }
        return db;
    }

    @Part(configPath = "mixing.dialect")
    private static DatabaseDialect dialect;

    @Override
    public void initialize() throws Exception {
        for (Entity e : Injector.context().getParts(Entity.class)) {
            EntityDescriptor ed = e.createDescriptor();
            ed.initialize();
            ed = applyMixins(ed);
            ed = applyModifications(ed);
            descriptorsByType.put(e.getClass(), ed);
        }

        List<Table> target = Lists.newArrayList();
        for (EntityDescriptor ed : descriptorsByType.values()) {
            target.add(ed.createTable());
        }

        SchemaTool tool = new SchemaTool("test", dialect);
        List<SchemaUpdateAction> actions = null;
        Database database = getDatabase();
        try (Connection c = database.getConnection()) {
            actions = tool.migrateSchemaTo(c, target, true);
        }

        for (SchemaUpdateAction action : actions) {
            if (!action.isDataLossPossible()) {
                //FIXME
                System.out.println(action.getReason());
                for (String statement : action.getSql()) {
                    try {
                        System.out.println(" --> " + statement);
                        database.createQuery(statement).executeUpdate();
                    } catch (SQLException e) {
                        // TODO
                        Exceptions.handle(e);
                    }
                }
            }
        }
    }

    private EntityDescriptor applyModifications(EntityDescriptor ed) {
        return ed;
    }

    private EntityDescriptor applyMixins(EntityDescriptor ed) {
        return ed;
    }
}