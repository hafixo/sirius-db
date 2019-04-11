/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.db.es

import sirius.kernel.BaseSpecification
import sirius.kernel.commons.Wait
import sirius.kernel.di.std.Part

class ReindexSpec extends BaseSpecification {

    @Part
    private static Elastic elastic

    def "reindex and move alias works"() {
        given:
        ElasticTestEntity e = new ElasticTestEntity()
        e.setAge(10)
        e.setFirstname("test")
        e.setLastname("test")
        and:
        elastic.update(e)
        and:
        Wait.seconds(2)
        when:
        elastic.getLowLevelClient().reindex(e.getDescriptor(), "reindex-test", {}, {})
        and:
        Wait.seconds(2)
        then:
        elastic.getLowLevelClient().indexExists("reindex-test")
        when:
        List indicesForAlias = elastic.getLowLevelClient().getIndicesForAlias(e.getDescriptor())
        then:
        indicesForAlias.size() == 1 && indicesForAlias.contains("elastictestentity")
        when:
        elastic.getLowLevelClient().moveActiveAlias(e.getDescriptor(), "reindex-test")
        and:
        indicesForAlias = elastic.getLowLevelClient().getIndicesForAlias(e.getDescriptor())
        then:
        indicesForAlias.size() == 1 && indicesForAlias.contains("reindex-test")
        and:
        Wait.seconds(2)
        and:
        elastic.select(ElasticTestEntity.class).eq(ElasticTestEntity.AGE, 10).queryOne().getFirstname() == "test"
    }
}