package com.cmas.main.gui.auth;

import com.cmas.main.dao.DatabaseController;
import com.cmas.main.gui.doctor.DoctorPanel;
import com.cmas.main.gui.patient.CMASDashboard;
import com.cmas.main.imageProcessing.PythonServerController;
import com.formdev.flatlaf.FlatLightLaf;

import java.sql.SQLException;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class LoginPanel extends JFrame {
    private static final String DOCTOR_USERNAME = "doctor";
    private static final String DOCTOR_PASSWORD = "password123";
    DatabaseController db = new DatabaseController();

    public LoginPanel() {
        setTitle("CMAS Login Portal");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Doctor Login", createDoctorLoginTab());
        tabbedPane.addTab("Patient Login", createPatientLoginTab());

        add(tabbedPane, BorderLayout.CENTER);
        setVisible(true);
    }

    private JPanel createDoctorLoginTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel userLabel = new JLabel("Username:");
        JTextField userField = new JTextField(15);

        JLabel passLabel = new JLabel("Password:");
        JPasswordField passField = new JPasswordField(15);

        JButton loginButton = new JButton("Login");

        JLabel statusLabel = new JLabel("");
        statusLabel.setForeground(Color.RED);

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(userLabel, gbc);
        gbc.gridx = 1;
        panel.add(userField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(passLabel, gbc);
        gbc.gridx = 1;
        panel.add(passField, gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        panel.add(loginButton, gbc);

        gbc.gridx = 1; gbc.gridy = 3;
        panel.add(statusLabel, gbc);

        loginButton.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());

            if (username.equals(DOCTOR_USERNAME) && password.equals(DOCTOR_PASSWORD)) {
                dispose();
                new DoctorPanel(new DatabaseController());
            } else {
                statusLabel.setText("Invalid credentials.");
            }
        });

        return panel;
    }

    private JPanel createPatientLoginTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel nameLabel = new JLabel("Your Name:");
        JTextField nameField = new JTextField(15);
        JButton loginButton = new JButton("Login");

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(nameLabel, gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(loginButton, gbc);

        loginButton.addActionListener(e -> {
            String enteredName = nameField.getText().trim();
            if (enteredName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter your name.");
                return;
            }

            try {
                List<String[]> matches = db.getMatchingPatientsByName(enteredName); // [PatientID, Name]

                if (matches.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No patient found with that name.");
                } else if (matches.size() == 1) {
                    String patientId = matches.get(0)[0];
                    JOptionPane.showMessageDialog(this, "Welcome " + enteredName + "! Logged in as " + patientId);
                    try {
                        FlatLightLaf.setup();
                        UIManager.put("Button.arc", 20);
                        UIManager.put("Component.arc", 10);
                        UIManager.put("CheckBox.icon.selectedBackground", new Color(60, 120, 200));
                        UIManager.put("CheckBox.icon.checkmarkColor", Color.WHITE);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    PythonServerController.startServer();

                    SwingUtilities.invokeLater(() -> {
                        try {
                            CMASDashboard dashboard = new CMASDashboard(patientId);
                            dashboard.setVisible(true);
                        } catch (SQLException e1) {
                            throw new RuntimeException(e1);
                        }
                    });

                    dispose();
                } else {
                    // Multiple matches: ask user to select
                    String[] options = matches.stream()
                            .map(arr -> arr[1] + " (ID: " + arr[0] + ")")
                            .toArray(String[]::new);

                    String selected = (String) JOptionPane.showInputDialog(
                            this,
                            "Multiple patients found. Please select:",
                            "Select Patient",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            options,
                            options[0]);

                    if (selected != null) {
                        String selectedId = selected.substring(selected.indexOf("ID: ") + 4, selected.length() - 1);
                        JOptionPane.showMessageDialog(this, "Welcome! Logged in as patient " + selectedId);
                        // new PatientPanel(selectedId);
                        // dispose();
                    }
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error accessing database: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        return panel;
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
                new LoginPanel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
