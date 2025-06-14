package scr;

import java.io.*;
import java.util.*;

/**
 * @brief KNNClassifier implementa un regressore basato su k-Nearest Neighbors
 *        (k-NN).
 *        Supporta normalizzazione delle feature, gestione di dataset da file
 *        CSV, e predizione di valori target continui.
 */
public class KNNClassifier {
    private List<Sample> trainingData;
    private KDTree kdtree;
    private int k;

    /** @brief Nomi delle feature usate nel modello */
    public static final String[] featureNames = {
            "Track2", "Track5", "Track8", "Track9", "Track10", "Track13", "Track16",
            "TrackPosition", "AngleToTrackAxis", "Speed", "SpeedY"
    };

    /** @brief Valori massimi per la normalizzazione delle feature */
    private static final double[] featureMaxValues = {
            200, 200, 200, 200, 200, 200, 200, 2, Math.PI, 300, 268
    };
    /** @brief Valori minimi dei target */
    private double[] targetMins;

    /** @brief Valori massimi dei target */
    private double[] targetMaxs;

    /**
     * @brief Costruttore principale che legge i dati da un file.
     * @param filename Percorso del file CSV contenente i dati
     * @param k        Numero di vicini da considerare nel KNN
     * @throws RuntimeException se il dataset è vuoto
     */
    public KNNClassifier(String filename, int k) {
        this.trainingData = new ArrayList<>();
        this.k = k;

        List<Sample> rawSamples = readRawSamples(filename);

        if (rawSamples.isEmpty()) {
            throw new RuntimeException("Dataset vuoto!");
        }
        // normalizzazione
        computeTargetMinMax(rawSamples);
        normalizeSamples(rawSamples);

        this.trainingData = rawSamples;
        this.kdtree = new KDTree(trainingData);

    }

    /**
     * @brief Costruttore alternativo che accetta direttamente dati già caricati.
     * @param trainingData Lista di campioni
     * @param k            Numero di vicini
     */
    public KNNClassifier(List<Sample> trainingData, int k) {
        this.k = k;
        // NORMALIZZAZIONE
        computeTargetMinMax(trainingData);
        normalizeSamples(trainingData);

        this.trainingData = new ArrayList<>(trainingData);
        this.kdtree = new KDTree(this.trainingData);
    }

    /**
     * @brief Legge i campioni dal file CSV.
     * @param filename Percorso del file CSV
     * @return Lista di campioni `Sample`
     */
    private List<Sample> readRawSamples(String filename) {
        List<Sample> rawSamples = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false; // salta intestazione
                    continue;
                }
                rawSamples.add(new Sample(line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rawSamples;
    }

    /**
     * @brief Trova i k vicini più vicini a un punto di test.
     * @param testPoint Punto di test
     * @return Lista di vicini `Sample`
     */
    private List<Sample> findKNearest(Sample testPoint) {
        return kdtree.kNearestNeighbors(testPoint, k);
    }

    /**
     * @brief Predice i valori target (accelerazione, frenata, sterzata) per
     *        un nuovo campione.
     * @param testPoint Campione di test (non normalizzato)
     * @return Array dei valori predetti [accelerate, brake, steering]
     */
    public double[] predict(Sample testPoint) {
        // NORMALIZZAZIONE
        double[] normalized = normalizeFeatures(testPoint.features.clone());
        testPoint.features = normalized;

        List<Sample> neighbors = findKNearest(testPoint);

        double[] result = new double[4]; // accelerazione, frenata, sterzata, marcia

        // Somma i target dei k vicini
        // (escludo l'ultimo target che è il gear)
        for (Sample s : neighbors) {
            for (int i = 0; i < (result.length - 1); i++) {
                result[i] += s.targets[i];
            }
        }

        // Media dei primi 3 target
        for (int i = 0; i < (result.length - 1); i++) {
            result[i] /= neighbors.size();
        }
        // NORMALIZZAZIONE
        double[] denormResult = denormalizeTargets(result);

        // NORMALIZZAZIONE
        return denormResult;
    }

    /**
     * @brief Calcola i valori minimi e massimi dei target.
     * @param samples Lista di campioni da analizzare
     */
    private void computeTargetMinMax(List<Sample> samples) {
        int numTargets = samples.get(0).targets.length; // Numero di target (accelerazione, frenata, sterzata, marcia)

        targetMins = new double[numTargets];
        targetMaxs = new double[numTargets];

        Arrays.fill(targetMins, Double.POSITIVE_INFINITY); // Inizializza i minimi a infinito
        Arrays.fill(targetMaxs, Double.NEGATIVE_INFINITY);

        for (Sample s : samples) {
            for (int i = 0; i < numTargets; i++) {
                double val = s.targets[i];
                if (val < targetMins[i])
                    targetMins[i] = val;
                if (val > targetMaxs[i])
                    targetMaxs[i] = val;
            }
        }
    }

    /**
     * @brief Normalizza le features e i target dei campioni forniti.
     * @param samples Lista di campioni da normalizzare
     */
    public void normalizeSamples(List<Sample> samples) {
        for (Sample s : samples) {
            s.features = normalizeFeatures(s.features); // Normalizza le features
            s.targets = normalizeTargets(s.targets); // Normalizza i target
        }
    }

    /**
     * @brief Normalizza un array di feature in base ai valori massimi predefiniti.
     * @param features Array delle feature
     * @return Array normalizzato
     */
    public double[] normalizeFeatures(double[] features) {
        if (features.length != featureNames.length) {
            throw new IllegalArgumentException("Mismatch: features.length = " + features.length
                    + " ma featureNames.length = " + featureNames.length);
        }

        double[] normalized = new double[features.length];
        for (int i = 0; i < features.length; i++) {
            if (featureMaxValues[i] == Math.PI) { // Normalizza l'angolo tra -pi e pi
                normalized[i] = (features[i] + Math.PI) / (2 * Math.PI);
            } else if (featureMaxValues[i] == 268) { // velocità laterale
                normalized[i] = (features[i] + 100.0) / 268.0;
            } else if (featureMaxValues[i] == 2) { // TrackPosition
                normalized[i] = (features[i] + 1.0) / 2.0; // Normalizza la posizione sulla pista tra -1 e 1
            } else {
                normalized[i] = features[i] / featureMaxValues[i]; // Normalizzazione generica feature / xmax
            }
        }
        return normalized;
    }

    /**
     * @brief Normalizza i target in un intervallo [0,1].
     * @param targets Array dei target originali
     * @return Array normalizzato
     */
    public double[] normalizeTargets(double[] targets) {
        double[] normalized = new double[targets.length];
        for (int i = 0; i < targets.length; i++) {
            if (targetMaxs[i] == targetMins[i]) {
                normalized[i] = 0; // Se min == max, il target è costante
            } else {
                normalized[i] = (targets[i] - targetMins[i]) / (targetMaxs[i] - targetMins[i]);
            }
        }
        return normalized;
    }

    /**
     * @brief Denormalizza i target riportandoli alla scala originale.
     * @param normalized Array normalizzato
     * @return Array denormalizzato
     */
    public double[] denormalizeTargets(double[] normalized) {
        double[] denormalized = new double[normalized.length];
        for (int i = 0; i < normalized.length; i++) {
            denormalized[i] = normalized[i] * (targetMaxs[i] - targetMins[i]) + targetMins[i];
        }
        return denormalized;
    }

}