package com.moneymind.classifier.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Matches bank-generated generic transaction descriptions against per-country Rule Packs.
 *
 * <p>Each pack covers only lines the bank writes in its own language: salary, rent, transfers,
 * savings deposits, utility direct debits. Merchant names (CONTINENTE, RYANAIR, NETFLIX…) are
 * NOT in any pack — they belong in the Merchant Directory (issue 0020/0021).
 *
 * <p>EXCLUDED-type outcomes (TRANSFERS) are the ONLY source of EXCLUDED in the whole pipeline;
 * they must always be listed first within a pack so they take precedence over any overlapping
 * expense keywords. No derived or ML layer may emit EXCLUDED.
 *
 * <p>Returns {@link Optional#empty()} when no pack rule matches, deferring to later layers.
 */
public final class RulePackMatcher {

    public record Rule(String keyword, String category) {}

    public static final String COUNTRY_PT = "PT";

    /**
     * Portuguese pack — bank-generic lines only.
     * EXCLUDED rules are listed first (self-transfer, savings-to-external) as the only
     * permitted source of EXCLUDED-type outcomes in the classification pipeline.
     */
    public static final List<Rule> PT_RULES = List.of(
        // EXCLUDED — self-transfer patterns; ONLY source of EXCLUDED in the pipeline
        new Rule("TRANSFERENCIA ENTRE CONTAS",     ClassificationRules.CAT_TRANSFERS),
        new Rule("TRANSF ENTRE CONTAS",            ClassificationRules.CAT_TRANSFERS),

        // SAVINGS & INVESTMENTS — bank-written deposit / PPR descriptions
        new Rule("POUPANCA",                       ClassificationRules.CAT_SAVINGS),
        new Rule("DEPOSITO A PRAZO",               ClassificationRules.CAT_SAVINGS),
        new Rule("DEPOSITO PRAZO",                 ClassificationRules.CAT_SAVINGS),
        new Rule("PPR ",                           ClassificationRules.CAT_SAVINGS),

        // INCOME — payroll and allowance descriptions written by the bank / employer
        new Rule("ORDENADO",                       ClassificationRules.CAT_INCOME),
        new Rule("VENCIMENTO",                     ClassificationRules.CAT_INCOME),
        new Rule("SALARIO",                        ClassificationRules.CAT_INCOME),
        new Rule("REMUNERACAO",                    ClassificationRules.CAT_INCOME),
        new Rule("SUBSIDIO FERIAS",                ClassificationRules.CAT_INCOME),
        new Rule("SUBSIDIO NATAL",                 ClassificationRules.CAT_INCOME),

        // HOUSING — rent and utility direct-debit references the bank writes
        new Rule("RENDA ",                         ClassificationRules.CAT_HOUSING),
        new Rule("CONDOMINIO",                     ClassificationRules.CAT_HOUSING),
        new Rule("EDP ",                           ClassificationRules.CAT_HOUSING),
        new Rule("ENDESA",                         ClassificationRules.CAT_HOUSING),
        new Rule("ENEL ",                          ClassificationRules.CAT_HOUSING),
        new Rule("GALP GAS",                       ClassificationRules.CAT_HOUSING),
        new Rule("AGUAS DE ",                      ClassificationRules.CAT_HOUSING),
        new Rule("AGUAS DO ",                      ClassificationRules.CAT_HOUSING)
    );

    private final Map<String, List<Rule>> packs;

    public RulePackMatcher(Map<String, List<Rule>> packs) {
        this.packs = Map.copyOf(packs);
    }

    /** Returns a matcher pre-loaded with all built-in country packs. */
    public static RulePackMatcher withBuiltinPacks() {
        return new RulePackMatcher(Map.of(COUNTRY_PT, PT_RULES));
    }

    /**
     * Returns the first matching category for the given country and description, or empty when
     * no rule matches (caller should defer to the next classifier layer).
     */
    public Optional<String> match(String country, String description) {
        if (country == null || description == null || description.isBlank()) {
            return Optional.empty();
        }
        List<Rule> pack = packs.get(country);
        if (pack == null) {
            return Optional.empty();
        }
        String normalised = ClassificationRules.normalise(description);
        for (Rule rule : pack) {
            if (normalised.contains(ClassificationRules.normalise(rule.keyword()))) {
                return Optional.of(rule.category());
            }
        }
        return Optional.empty();
    }

    /** Returns true when a Rule Pack is registered for the given country. */
    public boolean supports(String country) {
        return packs.containsKey(country);
    }
}
