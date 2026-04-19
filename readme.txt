Name: Kylie Gilbert
Course: CPSC 4140 – HW-4 (Fitts' Law Simulator)
Term: Spring 2026

------------------------------------------------------------
Overview
------------------------------------------------------------
FittsLawFX is a JavaFX GUI application that conducts a Fitts' Law
experiment. The user clicks 50 circular targets of varying sizes
displayed at random positions. The app records each trial's data to
a CSV file (fitts_results.csv) in the working directory.

------------------------------------------------------------
Files Included
------------------------------------------------------------
- src/main/java/com/example/hw4/FittsLawFX.java   (main source)
- src/main/java/module-info.java
- pom.xml
- Makefile
- readme.txt
- mvnw / .mvn/

------------------------------------------------------------
How to Compile and Run (SoC Linux / ada)
------------------------------------------------------------
All commands run from the project root (same directory as Makefile).

If mvnw is not executable after unzipping:
   chmod +x mvnw

Available Makefile targets:

  make compile    – compile only
  make run        – compile and launch the GUI
  make clean      – remove build artifacts
  make rebuild    – clean then compile

------------------------------------------------------------
How to Use
------------------------------------------------------------
1. Launch with: make run
2. Select File → Go! to begin.
3. A 5-to-0 countdown is displayed.
4. Click each circular target as quickly as possible.
5. After 50 trials, a dialog confirms completion.
6. Results are saved to fitts_results.csv in the current directory.

CSV format:
  Trial Number, Target Size (pixels), Distance to Target (pixels), Time to Click (ms)

------------------------------------------------------------
Design Notes
------------------------------------------------------------
- Targets vary in radius from 15 to 80 pixels.
- Only the circle itself responds to clicks (not the background pane).
- Distance is the Euclidean distance between consecutive target centers.
- Trial 1 reports distance = 0 (no prior target exists).
- The CSV is flushed after every row to prevent data loss.
- All program exits are routed through a single controlled exit method.
