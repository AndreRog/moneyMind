package com.moneymind.classifier;

import com.moneymind.classifier.domain.Transaction;
import com.moneymind.classifier.domain.PartialTransaction;
import com.moneymind.classifier.ports.Classifier;
import com.moneymind.classifier.ports.TrainingDataService;
import weka.classifiers.trees.RandomForest;
import weka.core.*;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Vibe coded
 */
public class WekaRandomForestClassifier implements Classifier {
    private static final int MIN_TRAINING_SAMPLES = 10;
    private static final double MIN_CONFIDENCE_THRESHOLD = 0.6;

    private final TrainingDataService trainingDataService;
    private RandomForest model;
    private String[] categoryLabels;
    private Map<String, Integer> categoryToIndex;
    private Map<String, Double> wordFeatures;
    private double minAmount, maxAmount;

    public WekaRandomForestClassifier(TrainingDataService trainingDataService) throws Exception {
        // Ensure headless mode for server environments
        System.setProperty("java.awt.headless", "true");

        WekaPackageManager.loadPackages(false);
        this.trainingDataService = trainingDataService;
        this.categoryToIndex = new HashMap<>();
        trainModel();
    }

    private void trainModel() throws Exception {
        List<Transaction> trainingData = trainingDataService.getTrainingSet();

        if (trainingData.size() < MIN_TRAINING_SAMPLES) {
            throw new IllegalStateException("Insufficient training data. Need at least " + MIN_TRAINING_SAMPLES + " samples");
        }

        // Build vocabulary from training data
        this.wordFeatures = buildVocabulary(trainingData);

        // Prepare features
        double[][] features = extractFeatures(trainingData);

        // Prepare labels
        Set<String> uniqueCategories = new HashSet<>();
        trainingData.forEach(t -> uniqueCategories.add(t.category()));

        this.categoryLabels = uniqueCategories.toArray(new String[0]);
        Arrays.sort(categoryLabels); // Ensure consistent ordering

        for (int i = 0; i < categoryLabels.length; i++) {
            categoryToIndex.put(categoryLabels[i], i);
        }

        int[] labels = trainingData.stream()
                .mapToInt(t -> categoryToIndex.get(t.category()))
                .toArray();

        // Calculate amount range for normalization
        List<Double> amounts = trainingData.stream()
                .map(t -> t.amount().doubleValue())
                .toList();
        this.minAmount = amounts.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        this.maxAmount = amounts.stream().mapToDouble(Double::doubleValue).max().orElse(1.0);

        // Create Weka Instances for training
        Instances trainingInstances = createWekaInstances(features, labels, true);
        
        // Train Weka RandomForest classifier
        this.model = new RandomForest();
        this.model.setNumIterations(100); // number of trees
        this.model.setMaxDepth(20);       // max depth
        this.model.setNumFeatures((int) Math.sqrt(wordFeatures.size() + 1)); // mtry
        this.model.buildClassifier(trainingInstances);
    }

    private Map<String, Double> buildVocabulary(List<Transaction> transactions) {
        Map<String, Integer> wordCount = new HashMap<>();
        int totalDocs = transactions.size();

        // Count word frequencies
        for (Transaction transaction : transactions) {
            Set<String> words = extractWords(transaction.description());
            for (String word : words) {
                wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
            }
        }

        // Calculate TF-IDF weights for top words
        Map<String, Double> vocabulary = new HashMap<>();
        wordCount.entrySet().stream()
                .filter(entry -> entry.getValue() > 1) // Filter rare words
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(200) // Top 200 words
                .forEach(entry -> {
                    double idf = Math.log((double) totalDocs / entry.getValue());
                    vocabulary.put(entry.getKey(), idf);
                });

        return vocabulary;
    }

    private Set<String> extractWords(String description) {
        Set<String> words = new HashSet<>();
        String[] tokens = description.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .split("\\s+");
        
        for (String token : tokens) {
            if (token.length() > 2) { // Filter short words
                words.add(token);
            }
        }
        return words;
    }

    private double[][] extractFeatures(List<Transaction> transactions) {
        int numFeatures = wordFeatures.size() + 1; // +1 for amount
        double[][] features = new double[transactions.size()][numFeatures];

        for (int i = 0; i < transactions.size(); i++) {
            Transaction transaction = transactions.get(i);
            
            // Text features using TF-IDF
            Set<String> words = extractWords(transaction.description());
            int featureIndex = 0;
            for (String word : wordFeatures.keySet()) {
                double tf = words.contains(word) ? 1.0 : 0.0;
                double idf = wordFeatures.get(word);
                features[i][featureIndex] = tf * idf;
                featureIndex++;
            }
            
            // Amount feature (normalized)
            double normalizedAmount = (transaction.amount().doubleValue() - minAmount) / 
                                    (maxAmount - minAmount + 1e-6); // Avoid division by zero
            features[i][featureIndex] = normalizedAmount;
        }

        return features;
    }

