/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.db.es.properties

import sirius.db.es.Elastic
import sirius.kernel.BaseSpecification
import sirius.kernel.di.std.Part

class ESMultiLanguageStringPropertySpec extends BaseSpecification {

    @Part
    private static Elastic elastic

    def "reading and writing works"() {
        when:
        def test = new ESMultiLanguageStringEntity()
        test.getMultiLang().put("de", "Das ist ein Test").put("en", "This is a test")
        elastic.update(test)
        def resolved = elastic.refreshOrFail(test)
        then:
        resolved.getMultiLang().size() == 2
        and:
        resolved.getMultiLang().getText("de").get() == "Das ist ein Test"
        resolved.getMultiLang().getText("en").get() == "This is a test"

        when:
        resolved.getMultiLang().modify().remove("de")
        and:
        elastic.update(resolved)
        and:
        resolved = elastic.refreshOrFail(test)
        then:
        resolved.getMultiLang().size() == 1
        and:
        !resolved.getMultiLang().contains("Das ist ein Test")
        resolved.getMultiLang().getText("en").get() == "This is a test"
    }

}
