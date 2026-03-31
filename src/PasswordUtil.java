import java.security.MessageDigest;
public class PasswordUtil {
    public static String HashPassword (String PASSWORD) {
        try{
            MessageDigest md=MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(PASSWORD.getBytes());

            StringBuilder hexString = new StringBuilder();

            for(byte h:hash)    {
                String hex = Integer.toHexString(0xff & h);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        }
        catch(Exception e)  {
            throw new RuntimeException(e);
        }      
    }
}