    private Instances createWekaInstances(double[][] features, int[] labels, boolean withLabels) {
        // Create attribute list
        ArrayList<Attribute> attributes = new ArrayList<>();
        
        // Add feature attributes
        for (int i = 0; i < wordFeatures.size(); i++) {
            attributes.add(new Attribute("word_" + i));
        }
        attributes.add(new Attribute("amount"));
        
        // Add class attribute if needed
        if (withLabels) {
            ArrayList<String> classValues = new ArrayList<>(Arrays.asList(categoryLabels));
            attributes.add(new Attribute("class", classValues));
        }
        
        // Create instances
        Instances instances = new Instances("TransactionClassification", attributes, features.length);
        if (withLabels) {
            instances.setClassIndex(instances.numAttributes() - 1);
        }
        
        // Add data
        for (int i = 0; i < features.length; i++) {
            Instance instance = new DenseInstance(attributes.size());
            instance.setDataset(instances);
            
            // Set feature values
            for (int j = 0; j < features[i].length; j++) {
                instance.setValue(j, features[i][j]);
            }
            
            // Set class value if provided
            if (withLabels) {
                instance.setValue(instances.classIndex(), categoryLabels[labels[i]]);
            }
            
            instances.add(instance);
        }
        
        return instances;
    }

    public ClassificationResult categorizeWithConfidence(Transaction transaction) throws Exception {
        if (model == null) {
            throw new IllegalStateException("Model not trained");
        }

        // Extract features for the transaction
        double[] transactionFeatures = extractFeatures(transaction);
        
        // Create Weka instance for prediction (add dummy class attribute)
        ArrayList<Attribute> attributes = new ArrayList<>();
        
        // Add feature attributes
        for (int i = 0; i < wordFeatures.size(); i++) {
            attributes.add(new Attribute("word_" + i));
        }
        attributes.add(new Attribute("amount"));
        
        // Add class attribute
        ArrayList<String> classValues = new ArrayList<>(Arrays.asList(categoryLabels));
        attributes.add(new Attribute("class", classValues));
        
        Instances predictionInstances = new Instances("TransactionClassification", attributes, 1);
        predictionInstances.setClassIndex(predictionInstances.numAttributes() - 1);
        
        Instance instance = new DenseInstance(attributes.size());
        instance.setDataset(predictionInstances);
        
        // Set feature values
        for (int j = 0; j < transactionFeatures.length; j++) {
            instance.setValue(j, transactionFeatures[j]);
        }
        // Class value will be missing for prediction
        
        predictionInstances.add(instance);
        instance = predictionInstances.firstInstance();
        
        // Get prediction from RandomForest
        double predictedIndex = model.classifyInstance(instance);
        String predictedCategory = categoryLabels[(int) predictedIndex];
        
        // Get probability distribution
        double[] probabilities = model.distributionForInstance(instance);
        
        // Find confidence (max probability)
        double confidence = Arrays.stream(probabilities).max().orElse(0.5);
        boolean isHighConfidence = confidence >= MIN_CONFIDENCE_THRESHOLD;
        
        // Get top predictions
        List<CategoryPrediction> alternatives = getTopPredictions(probabilities, 3);
        
        return new ClassificationResult(predictedCategory, confidence, isHighConfidence, alternatives);
    }

    private double[] extractFeatures(Transaction transaction) {
        int numFeatures = wordFeatures.size() + 1;
        double[] features = new double[numFeatures];

        // Text features using TF-IDF
        Set<String> words = extractWords(transaction.description());
        int featureIndex = 0;
        for (String word : wordFeatures.keySet()) {
            double tf = words.contains(word) ? 1.0 : 0.0;
            double idf = wordFeatures.get(word);
            features[featureIndex] = tf * idf;
            featureIndex++;
        }

        // Amount feature (normalized)
        double normalizedAmount = (transaction.amount().doubleValue() - minAmount) / 
                                (maxAmount - minAmount + 1e-6); // Avoid division by zero
        features[featureIndex] = normalizedAmount;

        return features;
    }


    private List<CategoryPrediction> getTopPredictions(double[] probabilities, int topN) {
        // Create index-probability pairs and sort by probability
        return IntStream.range(0, probabilities.length)
                .boxed()
                .sorted((a, b) -> Double.compare(probabilities[b], probabilities[a]))
                .limit(topN)
                .map(i -> new CategoryPrediction(categoryLabels[i], probabilities[i]))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public void retrainModel() throws Exception {
        trainModel();
    }

    public Set<String> getKnownCategories() {
        return new HashSet<>(Arrays.asList(categoryLabels));
    }

    public boolean isModelTrained() {
        return model != null;
    }

    @Override
    public ClassificationResult classify(Transaction transaction) throws Exception {
        ClassificationResult classificationResult = this.categorizeWithConfidence(transaction);
        return classificationResult;
    }

    @Override
    public List<ClassificationResult> classify(List<Transaction> transactions) throws Exception {
        return List.of();
    }


    // Inner classes for results
    public static class ClassificationResult {
        private final String predictedCategory;
        private final double confidence;
        private final boolean isHighConfidence;
        private final List<CategoryPrediction> alternatives;

        public ClassificationResult(String predictedCategory, double confidence,
                                    boolean isHighConfidence, List<CategoryPrediction> alternatives) {
            this.predictedCategory = predictedCategory;
            this.confidence = confidence;
            this.isHighConfidence = isHighConfidence;
            this.alternatives = alternatives;
        }

        // Getters
        public String getPredictedCategory() { return predictedCategory; }
        public double getConfidence() { return confidence; }
        public boolean isHighConfidence() { return isHighConfidence; }
        public List<CategoryPrediction> getAlternatives() { return alternatives; }
    }

    public static class CategoryPrediction {
        private final String category;
        private final double probability;

        public CategoryPrediction(String category, double probability) {
            this.category = category;
            this.probability = probability;
        }

        public String getCategory() { return category; }
        public double getProbability() { return probability; }
    }
}
