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

                if (matches.size() == 1) {
                    selectedPatientId = matches.get(0).get("id");
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
                }

                Map<String, Object> data = controller.getFullPatientOverview(selectedPatientId, detailed);

                String name = (String) data.get("name");
                outputArea.append("Patient ID: " + selectedPatientId + "\n");
                outputArea.append("Patient Name: " + (name.isEmpty() ? "(not found)" : name) + "\n\n");

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