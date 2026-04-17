# JavaFX 26 Project (Java 25)

This is a baseline JavaFX project configured for **Java 25** and **JavaFX 26**.

## Project Layout
ChainReaction/ src/ module-info.java app/ Main.java bin/

## Prerequisites

1. **JDK 25**  
   Download: [https://jdk.java.net/25/](https://jdk.java.net/25/)

2. **JavaFX 26 SDK**  
   Download: [https://jdk.java.net/javafx26/](https://jdk.java.net/javafx26/)

## Local Setup (Eclipse)

### 1) Define Environment Variable

Create a system environment variable so paths are not hardcoded:

- **Variable Name:** `PATH_TO_FX`
- **Variable Value:** full path to JavaFX SDK `lib` directory  
  Example: `C:\javafx-sdk-26\lib`

### 2) Import Project

1. `File` -> `Import` -> `General` -> `Existing Projects into Workspace`
2. Select this project folder and import it.

### 3) Add JavaFX Libraries on Modulepath

1. Right-click project -> `Properties` -> `Java Build Path`
2. Open `Libraries` tab
3. Select `Modulepath` -> `Add Library...`
4. Choose `User Library` -> `New...` -> name it `JavaFX26`
5. Click `Add External JARs...` and select all JAR files in `%PATH_TO_FX%`
6. `Apply and Close`

### 4) Run Configuration

1. `Run` -> `Run Configurations...`
2. Select your Java application (`app.Main`)
3. Open `Arguments` tab
4. Paste this into **VM arguments**:

