package com.moneymind.classifier;

import com.moneymind.classifier.domain.ClassificationResult;
import com.moneymind.classifier.domain.ClassificationRules;
import com.moneymind.classifier.domain.Transaction;
import com.moneymind.classifier.ports.Classifier;

import java.util.List;
import java.util.Optional;

/**
 * Rules-first hybrid classifier: applies deterministic keyword rules before falling back to the
 * Weka ML model. When ML confidence is below the threshold (or the model is unavailable), the
 * fallback uses the transaction's amount sign to pick a safe category:
 * <ul>
 *   <li>Credit (positive amount) → INCOME — keeps credits in an INCOME-type category so they
 *       never inflate the expense total with a negative contribution.
 *   <li>Debit (negative/zero amount) → MISCELLANEOUS — standard expense catch-all.
 * </ul>
 *
 * <p>Confidence threshold matches WekaRandomForestClassifier.MIN_CONFIDENCE_THRESHOLD (0.6).
 */
public class HybridClassifier implements Classifier {

    static final String MISCELLANEOUS = "MISCELLANEOUS";
    static final double MIN_CONFIDENCE = 0.6;

    private final ClassificationRules rules;
    private final Classifier mlClassifier;

    public HybridClassifier(ClassificationRules rules, Classifier mlClassifier) {
        this.rules = rules;
        this.mlClassifier = mlClassifier;
    }

    @Override
    public ClassificationResult classify(Transaction transaction) throws Exception {
        Optional<String> ruleMatch = rules.match(transaction.description());
        if (ruleMatch.isPresent()) {
            return new ClassificationResult(ruleMatch.get(), 1.0, true);
        }

        try {
            ClassificationResult mlResult = mlClassifier.classify(transaction);
            if (mlResult.confidence() >= MIN_CONFIDENCE) {
                return mlResult;
            }
            return amountSignFallback(transaction, mlResult.confidence());
        } catch (Exception e) {
            // ML unavailable (e.g. insufficient training data): fall back on amount sign
            return amountSignFallback(transaction, 0.0);
        }
    }

    /**
     * Uses the transaction amount sign as a tie-breaker when ML is low-confidence or unavailable.
     * Credits (positive) belong in INCOME; debits (negative/zero/null) belong in MISCELLANEOUS.
     */
    private static ClassificationResult amountSignFallback(Transaction transaction, double confidence) {
        boolean isCredit = transaction.amount() != null && transaction.amount().signum() > 0;
        String category = isCredit ? ClassificationRules.CAT_INCOME : MISCELLANEOUS;
        return new ClassificationResult(category, confidence, false);
    }

    @Override
    public List<ClassificationResult> classify(List<Transaction> transactions) throws Exception {
        return transactions.stream()
                .map(t -> {
                    try {
                        return classify(t);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }
}
