package com.cmas.main.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseController {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/cmas-scoring"; // change this
    private static final String USER = "root"; // change this
    private static final String PASSWORD = "Newtonsappl3"; // change this

    private Connection conn;

    public DatabaseController() throws SQLException {
        conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
    }

    // Insert a CMAS score
    public void insertCMASScore(String scoreDate, String scoreType, int scoreValue) throws SQLException {
        String sql = "INSERT INTO CMAS (score_date, score_type, score_value) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, scoreDate);
            stmt.setString(2, scoreType);
            stmt.setInt(3, scoreValue);
            stmt.executeUpdate();
        }
    }

    // Retrieve all CMAS scores
    public List<String> getAllCMASScores() throws SQLException {
        List<String> scores = new ArrayList<>();
        String sql = "SELECT * FROM CMAS ORDER BY score_date DESC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String row = String.format("Date: %s | Type: %s | Value: %d",
                        rs.getString("score_date"),
                        rs.getString("score_type"),
                        rs.getInt("score_value"));
                scores.add(row);
            }
        }
        return scores;
    }

    // Insert a new patient
    public void insertPatient(String patientId, String name) throws SQLException {
        String sql = "INSERT INTO Patients (PatientID, Name) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, patientId);
            stmt.setString(2, name);
            stmt.executeUpdate();
        }
    }

    // Retrieve lab results for a patient
    public List<String> getLabResultsByPatient(String patientId) throws SQLException {
        List<String> results = new ArrayList<>();
        String sql = "SELECT * FROM LabResult WHERE PatientID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String result = String.format("ID: %s | Name: %s | Unit: %s",
                            rs.getString("LabResultID"),
                            rs.getString("ResultName"),
                            rs.getString("Unit"));
                    results.add(result);
                }
            }
        }
        return results;
    }

    // Insert measurement
    public void insertMeasurement(String measurementId, String labResultId, String dateTime, String value) throws SQLException {
        String sql = "INSERT INTO Measurement (MeasurementID, LabResultID, DateTime, Value) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, measurementId);
            stmt.setString(2, labResultId);
            stmt.setString(3, dateTime);
            stmt.setString(4, value);
            stmt.executeUpdate();
        }
    }

    // Close DB connection
    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }
}
