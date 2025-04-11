package com.cmas.main.gui.doctor;

import com.cmas.main.dao.DatabaseController;
import com.formdev.flatlaf.FlatLightLaf;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class DoctorPanel extends JFrame {
    private final DatabaseController controller;

    public DoctorPanel(DatabaseController controller) {
        this.controller = controller;
        FlatLightLaf.setup(); // Apply FlatLaf theme

        setTitle("Doctor Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Patient Overview", createPatientSummaryPanel());
        tabbedPane.addTab("Register Patient", createPatientRegistrationPanel());
        tabbedPane.addTab("Assign Patient Information", createAssignLabGroupPanel());
        tabbedPane.addTab("Register Measurement", createMeasurementEntryPanel());
        tabbedPane.addTab("CMAS Results", createCmasViewerPanel());

        add(tabbedPane);
        setVisible(true);
    }

    private JPanel createPatientSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        Font textFont = new Font("JetBrains Mono", Font.PLAIN, 13);

        JTextField patientField = new JTextField(15);
        patientField.setFont(textFont);

        JCheckBox detailedCheckbox = new JCheckBox("Show IDs");
        JButton searchButton = new JButton("Search");
        searchButton.setFont(new Font("SansSerif", Font.BOLD, 13));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.add(new JLabel("Patient Name:"));
        topPanel.add(patientField);
        topPanel.add(searchButton);
        topPanel.add(detailedCheckbox);

        JTextArea outputArea = new JTextArea();
        outputArea.setFont(textFont);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setEditable(false);
        outputArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane textScroll = new JScrollPane(outputArea);
        textScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel chartContainer = new JPanel();
        chartContainer.setLayout(new BoxLayout(chartContainer, BoxLayout.Y_AXIS));
        chartContainer.setBackground(Color.WHITE);
        chartContainer.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JPanel resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.add(textScroll);
        resultsPanel.add(chartContainer);

        JScrollPane finalScroll = new JScrollPane(resultsPanel);
        finalScroll.getVerticalScrollBar().setUnitIncrement(16);

        searchButton.addActionListener(e -> {
            String inputName = patientField.getText().trim();
            boolean detailed = detailedCheckbox.isSelected();
            outputArea.setText("");
            chartContainer.removeAll();

            try {
                List<Map<String, String>> matches = controller.findPatientsByName(inputName);

                if (matches.isEmpty()) {
                    outputArea.setText("No patient found with that name.");
                    return;
                }

                String selectedPatientId;
                String selectedGroup = "Unknown";

                if (matches.size() == 1) {
                    selectedPatientId = matches.get(0).get("id");
                    selectedGroup = matches.get(0).getOrDefault("group", "Unknown");
                } else {
                    String[] options = matches.stream()
                            .map(p -> String.format("Name: %s | ID: %s | Group: %s",
                                    p.get("name"), p.get("id"), p.getOrDefault("group", "Unknown")))
                            .toArray(String[]::new);

                    String selection = (String) JOptionPane.showInputDialog(
                            this,
                            "Multiple patients found. Select one:",
                            "Select Patient",
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            options,
                            options[0]);

                    if (selection == null) return;

                    selectedPatientId = selection.substring(selection.indexOf("ID: ") + 4, selection.indexOf(" | Group")).trim();
                    selectedGroup = selection.substring(selection.indexOf("Group: ") + 7).trim();
                }

                Map<String, Object> data = controller.getFullPatientOverview(selectedPatientId, detailed);
                String name = (String) data.get("name");

                outputArea.append("Patient ID: " + selectedPatientId + "\n");
                outputArea.append("Patient Name: " + (name.isEmpty() ? "(not found)" : name) + "\n");
                outputArea.append("Assigned Group: " + selectedGroup + "\n\n");

                List<Map<String, String>> meds = (List<Map<String, String>>) data.get("medications");
                Map<String, String> groupMap = (Map<String, String>) data.get("groupNames");

                if (meds.isEmpty()) {
                    outputArea.append("No measurement data found.\n");
                } else {
                    outputArea.append("Measurement Records:\n");

                    Map<String, List<Map<String, String>>> grouped = new HashMap<>();
                    for (Map<String, String> entry : meds) {
                        String nameKey = entry.get("name");
                        grouped.computeIfAbsent(nameKey, k -> new ArrayList<>()).add(entry);
                    }

                    for (Map.Entry<String, List<Map<String, String>>> resultEntry : grouped.entrySet()) {
                        List<Map<String, String>> entries = resultEntry.getValue();
                        String nameKey = resultEntry.getKey();

                        for (Map<String, String> entry : entries) {
                            String groupId = entry.get("groupId");
                            String groupName = groupMap.getOrDefault(groupId, "Unknown Group");

                            String line = String.format(
                                    "- [%s] %s: %s %s (Group: %s)",
                                    entry.get("datetime"),
                                    entry.get("name"),
                                    entry.get("value"),
                                    entry.get("unit") != null ? entry.get("unit") : "",
                                    groupName
                            );

                            if (detailed) {
                                line += String.format(" | LabResultID: %s | GroupID: %s",
                                        entry.get("labResultId"),
                                        groupId != null ? groupId : "N/A");
                            }

                            outputArea.append(line + "\n");
                        }

                        if (entries.size() > 1) {
                            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                            String unit = entries.get(0).get("unit");

                            for (Map<String, String> point : entries) {
                                try {
                                    double value = Double.parseDouble(point.get("value"));
                                    String date = point.get("datetime");
                                    dataset.addValue(value, nameKey, date);
                                } catch (NumberFormatException ignored) {}
                            }

                            JFreeChart chart = ChartFactory.createLineChart(
                                    nameKey + (unit != null ? " (" + unit + ")" : ""),
                                    "Date",
                                    "Value",
                                    dataset
                            );

                            CategoryPlot plot = chart.getCategoryPlot();
                            LineAndShapeRenderer renderer = new LineAndShapeRenderer();
                            renderer.setDefaultShapesVisible(true);
                            renderer.setDefaultLinesVisible(true);
                            renderer.setDefaultShape(new java.awt.geom.Ellipse2D.Double(-3.0, -3.0, 6.0, 6.0));
                            renderer.setDefaultPaint(Color.BLUE);
                            plot.setRenderer(renderer);

                            ChartPanel chartPanel = new ChartPanel(chart);
                            chartPanel.setPreferredSize(new Dimension(800, 300));
                            chartPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                            chartContainer.add(chartPanel);
                        }
                    }
                }

                chartContainer.revalidate();
                chartContainer.repaint();

            } catch (SQLException ex) {
                outputArea.setText("Error retrieving patient data:\n" + ex.getMessage());
                ex.printStackTrace();
            }
        });

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(finalScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createPatientRegistrationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);

        Font labelFont = new Font("SansSerif", Font.BOLD, 14);
        Font inputFont = new Font("JetBrains Mono", Font.PLAIN, 13);

        JLabel nameLabel = new JLabel("Enter Patient Name:");
        nameLabel.setFont(labelFont);

        JTextField nameField = new JTextField(20);
        nameField.setFont(inputFont);

        JButton registerButton = new JButton("Register Patient");
        registerButton.setFont(new Font("SansSerif", Font.BOLD, 14));

        JTextArea outputArea = new JTextArea(4, 30);
        outputArea.setFont(inputFont);
        outputArea.setEditable(false);
        outputArea.setWrapStyleWord(true);
        outputArea.setLineWrap(true);
        outputArea.setMargin(new Insets(10, 10, 10, 10));

        registerButton.addActionListener(e -> {
            String name = nameField.getText().trim();

            if (name.isEmpty()) {
                outputArea.setText("Please enter a patient name.");
                return;
            }

            try {
                String patientId = UUID.randomUUID().toString();
                controller.insertPatient(patientId, name);
                outputArea.setText("Patient registered successfully.\n\n" +
                        "Name: " + name + "\nPatient ID: " + patientId);
                nameField.setText("");
            } catch (Exception ex) {
                outputArea.setText("Error registering patient:\n" + ex.getMessage());
                ex.printStackTrace();
            }
        });

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.add(nameLabel);
        inputPanel.add(nameField);
        inputPanel.add(registerButton);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createAssignLabGroupPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);

        Font labelFont = new Font("SansSerif", Font.BOLD, 14);
        Font inputFont = new Font("JetBrains Mono", Font.PLAIN, 13);

        JTextField nameField = new JTextField(20);
        JComboBox<String> patientSelector = new JComboBox<>();
        JComboBox<String> groupSelector = new JComboBox<>();

        JTextField resultNameField = new JTextField(20);
        JTextField resultEnglishField = new JTextField(20);
        JTextField unitField = new JTextField(20);

        JTextArea outputArea = new JTextArea(6, 40);
        outputArea.setFont(inputFont);
        outputArea.setEditable(false);
        outputArea.setMargin(new Insets(10, 10, 10, 10));

        JButton searchBtn = new JButton("Find Patients");
        JButton assignBtn = new JButton("Assign Group");

        // Load group data
        Map<String, String> groupMap = new HashMap<>();
        try {
            List<Map<String, String>> groups = controller.getAllLabResultGroups();
            for (Map<String, String> group : groups) {
                String display = group.get("name") + " (" + group.get("id") + ")";
                groupSelector.addItem(display);
                groupMap.put(display, group.get("id"));
            }
        } catch (Exception e) {
            outputArea.setText("Error loading lab result groups: " + e.getMessage());
            e.printStackTrace();
        }

        // Find matching patients by name
        searchBtn.addActionListener(e -> {
            patientSelector.removeAllItems();
            String name = nameField.getText().trim();
            try {
                List<Map<String, String>> matches = controller.findPatientsByName(name);
                if (matches.isEmpty()) {
                    outputArea.setText("No patients found with that name.");
                } else {
                    for (Map<String, String> p : matches) {
                        String label = p.get("name") + " (" + p.get("id") + ")";
                        patientSelector.addItem(label);
                    }
                    outputArea.setText("Select the correct patient from the dropdown.");
                }
            } catch (Exception ex) {
                outputArea.setText("Error finding patients: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        // Assign group to patient
        assignBtn.addActionListener(e -> {
            if (patientSelector.getSelectedItem() == null || groupSelector.getSelectedItem() == null) {
                outputArea.setText("Please select both a patient and a group.");
                return;
            }

            try {
                String patientItem = (String) patientSelector.getSelectedItem();
                String patientId = patientItem.substring(patientItem.indexOf("(") + 1, patientItem.indexOf(")"));

                String groupItem = (String) groupSelector.getSelectedItem();
                String groupId = groupMap.get(groupItem);

                String resultName = resultNameField.getText().trim();
                String resultEng = resultEnglishField.getText().trim();
                String unit = unitField.getText().trim();
                if (resultName.isEmpty() || resultEng.isEmpty()) {
                    outputArea.setText("Result name and English name are required.");
                    return;
                }

                String labResultId = UUID.randomUUID().toString();
                controller.insertLabResultEN(labResultId, groupId, patientId, resultName, resultEng, unit.isEmpty() ? null : unit);

                outputArea.setText("Patient assigned successfully:\nPatientID: " + patientId +
                        "\nGroupID: " + groupId + "\nLabResultID: " + labResultId);

                resultNameField.setText("");
                resultEnglishField.setText("");
                unitField.setText("");

            } catch (Exception ex) {
                outputArea.setText("Error assigning group: " + ex.getMessage());
                ex.printStackTrace();
            }
        });


        // Layout
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Patient Name:"));
        top.add(nameField);
        top.add(searchBtn);
        top.add(patientSelector);

        JPanel mid = new JPanel(new GridLayout(4, 2, 10, 10));
        mid.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        mid.add(new JLabel("Select Group:"));
        mid.add(groupSelector);
        mid.add(new JLabel("Result Name:"));
        mid.add(resultNameField);
        mid.add(new JLabel("Result Name (English):"));
        mid.add(resultEnglishField);
        mid.add(new JLabel("Unit (optional):"));
        mid.add(unitField);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(assignBtn);

        JPanel bottomCombined = new JPanel(new BorderLayout());
        bottomCombined.add(bottom, BorderLayout.NORTH);
        bottomCombined.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        panel.add(top, BorderLayout.NORTH);
        panel.add(mid, BorderLayout.CENTER);
        panel.add(bottomCombined, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createMeasurementEntryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);

        Font inputFont = new Font("JetBrains Mono", Font.PLAIN, 13);

        JTextField nameField = new JTextField(20);
        JComboBox<String> patientSelector = new JComboBox<>();
        JTextField valueField = new JTextField(15);
        JTextField datetimeField = new JTextField(20);
        JCheckBox nowCheckbox = new JCheckBox("Use current date & time");
        JButton searchBtn = new JButton("Find Patients");
        JButton registerBtn = new JButton("Register Measurement");

        JTextArea outputArea = new JTextArea(5, 40);
        outputArea.setFont(inputFont);
        outputArea.setEditable(false);
        outputArea.setMargin(new Insets(10, 10, 10, 10));

        // Disable manual time entry if checkbox is active
        nowCheckbox.addActionListener(e -> {
            datetimeField.setEnabled(!nowCheckbox.isSelected());
            if (nowCheckbox.isSelected()) {
                datetimeField.setText("");
            }
        });

        // Load patients from name
        searchBtn.addActionListener(e -> {
            patientSelector.removeAllItems();
            String name = nameField.getText().trim();
            try {
                List<Map<String, String>> matches = controller.findPatientsByName(name);
                if (matches.isEmpty()) {
                    outputArea.setText("No patients found.");
                } else {
                    for (Map<String, String> p : matches) {
                        patientSelector.addItem(p.get("name") + " (" + p.get("id") + ")");
                    }
                    outputArea.setText("Select the correct patient from the dropdown.");
                }
            } catch (Exception ex) {
                outputArea.setText("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        // Register measurement
        registerBtn.addActionListener(e -> {
            if (patientSelector.getSelectedItem() == null) {
                outputArea.setText("No patient selected.");
                return;
            }

            String patientItem = (String) patientSelector.getSelectedItem();
            String patientId = patientItem.substring(patientItem.indexOf("(") + 1, patientItem.indexOf(")"));
            String value = valueField.getText().trim();
            if (value.isEmpty()) {
                outputArea.setText("Measurement value cannot be empty.");
                return;
            }

            String datetime;
            if (nowCheckbox.isSelected()) {
                datetime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyyHH:mm"));
            } else {
                datetime = datetimeField.getText().trim();
                if (datetime.isEmpty()) {
                    outputArea.setText("Please enter a date and time.");
                    return;
                }
            }

            try {
                String labResultId = controller.getFirstLabResultIdForPatient(patientId);
                if (labResultId == null) {
                    outputArea.setText("No LabResult found for this patient.");
                    return;
                }

                String measurementId = UUID.randomUUID().toString();
                controller.insertMeasurement(measurementId, labResultId, datetime, value);

                outputArea.setText("Measurement successfully registered.\n" +
                        "MeasurementID: " + measurementId +
                        "\nLabResultID: " + labResultId +
                        "\nDateTime: " + datetime +
                        "\nValue: " + value);

                valueField.setText("");
                datetimeField.setText("");

            } catch (Exception ex) {
                outputArea.setText("Error saving measurement: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        // Layout
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Patient Name:"));
        searchPanel.add(nameField);
        searchPanel.add(searchBtn);
        searchPanel.add(patientSelector);

        JPanel form = new JPanel(new GridLayout(4, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        form.add(new JLabel("Measurement Value:"));
        form.add(valueField);
        form.add(new JLabel("DateTime (dd-MM-yyyyHH:mm):"));
        form.add(datetimeField);
        form.add(new JLabel(""));
        form.add(nowCheckbox);
        form.add(new JLabel(""));
        form.add(registerBtn);

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);
        panel.add(new JScrollPane(outputArea), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createCmasViewerPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);

        Font textFont = new Font("JetBrains Mono", Font.PLAIN, 13);

        JTextField nameField = new JTextField(20);
        JComboBox<String> patientSelector = new JComboBox<>();
        JTextArea scoreArea = new JTextArea(10, 30);
        scoreArea.setFont(textFont);
        scoreArea.setEditable(false);
        scoreArea.setMargin(new Insets(10, 10, 10, 10));

        JPanel chartPanelContainer = new JPanel(new BorderLayout());
        chartPanelContainer.setPreferredSize(new Dimension(500, 300));
        chartPanelContainer.setBackground(Color.WHITE);

        JButton searchBtn = new JButton("Find Patients");
        JButton viewBtn = new JButton("View Scores");

        // Top search controls
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Patient Name:"));
        topPanel.add(nameField);
        topPanel.add(searchBtn);
        topPanel.add(patientSelector);
        topPanel.add(viewBtn);

        // Combine text area and chart in a horizontal split pane
        JScrollPane textScroll = new JScrollPane(scoreArea);
        textScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, textScroll, chartPanelContainer);
        splitPane.setResizeWeight(0.5); // balance text/chart size
        splitPane.setDividerSize(8);
        splitPane.setBorder(null);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // Search patients
        searchBtn.addActionListener(e -> {
            patientSelector.removeAllItems();
            chartPanelContainer.removeAll();
            scoreArea.setText("");

            String name = nameField.getText().trim();
            try {
                List<Map<String, String>> matches = controller.findPatientsByName(name);
                if (matches.isEmpty()) {
                    scoreArea.setText("No matching patients found.");
                } else {
                    for (Map<String, String> p : matches) {
                        patientSelector.addItem(p.get("name") + " (" + p.get("id") + ")");
                    }
                    scoreArea.setText("Select the correct patient from the dropdown.");
                }
            } catch (Exception ex) {
                scoreArea.setText("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        // View CMAS data
        viewBtn.addActionListener(e -> {
            chartPanelContainer.removeAll();
            scoreArea.setText("");

            if (patientSelector.getSelectedItem() == null) {
                scoreArea.setText("No patient selected.");
                return;
            }

            String selected = (String) patientSelector.getSelectedItem();
            String patientId = selected.substring(selected.indexOf("(") + 1, selected.indexOf(")"));

            try {
                List<Map<String, Object>> scores = controller.getCmasScoresForPatient(patientId);
                if (scores.isEmpty()) {
                    scoreArea.setText("No CMAS scores found for this patient.");
                    return;
                }

                scores.sort(Comparator.comparing(m -> (String) m.get("date"))); // ascending for graph

                DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                List<String> scoreLines = new ArrayList<>();

                for (Map<String, Object> row : scores) {
                    String date = (String) row.get("date");
                    int score = (int) row.get("score");
                    dataset.addValue(score, "CMAS Score", date);
                    scoreLines.add(String.format("Date: %s | Score: %d", date, score));
                }

                scoreLines.sort(Comparator.reverseOrder());

                scoreArea.append("Patient ID: " + patientId + "\n\nCMAS Scores:\n");
                scoreLines.forEach(line -> scoreArea.append(line + "\n"));

                JFreeChart chart = ChartFactory.createLineChart(
                        "CMAS Score Over Time",
                        "Date",
                        "Score",
                        dataset
                );

                LineAndShapeRenderer renderer = new LineAndShapeRenderer();
                renderer.setDefaultShapesVisible(true);
                renderer.setDefaultLinesVisible(true);
                renderer.setDefaultShape(new java.awt.geom.Ellipse2D.Double(-3.0, -3.0, 6.0, 6.0));
                renderer.setSeriesPaint(0, Color.RED);
                chart.getCategoryPlot().setRenderer(renderer);

                ChartPanel chartPanel = new ChartPanel(chart);
                chartPanel.setPreferredSize(new Dimension(500, 300));
                chartPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                chartPanelContainer.add(chartPanel, BorderLayout.CENTER);

                chartPanelContainer.revalidate();
                chartPanelContainer.repaint();

            } catch (Exception ex) {
                scoreArea.setText("Error retrieving CMAS scores: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        return mainPanel;
    }

//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(() -> {
//            try {
//                FlatLightLaf.setup();
//                UIManager.put("TextComponent.arc", 10);
//                UIManager.put("Button.arc", 10);
//                UIManager.put("Component.arc", 10);
//                UIManager.put("ProgressBar.arc", 10);
//
//                new DoctorPanel(new DatabaseController());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//    }
}