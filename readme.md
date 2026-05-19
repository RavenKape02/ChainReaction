# Chain Reaction

JavaFX 26 recreation of the Chain Reaction game for **Java 25 LTS**.

## Features

- Main menu with player-count and board-size selection
- Settings page for customizing the 8 player colors
- Game board for `9 x 6` and `15 x 10`
- Critical-mass explosion logic based on orthogonal neighbors
- Chain reactions that recolor neighboring cells to the exploding player
- Near-critical orb animation using `RotateTransition` and `ScaleTransition`
- Modular Java setup with `module-info.java`

## Project Layout

```text
ChainReaction/
  build.gradle              ← Gradle build (handles JavaFX automatically)
  settings.gradle
  setup.sh                  ← One-command Linux installer
  src/
    module-info.java
    app/
      Main.java
      logic/
      model/
      ui/
```

---

## Quick Start — Linux (Ubuntu 18.04 / 22.04)

**One command.** No Eclipse, no manual JavaFX setup, no VM-argument fiddling.

```bash
chmod +x setup.sh && ./setup.sh
```

What it does automatically:

1. Installs `curl`, `unzip`, `zip` via `apt`
2. Installs **SDKMAN** (if not present)
3. Installs **Java 25 LTS** via SDKMAN (or Adoptium fallback)
4. Installs **Gradle 8.14** and generates the Gradle wrapper
5. Downloads **JavaFX 26** as a managed dependency — no manual SDK download
6. Launches the game with `./gradlew run`

After the first run, you only need:

```bash
./gradlew run
```

---

## Prerequisites (manual setup)

1. Install **JDK 25**
2. Install **JavaFX 26 SDK**

## Eclipse Setup

### 1. Create an environment variable

- Name: `PATH_TO_FX`
- Value: path to the JavaFX `lib` folder

Example:

```text
C:\javafx-sdk-26\lib
```

### 2. Import the project

1. `File` -> `Import`
2. `General` -> `Existing Projects into Workspace`
3. Select this folder

### 3. Add JavaFX to the module path

1. Right-click project -> `Properties`
2. Open `Java Build Path`
3. Open `Libraries`
4. Select `Modulepath`
5. Add the JavaFX SDK JAR files from `%PATH_TO_FX%`

### 4. Run configuration

1. Open `Run Configurations...`
2. Run `app.Main`
3. In `Arguments`, use:

```text
--module-path ${env_var:PATH_TO_FX} --add-modules javafx.controls --enable-native-access=javafx.graphics
```

## Module File

The project uses this module declaration:

```java
module ChainReaction {
    requires javafx.controls;

    exports app;
}
```

## Notes

- The app is code-first JavaFX and does not use FXML.
- If Eclipse does not start the app, verify that JavaFX JARs are on the **Modulepath**, not the **Classpath**.
