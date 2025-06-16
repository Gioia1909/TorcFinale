Progetto di Guida Autonoma – TORCS + KNN

Questo repository contiene tutto il materiale relativo al progetto di guida autonoma sviluppato per il Gran Premio MIVIA 2025, nell’ambito del corso di AI.
------

All’interno della cartella sono presenti i seguenti elementi richiesti:

1. Report tecnico sintetico ('RelazioneGruppo2.pdf')
Documento che descrive:
- Obiettivo del progetto
- Approccio alla progettazione
- Scelte implementative (KNN, selezione feature, normalizzazione, KDTree)
- Ottimizzazioni apportate
- Risultati sperimentali

------

2. Codice sorgente ('/src')
Implementazione in Java del sistema:
- 'SimpleDriver.java': driver di controllo del veicolo
- 'KNNClassifier.java': classificatore KNN con struttura KDTree
- 'Sample.java': oggetto campione per rappresentare ogni istanza del dataset
- 'KDTree.java': struttura per ricerca ottimizzata dei vicini
- 'ContinuousCharReader.java': Inserimento di caratteri da tastiera
- 'ManualDriver.java': scrittura sul dataset.csv ogni 50ms

Il dataset raccolto ('dataset_50ms.csv') e l’output delle predizioni ('predizioni_test.csv') sono inclusi per verifica e test.

-----

3. File 'README.md'
Il presente file contiene:
- Descrizione del progetto
- Istruzioni per l’esecuzione autonoma del codice
- Struttura del repository
- Requisiti tecnici

-----

4. Presentazione del progetto ('MIVIAPPTX.pdf')
Slides che illustrano:
- Obiettivo e contesto
- Architettura del sistema
- Strategie di addestramento
- Ottimizzazione delle feature
- Risultati e interpretazione

----

5. Video dimostrativo ('video.mp4')
Video che mostra il funzionamento del sistema in azione all’interno del simulatore TORCS, con veicolo che completa 3 giri in autonomia.

----

Esecuzione del sistema

Requisiti:
- Java 17 o superiore
- TORCS installato con supporto client/server
- File 'dataset.csv' disponibile

Avvio:

1. Posizionarsi nella directory principale del progetto.
2. Compilare i file Java:
   javac -cp . src/scr/*.java
3. Digitare nella shell il comando:
   java scr.Client scr.SimpleDriver host:localhost port:3001 verbose:on

