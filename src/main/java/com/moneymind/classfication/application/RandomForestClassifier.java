package com.moneymind.classfication.application;

import com.moneymind.classfication.ports.TrainingDataService;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.ports.TransactionClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Classifies financial transactions using a RandomForest algorithm
 */
public class RandomForestClassifier implements TransactionClassifier {
    private static final int MIN_TRAINING_SAMPLE = 5;
    private final TrainingDataService trainingDataService;
    private RandomForest classifier;
    private StringToWordVector filter;
    private Set<String> knownCategories;
    private Instances dataStructure;
    private Instances filteredTrainingData;

    public RandomForestClassifier(final TrainingDataService trainingDataService) throws Exception {
        this.trainingDataService = trainingDataService;
        this.knownCategories = new HashSet<>();

        // Get training data
        List<PartialTransaction> transactions = trainingDataService.getTrainingSet();
        if (!hasEnoughSamples(transactions)) {
            throw new IllegalStateException("Not enough training samples. Need at least " + MIN_TRAINING_SAMPLE);
        }

        // Create and store the original instances
        Instances originalTrainingData = createInstancesFromTransactions(transactions);
        // Important: set class index before filtering
        originalTrainingData.setClassIndex(originalTrainingData.numAttributes() - 1);

        // Initialize and configure the text to vector filter
        this.filter = new StringToWordVector();
        this.filter.setAttributeIndices("1");  // Apply to first attribute (description)
        this.filter.setWordsToKeep(1000);
        this.filter.setLowerCaseTokens(true);

        // Critical: set input format before using the filter
        this.filter.setInputFormat(originalTrainingData);

        // Apply the filter to the training data
        this.filteredTrainingData = Filter.useFilter(originalTrainingData, this.filter);

        // Store all known categories
        this.knownCategories = transactions.stream()
                .filter(tx -> tx.category() != null && !tx.category().isBlank())
                .map(PartialTransaction::category)
                .collect(Collectors.toSet());

        // Initialize and train the RandomForest classifier
        this.classifier = new RandomForest();
        this.classifier.buildClassifier(this.filteredTrainingData);

        // Save the data structure for future classifications
        this.dataStructure = originalTrainingData;
    }

    private boolean hasEnoughSamples(List<PartialTransaction> transactions) {
        int validSamples = (int) transactions.stream()
                .filter(tx -> tx.category() != null && !tx.category().isBlank())
                .count();
        return validSamples >= MIN_TRAINING_SAMPLE;
    }

    private Instances createInstancesFromTransactions(List<PartialTransaction> transactions) {
        // Define attributes for the dataset
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("description", (ArrayList<String>) null)); // String attribute
        attributes.add(new Attribute("amount"));

        // Define class attribute (category) with possible values
        ArrayList<String> classValues = transactions.stream()
                .filter(tx -> tx.category() != null && !tx.category().isBlank())
                .map(PartialTransaction::category)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));

        // Make sure we have at least one category
        if (classValues.isEmpty()) {
            classValues.add("UNKNOWN");
        }

        attributes.add(new Attribute("category", classValues));

        // Create Instances object with these attributes
        Instances data = new Instances("TransactionData", attributes, transactions.size());
        data.setClassIndex(attributes.size() - 1);  // Set class index immediately

        // Populate data with each transaction
        for (PartialTransaction transaction : transactions) {
            Instance instance = new DenseInstance(attributes.size());
            instance.setDataset(data);  // Associate with dataset immediately

            instance.setValue(attributes.get(0), transaction.description());
            instance.setValue(attributes.get(1), transaction.amount().doubleValue());

            if (transaction.category() != null && !transaction.category().isBlank() &&
                    classValues.contains(transaction.category())) {
                instance.setValue(attributes.get(2), transaction.category());
            } else if (!classValues.isEmpty()) {
                // Use first category as default if category is invalid
                instance.setValue(attributes.get(2), classValues.get(0));
            }

            data.add(instance);
        }
        return data;
    }

    public String categorize(final PartialTransaction partialTransaction) throws Exception {
        return classifyTransaction(partialTransaction);
    }

    public String classifyTransaction(PartialTransaction transaction) throws Exception {
        if (classifier == null || filteredTrainingData == null) {
            throw new IllegalStateException("Classifier not initialized properly");
        }

        // Create a new Instances object with a single instance for the transaction
        Instances testInstances = new Instances(dataStructure, 0);

        Instance instance = new DenseInstance(dataStructure.numAttributes());
        instance.setDataset(testInstances);
        instance.setValue(dataStructure.attribute("description"), transaction.description());
        instance.setValue(dataStructure.attribute("amount"), transaction.amount().doubleValue());

        // Add default value for class attribute (will be ignored during classification)
        if (dataStructure.classAttribute().numValues() > 0) {
            instance.setValue(dataStructure.classAttribute(), dataStructure.classAttribute().value(0));
        }

        testInstances.add(instance);

        // Apply the same filter used during training
        Instances filteredTestInstances = Filter.useFilter(testInstances, filter);

        // Classify the filtered instance
        double predictionIndex = classifier.classifyInstance(filteredTestInstances.instance(0));

        // Return the predicted category
        return filteredTrainingData.classAttribute().value((int) predictionIndex);
    }

    @Override
    public List<FinancialRecord> classify() {
        return List.of(); // Implementation needed
    }

    @Override
    public FinancialRecord classify(FinancialRecord financialRecord) {
        // Implementation needed - this would convert FinancialRecord to PartialTransaction
        // then use classifyTransaction method, and update the FinancialRecord
        return null;
    }

    @Override
    public List<FinancialRecord> classify(List<FinancialRecord> financialRecords) throws Exception {
        for(FinancialRecord fr : financialRecords) {
            // TODO: REDESIGN SOLUTION AND SAVE CHANGES IN UI
            String category = this.classifyTransaction(new PartialTransaction(
                    fr.getDescription(), fr.getAmount(), null
            ));
        }

        return List.of(); // Implementation needed
    }
}
