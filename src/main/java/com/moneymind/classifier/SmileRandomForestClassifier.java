package com.moneymind.classifier;

package com.moneymind.classfication.application;

import com.moneymind.classifier.domain.Transaction;
import com.moneymind.classifier.ports.TrainingDataService;
import smile.classification.RandomForest;
import smile.data.DataFrame;
import smile.data.type.DataTypes;
import smile.data.type.StructType;
import smile.feature.transform.BagOfWords;
import smile.feature.transform.Normalizer;
import smile.nlp.SimpleCorpus;
import smile.nlp.Text;
import smile.nlp.tokenizer.SimpleTokenizer;

import java.util.*;
import java.util.stream.IntStream;

public class SmileRandomForestClassifier {
    private static final int MIN_TRAINING_SAMPLES = 10;
    private static final double MIN_CONFIDENCE_THRESHOLD = 0.6;

    private final TrainingDataService trainingDataService;
    private RandomForest model;
    private BagOfWords bagOfWords;
    private Normalizer normalizer;
    private String[] categoryLabels;
    private Map<String, Integer> categoryToIndex;

    public SmileRandomForestClassifier(TrainingDataService trainingDataService) throws Exception {
        this.trainingDataService = trainingDataService;
        this.categoryToIndex = new HashMap<>();
        trainModel();
    }

    private void trainModel() throws Exception {
        List<Transaction> trainingData = trainingDataService.getTrainingSet();

        if (trainingData.size() < MIN_TRAINING_SAMPLES) {
            throw new IllegalStateException("Insufficient training data. Need at least " + MIN_TRAINING_SAMPLES + " samples");
        }

        // Prepare text data for bag of words
        String[] descriptions = trainingData.stream()
                .map(Transaction::description)
                .toArray(String[]::new);

        // Create bag of words from descriptions
        SimpleCorpus corpus = new SimpleCorpus();
        SimpleTokenizer tokenizer = new SimpleTokenizer(true); // Remove punctuation

        for (String description : descriptions) {
            Text text = new Text(description);
            text.tokens = tokenizer.split(description.toLowerCase());
            corpus.add(text);
        }

        // Build bag of words with TF-IDF
        this.bagOfWords = BagOfWords.fit(corpus, 500); // Top 500 features

        // Transform descriptions to feature vectors
        double[][] textFeatures = bagOfWords.apply(corpus);

        // Prepare numerical features (amount)
        double[][] amounts = trainingData.stream()
                .mapToDouble(t -> t.amount().doubleValue())
                .mapToObj(amount -> new double[]{amount})
                .toArray(double[][]::new);

        // Normalize amounts
        this.normalizer = Normalizer.fit(amounts);
        double[][] normalizedAmounts = normalizer.apply(amounts);

        // Combine features
        double[][] features = combineFeatures(textFeatures, normalizedAmounts);

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

        // Train Random Forest
        this.model = RandomForest.fit(
                features,
                labels,
                100,  // Number of trees
                20,   // Max features per split
                5,    // Min samples split
                2,    // Min samples leaf
                100,  // Max nodes
                0.632 // Subsample rate
        );
    }

    public ClassificationResult categorizeWithConfidence(PartialTransaction transaction) throws Exception {
        if (model == null) {
            throw new IllegalStateException("Model not trained");
        }

        // Transform description to features
        SimpleCorpus singleCorpus = new SimpleCorpus();
        SimpleTokenizer tokenizer = new SimpleTokenizer(true);

        Text text = new Text(transaction.description());
        text.tokens = tokenizer.split(transaction.description().toLowerCase());
        singleCorpus.add(text);

        double[][] textFeatures = bagOfWords.apply(singleCorpus);

        // Transform amount
        double[][] amount = {{transaction.amount().doubleValue()}};
        double[][] normalizedAmount = normalizer.apply(amount);

        // Combine features
        double[][] features = combineFeatures(textFeatures, normalizedAmount);
        double[] feature = features[0];

        // Get prediction
        int prediction = model.predict(feature);
        String predictedCategory = categoryLabels[prediction];

        // Get confidence scores (probability distribution)
        double[] probabilities = new double[categoryLabels.length];
        model.predict(feature, probabilities);

        double confidence = probabilities[prediction];

        // Get top predictions
        List<CategoryPrediction> topPredictions = getTopPredictions(probabilities, 3);

        return new ClassificationResult(
                predictedCategory,
                confidence,
                confidence >= MIN_CONFIDENCE_THRESHOLD,
                topPredictions
        );
    }

    private double[][] combineFeatures(double[][] textFeatures, double[][] amounts) {
        int numSamples = textFeatures.length;
        int textDim = textFeatures[0].length;
        int amountDim = amounts[0].length;

        double[][] combined = new double[numSamples][textDim + amountDim];

        for (int i = 0; i < numSamples; i++) {
            // Copy text features
            System.arraycopy(textFeatures[i], 0, combined[i], 0, textDim);
            // Copy amount features
            System.arraycopy(amounts[i], 0, combined[i], textDim, amountDim);
        }

        return combined;
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
