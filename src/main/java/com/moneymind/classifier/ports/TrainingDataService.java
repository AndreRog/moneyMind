package com.moneymind.classifier.ports;

import com.moneymind.classifier.domain.Transaction;

import java.util.List;

public interface TrainingDataService {
    List<Transaction> getTrainingSet();
}
