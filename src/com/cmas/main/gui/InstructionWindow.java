package com.cmas.main.gui;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Timer;
import java.util.TimerTask;

public class InstructionWindow extends JDialog {

    private final JTextArea outputArea;
    private final JButton startButton;
    private final JLabel countdownLabel;
    private final TestFunction testFunction;

    public InstructionWindow(JFrame parent, String testName, String instructions, TestFunction testFunction) {
        super(parent, "Instructions – " + testName, true);
        this.testFunction = testFunction;

        setSize(1080, 400);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel(testName, SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));

        JTextArea instructionText = new JTextArea(instructions);
        instructionText.setWrapStyleWord(true);
        instructionText.setLineWrap(true);
        instructionText.setEditable(false);
        instructionText.setFont(new Font("SansSerif", Font.PLAIN, 16));
        instructionText.setBackground(getBackground());

        JScrollPane instructionScroll = new JScrollPane(instructionText);
        instructionScroll.setBorder(BorderFactory.createTitledBorder("Instructions"));

        countdownLabel = new JLabel("", SwingConstants.CENTER);
        countdownLabel.setFont(new Font("SansSerif", Font.BOLD, 20));

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(BorderFactory.createTitledBorder("Test Feedback"));

        startButton = new JButton("Start Test");
        startButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        startButton.addActionListener(e -> startTest());

        JPanel southPanel = new JPanel(new BorderLayout(10, 10));
        southPanel.add(countdownLabel, BorderLayout.NORTH);
        southPanel.add(startButton, BorderLayout.SOUTH);

        WebcamPanel webcamPanel = new WebcamPanel("http://localhost:8080/video_feed");
        webcamPanel.setPreferredSize(new Dimension(480, 360));

        add(title, BorderLayout.NORTH);
        add(instructionScroll, BorderLayout.WEST);
        add(outputScroll, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);
        add(webcamPanel, BorderLayout.EAST); // Or SOUTH or CENTER depending on layout

        redirectConsoleOutput();
    }

    private void startTest() {
        startButton.setEnabled(false);
        countdownLabel.setText("Starting in 5...");
        Timer timer = new Timer();
        final int[] secondsLeft = {5};

        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    secondsLeft[0]--;
                    if (secondsLeft[0] > 0) {
                        countdownLabel.setText("Starting in " + secondsLeft[0] + "...");
                    } else {
                        countdownLabel.setText("Test running...");
                        timer.cancel();

                        new Thread(() -> {
                            try {
                                int score = testFunction.run();
                                SwingUtilities.invokeLater(() -> {
                                    countdownLabel.setText("Test finished. Score: " + score);
                                });
                            } catch (Exception ex) {
                                SwingUtilities.invokeLater(() -> {
                                    countdownLabel.setText("Test failed: " + ex.getMessage());
                                });
                                ex.printStackTrace();
                            }
                        }).start();
                    }
                });
            }
        }, 1000, 1000);
    }

    private void redirectConsoleOutput() {
        PrintStream consoleStream = new PrintStream(new OutputStream() {
            public void write(int b) {
                outputArea.append(String.valueOf((char) b));
                outputArea.setCaretPosition(outputArea.getDocument().getLength());
            }
        });
        System.setOut(consoleStream);
        System.setErr(consoleStream);
    }
}