package com.cmas.main.imageProcessing;

import java.io.IOException;

public class PythonServerController {
    private static Process pythonProcess;

    public static void startServer() {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "python3", "src/com/cmas/main/imageProcessing/VideoProcessing.py"
            );
            builder.redirectErrorStream(true);
            pythonProcess = builder.start();
            System.out.println("Python server started.");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to start Python server.");
        }
    }

    public static void stopServer() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            pythonProcess.destroy();
            System.out.println("Python server stopped.");
        }
    }
}