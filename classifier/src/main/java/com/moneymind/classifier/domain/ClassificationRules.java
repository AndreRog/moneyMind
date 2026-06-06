package com.moneymind.classifier.domain;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;

/**
 * Data-driven keyword to category map applied before the ML model. Rules are checked in order;
 * first match wins. Descriptions are normalised to uppercase ASCII before matching so accented
 * variants ("FARMACIA" == "FARMACIA") are treated identically.
 *
 * EXCLUDED-type outcomes (TRANSFERS) must only arrive here via an explicit rule, never from a
 * low-confidence ML prediction. Self-transfer and savings rules are listed first so they take
 * precedence over any overlapping expense keywords.
 */
public final class ClassificationRules {

    public record Rule(String keyword, String category) {}

    public static final String CAT_TRANSFERS     = "TRANSFERS";
    public static final String CAT_SAVINGS       = "SAVINGS & INVESTMENTS";
    public static final String CAT_INCOME        = "INCOME";
    public static final String CAT_HOUSING       = "HOUSING";
    public static final String CAT_FOOD          = "FOOD & DINING";
    public static final String CAT_TRANSPORT     = "TRANSPORT";
    public static final String CAT_HEALTH        = "HEALTH";
    public static final String CAT_SUBSCRIPTIONS = "SUBSCRIPTIONS";
    public static final String CAT_TRAVEL        = "TRAVEL";
    public static final String CAT_ENTERTAINMENT = "ENTERTAINMENT";
    public static final String CAT_MISCELLANEOUS = "MISCELLANEOUS";

