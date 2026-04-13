import java.sql.*;
import java.util.*;

public class TaskDAO {

    // ===== GET TASKS FOR A SPECIFIC DATE =====
    public List<Task> getTasksByDate(int userId, java.sql.Date date) {
        List<Task> tasks = new ArrayList<>();

        String query = "SELECT * FROM TASKS WHERE USER_ID = ? AND START_TIME = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, userId);
            ps.setDate(2, date);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                tasks.add(mapTask(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return tasks;
    }


    // ===== GET TASKS FOR A WHOLE MONTH =====
    public List<Task> getTasksByMonth(int userId, java.sql.Date anyDateInMonth) {
        List<Task> tasks = new ArrayList<>();

        String query = "SELECT * FROM TASKS WHERE USER_ID = ? " +
                       "AND START_TIME >= TRUNC(?, 'MM') " +
                       "AND START_TIME < ADD_MONTHS(TRUNC(?, 'MM'), 1)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, userId);
            ps.setDate(2, anyDateInMonth);
            ps.setDate(3, anyDateInMonth);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                tasks.add(mapTask(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return tasks;
    }


    // ===== ADD TASK (UPDATED WITH CATEGORY & DESCRIPTION) =====
    public void addTask(int userId, String title, String category, String description, 
                        java.sql.Date date, int startHour, int startMin,
                        int endHour, int endMin) {

        // Added CATEGORY and DESCRIPTION to the SQL query
        String query = "INSERT INTO TASKS " +
                "(TASK_ID, USER_ID, TITLE, CATEGORY, DESCRIPTION, START_TIME, START_HOUR, START_MIN, END_HOUR, END_MIN, STATUS) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            int taskId = (int) (System.currentTimeMillis() % 1000000);

            ps.setInt(1, taskId);
            ps.setInt(2, userId);
            ps.setString(3, title);
            ps.setString(4, category);       // NEW
            ps.setString(5, description);    // NEW
            ps.setDate(6, date);
            ps.setInt(7, startHour);
            ps.setInt(8, startMin);
            ps.setInt(9, endHour);
            ps.setInt(10, endMin);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ===== COMMON MAPPER (UPDATED) =====
    private Task mapTask(ResultSet rs) throws SQLException {
        // Updated to extract CATEGORY and DESCRIPTION and pass them to the Task constructor
        return new Task(
                rs.getInt("TASK_ID"),
                rs.getInt("USER_ID"),
                rs.getString("TITLE"),
                rs.getString("CATEGORY"),      // NEW
                rs.getString("DESCRIPTION"),   // NEW
                rs.getDate("START_TIME"),
                rs.getInt("START_HOUR"),
                rs.getInt("START_MIN"),
                rs.getInt("END_HOUR"),
                rs.getInt("END_MIN")
        );
    }

    // ===== DELETE TASK =====
    public void deleteTask(int taskId) {
        String query = "DELETE FROM TASKS WHERE TASK_ID = ?";
        try (Connection conn = DBConnection.getConnection();
           PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, taskId);
            ps.executeUpdate();

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== UPDATE TASK =====
    public void updateTask(int taskId, String title, String category, String description, 
                           java.sql.Date date, int startHour, int startMin,
                           int endHour, int endMin) {

        String query = "UPDATE TASKS SET " +
                "TITLE = ?, CATEGORY = ?, DESCRIPTION = ?, START_TIME = ?, " +
                "START_HOUR = ?, START_MIN = ?, END_HOUR = ?, END_MIN = ? " +
                "WHERE TASK_ID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, title);
            ps.setString(2, category);
            ps.setString(3, description);
            ps.setDate(4, date);
            ps.setInt(5, startHour);
            ps.setInt(6, startMin);
            ps.setInt(7, endHour);
            ps.setInt(8, endMin);
            ps.setInt(9, taskId);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}