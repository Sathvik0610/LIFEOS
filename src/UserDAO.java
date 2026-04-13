import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class UserDAO {
    public void registerUser(int id, String name, String email, String password, java.sql.Date dob) {
        try {
            Connection conn = DBConnection.getConnection();
            String hashed = PasswordUtil.HashPassword(password);
            // Default height to 170cm on registration
            String query = "INSERT INTO USERS (USER_ID, NAME, EMAIL, PASSWORD_HASH, DOB, HEIGHT) VALUES (?,?,?,?,?, 170)";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, id);
            ps.setString(2, name);
            ps.setString(3, email);
            ps.setString(4, hashed);
            ps.setDate(5, dob);

            ps.executeUpdate();
            System.out.println("User registered");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int login(String email, String password) {
        try {
            Connection conn = DBConnection.getConnection();
            String query = "SELECT USER_ID, PASSWORD_HASH FROM USERS WHERE EMAIL = ?";
            PreparedStatement ps = conn.prepareStatement(query);

            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("PASSWORD_HASH");
                String inputHash = PasswordUtil.HashPassword(password);

                if (storedHash.equals(inputHash)) {
                    return rs.getInt("USER_ID");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    // --- FETCH USER DETAILS ---
    public Map<String, String> getUserDetails(int userId) {
        Map<String, String> userDetails = new HashMap<>();
        String query = "SELECT NAME, EMAIL, DOB, HEIGHT FROM USERS WHERE USER_ID = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
             
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                userDetails.put("name", rs.getString("NAME"));
                userDetails.put("email", rs.getString("EMAIL"));
                userDetails.put("dob", rs.getDate("DOB") != null ? rs.getDate("DOB").toString() : "");
                
                double height = rs.getDouble("HEIGHT");
                if (rs.wasNull() || height == 0) height = 170.0; // Fallback
                userDetails.put("height", String.valueOf(height));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userDetails;
    }

    // --- UPDATE PROFILE INFO ---
    public boolean updateUserDetails(int userId, String name, String email, java.sql.Date dob, double height) {
        String query = "UPDATE USERS SET NAME=?, EMAIL=?, DOB=?, HEIGHT=? WHERE USER_ID=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setDate(3, dob);
            ps.setDouble(4, height);
            ps.setInt(5, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { 
            e.printStackTrace(); 
            return false; 
        }
    }

    // --- UPDATE PASSWORD SECURELY ---
    public boolean updatePassword(int userId, String currentPassword, String newPassword) {
        String query = "SELECT PASSWORD_HASH FROM USERS WHERE USER_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("PASSWORD_HASH");
                // Verify the current password matches the database
                if (storedHash.equals(PasswordUtil.HashPassword(currentPassword))) {
                    String update = "UPDATE USERS SET PASSWORD_HASH=? WHERE USER_ID=?";
                    try (PreparedStatement ps2 = conn.prepareStatement(update)) {
                        ps2.setString(1, PasswordUtil.HashPassword(newPassword));
                        ps2.setInt(2, userId);
                        return ps2.executeUpdate() > 0;
                    }
                }
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        return false; // Returns false if current password was wrong or DB error
    }
}