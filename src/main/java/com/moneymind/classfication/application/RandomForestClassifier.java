package com.moneymind.classfication.application;

import com.moneymind.classfication.ports.TrainingDataService;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.util.*;

public class RandomForestClassifier {
    private static final int MIN_TRAINING_SAMPLE = 5;
    private final TrainingDataService trainingDataService;
    private RandomForest classifier;
    private StringToWordVector textToVector;
    private Set<String> knownCategories;
    private final StringToWordVector filter;
    private final Instances trainingData;
    private Instances dataStructure;

    public RandomForestClassifier(final TrainingDataService trainingDataService) throws Exception {
        this.trainingDataService = trainingDataService;
        this.knownCategories = new HashSet<>();

        Instances trainingData = createInstancesFromTransactions(trainingDataService.getTrainingSet());
        trainingData.setClassIndex(trainingData.numAttributes() - 1);

        // Initialize the text to vector filter for description field
        this.filter = new StringToWordVector();
        this.filter.setAttributeIndices("1");  // Apply to first attribute (description)
        this.filter.setWordsToKeep(1000);
        this.filter.setLowerCaseTokens(true);

        trainingData = Filter.useFilter(trainingData, this.filter);

        this.trainingData = trainingData;
        // Initialize and train the RandomForest classifier
        classifier = new RandomForest();
        classifier.buildClassifier(trainingData);
    }

    private boolean hasEnoughSamples(List<PartialTransaction> transactions) {
        return transactions.size() >= MIN_TRAINING_SAMPLE;
    }

    private Instances createInstancesFromTransactions(List<PartialTransaction> transactions) {
        // Define attributes for the dataset
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("description", (ArrayList<String>) null)); // String attribute
        attributes.add(new Attribute("amount"));

        // Define class attribute (category) with possible values
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("Groceries");
        classValues.add("Investment");
        classValues.add("Salary");
        classValues.add("General_Expenses");
        classValues.add("Restaurants");
        attributes.add(new Attribute("category", classValues));

        // Create Instances object with these attributes
        Instances data = new Instances("TransactionData", attributes, transactions.size());

        // Populate data with each transaction
        for (PartialTransaction transaction : transactions) {
            Instance instance = new DenseInstance(attributes.size());
            instance.setValue(attributes.get(0), transaction.description());
            instance.setValue(attributes.get(1), transaction.amount().doubleValue());

            if (transaction.category() != null) {
                instance.setValue(attributes.get(2), transaction.category());
            }
            data.add(instance);
        }
        return data;
    }
    public String categorize(final PartialTransaction partialTransaction) throws Exception {

        return classifyTransaction(partialTransaction);
    }

    public String classifyTransaction(PartialTransaction transaction) throws Exception {
        // Create a new instance from the transaction data
        Instance instance = new DenseInstance(trainingData.numAttributes());
        instance.setDataset(trainingData);
        instance.setValue(trainingData.attribute("description"), transaction.description());
        instance.setValue(trainingData.attribute("amount"), transaction.amount().doubleValue());

        // Apply the StringToWordVector filter to this instance
        StringToWordVector filter = new StringToWordVector();
        filter.setInputFormat(trainingData);
        instance = Filter.useFilter(instance.dataset(), filter).instance(0);

        // Classify the instance
        double categoryIndex = classifier.classifyInstance(instance);
        return trainingData.classAttribute().value((int) categoryIndex);
    }

}
