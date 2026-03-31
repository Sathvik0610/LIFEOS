import java.sql.*;
public class UserDAO {
    public void registerUser(int id,String name,String email,String password,java.sql.Date dob)   {
        try{
            Connection conn = DBConnection.getConnection();
            String hashed = PasswordUtil.HashPassword(password);
            String query = "INSERT INTO USERS (USER_ID,NAME,EMAIL,PASSWORD_HASH,DOB) VALUES (?,?,?,?,?)";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, id);
            ps.setString(2, name);
            ps.setString(3,email);
            ps.setString(4,hashed);
            ps.setDate(5, dob);

            ps.executeUpdate();
            System.out.println("User registered");
        }
        catch(Exception e)  {
            e.printStackTrace();
        }
    }


    public boolean login (String email,String password) {
        try{
            Connection conn=DBConnection.getConnection();
            String query = "SELECT PASSWORD_HASH FROM USERS WHERE EMAIL = ?";
            PreparedStatement ps = conn.prepareStatement(query);

            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();

            if(rs.next())   {
                String storedHash = rs.getString("PASSWORD_HASH");
                String inputHash = PasswordUtil.HashPassword(password);
                return storedHash.equals(inputHash);
            }
        }
        catch(Exception e)  {
            e.printStackTrace();
        }
        return false;
    }
}