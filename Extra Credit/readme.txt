Name: Kylie Gilbert
Course: CPSC 4140 – HW-4 Extra Credit (Fitts' Law Simulator + Kids Game)
Term: Spring 2026

------------------------------------------------------------
Overview
------------------------------------------------------------
FittsLawTabApp extends the HW-4 simulator with a TabPane containing:

  Tab 1 – Experiment: standard Fitts' Law circles, clinical UI
  Tab 2 – Kids' Game ⭐: star targets, colorful UI, bounce animations

Both tabs produce independent CSV files and share the same top bar.

------------------------------------------------------------
Project Structure
------------------------------------------------------------
src/main/java/
  funshapes/app/        FittsLawTabApp.java           (entry point)
  funshapes/controller/ ExperimentController.java      (standard trials)
                        KidsExperimentController.java  (kids trials)
  funshapes/model/      AppModel.java
  funshapes/shapes/     CircleSpawner.java
                        KidsTargetSpawner.java         (star targets)
                        ShapeSpawner.java
                        SpawnContext.java
  module-info.java
pom.xml / Makefile / mvnw

------------------------------------------------------------
How to Compile and Run (SoC Linux / ada)
------------------------------------------------------------
From the Extra Credit folder:

  chmod +x mvnw
  make run

------------------------------------------------------------
How to Use
------------------------------------------------------------
1. make run — opens the tabbed window
2. Select a tab (Experiment or Kids' Game)
3. Press Go! to begin that tab's experiment
4. Click targets as quickly as possible
5. After 50 trials, results are saved and Go! re-enables

CSV files:
  fitts_results.csv       (Experiment tab)
  fitts_results_kids.csv  (Kids' Game tab)

------------------------------------------------------------
Kids Game Design Notes
------------------------------------------------------------
- Star-shaped targets with emoji labels (⭐ 🌟 💫 🎯 etc.)
- Bright, child-friendly color palette (coral, yellow, green, pink…)
- Pop-in bounce animation (scale 0→1, 180ms) on each spawn
- Warm yellow drawing pane background (#FFF9C4)
- Large bold font and colorful status labels
- Celebratory completion dialog with emoji
- Timing starts AFTER the bounce animation completes (fair measurement)
- Same CSV format and 50-trial structure as the standard tab
