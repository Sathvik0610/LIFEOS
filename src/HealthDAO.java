import java.sql.*;
import java.util.*;

public class HealthDAO {

    // ==========================================
    // DATA MODELS
    // ==========================================
    
    public static class HealthRecord {
        public double sleep = 0.0;
        public int steps = 0;
        public double weight = 0.0;
    }

    public static class Medication {
        public int medId;
        public String name;
        public String dosage;
        public java.sql.Date medTime;
        public int gap;
    }

    public static class ActivityLog {
        public int logId;
        public String activity;
        public int duration; // in minutes
        public java.sql.Timestamp startTime;
        public java.sql.Timestamp endTime;
    }

    // ==========================================
    // CONNECTION HELPER
    // ==========================================
    
    // Safely gets a connection and prevents NullPointerExceptions if the database is offline
    private Connection getSafeConnection() {
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            System.err.println("HealthDAO Error: Could not connect to the database!");
        }
        return conn;
    }

    // ==========================================
    // DAILY HEALTH RECORDS (OVERVIEW TAB)
    // ==========================================
    
    public HealthRecord getDailyRecord(int userId, java.sql.Date date) {
        HealthRecord record = new HealthRecord();
        boolean foundToday = false;
        
        // 1. Try to get today's record first
        String query = "SELECT SLEEP, STEPS, WEIGHT FROM HEALTH_RECORDS WHERE USER_ID = ? AND TRUNC(RECORD_DATE) = TRUNC(?)";
        try (Connection conn = getSafeConnection();
             PreparedStatement ps = (conn != null) ? conn.prepareStatement(query) : null) {
            
            if (ps != null) {
                ps.setInt(1, userId);
                ps.setDate(2, date);
                ResultSet rs = ps.executeQuery();
                
                if (rs.next()) {
                    record.sleep = rs.getDouble("SLEEP");
                    record.steps = rs.getInt("STEPS");
                    record.weight = rs.getDouble("WEIGHT");
                    foundToday = true;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        // 2. If no record exists for today, find the MOST RECENT weight so it stays constant!
        if (!foundToday || record.weight == 0.0) {
            String recentWeightQuery = "SELECT WEIGHT FROM (SELECT WEIGHT FROM HEALTH_RECORDS WHERE USER_ID = ? AND WEIGHT > 0 ORDER BY RECORD_DATE DESC) WHERE ROWNUM <= 1";
            try (Connection conn = getSafeConnection();
                 PreparedStatement ps = (conn != null) ? conn.prepareStatement(recentWeightQuery) : null) {
                 
                if (ps != null) {
                    ps.setInt(1, userId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        record.weight = rs.getDouble("WEIGHT");
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        
        return record;
    }

    public void saveDailyRecord(int userId, java.sql.Date date, HealthRecord record) {
        // First, check if a record already exists for today
        String checkQuery = "SELECT RECORD_ID FROM HEALTH_RECORDS WHERE USER_ID = ? AND TRUNC(RECORD_DATE) = TRUNC(?)";
        int existingId = -1;
        
        try (Connection conn = getSafeConnection();
             PreparedStatement ps = (conn != null) ? conn.prepareStatement(checkQuery) : null) {
             
            if (ps == null) return;
            
            ps.setInt(1, userId);
            ps.setDate(2, date);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                existingId = rs.getInt(1);
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }

        // If it exists, UPDATE. If it doesn't, INSERT a new row.
        String sql;
        if (existingId != -1) {
            sql = "UPDATE HEALTH_RECORDS SET SLEEP = ?, STEPS = ?, WEIGHT = ? WHERE RECORD_ID = ?";
        } else {
            sql = "INSERT INTO HEALTH_RECORDS (RECORD_ID, USER_ID, SLEEP, STEPS, WEIGHT, RECORD_DATE) VALUES (?, ?, ?, ?, ?, ?)";
        }

        try (Connection conn = getSafeConnection();
             PreparedStatement ps = (conn != null) ? conn.prepareStatement(sql) : null) {
             
            if (ps == null) return;
            
            if (existingId != -1) {
                // Parameters for UPDATE
                ps.setDouble(1, record.sleep);
                ps.setInt(2, record.steps);
                ps.setDouble(3, record.weight);
                ps.setInt(4, existingId);
            } else {
                // Parameters for INSERT
                int newRecordId = (int) (System.currentTimeMillis() % 1000000);
                ps.setInt(1, newRecordId);
                ps.setInt(2, userId);
                ps.setDouble(3, record.sleep);
                ps.setInt(4, record.steps);
                ps.setDouble(5, record.weight);
                ps.setDate(6, date);
            }
            
            ps.executeUpdate();
            
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    // Fetches user height to calculate BMI accurately
    public double getUserHeight(int userId) {
        double height = 170.0; // Default height in cm
        String query = "SELECT HEIGHT FROM USERS WHERE USER_ID = ?";
        
        try (Connection conn = getSafeConnection();
             PreparedStatement ps = (conn != null) ? conn.prepareStatement(query) : null) {
             
            if (ps == null) return height;
            
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                height = rs.getDouble("HEIGHT");
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        return height;
    }

    // Calculates how many consecutive days the user has hit 10,000 steps
    public int getStepStreak(int userId) {
        int streak = 0;
        String query = "SELECT STEPS FROM HEALTH_RECORDS WHERE USER_ID = ? AND RECORD_DATE <= TRUNC(SYSDATE) ORDER BY RECORD_DATE DESC";
        
        try (Connection conn = getSafeConnection();
             PreparedStatement ps = (conn != null) ? conn.prepareStatement(query) : null) {
             
            if (ps == null) return 0;
            
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                if (rs.getInt("STEPS") >= 10000) {
                    streak++;
                } else {
                    break; // Streak is broken, stop counting
                }
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        return streak;
    }

    // ==========================================
    // ACTIVITY JOURNAL LOGIC
    // ==========================================
    
    public List<ActivityLog> getActivities(int userId) {
        List<ActivityLog> logs = new ArrayList<>();
        String query = "SELECT * FROM TIME_LOGS WHERE USER_ID = ? ORDER BY START_TIME DESC";
        
        try (Connection conn = getSafeConnection();
             PreparedStatement ps = (conn != null) ? conn.prepareStatement(query) : null) {
             
            if (ps == null) return logs;
            
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                ActivityLog log = new ActivityLog();
                log.logId = rs.getInt("LOG_ID");
                log.activity = rs.getString("ACTIVITY");
                log.duration = rs.getInt("DURATION");
                log.startTime = rs.getTimestamp("START_TIME");
                log.endTime = rs.getTimestamp("END_TIME");
                logs.add(log);
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        return logs;
    }

    public void addActivity(int userId, String activity, int duration, java.sql.Timestamp start, java.sql.Timestamp end) {
        String query = "INSERT INTO TIME_LOGS (LOG_ID, USER_ID, ACTIVITY, DURATION, START_TIME, END_TIME, LOG_DATE) VALUES (?, ?, ?, ?, ?, ?, SYSDATE)";
        
        try (Connection conn = getSafeConnection();
             PreparedStatement ps = (conn != null) ? conn.prepareStatement(query) : null) {
             
            if (ps == null) return;
            
            int newLogId = (int) (System.currentTimeMillis() % 1000000);
            ps.setInt(1, newLogId);
            ps.setInt(2, userId);
            ps.setString(3, activity);
            ps.setInt(4, duration);
            ps.setTimestamp(5, start);
            ps.setTimestamp(6, end);
            
            ps.executeUpdate();
            
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    public void deleteActivity(int logId) {
        String query = "DELETE FROM TIME_LOGS WHERE LOG_ID = ?";
        
        try (Connection conn = getSafeConnection();
             PreparedStatement ps = (conn != null) ? conn.prepareStatement(query) : null) {
             
            if (ps != null) { 
                ps.setInt(1, logId); 
                ps.executeUpdate(); 
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    // ==========================================
    // MEDICATIONS LOGIC
    // ==========================================
    
    public List<Medication> getMedications(int userId) {
        List<Medication> meds = new ArrayList<>();
        String query = "SELECT * FROM MEDICATIONS WHERE USER_ID = ?";
        
        try (Connection conn = getSafeConnection();
             PreparedStatement ps = (conn != null) ? conn.prepareStatement(query) : null) {
             
            if (ps == null) return meds;
            
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Medication m = new Medication();
                m.medId = rs.getInt("MED_ID");
                m.name = rs.getString("NAME");
                m.dosage = rs.getString("DOSAGE");
                m.medTime = rs.getDate("MED_TIME");
                m.gap = rs.getInt("GAP");
                meds.add(m);
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        return meds;
    }

    public void addMedication(int userId, String name, String dosage, java.sql.Date medTime, int gap) {
        String query = "INSERT INTO MEDICATIONS (MED_ID, USER_ID, NAME, DOSAGE, MED_TIME, GAP) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getSafeConnection();
             PreparedStatement ps = (conn != null) ? conn.prepareStatement(query) : null) {
             
            if (ps == null) return;
            
            int newMedId = (int) (System.currentTimeMillis() % 1000000);
            ps.setInt(1, newMedId);
            ps.setInt(2, userId);
            ps.setString(3, name);
            ps.setString(4, dosage);
            ps.setDate(5, medTime);
            ps.setInt(6, gap);
            
            ps.executeUpdate();
            
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    public void deleteMedication(int medId) {
        String query = "DELETE FROM MEDICATIONS WHERE MED_ID = ?";
        
        try (Connection conn = getSafeConnection();
             PreparedStatement ps = (conn != null) ? conn.prepareStatement(query) : null) {
             
            if (ps != null) { 
                ps.setInt(1, medId); 
                ps.executeUpdate(); 
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    // ==========================================
    // WEEKLY CHART DATA LOGIC
    // ==========================================
    
    public Map<java.time.LocalDate, Double> getWeeklyMetric(int userId, String columnName) {
        Map<java.time.LocalDate, Double> data = new LinkedHashMap<>();
        
        // Pre-fill the last 7 days with 0.0 so empty days still show on the graph
        for (int i = 6; i >= 0; i--) {
            data.put(java.time.LocalDate.now().minusDays(i), 0.0);
        }

        // Fetch actual data from the last 7 days
        String query = "SELECT RECORD_DATE, " + columnName + " FROM HEALTH_RECORDS " +
                       "WHERE USER_ID = ? AND RECORD_DATE >= TRUNC(SYSDATE) - 6 " +
                       "ORDER BY RECORD_DATE ASC";

        try (Connection conn = getSafeConnection();
             PreparedStatement ps = (conn != null) ? conn.prepareStatement(query) : null) {
             
            if (ps == null) return data;
            
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                java.time.LocalDate date = rs.getDate("RECORD_DATE").toLocalDate();
                double val = rs.getDouble(columnName);
                if (data.containsKey(date)) {
                    data.put(date, val); // Overwrite the 0 with actual data
                }
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        
        return data;
    }
}