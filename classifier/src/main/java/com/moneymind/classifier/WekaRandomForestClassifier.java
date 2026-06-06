package com.moneymind.classifier;

import com.moneymind.classifier.domain.ClassificationResult;
import com.moneymind.classifier.domain.FeatureExtractor;
import com.moneymind.classifier.domain.Transaction;
import com.moneymind.classifier.ports.Classifier;
import com.moneymind.classifier.ports.TrainingDataService;
import weka.classifiers.trees.RandomForest;
import weka.core.*;

import java.util.*;

/**
 * Weka RandomForest classifier wired to the pure {@link FeatureExtractor} (ADR-0001): it trains
 * and predicts on TF-IDF(description) + a nominal bucketed amount range + a nominal day-of-month
 * period — never exact amounts or dates.
 */
public class WekaRandomForestClassifier implements Classifier {
    private static final int MIN_TRAINING_SAMPLES = 10;
    private static final double MIN_CONFIDENCE_THRESHOLD = 0.6;

    private final TrainingDataService trainingDataService;
    private final FeatureExtractor featureExtractor;
    private RandomForest model;
    private String[] categoryLabels;
    private Map<String, Integer> categoryToIndex;
    private Map<String, Double> wordFeatures;
    private List<String> vocabulary;

    public WekaRandomForestClassifier(TrainingDataService trainingDataService) throws Exception {
        // Ensure headless mode for server environments
        System.setProperty("java.awt.headless", "true");

        WekaPackageManager.loadPackages(false);
        this.trainingDataService = trainingDataService;
        this.featureExtractor = new FeatureExtractor();
        this.categoryToIndex = new HashMap<>();
        trainModel();
    }

    private void trainModel() throws Exception {
        List<Transaction> trainingData = trainingDataService.getTrainingSet();

        if (trainingData.size() < MIN_TRAINING_SAMPLES) {
            throw new IllegalStateException("Insufficient training data. Need at least " + MIN_TRAINING_SAMPLES + " samples");
        }

        // Build vocabulary (idf weights) from training data, keeping a stable word ordering
        this.wordFeatures = buildVocabulary(trainingData);
        this.vocabulary = new ArrayList<>(wordFeatures.keySet());

        // Prepare labels
        Set<String> uniqueCategories = new HashSet<>();
        trainingData.forEach(t -> uniqueCategories.add(t.category()));

        this.categoryLabels = uniqueCategories.toArray(new String[0]);
        Arrays.sort(categoryLabels); // Ensure consistent ordering

        categoryToIndex.clear();
        for (int i = 0; i < categoryLabels.length; i++) {
            categoryToIndex.put(categoryLabels[i], i);
        }

        // Create Weka Instances for training (one row per labeled transaction)
        Instances trainingInstances = newInstances(true);
        for (Transaction transaction : trainingData) {
            addInstance(trainingInstances, transaction, transaction.category());
        }

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
            Set<String> words = featureExtractor.tokens(transaction.description());
            for (String word : words) {
                wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
            }
        }

        // Calculate TF-IDF weights for top words, preserving insertion order for stable indexing.
        // Single-occurrence words are retained: in Portuguese banking data most merchants appear
        // once or twice per statement, so they are exactly the discriminative signal we need.
        Map<String, Double> vocabulary = new LinkedHashMap<>();
        wordCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(200) // Top 200 words
                .forEach(entry -> {
                    double idf = Math.log((double) totalDocs / entry.getValue());
                    vocabulary.put(entry.getKey(), idf);
                });

        return vocabulary;
    }

    /** Attribute layout: word_0..word_n (numeric tf-idf), amount_bucket (nominal), period (nominal), [class]. */
    private Instances newInstances(boolean withLabels) {
        ArrayList<Attribute> attributes = new ArrayList<>();

        for (int i = 0; i < vocabulary.size(); i++) {
            attributes.add(new Attribute("word_" + i));
        }
        attributes.add(new Attribute("amount_bucket", FeatureExtractor.AMOUNT_BUCKETS));
        attributes.add(new Attribute("period", FeatureExtractor.PERIODS));

        if (withLabels) {
            attributes.add(new Attribute("class", new ArrayList<>(Arrays.asList(categoryLabels))));
        }

        Instances instances = new Instances("TransactionClassification", attributes, 0);
        if (withLabels) {
            instances.setClassIndex(instances.numAttributes() - 1);
        }
        return instances;
    }

    /** Builds a feature row for the transaction and appends it to {@code dataset}. */
    private void addInstance(Instances dataset, Transaction transaction, String label) {
        Instance instance = new DenseInstance(dataset.numAttributes());
        instance.setDataset(dataset);

        Set<String> tokens = featureExtractor.tokens(transaction.description());
        for (int i = 0; i < vocabulary.size(); i++) {
            String word = vocabulary.get(i);
            double tf = tokens.contains(word) ? 1.0 : 0.0;
            instance.setValue(i, tf * wordFeatures.get(word));
        }

        int bucketIndex = vocabulary.size();
        instance.setValue(dataset.attribute(bucketIndex), featureExtractor.amountBucket(transaction.amount()));
        instance.setValue(dataset.attribute(bucketIndex + 1), featureExtractor.period(transaction.date()));

        if (label != null && dataset.classIndex() >= 0) {
            instance.setValue(dataset.classIndex(), label);
        }

        dataset.add(instance);
    }

    private WekaResult categorizeWithConfidence(Transaction transaction) throws Exception {
        if (model == null) {
            throw new IllegalStateException("Model not trained");
        }

        Instances predictionInstances = newInstances(true);
        addInstance(predictionInstances, transaction, null); // class value left missing
        Instance instance = predictionInstances.firstInstance();

        // Get prediction from RandomForest
        double predictedIndex = model.classifyInstance(instance);
        String predictedCategory = categoryLabels[(int) predictedIndex];

        // Get probability distribution
        double[] probabilities = model.distributionForInstance(instance);

        // Find confidence (max probability)
        double confidence = Arrays.stream(probabilities).max().orElse(0.5);
        boolean isHighConfidence = confidence >= MIN_CONFIDENCE_THRESHOLD;

        return new WekaResult(predictedCategory, confidence, isHighConfidence);
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
        WekaResult result = this.categorizeWithConfidence(transaction);
        return new ClassificationResult(result.predictedCategory(), result.confidence(), result.highConfidence());
    }

    @Override
    public List<ClassificationResult> classify(List<Transaction> transactions) throws Exception {
        return List.of();
    }

    private record WekaResult(String predictedCategory, double confidence, boolean highConfidence) {}
}
