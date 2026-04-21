Name: Kylie Gilbert
Course: CPSC 4140 – HW-4 (Fitts' Law Simulator)
Term: Spring 2026

------------------------------------------------------------
Overview
------------------------------------------------------------
FittsLawApp is a JavaFX GUI that runs a 50-trial Fitts' Law
experiment. The user clicks circular targets of varying sizes at
random positions. Data is written to fitts_results.csv.

------------------------------------------------------------
Project Structure
------------------------------------------------------------
src/main/java/
  funshapes/app/        FittsLawApp.java          (entry point)
  funshapes/controller/ ExperimentController.java  (trial loop, CSV)
  funshapes/model/      AppModel.java              (RNG, palette)
  funshapes/shapes/     CircleSpawner.java
                        ShapeSpawner.java
                        SpawnContext.java
  module-info.java
Extra Credit/           (see that folder's readme.txt)
pom.xml / Makefile / mvnw

------------------------------------------------------------
How to Compile and Run (SoC Linux / ada)
------------------------------------------------------------
From the project root (same directory as Makefile):

  chmod +x mvnw          # if needed after unzip
  make compile           # compile
  make run               # compile and launch
  make clean             # remove build artifacts
  make rebuild           # clean then compile

------------------------------------------------------------
How to Use
------------------------------------------------------------
1. make run
2. Press Go! (or Ctrl+G) to begin.
3. Watch the 5-to-0 countdown.
4. Click each circular target as quickly as possible.
5. After 50 trials a dialog confirms completion.
6. CSV data: fitts_results.csv (in the working directory)

CSV format:
  Trial Number, Target Size (pixels), Distance (pixels), Time (ms)

------------------------------------------------------------
Design Notes
------------------------------------------------------------
- HBox top bar: Go! and Quit! buttons (no menu bar)
- Window X button / minimize work as normal (OS decorations untouched)
- Right panel shows live trial status bound to controller property
- Target radius: random 15–80px per trial
- Only the circle target responds to clicks (background ignored)
- Distance = Euclidean distance between consecutive target centers
- Trial 1 distance = 0 (no prior target)
- CSV flushed per row to prevent data loss on early exit
- Go! disabled mid-session; re-enabled after completion for repeat runs
- Ctrl+G = Go!, Ctrl+Q = Quit
