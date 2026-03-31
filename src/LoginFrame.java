import javax.swing.*;
import java.awt.event.*;

public class LoginFrame extends JFrame {

    private final JTextField emailField;
    private final JPasswordField passwordField;
    private final JButton loginButton;
    private JLabel errorLabel;

    private UserDAO dao = new UserDAO();

    public LoginFrame() {
        setTitle("LifeOS Login");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new java.awt.GridBagLayout());
        getContentPane().setBackground(new java.awt.Color(232, 240, 255));

        JPanel panel = new JPanel();
        panel.setLayout(new java.awt.GridBagLayout());
        panel.setBackground(new java.awt.Color(255, 255, 255));
        panel.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(10, 10, 10, 10);
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("LIFEOS");
        title.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 28));
        title.setForeground(new java.awt.Color(100, 149, 237)); // light blue

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = java.awt.GridBagConstraints.CENTER;
        panel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;

        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 20));

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(emailLabel, gbc);

        emailField = new JTextField(20);
        emailField.setPreferredSize(new java.awt.Dimension(250, 35));
        emailField.setBackground(java.awt.Color.WHITE);
        emailField.setForeground(java.awt.Color.BLACK);
        emailField.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 18));

        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(emailField, gbc);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 20));

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(passwordLabel, gbc);

        passwordField = new JPasswordField(20);
        passwordField.setPreferredSize(new java.awt.Dimension(250, 35));
        passwordField.setBackground(java.awt.Color.WHITE);
        passwordField.setForeground(java.awt.Color.BLACK);
        passwordField.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 18));

        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(passwordField, gbc);

        errorLabel = new JLabel("");
        errorLabel.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 14));
        errorLabel.setForeground(java.awt.Color.RED);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(errorLabel, gbc);

        loginButton = new JButton("Login");
        loginButton.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 18));
        loginButton.setForeground(java.awt.Color.WHITE);
        loginButton.setBackground(new java.awt.Color(100, 149, 237));
        loginButton.setOpaque(true);
        loginButton.setBorderPainted(false);
        loginButton.setFocusPainted(false);
        loginButton.setPreferredSize(new java.awt.Dimension(150, 40));

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(loginButton, gbc);

        JLabel signupLabel = new JLabel("New user? Sign Up");
        signupLabel.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 14));
        signupLabel.setForeground(new java.awt.Color(100, 149, 237));
        signupLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        signupLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                new SignupFrame(LoginFrame.this);
                setVisible(false);
            }
        });

        gbc.gridy = 5;
        panel.add(signupLabel, gbc);

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
        errorLabel.setText("");
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());

        boolean success = dao.login(email, password);

        if (success) {
            dispose();
            new DashboardFrame();
            // TODO: open dashboard
        } else {
            errorLabel.setText("Invalid Email/Password");
        }
    }
}