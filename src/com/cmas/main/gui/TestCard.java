package com.cmas.main.gui;

import javax.swing.*;
import java.awt.*;

public class TestCard extends JPanel {
    private final JCheckBox checkBox;
    private final JButton actionButton;
    private final String label;
    private final TestFunction testMethod;

    public TestCard(String label, TestFunction testMethod) {
        this.label = label;
        this.testMethod = testMethod;

        setLayout(new BorderLayout(10, 10));
        setBackground(UIManager.getColor("Panel.background"));

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel nameLabel = new JLabel(label);
        nameLabel.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD, 16));

        checkBox = new JCheckBox();
        checkBox.setEnabled(false);
        checkBox.setOpaque(false);

        actionButton = new JButton("Start Test");
        actionButton.setFont(UIManager.getFont("Button.font"));
        actionButton.setFocusPainted(false);
        actionButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        actionButton.setOpaque(true);
        actionButton.setBorderPainted(false);
        actionButton.setBackground(new Color(60, 120, 200));
        actionButton.setForeground(Color.WHITE);

        // Hover effect
        actionButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                actionButton.setBackground(new Color(70, 140, 220));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                actionButton.setBackground(new Color(60, 120, 200));
            }
        });

        // New: open instruction window instead of starting test directly
        actionButton.addActionListener(e -> {
            InstructionWindow window = new InstructionWindow(
                    (JFrame) SwingUtilities.getWindowAncestor(this),
                    label,
                    getInstructionsForTest(label),
                    () -> runAndDisplayScore()
            );
            window.setVisible(true);
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(nameLabel, BorderLayout.WEST);
        topPanel.add(checkBox, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        add(actionButton, BorderLayout.SOUTH);
    }

    // This method will be passed to the InstructionWindow and run after the countdown
    private int runAndDisplayScore() throws Exception {
        int score = testMethod.run();
        SwingUtilities.invokeLater(() -> {
            checkBox.setSelected(true);
            actionButton.setText("Score: " + score);
            actionButton.setEnabled(false);
            actionButton.setBackground(new Color(200, 230, 200));
            actionButton.setForeground(Color.DARK_GRAY);
            setBackground(new Color(76, 175, 80));
        });
        return score;
    }

    // Instructions
    private String getInstructionsForTest(String label) {
        return switch (label) {
            case "1. Head Elevation" -> "Lie on your back with your arms at your side. When prompted, lift your head off the ground and hold it up.";
            case "2. Leg Raise" -> "While lying flat on your back, lift one leg straight up into the air as high as you can.";
            case "3. Leg Lift Duration" -> "Lift one leg while keeping it straight and hold it up for as long as you can without lowering.";
            case "4. Supine to Prone" -> "Roll over from lying on your back (supine) to lying on your stomach (prone) without assistance.";
            case "5. Sit-Ups" -> "From a lying position, perform six sit-ups in a row without pausing for too long between each.";
            case "6. Supine to Sit" -> "Lie on your back and then sit up fully, using as little assistance as possible.";
            case "7. Arm Raise / Straighten" -> "Raise your arms above your head and straighten them fully.";
            case "8. Arm Raise / Duration" -> "Raise your arms above your head and hold them there for as long as you can.";
            case "9. Floor Sit" -> "Transition from standing to sitting cross-legged or with legs extended on the floor.";
            case "10. All Fours Maneuver" -> "Get onto all fours (hands and knees), then raise your head and attempt a crawling motion.";
            case "11. Floor Rise" -> "From a sitting or lying position on the floor, rise up to a standing position using minimal support.";
            case "12. Chair Rise" -> "Stand up from a seated position in a chair without using your hands for support.";
            case "13. Stool Step" -> "Step up onto a low stool using one leg, showing balance and control.";
            case "14. Pick-Up Object" -> "Bend down and pick up a small object from the floor, such as a pen, using minimal support.";
            default -> "Follow the instructions provided to perform this test.";
        };
    }
}