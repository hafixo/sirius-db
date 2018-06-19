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

import java.time.Duration

class BulkContextSpec extends BaseSpecification {

    @Part
    private static Elastic elastic

    def setupSpec() {
        elastic.getReadyFuture().await(Duration.ofSeconds(60))
    }

    def "batch insert works"() {
        setup:
        BulkContext btx = elastic.batch()
        when:
        btx.tryUpdate(new BatchTestEntity().withValue(1))
        btx.tryUpdate(new BatchTestEntity().withValue(2))
        btx.tryUpdate(new BatchTestEntity().withValue(3))
        btx.commit()
        Wait.seconds(2)
        then:
        elastic.select(BatchTestEntity.class).count() == 3
    }

    def "optimistic locking with batchcontext works"() {
        setup:
        BulkContext btx = elastic.batch()
        when:
        BatchTestEntity modified = new BatchTestEntity().withValue(100)
        elastic.update(modified)
        Wait.seconds(2)
        BatchTestEntity original = elastic.refreshOrFail(modified)
        elastic.update(modified.withValue(200))
        Wait.seconds(2)
        btx.tryUpdate(original.withValue(150))
        btx.commit()
        Wait.seconds(2)
        then:
        elastic.refreshOrFail(original).getValue() == 200
    }

    def "overwriting with batchcontext works"() {
        setup:
        BulkContext btx = elastic.batch()
        when:
        BatchTestEntity modified = new BatchTestEntity().withValue(100)
        elastic.update(modified)
        Wait.seconds(2)
        BatchTestEntity original = elastic.refreshOrFail(modified)
        elastic.update(modified.withValue(200))
        Wait.seconds(2)
        btx.overwrite(original.withValue(150))
        btx.commit()
        Wait.seconds(2)
        then:
        elastic.refreshOrFail(original).getValue() == 150
    }

    def "delete with batchcontext works"() {
        setup:
        BulkContext btx = elastic.batch()
        when:
        BatchTestEntity test = new BatchTestEntity().withValue(100)
        elastic.update(test)
        Wait.seconds(2)
        btx.tryDelete(test)
        btx.commit()
        Wait.seconds(2)
        then:
        !elastic.find(BatchTestEntity.class, test.getId()).isPresent()
    }

}