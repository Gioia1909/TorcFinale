package scr;

/**
 * @brief Controller di guida autonoma basato su KNN.
 *        Utilizza un classificatore KNN per predire le azioni di guida
 *        (accelerazione, frenata, sterzata)
 *        sulla base delle letture dei sensori, con logica di gestione per
 *        marcia, frizione e recupero da situazione di blocco.
 */
public class SimpleDriver extends Controller {

	private KNNClassifier classifier;
	/* Costanti di cambio marcia */
	private long lastGearChange = 0;
	private static final long MIN_GEAR_INTERVAL_MS = 1500; // 1,5 secondo tra i cambi di marcia
	final int[] gearUp = { 5000, 6000, 6000, 6500, 7000, 0 };
	final int[] gearDown = { 0, 2500, 3000, 3000, 3500, 3500 };

	/* Constanti */
	final int stuckTime = 25;
	final float stuckAngle = (float) 0.523598775; // PI/6

	/* Costanti di sterzata */
	final float steerLock = (float) 0.785398;

	final float clutchMax = (float) 0.5;
	final float clutchDelta = (float) 0.05;
	final float clutchRange = (float) 0.82;
	final float clutchDeltaTime = (float) 0.02;
	final float clutchDeltaRaced = 10;
	final float clutchDec = (float) 0.01;
	final float clutchMaxModifier = (float) 1.3;
	final float clutchMaxTime = (float) 1.5;

	private int stuck = 0;

	// current clutch
	private float clutch = 0;

	/**
	 * @brief Costruttore del SimpleDriver.
	 *        Inizializza il classificatore KNN utilizzando un dataset di
	 *        addestramento.
	 */
	public SimpleDriver() {
		classifier = new KNNClassifier("dataset_50ms.csv", 3);
	}

	/**
	 * @brief Metodo chiamato quando la gara viene resettata.
	 */
	public void reset() {
		System.out.println("Restarting the race!");

	}

	/**
	 * @brief Metodo chiamato alla chiusura del simulatore.
	 */
	public void shutdown() {
		System.out.println("Bye bye!");
	}

	/**
	 * @brief Determina la marcia da inserire in base al regime motore e al tempo
	 *        dall'ultimo cambio.
	 * @param sensors Sensori del veicolo
	 * @return Marcia selezionata
	 */
	private int getGear(SensorModel sensors) {
		long now = System.currentTimeMillis();
		int currentGear = sensors.getGear();
		double rpm = sensors.getRPM();

		if (currentGear < 1)
			return 1;

		if (now - lastGearChange < MIN_GEAR_INTERVAL_MS)
			return currentGear;

		if (currentGear < 6 && rpm >= gearUp[currentGear - 1]) {
			lastGearChange = now;
			return currentGear + 1;
		}

		if (currentGear > 1 && rpm <= gearDown[currentGear - 1]) {
			lastGearChange = now;
			return currentGear - 1;
		}

		return currentGear;
	}

	/**
	 * @brief Metodo di controllo principale eseguito ad ogni ciclo del simulatore.
	 *        La logica di recovery da situazione di blocco è stata mantenuta
	 *        (fallback), ma la parte
	 *        di predizione delle azioni è stata modificata per utilizzare un
	 *        classificatore KNN che predice le azioni di guida sulla base dei
	 *        sensori.
	 * @param sensors Il modello dei sensori del veicolo
	 * @return L'azione da eseguire
	 */
	public Action control(SensorModel sensors) {
		// Gestione recupero in caso di auto bloccata

		if (Math.abs(sensors.getAngleToTrackAxis()) > stuckAngle) {
			stuck++;
		} else {
			stuck = 0;
		}

		// lasciata la logica iniziale di stuck
		if (stuck > stuckTime) {
			// Recovery logic: retromarcia e sterzo per uscire dalla situazione di
			// difficoltà
			float steer = (float) (-sensors.getAngleToTrackAxis() / steerLock);
			int gear = -1;
			if (sensors.getAngleToTrackAxis() * sensors.getTrackPosition() > 0) {
				gear = 1;
				steer = -steer;
			}
			clutch = clutching(sensors, clutch);
			Action action = new Action();
			action.gear = gear;
			action.steering = steer;
			action.accelerate = 1.0f;
			action.brake = 0f;
			action.clutch = clutch;
			return action;
		}

		// Parte modificata
		double[] features = new double[11]; // basato su CSV manual driver (24 features)
		double[] trackSensors = sensors.getTrackEdgeSensors();

		features[0] = trackSensors[2];
		features[1] = trackSensors[5];
		features[2] = trackSensors[8];
		features[3] = trackSensors[9];
		features[4] = trackSensors[10];
		features[5] = trackSensors[13];
		features[6] = trackSensors[16];
		features[7] = sensors.getTrackPosition();
		features[8] = sensors.getAngleToTrackAxis();
		features[9] = sensors.getSpeed();
		features[10] = sensors.getLateralSpeed();

		// Creazione del sample e predizione
		Sample currentSample = new Sample(features, new double[4]);
		double[] prediction = classifier.predict(currentSample);

		// Costruzione dell'azione
		Action action = new Action();
		action.accelerate = (float) prediction[0];
		action.brake = (float) prediction[1];
		action.steering = (float) prediction[2];
		action.gear = getGear(sensors);
		action.clutch = clutching(sensors, clutch);
		return action;
	}

	/**
	 * @brief NON MODIFICATA, Calcola il valore della frizione da usare in base alla
	 *        situazione corrente.
	 * @param sensors Il modello dei sensori
	 * @param clutch  Valore attuale della frizione
	 * @return Nuovo valore della frizione
	 */
	float clutching(SensorModel sensors, float clutch) {

		float maxClutch = clutchMax;

		if (sensors.getCurrentLapTime() < clutchDeltaTime && getStage() == Stage.RACE
				&& sensors.getDistanceRaced() < clutchDeltaRaced)
			clutch = maxClutch;
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
				clutch = Math.max((float) 0.0, clutch);
			} else
				clutch -= clutchDec;
		}
		return clutch;
	}

	/**
	 * @brief NON MODIFICATA: Inizializza gli angoli dei sensori di bordo pista.
	 * @return Array con 19 angoli tra -90° e +90°
	 */

	public float[] initAngles() {

		float[] angles = new float[19];

		/*
		 * set angles as
		 * {-90,-75,-60,-45,-30,-20,-15,-10,-5,0,5,10,15,20,30,45,60,75,90}
		 */
		for (int i = 0; i < 5; i++) {
			angles[i] = -90 + i * 15;
			angles[18 - i] = 90 - i * 15;
		}

		for (int i = 5; i < 9; i++) {
			angles[i] = -20 + (i - 5) * 5;
			angles[18 - i] = 20 - (i - 5) * 5;
		}
		angles[9] = 0;
		return angles;
	}
}
