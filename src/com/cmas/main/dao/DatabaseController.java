package com.cmas.main.dao;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

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

    public void saveCMASScore(String patientID,int scoreValue) throws SQLException {
        String sql = "INSERT INTO CMAS (patientID, score_date, score_type, score_value) VALUES (?, ?, ?)";
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

    public String getFirstLabResultIdForPatient(String patientId) throws SQLException {
        String sql = "SELECT LabResultID FROM LabResults_EN WHERE PatientID = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("LabResultID");
                }
            }
        }
        return null;
    }

    // Fetch CMAS scores with date and type
    public List<Map<String, Object>> getAllCMASScores() throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT * FROM CMAS ORDER BY score_date DESC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("date", rs.getDate("score_date"));
                row.put("type", rs.getString("score_type"));
                row.put("value", rs.getInt("score_value"));
                result.add(row);
            }
        }
        return result;
    }

    // Fetch measurements joined with result names and units
    public List<Map<String, String>> getDetailedLabMeasurements(String patientId) throws SQLException {
        List<Map<String, String>> data = new ArrayList<>();
        String sql = """
        SELECT lr.ResultName, lr.Unit, m.DateTime, m.Value
        FROM Measurement m
        JOIN LabResult lr ON lr.LabResultID = m.LabResultID
        WHERE lr.PatientID = ?
        ORDER BY m.DateTime DESC
    """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> entry = new HashMap<>();
                    entry.put("name", rs.getString("ResultName"));
                    entry.put("unit", rs.getString("Unit"));
                    entry.put("value", rs.getString("Value"));
                    entry.put("datetime", rs.getString("DateTime"));
                    data.add(entry);
                }
            }
        }
        return data;
    }

    // Get basic patient info
    public String getPatientName(String patientId) throws SQLException {
        String sql = "SELECT Name FROM Patients WHERE PatientID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, patientId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("Name");
            }
        }
        return null;
    }


    // Get CMAS scores for a specific patient
    public List<Map<String, Object>> getCmasScoresForPatient(String patientId) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        String sql = "SELECT score_date, score_value FROM CMAS WHERE PatientID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("date", rs.getString("score_date"));
                    row.put("score", rs.getInt("score_value"));
                    results.add(row);
                }
            }
        }
        return results;
    }

    public Map<String, Object> getFullPatientOverview(String patientId, boolean detailed) throws SQLException {
        Map<String, Object> data = new HashMap<>();

        // Patient name
        String sqlPatient = "SELECT Name FROM Patients WHERE PatientID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sqlPatient)) {
            stmt.setString(1, patientId.trim());
            ResultSet rs = stmt.executeQuery();
            data.put("name", rs.next() ? rs.getString("Name") : "");
        }

        // LabResults_EN entries
        List<Map<String, String>> labResults = new ArrayList<>();
        String sqlLab = """
        SELECT LabResultID, LabResultGroupID, ResultName_English, Unit
        FROM LabResults_EN
        WHERE PatientID = ?
    """;

        try (PreparedStatement stmt = conn.prepareStatement(sqlLab)) {
            stmt.setString(1, patientId.trim());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, String> result = new HashMap<>();
                result.put("labResultId", rs.getString("LabResultID"));
                result.put("labResultGroupId", rs.getString("LabResultGroupID"));
                result.put("resultNameEnglish", rs.getString("ResultName_English"));
                result.put("unit", rs.getString("Unit"));
                labResults.add(result);
            }
        }

        // Get group names for all modes
        Map<String, String> groupIdToName = new HashMap<>();
        String sqlGroups = "SELECT LabResultGroupID, GroupName FROM LabResultGroup";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlGroups)) {
            while (rs.next()) {
                groupIdToName.put(rs.getString("LabResultGroupID"), rs.getString("GroupName"));
            }
        }

        // Measurements
        List<Map<String, String>> measurementEntries = new ArrayList<>();

        for (Map<String, String> result : labResults) {
            String labResultId = result.get("labResultId");
            String groupId = result.get("labResultGroupId");

            String sqlMeas = "SELECT DateTime, Value FROM Measurement WHERE LabResultID = ? ORDER BY DateTime DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sqlMeas)) {
                stmt.setString(1, labResultId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Map<String, String> row = new HashMap<>();
                    row.put("datetime", rs.getString("DateTime"));
                    row.put("value", rs.getString("Value"));
                    row.put("unit", result.get("unit"));
                    row.put("name", result.get("resultNameEnglish"));
                    row.put("labResultId", detailed ? labResultId : null);
                    row.put("groupId", groupId);
                    measurementEntries.add(row);
                }
            }
        }

        data.put("medications", measurementEntries);
        data.put("groupNames", groupIdToName);
        return data;
    }

    public List<String[]> getMatchingPatientsByName(String name) throws Exception {
        List<String[]> results = new ArrayList<>();
        String sql = "SELECT PatientID, Name FROM Patients WHERE Name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(new String[]{rs.getString("PatientID"), rs.getString("Name")});
            }
        }
        return results;
    }

    public List<Map<String, String>> findPatientsByName(String name) throws SQLException {
        List<Map<String, String>> results = new ArrayList<>();

        String sql = """
            SELECT p.PatientID, p.Name, MIN(g.GroupName) AS GroupName
            FROM Patients p
            LEFT JOIN LabResults_EN l ON p.PatientID = l.PatientID
            LEFT JOIN LabResultGroup g ON l.LabResultGroupID = g.LabResultGroupID
            WHERE p.Name = ?
            GROUP BY p.PatientID, p.Name
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, String> patient = new HashMap<>();
                patient.put("id", rs.getString("PatientID"));
                patient.put("name", rs.getString("Name"));
                patient.put("group", rs.getString("GroupName"));
                results.add(patient);
            }
        }

        return results;
    }

    public List<String> getCMASScoresByPatient(String patientId) throws SQLException {
        List<String> scores = new ArrayList<>();

        String sql = "SELECT score_date, score_type, score_value FROM CMAS WHERE PatientID = ? ORDER BY score_date DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, patientId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String entry = String.format("Date: %s | Type: %s | Value: %d",
                        rs.getString("score_date"),
                        rs.getString("score_type"),
                        rs.getInt("score_value"));
                scores.add(entry);
            }
        }

        return scores;
    }

    public List<Map<String, String>> getAllLabResultGroups() throws SQLException {
        List<Map<String, String>> results = new ArrayList<>();
        String sql = "SELECT LabResultGroupID, GroupName FROM LabResultGroup";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, String> group = new HashMap<>();
                group.put("id", rs.getString("LabResultGroupID"));
                group.put("name", rs.getString("GroupName"));
                results.add(group);
            }
        }
        return results;
    }

    public void insertLabResultEN(String labResultId, String groupId, String patientId, String resultName, String resultNameEng, String unit) throws SQLException {
        String sql = "INSERT INTO LabResults_EN (LabResultID, LabResultGroupID, PatientID, ResultName, ResultName_English, Unit) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, labResultId);
            stmt.setString(2, groupId);
            stmt.setString(3, patientId);
            stmt.setString(4, resultName);
            stmt.setString(5, resultNameEng);
            if (unit != null) {
                stmt.setString(6, unit);
            } else {
                stmt.setNull(6, java.sql.Types.VARCHAR);
            }
            stmt.executeUpdate();
        }
    }

    //Setup method used to map patient IDs with no name to randomly generated names

//    public void mapPatientIdsToNames(String csvPath) throws Exception {
//        // Step 1: Fetch all unique PatientIDs from the database
//        Set<String> patientIds = new LinkedHashSet<>();
//
//        String fetchSql = "SELECT DISTINCT PatientID FROM LabResults_EN WHERE PatientID IS NOT NULL AND PatientID != ''";
//        try (PreparedStatement stmt = conn.prepareStatement(fetchSql);
//             ResultSet rs = stmt.executeQuery()) {
//            while (rs.next()) {
//                patientIds.add(rs.getString("PatientID"));
//            }
//        }
//
//        System.out.println("Found " + patientIds.size() + " unique patient IDs.");
//
//        // Step 2: Read names from the CSV file
//        List<String> names = new ArrayList<>();
//        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
//            String line;
//            boolean isFirstLine = true;
//            while ((line = reader.readLine()) != null) {
//                if (isFirstLine) {
//                    isFirstLine = false; // skip header
//                    continue;
//                }
//
//                String[] parts = line.split(",");
//                if (parts.length < 2) continue;
//
//                String firstName = parts[0].trim().replaceAll("^\"|\"$", "");
//                String lastName = parts[1].trim().replaceAll("^\"|\"$", "");
//                String fullName = firstName + " " + lastName;
//                if (!fullName.isBlank()) {
//                    names.add(fullName);
//                }
//            }
//        }

//        if (names.size() < patientIds.size()) {
//            throw new RuntimeException("Not enough names in the CSV for the number of unique patient IDs.");
//        }
//
//        // Step 3: Map and insert
//        Iterator<String> idIterator = patientIds.iterator();
//        Iterator<String> nameIterator = names.iterator();
//
//        String insertSql = "INSERT INTO Patients (PatientID, Name) VALUES (?, ?)";
//        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
//            while (idIterator.hasNext() && nameIterator.hasNext()) {
//                String patientId = idIterator.next();
//                String name = nameIterator.next();
//
//                insertStmt.setString(1, patientId);
//                insertStmt.setString(2, name);
//                insertStmt.executeUpdate();
//            }
//        }
//
//        System.out.println("Mapped and inserted " + patientIds.size() + " patients.");
//    }


    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

}