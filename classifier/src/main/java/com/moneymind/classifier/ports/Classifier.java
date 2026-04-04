package com.moneymind.classifier.ports;

import com.moneymind.classifier.domain.ClassificationResult;
import com.moneymind.classifier.domain.Transaction;

import java.util.List;

public interface Classifier {

    ClassificationResult classify(Transaction transaction) throws Exception;

    List<ClassificationResult> classify(List<Transaction> transactions) throws Exception;
}
