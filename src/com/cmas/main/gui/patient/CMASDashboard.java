package com.cmas.main.gui.patient;

import com.cmas.main.dao.DatabaseController;
import com.cmas.main.imageProcessing.PythonServerController;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class CMASDashboard extends JFrame {
    private final DatabaseController db = new DatabaseController();
    private ResultsDashboardPanel resultsPanel = null;
    private static String PatientID = "";
//    public static void main(String[] args) {
//        try {
//            FlatLightLaf.setup();
//            UIManager.put("Button.arc", 20);
//            UIManager.put("Component.arc", 10);
//            UIManager.put("CheckBox.icon.selectedBackground", new Color(60, 120, 200));
//            UIManager.put("CheckBox.icon.checkmarkColor", Color.WHITE);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//
//        PythonServerController.startServer();
//
//        SwingUtilities.invokeLater(() -> {
//            try {
//                CMASDashboard dashboard = new CMASDashboard(PatientID);
//                dashboard.setVisible(true);
//            } catch (SQLException e) {
//                throw new RuntimeException(e);
//            }
//        });
//    }

    public CMASDashboard(String PatientID) throws SQLException {
        setTitle("CMAS Assessment System");
        setSize(1000, 700);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.PatientID = PatientID;
        resultsPanel = new ResultsDashboardPanel(db.getCMASScoresByPatient(PatientID));

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                PythonServerController.stopServer();
                dispose();
                System.exit(0);
            }
        });

        setLocationRelativeTo(null);

        // Test tab will now notify when complete
        TestDashboardPanel testPanel = new TestDashboardPanel(() -> {
            List<String> updatedScores = null;
            try {
                updatedScores = db.getPastScores();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            resultsPanel.refreshScores(updatedScores);
        });

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Perform Tests", testPanel);
        tabs.addTab("View Results", resultsPanel);

        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);
    }

    public static String getPatientID() {
        return PatientID;
    }
}