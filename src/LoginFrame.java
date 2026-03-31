import javax.swing.*;
import java.awt.event.*;

public class LoginFrame extends JFrame {

    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginButton;

    private UserDAO dao = new UserDAO();

    public LoginFrame() {
        setTitle("LifeOS Login");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new java.awt.GridBagLayout());
        getContentPane().setBackground(new java.awt.Color(245, 245, 245));

        JPanel panel = new JPanel();
        panel.setLayout(new java.awt.GridBagLayout());
        panel.setBackground(new java.awt.Color(255, 255, 255));
        panel.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(10, 10, 10, 10);
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("LifeOS");
        title.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 28));

        gbc.gridx = 0;
        gbc.gridy = -1;
        gbc.gridwidth = 2;
        gbc.anchor = java.awt.GridBagConstraints.CENTER;
        panel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 0;

        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 20));

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(emailLabel, gbc);

        emailField = new JTextField(20);
        emailField.setPreferredSize(new java.awt.Dimension(250, 35));
        emailField.setBackground(java.awt.Color.WHITE);
        emailField.setForeground(java.awt.Color.BLACK);
        emailField.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 18));

        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(emailField, gbc);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 20));

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(passwordLabel, gbc);

        passwordField = new JPasswordField(20);
        passwordField.setPreferredSize(new java.awt.Dimension(250, 35));
        passwordField.setBackground(java.awt.Color.WHITE);
        passwordField.setForeground(java.awt.Color.BLACK);
        passwordField.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 18));

        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(passwordField, gbc);

        loginButton = new JButton("Login");
        loginButton.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 18));
        loginButton.setPreferredSize(new java.awt.Dimension(150, 40));

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(loginButton, gbc);

        add(panel);

        // BUTTON ACTION
        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleLogin();
            }
        });

        setVisible(true);
    }

    private void handleLogin() {
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());

        boolean success = dao.login(email, password);

        if (success) {
            JOptionPane.showMessageDialog(this, "Login Successful", "Success", JOptionPane.PLAIN_MESSAGE);
            // TODO: open dashboard
        } else {
            JOptionPane.showMessageDialog(this, "Login Failed", "Error", JOptionPane.PLAIN_MESSAGE);
        }
    }
}