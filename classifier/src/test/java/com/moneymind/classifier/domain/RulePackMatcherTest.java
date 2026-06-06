package com.moneymind.classifier.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests (no mocks) for {@link RulePackMatcher}.
 */
class RulePackMatcherTest {

    private final RulePackMatcher matcher = RulePackMatcher.withBuiltinPacks();

    // --- PT pack: bank-generic keywords resolve ---

    @Test
    void ptPack_renda_resolvesHousing() {
        assertEquals(Optional.of(ClassificationRules.CAT_HOUSING),
                matcher.match(RulePackMatcher.COUNTRY_PT, "RENDA APARTAMENTO T2 MAIO 2025"));
    }

    @Test
    void ptPack_ordenado_resolvesIncome() {
        assertEquals(Optional.of(ClassificationRules.CAT_INCOME),
                matcher.match(RulePackMatcher.COUNTRY_PT, "ORDENADO TECHCORP LDA MAIO"));
    }

    @Test
    void ptPack_vencimento_resolvesIncome() {
        assertEquals(Optional.of(ClassificationRules.CAT_INCOME),
                matcher.match(RulePackMatcher.COUNTRY_PT, "VENCIMENTO ABRIL 2025"));
    }

    @Test
    void ptPack_poupanca_resolvesSavings() {
        // Accented input normalises to POUPANCA
        assertEquals(Optional.of(ClassificationRules.CAT_SAVINGS),
                matcher.match(RulePackMatcher.COUNTRY_PT, "poupança automática maio"));
    }

    @Test
    void ptPack_depositoPrazo_resolvesSavings() {
        assertEquals(Optional.of(ClassificationRules.CAT_SAVINGS),
                matcher.match(RulePackMatcher.COUNTRY_PT, "DEPOSITO A PRAZO 12M CGD"));
    }

    @Test
    void ptPack_edp_resolvesHousing() {
        assertEquals(Optional.of(ClassificationRules.CAT_HOUSING),
                matcher.match(RulePackMatcher.COUNTRY_PT, "EDP COMERCIAL FATURA ELETRICIDADE"));
    }

    @Test
    void ptPack_aguasDe_resolvesHousing() {
        assertEquals(Optional.of(ClassificationRules.CAT_HOUSING),
                matcher.match(RulePackMatcher.COUNTRY_PT, "AGUAS DE LISBOA E VALE DO TEJO"));
    }

    // --- EXCLUDED: self-transfer rules fire first ---

    @Test
    void ptPack_transferenciaEntreContas_resolvesTransfers() {
        assertEquals(Optional.of(ClassificationRules.CAT_TRANSFERS),
                matcher.match(RulePackMatcher.COUNTRY_PT, "TRANSFERENCIA ENTRE CONTAS PROPRIAS"));
    }

    @Test
    void ptPack_mbWaySaida_resolvesTransfers() {
        assertEquals(Optional.of(ClassificationRules.CAT_TRANSFERS),
                matcher.match(RulePackMatcher.COUNTRY_PT, "MB WAY SAIDA 25EUR JOAO SILVA"));
    }

    @Test
    void excludedRuleBeatsOverlappingExpenseKeyword() {
        // A pack where a TRANSFERS rule and an expense rule could both match — EXCLUDED wins
        RulePackMatcher custom = new RulePackMatcher(Map.of("XX", List.of(
                new RulePackMatcher.Rule("TRANSFERENCIA ENTRE CONTAS", ClassificationRules.CAT_TRANSFERS),
                new RulePackMatcher.Rule("GALP",                       ClassificationRules.CAT_TRANSPORT)
        )));
        assertEquals(Optional.of(ClassificationRules.CAT_TRANSFERS),
                custom.match("XX", "TRANSFERENCIA ENTRE CONTAS GALP REFERENCIA"));
    }

    // --- Country isolation: PT keywords must not fire under a different country ---

    @Test
    void ptKeyword_doesNotFireUnderUnknownCountry() {
        assertTrue(matcher.match("ES", "ORDENADO TECHCORP LDA").isEmpty());
    }

    @Test
    void ptKeyword_doesNotFireUnderNullCountry() {
        assertTrue(matcher.match(null, "ORDENADO TECHCORP LDA").isEmpty());
    }

    // --- Merchant entries are NOT in the pack ---

    @Test
    void merchantContinente_returnsEmpty() {
        // Merchant entries belong in the Merchant Directory, not the Rule Pack
        assertTrue(matcher.match(RulePackMatcher.COUNTRY_PT, "CONTINENTE HIPERMERCADOS LISBOA").isEmpty());
    }

    @Test
    void merchantRyanair_returnsEmpty() {
        assertTrue(matcher.match(RulePackMatcher.COUNTRY_PT, "RYANAIR LTD DUBLIN").isEmpty());
    }

    @Test
    void merchantNetflix_returnsEmpty() {
        assertTrue(matcher.match(RulePackMatcher.COUNTRY_PT, "NETFLIX.COM").isEmpty());
    }

    // --- Unsupported country and edge cases ---

    @Test
    void unsupportedCountry_returnsEmpty() {
        assertTrue(matcher.match("ZZ", "RENDA APARTAMENTO T2").isEmpty());
    }

    @Test
    void nullDescription_returnsEmpty() {
        assertTrue(matcher.match(RulePackMatcher.COUNTRY_PT, null).isEmpty());
    }

    @Test
    void blankDescription_returnsEmpty() {
        assertTrue(matcher.match(RulePackMatcher.COUNTRY_PT, "   ").isEmpty());
    }

    @Test
    void unknownBankLine_returnsEmpty() {
        assertTrue(matcher.match(RulePackMatcher.COUNTRY_PT, "RANDOM UNKNOWN DESCRIPTION XYZ 1234").isEmpty());
    }

    // --- supports() ---

    @Test
    void supports_ptReturnsTrue() {
        assertTrue(matcher.supports(RulePackMatcher.COUNTRY_PT));
    }

    @Test
    void supports_unknownCountryReturnsFalse() {
        assertFalse(matcher.supports("ZZ"));
    }

    // --- Custom packs ---

    @Test
    void customPack_firstMatchWins() {
        RulePackMatcher custom = new RulePackMatcher(Map.of("XX", List.of(
                new RulePackMatcher.Rule("ALPHA", "CAT_A"),
                new RulePackMatcher.Rule("ALPHA", "CAT_B")
        )));
        assertEquals(Optional.of("CAT_A"), custom.match("XX", "ALPHA BETA"));
    }

    @Test
    void emptyPack_alwaysReturnsEmpty() {
        RulePackMatcher empty = new RulePackMatcher(Map.of("XX", List.of()));
        assertTrue(empty.match("XX", "ORDENADO CONTINENTE RYANAIR").isEmpty());
    }
}
