/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.db.mongo.properties

import sirius.db.mongo.Mango
import sirius.db.mongo.Mongo
import sirius.kernel.BaseSpecification
import sirius.kernel.async.CallContext
import sirius.kernel.di.std.Part
import sirius.kernel.health.HandledException

class MongoMultiLanguageStringPropertySpec extends BaseSpecification {

    @Part
    private static Mango mango

    @Part
    private static Mongo mongo

    def "invalid language"() {
        given:
        def entity = new MongoMultiLanguageStringEntity()
        entity.getMultiLangText().addText("00", "")

        when:
        mango.update(entity)

        then:
        thrown(HandledException)
    }

    def "store retrieve and validate"() {
        given:
        def entity = new MongoMultiLanguageStringEntity()
        entity.getMultiLangText().addText("de", "Schmetterling")
        entity.getMultiLangText().addText("en", "Butterfly")
        mango.update(entity)

        when:
        def output = mango.refreshOrFail(entity)

        then:
        output.getMultiLangText().size() == 2
        output.getMultiLangText().hasText("de")
        !output.getMultiLangText().hasText("fr")
        output.getMultiLangText().fetchText("de") == "Schmetterling"
        output.getMultiLangText().fetchText("fr") == null
        output.getMultiLangText().fetchText("de", "en") == "Schmetterling"
        output.getMultiLangText().fetchText("fr", "en") == "Butterfly"
        output.getMultiLangText().fetchText("fr", "es") == null
        output.getMultiLangText().getText("de") == Optional.of("Schmetterling")
        output.getMultiLangText().getText("fr") == Optional.empty()

        when:
        CallContext.getCurrent().setLang("en")

        then:
        output.getMultiLangText().fetchText() == "Butterfly"
        output.getMultiLangText().getText() == Optional.of("Butterfly")

        when:
        CallContext.getCurrent().setLang("fr")

        then:
        output.getMultiLangText().fetchText() == null
        output.getMultiLangText().getText() == Optional.empty()
    }

    def "store using default language"() {
        given:
        CallContext.getCurrent().setLang("en")
        def entity = new MongoMultiLanguageStringEntity()
        entity.getMultiLangText().addText("Butterfly")
        mango.update(entity)

        when:
        def output = mango.refreshOrFail(entity)

        then:
        output.getMultiLangText().fetchText() == "Butterfly"
        output.getMultiLangText().fetchText("de") == null
    }

    def "raw data check"() {
        given:
        def entity = new MongoMultiLanguageStringEntity()
        entity.getMultiLangText().addText("pt", "Borboleta")
        entity.getMultiLangText().addText("es", "Mariposa")
        entity.getMultiLangText().addText("en", "")
        entity.getMultiLangText().addText("de", null)
        mango.update(entity)

        when:
        def expectedString = "[Document{{lang=pt, text=Borboleta}}, Document{{lang=es, text=Mariposa}}, Document{{lang=en, text=}}]"
        def storedString = mongo.find()
                                .where("id", entity.getId())
                                .singleIn("mongomultilanguagestringentity")
                                .get()
                                .get("multiLangText")
                                .asString()

        then:
        expectedString == storedString
    }

    def "fallback can not be added to field without fallback enabled"() {
        given:
        def entity = new MongoMultiLanguageStringEntity()
        when:
        entity.getMultiLangText().addFallback("test")

        then:
        thrown(IllegalStateException)
    }

    def "fallback can be added and retrieved"() {
        given:
        def entity = new MongoMultiLanguageStringEntity()
        entity.getMultiLangTextWithFallback().addText("de", "In Ordnung")
        entity.getMultiLangTextWithFallback().addText("en", "Fine")
        entity.getMultiLangTextWithFallback().addFallback("OK")
        mango.update(entity)

        when:
        def output = mango.refreshOrFail(entity)

        then:
        output.getMultiLangTextWithFallback().size() == 3
        output.getMultiLangTextWithFallback().hasText("de")
        output.getMultiLangTextWithFallback().hasText("en")
        output.getMultiLangTextWithFallback().hasFallback()
        !output.getMultiLangTextWithFallback().hasText("fr")

        output.getMultiLangTextWithFallback().fetchTextOrFallback("de") == "In Ordnung"
        output.getMultiLangTextWithFallback().fetchTextOrFallback("en") == "Fine"
        output.getMultiLangTextWithFallback().fetchTextOrFallback("fr") == "OK"

        output.getMultiLangTextWithFallback().fetchText("de") == "In Ordnung"
        output.getMultiLangTextWithFallback().fetchText("fr") == null

        output.getMultiLangTextWithFallback().getText("de") == Optional.of("In Ordnung")
        output.getMultiLangTextWithFallback().getText("fr") == Optional.of("OK")

        when:
        CallContext.getCurrent().setLang("en")

        then:
        output.getMultiLangTextWithFallback().fetchText() == "Fine"
        output.getMultiLangTextWithFallback().getText() == Optional.of("Fine")

        when:
        CallContext.getCurrent().setLang("fr")

        then:
        output.getMultiLangTextWithFallback().fetchTextOrFallback() == "OK"
        output.getMultiLangTextWithFallback().getText() == Optional.of("OK")
    }
}