    /**
     * Default Portuguese-merchant rule set. EXCLUDED-producing rules (TRANSFERS) are listed first
     * so a description matching both a self-transfer keyword and an expense keyword resolves to
     * TRANSFERS.
     */
    static final List<Rule> DEFAULT_RULES = List.of(
        // EXCLUDED — explicit self-transfer patterns only
        new Rule("TRANSFERENCIA ENTRE CONTAS",     CAT_TRANSFERS),
        new Rule("TRANSF ENTRE CONTAS",            CAT_TRANSFERS),
        new Rule("MB WAY SAIDA",                   CAT_TRANSFERS),

        // SAVINGS & INVESTMENTS
        new Rule("POUPANCA",                       CAT_SAVINGS),
        new Rule("DEPOSITO A PRAZO",               CAT_SAVINGS),
        new Rule("DEPOSITO PRAZO",                 CAT_SAVINGS),
        new Rule("PPR ",                           CAT_SAVINGS),

        // INCOME
        new Rule("ORDENADO",                       CAT_INCOME),
        new Rule("VENCIMENTO",                     CAT_INCOME),
        new Rule("SALARIO",                        CAT_INCOME),
        new Rule("REMUNERACAO",                    CAT_INCOME),
        new Rule("SUBSIDIO FERIAS",                CAT_INCOME),
        new Rule("SUBSIDIO NATAL",                 CAT_INCOME),

        // HOUSING
        new Rule("RENDA ",                         CAT_HOUSING),
        new Rule("CONDOMINIO",                     CAT_HOUSING),
        new Rule("EDP ",                           CAT_HOUSING),
        new Rule("ENDESA",                         CAT_HOUSING),
        new Rule("ENEL ",                          CAT_HOUSING),
        new Rule("GALP GAS",                       CAT_HOUSING),
        new Rule("AGUAS DE ",                      CAT_HOUSING),
        new Rule("AGUAS DO ",                      CAT_HOUSING),

        // FOOD & DINING
        new Rule("CONTINENTE",                     CAT_FOOD),
        new Rule("PINGO DOCE",                     CAT_FOOD),
        new Rule("AUCHAN",                         CAT_FOOD),
        new Rule("LIDL",                           CAT_FOOD),
        new Rule("MINIPRECO",                      CAT_FOOD),
        new Rule("INTERMARCHE",                    CAT_FOOD),
        new Rule("MERCADONA",                      CAT_FOOD),
        new Rule("TELEPIZZA",                      CAT_FOOD),
        new Rule("DOMINOS",                        CAT_FOOD),
        new Rule("MCDONALD",                       CAT_FOOD),
        new Rule("KFC",                            CAT_FOOD),
        new Rule("NANDOS",                         CAT_FOOD),
        new Rule("PIZZA HUT",                      CAT_FOOD),
        new Rule("UBER EATS",                      CAT_FOOD),
        new Rule("GLOVO",                          CAT_FOOD),
        new Rule("ZOMATO",                         CAT_FOOD),

        // TRANSPORT
        new Rule("VIA VERDE",                      CAT_TRANSPORT),
        new Rule("VIA-VERDE",                      CAT_TRANSPORT),
        new Rule("CP COMBOIOS",                    CAT_TRANSPORT),
        new Rule("COMBOIOS DE PORTUGAL",           CAT_TRANSPORT),
        new Rule("METRO DE LISBOA",                CAT_TRANSPORT),
        new Rule("METRO DO PORTO",                 CAT_TRANSPORT),
        new Rule("REPSOL",                         CAT_TRANSPORT),
        new Rule("CEPSA",                          CAT_TRANSPORT),
        new Rule("GALP",                           CAT_TRANSPORT),
        new Rule("GIRA ",                          CAT_TRANSPORT),
        new Rule("BOLT ",                          CAT_TRANSPORT),
        new Rule("UBER ",                          CAT_TRANSPORT),
        new Rule("CABIFY",                         CAT_TRANSPORT),
        new Rule("ESSO ",                          CAT_TRANSPORT),
        new Rule("BP ",                            CAT_TRANSPORT),

        // HEALTH
        new Rule("FARMACIA",                       CAT_HEALTH),
        new Rule("CLINICA",                        CAT_HEALTH),
        new Rule("DENTISTA",                       CAT_HEALTH),
        new Rule("DENTAL",                         CAT_HEALTH),
        new Rule("HOSPITAL",                       CAT_HEALTH),
        new Rule("LABORATORIO",                    CAT_HEALTH),
        new Rule("OPTICA",                         CAT_HEALTH),
        new Rule("HOLMES PLACE",                   CAT_HEALTH),
        new Rule("GINASIO",                        CAT_HEALTH),

        // SUBSCRIPTIONS
        new Rule("NETFLIX",                        CAT_SUBSCRIPTIONS),
        new Rule("SPOTIFY",                        CAT_SUBSCRIPTIONS),
        new Rule("HBO MAX",                        CAT_SUBSCRIPTIONS),
        new Rule("PRIME VIDEO",                    CAT_SUBSCRIPTIONS),
        new Rule("APPLE.COM",                      CAT_SUBSCRIPTIONS),
        new Rule("APPLE SUBSCRIPTIONS",            CAT_SUBSCRIPTIONS),
        new Rule("GOOGLE ONE",                     CAT_SUBSCRIPTIONS),
        new Rule("YOUTUBE PREMIUM",                CAT_SUBSCRIPTIONS),
        new Rule("NOS ",                           CAT_SUBSCRIPTIONS),
        new Rule("MEO ",                           CAT_SUBSCRIPTIONS),
        new Rule("VODAFONE",                       CAT_SUBSCRIPTIONS),
        new Rule("NESPRESSO",                      CAT_SUBSCRIPTIONS),

        // TRAVEL
        new Rule("RYANAIR",                        CAT_TRAVEL),
        new Rule("TAP AIR",                        CAT_TRAVEL),
        new Rule("EASYJET",                        CAT_TRAVEL),
        new Rule("TRANSAVIA",                      CAT_TRAVEL),
        new Rule("VUELING",                        CAT_TRAVEL),
        new Rule("BOOKING.COM",                    CAT_TRAVEL),
        new Rule("AIRBNB",                         CAT_TRAVEL),
        new Rule("RENTALCARS",                     CAT_TRAVEL),
        new Rule("HERTZ",                          CAT_TRAVEL),
        new Rule("EUROPCAR",                       CAT_TRAVEL),

        // ENTERTAINMENT
        new Rule("NOS CINEMAS",                    CAT_ENTERTAINMENT),
        new Rule("CINEMA",                         CAT_ENTERTAINMENT),
        new Rule("STEAM",                          CAT_ENTERTAINMENT),
        new Rule("PLAYSTATION",                    CAT_ENTERTAINMENT),
        new Rule("XBOX",                           CAT_ENTERTAINMENT),
        new Rule("NINTENDO",                       CAT_ENTERTAINMENT)
    );

    private final List<Rule> rules;

    public ClassificationRules() {
        this(DEFAULT_RULES);
    }

    public ClassificationRules(List<Rule> rules) {
        this.rules = rules;
    }

    /**
     * Returns the first category whose keyword is found as a substring in the normalised
     * description, or empty when no rule matches.
     */
    public Optional<String> match(String description) {
        if (description == null || description.isBlank()) {
            return Optional.empty();
        }
        String normalised = normalise(description);
        for (Rule rule : rules) {
            if (normalised.contains(normalise(rule.keyword()))) {
                return Optional.of(rule.category());
            }
        }
        return Optional.empty();
    }

    /** Uppercases, strips combining diacritical marks, collapses punctuation and multiple spaces. */
    static String normalise(String text) {
        String decomposed = Normalizer.normalize(text.toUpperCase(), Normalizer.Form.NFD);
        return decomposed
                .replaceAll("[\\u0300-\\u036f]", "")
                .replaceAll("[^A-Z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
