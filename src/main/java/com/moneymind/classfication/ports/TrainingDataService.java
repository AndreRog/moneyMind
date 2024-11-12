package com.moneymind.classfication.ports;

import com.moneymind.classfication.application.PartialTransaction;

import java.util.List;

public interface TrainingDataService {

    List<PartialTransaction> getTrainingSet();
}
