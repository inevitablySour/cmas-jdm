package com.cmas.main.dao;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DatabaseController {
    private static final String DB_URL;
    private static Connection conn;

    static {
        try {
            Properties props = Encrypt.loadDecryptedConfig("src/resources/config.enc");

            DB_URL = props.getProperty("db.url");

            if (DB_URL == null || DB_URL.isEmpty()) {
                throw new RuntimeException("db.url not found in decrypted config");
            }

            conn = DriverManager.getConnection(DB_URL);

        } catch (IOException | SQLException | RuntimeException e) {
            throw new RuntimeException("Failed to load DB config or connect to database", e);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public void saveCMASScore(int scoreValue) throws SQLException {
        String sql = "INSERT INTO CMAS (score_date, score_type, score_value) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(LocalDate.now()));
            stmt.setString(2, "total");
            stmt.setInt(3, scoreValue);
            stmt.executeUpdate();
        }
    }

    public List<String> getPastScores() throws SQLException {
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

    public void insertPatient(String patientId, String name) throws SQLException {
        String sql = "INSERT INTO Patients (PatientID, Name) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, patientId);
            stmt.setString(2, name);
            stmt.executeUpdate();
        }
    }

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

    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }
}