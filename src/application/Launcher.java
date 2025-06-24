package application;

import javafx.application.Application;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * Launcher class to avoid module system issues with JavaFX
 * This class should be set as the main class in your JAR manifest
 */
public class Launcher {
    public static void main(String[] args) {
    	  suppressJavaFXWarnings();
        // Launch the actual JavaFX Application
        // This approach helps bypass module system restrictions
        Application.launch(Main.class, args);
    }
    
    private static void suppressJavaFXWarnings() {
        // Suppress the specific JavaFX warning about unnamed modules
        Logger.getLogger("com.sun.javafx.application.PlatformImpl").setLevel(Level.SEVERE);
        Logger.getLogger("com.sun.javafx").setLevel(Level.SEVERE);
        Logger.getLogger("javafx").setLevel(Level.SEVERE);
        
        // Alternative: Set system properties
        System.setProperty("prism.verbose", "false");
        System.setProperty("javafx.verbose", "false");
    }
}