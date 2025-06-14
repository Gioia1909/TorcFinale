/**
 * @file Sample.java
 * @brief Rappresenta un campione del dataset, contenente input dai sensori e output di controllo del veicolo.
 * 
 * La classe Sample è utilizzata per rappresentare i dati grezzi e normalizzati derivanti da un file CSV o da input diretti. 
 * Ogni campione contiene un array di feature (input dai sensori) e un array di target (accelerazione, frenata, sterzata, marcia).
 */

package scr;

/**
 * @class Sample
 * @brief Rappresenta un singolo campione del dataset, contenente input sensoriali e output delle azioni del veicolo.
 */
public class Sample {

    /** 
     * Array delle feature di input (es. sensori, posizione, velocità).
     */
    public double[] features;

    /** 
     * Array dei valori target (accelerazione, frenata, sterzata, marcia).
     */
    public double[] targets;

    /**
     * @brief Costruttore che crea un Sample a partire da una riga CSV.
     * 
     * Assume che le ultime 4 colonne siano i valori target: accelerazione, frenata, sterzata e marcia.
     * 
     * @param line Stringa CSV contenente feature e target separati da virgole.
     */
    public Sample(String line) {
        String[] parts = line.split(",");
        int n = parts.length;

        features = new double[n - 4]; ///< Feature: tutte le colonne tranne le ultime 4
        targets = new double[4]; ///< Target: ultime 4 colonne

        for (int i = 0; i < features.length; i++) {
            features[i] = Double.parseDouble(parts[i].trim());
        }

        targets[0] = Double.parseDouble(parts[n - 4].trim()); ///< Accelerazione
        targets[1] = Double.parseDouble(parts[n - 3].trim()); ///< Frenata
        targets[2] = Double.parseDouble(parts[n - 2].trim()); ///< Sterzata
        targets[3] = Double.parseDouble(parts[n - 1].trim()); ///< Marcia

    }

    /**
     * @brief Costruttore che crea un Sample a partire da array noti di feature e target.
     * 
     * @param features Array delle feature (input).
     * @param targets Array dei target (output: accelerate, brake, steer, gear).
     */
    public Sample(double[] features, double[] targets) {
        this.features = features;
        this.targets = targets;
    }

    /**
     * @brief Costruttore che crea un Sample solo con le feature.
     * 
     * Inizializza i target a zero. Utile quando i target non sono noti (es. dati di test).
     * 
     * @param features Array delle feature (input).
     */
    public Sample(double[] features) {
        this.features = features;
        this.targets = new double[4];
    }


    /**
     * @brief Calcola la distanza euclidea tra le feature di due Sample.
     * 
     * @param other L'altro sample da confrontare.
     * @return La distanza euclidea tra le feature di this e other.
     */
    public double distance(Sample other) {
        double sum = 0;
        for (int i = 0; i < this.features.length; i++) {
            sum += Math.pow(this.features[i] - other.features[i], 2);
        }
        return Math.sqrt(sum);
    }
}