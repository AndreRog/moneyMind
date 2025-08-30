package com.moneymind.classifier.ports;


import com.moneymind.classifier.WekaRandomForestClassifier;
import com.moneymind.classifier.domain.Transaction;

import java.util.List;

public interface Classifier {

    WekaRandomForestClassifier.ClassificationResult classify(Transaction transaction) throws Exception;

    List<WekaRandomForestClassifier.ClassificationResult> classify(List<Transaction> transactions) throws Exception;
}
