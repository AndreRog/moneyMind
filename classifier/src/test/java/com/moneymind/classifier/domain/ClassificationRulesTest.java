package com.moneymind.classifier.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests (no mocks) for {@link ClassificationRules}.
 */
class ClassificationRulesTest {

    private final ClassificationRules rules = new ClassificationRules();

    // --- normalise ---

    @Test
    void normalise_uppercasesAndStripsPunctuation() {
        assertEquals("PINGO DOCE LISBOA", ClassificationRules.normalise("pingo.doce lisboa"));
    }

    @Test
    void normalise_stripsCombiningAccents() {
        // U+00C1 LATIN CAPITAL LETTER A WITH ACUTE
        String farmacia = "FARMÁCIA";
        assertEquals("FARMACIA", ClassificationRules.normalise(farmacia));
    }

    @Test
    void normalise_stripsLowercaseAccents() {
        // U+00E1 LATIN SMALL LETTER A WITH ACUTE
        String farmacia = "farmácia";
        assertEquals("FARMACIA", ClassificationRules.normalise(farmacia));
    }

    @Test
    void normalise_collapsesMultipleSpaces() {
        assertEquals("A B C", ClassificationRules.normalise("a  b   c"));
    }

    // --- match: known merchants ---

    @Test
    void knownMerchant_continente_matchesFood() {
        assertEquals(Optional.of(ClassificationRules.CAT_FOOD), rules.match("CONTINENTE HIPERMERCADOS LISBOA"));
    }

    @Test
    void knownMerchant_ryanair_matchesTravel() {
        assertEquals(Optional.of(ClassificationRules.CAT_TRAVEL), rules.match("RYANAIR LTD DUBLIN"));
    }

    @Test
    void knownMerchant_bookingCom_matchesTravel() {
        // description has a space where the dot was; keyword must be normalised too
        assertEquals(Optional.of(ClassificationRules.CAT_TRAVEL), rules.match("BOOKING COM HOTEL MADRID"));
    }

    @Test
    void knownMerchant_ordenado_matchesIncome() {
        assertEquals(Optional.of(ClassificationRules.CAT_INCOME), rules.match("ORDENADO TECHCORP LDA"));
    }

    @Test
    void knownMerchant_netflix_matchesSubscriptions() {
        assertEquals(Optional.of(ClassificationRules.CAT_SUBSCRIPTIONS), rules.match("NETFLIX.COM"));
    }

    @Test
    void accentedDescription_farmacia_matchesHealth() {
        // U+00C1 = A-acute: "FARMÁCIA SANTA ANA" normalises to "FARMACIA SANTA ANA"
        assertEquals(Optional.of(ClassificationRules.CAT_HEALTH),
                rules.match("FARMÁCIA SANTA ANA LISBOA"));
    }

    @Test
    void accentedDescription_poupanca_matchesSavings() {
        // U+00E7 = c-cedilla, U+00E3 = a-tilde: "poupança" -> "POUPANCA"
        assertEquals(Optional.of(ClassificationRules.CAT_SAVINGS),
                rules.match("poupança automática maio"));
    }

    // --- match: EXCLUDED only via explicit rule ---

    @Test
    void selfTransferKeyword_mapsToTransfers() {
        assertEquals(Optional.of(ClassificationRules.CAT_TRANSFERS),
                rules.match("TRANSFERENCIA ENTRE CONTAS PROPRIAS"));
    }

    @Test
    void transferRuleTakesPrecedenceOverExpenseKeyword() {
        ClassificationRules withTransferFirst = new ClassificationRules(List.of(
                new ClassificationRules.Rule("TRANSFERENCIA ENTRE CONTAS", ClassificationRules.CAT_TRANSFERS),
                new ClassificationRules.Rule("GALP", ClassificationRules.CAT_TRANSPORT)
        ));
        assertEquals(Optional.of(ClassificationRules.CAT_TRANSFERS),
                withTransferFirst.match("TRANSFERENCIA ENTRE CONTAS GALP REFERENCIA"));
    }

    // --- match: unknown merchant ---

    @Test
    void unknownMerchant_returnsEmpty() {
        assertTrue(rules.match("RANDOM UNKNOWN MERCHANT XYZ 1234").isEmpty());
    }

    @Test
    void nullDescription_returnsEmpty() {
        assertTrue(rules.match(null).isEmpty());
    }

    @Test
    void blankDescription_returnsEmpty() {
        assertTrue(rules.match("   ").isEmpty());
    }

    // --- custom rules ---

    @Test
    void customRules_firstMatchWins() {
        ClassificationRules custom = new ClassificationRules(List.of(
                new ClassificationRules.Rule("ALPHA", "CAT_A"),
                new ClassificationRules.Rule("ALPHA", "CAT_B")
        ));
        assertEquals(Optional.of("CAT_A"), custom.match("ALPHA BETA"));
    }

    @Test
    void emptyRuleList_alwaysReturnsEmpty() {
        ClassificationRules empty = new ClassificationRules(List.of());
        assertTrue(empty.match("NETFLIX CONTINENTE RYANAIR").isEmpty());
    }
}
