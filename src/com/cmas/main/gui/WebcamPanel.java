package com.cmas.main.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class WebcamPanel extends JPanel {
    private final JLabel imageLabel = new JLabel("Loading...", SwingConstants.CENTER);
    private final AtomicReference<BufferedImage> latestFrame = new AtomicReference<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private Thread readerThread;
    private Timer updateTimer;

    public WebcamPanel(String streamUrl) {
        setLayout(new BorderLayout());
        add(imageLabel, BorderLayout.CENTER);

        startReadingStream(streamUrl);
        startUIUpdater();
    }

    private void startReadingStream(String streamUrl) {
        readerThread = new Thread(() -> {
            try {
                URL url = new URL(streamUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(5000);
                connection.connect();
                InputStream stream = connection.getInputStream();

                ByteArrayOutputStream jpegBuffer = new ByteArrayOutputStream();
                boolean recording = false;

                while (running.get()) {
                    int b = stream.read();
                    if (b == -1) break;

                    if (!recording && b == 0xFF) {
                        int b2 = stream.read();
                        if (b2 == 0xD8) {
                            jpegBuffer.reset();
                            jpegBuffer.write(0xFF);
                            jpegBuffer.write(0xD8);
                            recording = true;
                            continue;
                        }
                    }

                    if (recording) {
                        jpegBuffer.write(b);

                        if (b == 0xFF) {
                            int b2 = stream.read();
                            jpegBuffer.write(b2);
                            if (b2 == 0xD9) {
                                recording = false;
                                byte[] jpegBytes = jpegBuffer.toByteArray();
                                BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(jpegBytes));
                                if (img != null) {
                                    latestFrame.set(img);
                                }
                            }
                        }
                    }
                }

                stream.close();
            } catch (Exception e) {
                if (running.get()) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> imageLabel.setText("Unable to load camera stream."));
                }
            }
        }, "MJPEG Reader");

        readerThread.start();
    }

    private void startUIUpdater() {
        updateTimer = new Timer(33, e -> {
            BufferedImage frame = latestFrame.getAndSet(null);
            if (frame != null) {
                Image scaled = frame.getScaledInstance(480, 360, Image.SCALE_FAST);
                imageLabel.setIcon(new ImageIcon(scaled));
                imageLabel.setText("");
            }
        });
        updateTimer.start();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        shutdown();
    }

    public void shutdown() {
        running.set(false);
        if (updateTimer != null) {
            updateTimer.stop();
        }
        if (readerThread != null && readerThread.isAlive()) {
            try {
                readerThread.join(1000); // wait max 1 sec
            } catch (InterruptedException ignored) {
            }
        }
    }
}