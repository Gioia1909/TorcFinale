package scr;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import scr.Controller;

/**
 * @brief ManualDriver consente il controllo manuale dell'auto tramite tastiera
 *        e la registrazione dei dati su file CSV per addestramento supervisato.
 *        È possibile alternare tra modalità automatica e manuale, registrare i
 *        comandi,
 *        e salvare i sensori in tempo reale ogni 150ms.
 */
public class ManualDriver extends Controller {

    // Stato dei comandi manuali
    private boolean accel = false, brake = false, left = false, right = false;
    private boolean recording = false, manual = false, automatic = true;
    private float currentAccel = 0f, currentBrake = 0f, steering = 0f, clutch = 0f;
    private long lastSaveTime = 0;

    private static final long MIN_SAVE_INTERVAL_MS = 150;

    // Parametri della frizione
    final float clutchMax = 0.5f;
    final float clutchDelta = 0.05f;
    final float clutchRange = 0.82f;
    final float clutchDeltaTime = 0.02f;
    final float clutchDeltaRaced = 10f;
    final float clutchDec = 0.01f;
    final float clutchMaxModifier = 1.3f;
    final float clutchMaxTime = 1.5f;

    // Tabelle per cambio marcia automatico
    final int[] gearUp = { 5000, 6000, 6000, 6500, 7000, 0 };
    final int[] gearDown = { 0, 2500, 3000, 3000, 3500, 3500 };

    /**
     * @brief Costruttore. Inizializza il lettore di input da tastiera e associa i
     *        tasti ai comandi.
     */
    public ManualDriver() {
        ContinuousCharReader reader = new ContinuousCharReader();
        reader.addCharListener(new ContinuousCharReader.CharListener() {
            @Override
            public void charChanged(char ch, boolean pressed) {
                switch (ch) {
                    case 'w' -> accel = pressed;
                    case 's' -> brake = pressed;
                    case 'a' -> left = pressed;
                    case 'd' -> right = pressed;

                    case 'e' -> {
                        if (pressed) {
                            automatic = true;
                            manual = false;
                            System.out.println("Modalità automatica attivata");
                        }
                    }

                    case 'r' -> {
                        if (pressed) {
                            manual = true;
                            automatic = false;
                            System.out.println("Modalità manuale attivata");
                        }
                    }

                    case '1' -> {
                        if (pressed) {
                            recording = true;
                            System.out.println("Scrittura attivata");
                        }
                    }

                    case '0' -> {
                        if (pressed) {
                            recording = false;
                            System.out.println("Scrittura disattivata");
                        }
                    }

                    default -> {
                        if (pressed) {
                            System.out.println("Comando non riconosciuto: " + ch);
                        }
                    }
                }
            }
        });
    }

    /**
     * @brief Metodo principale di controllo. Genera l'azione da eseguire.
     * @param sensors Il modello sensoriale con i dati del veicolo
     * @return L'azione da eseguire in questo ciclo
     */
    @Override
    public Action control(SensorModel sensors) {
        Action action = new Action();
        updateState(sensors);
        action.accelerate = currentAccel;
        action.brake = currentBrake;
        action.steering = steering;
        action.gear = getGear(sensors);
        action.clutch = clutching(sensors, clutch);

        double speed = sensors.getSpeed();
        double speedY = sensors.getLateralSpeed();
        if (recording) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastSaveTime >= MIN_SAVE_INTERVAL_MS) {
                lastSaveTime = currentTime;
                try {
                    File file = new File("dataset_150ms.csv");
                    boolean fileExists = file.exists();
                    boolean fileIsEmpty = file.length() == 0;

                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
                        if (!fileExists || fileIsEmpty) {
                            bw.write(
                                    "Track2,Track5,Track8,Track9,Track10,Track13,Track16,TrackPosition,AngleToTrackAxis,Speed,SpeedY,Accelerate,Brake,Steering,Gear\n");
                        }
                        double[] trackSensors = sensors.getTrackEdgeSensors();
                        bw.write(
                                trackSensors[2] + "," +
                                        trackSensors[5] + "," +
                                        trackSensors[8] + "," +
                                        trackSensors[9] + "," +
                                        trackSensors[10] + "," +
                                        trackSensors[13] + "," +
                                        trackSensors[16] + "," +
                                        sensors.getTrackPosition() + "," +
                                        sensors.getAngleToTrackAxis() + "," +
                                        speed + "," +
                                        speedY + "," +
                                        action.accelerate + "," +
                                        action.brake + "," +
                                        action.steering + "," +
                                        action.gear + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return action;
    }

    /**
     * @brief Calcola il valore di frizione in base allo stato del veicolo.
     * @param sensors Il modello sensoriale
     * @param clutch  Il valore corrente della frizione
     * @return Il nuovo valore della frizione
     */
    private float clutching(SensorModel sensors, float clutch) {
        float maxClutch = clutchMax;

        if (sensors.getCurrentLapTime() < clutchDeltaTime && getStage() == Stage.RACE
                && sensors.getDistanceRaced() < clutchDeltaRaced) {
            clutch = maxClutch;
        }

        if (clutch > 0) {
            double delta = clutchDelta;
            if (sensors.getGear() < 2) {
                delta /= 2;
                maxClutch *= clutchMaxModifier;
                if (sensors.getCurrentLapTime() < clutchMaxTime)
                    clutch = maxClutch;
            }

            clutch = Math.min(maxClutch, clutch);

            if (clutch != maxClutch) {
                clutch -= delta;
                clutch = Math.max(0f, clutch);
            } else {
                clutch -= clutchDec;
            }
        }
        return clutch;
    }

    /**
     * @brief Calcola la marcia da usare (automatica).
     * @param sensors Il modello sensoriale
     * @return La marcia selezionata
     */
    private int getGear(SensorModel sensors) {
        int gear = sensors.getGear();
        double rpm = sensors.getRPM();

        if (manual)
            return -1;

        if (automatic) {
            if (gear < 1)
                return 1;
            if (gear < 6 && rpm >= gearUp[gear - 1])
                return gear + 1;
            if (gear > 1 && rpm <= gearDown[gear - 1])
                return gear - 1;
            return gear;
        }

        return 1;
    }

    /**
     * @brief Metodo chiamato alla fase di reset del controller.
     */
    @Override
    public void reset() {
        System.out.println("Reset!");
    }

    /**
     * @brief Metodo chiamato alla chiusura del controller.
     */
    @Override
    public void shutdown() {
        System.out.println("Shutdown!");
    }

    /**
     * @brief Inizializza gli angoli dei sensori di bordo pista.
     * @return Array di angoli da -90° a +90°
     */
    @Override
    public float[] initAngles() {
        return super.initAngles();
    }

    /**
     * @brief Aggiorna lo stato dei comandi sulla base della tastiera e della
     *        velocità.
     * @param sensor Il modello sensoriale
     */
    private void updateState(SensorModel sensor) {
        // Gestione acceleratore
        if (accel && !brake) {
            currentAccel = 1f;
            currentBrake = 0f;
        } else if (brake) {
            currentAccel = 0f;
            currentBrake = 0.6f;
        } else {// né accel né brake premuti
            currentAccel = 0f;
            currentBrake = 0f;
        }

        // Gestione sterzo
        if (left) {
            steering = (sensor.getSpeed() < 40) ? 0.6f : 0.3f;
        } else if (right) {
            steering = (sensor.getSpeed() < 40) ? -0.6f : -0.3f;
        } else {
            steering = 0f;
        }

    }

}