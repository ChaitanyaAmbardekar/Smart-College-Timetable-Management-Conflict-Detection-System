package in.frcce.ecs.ui;

import in.frcce.ecs.model.User;
import in.frcce.ecs.service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Login screen — entry point of the UI.
 */
public class LoginFrame extends JFrame {

    private final AuthService authService = new AuthService();

    private JTextField  usernameField;
    private JPasswordField passwordField;
    private JButton     loginButton;
    private JLabel      statusLabel;

    public LoginFrame() {
        super("ECS Timetable Generator — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        buildUI();
        pack();
        setLocationRelativeTo(null);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(0x1A, 0x73, 0xE8));

        // ---- Header banner ----
        JPanel header = new JPanel(new GridLayout(3, 1, 0, 2));
        header.setBackground(new Color(0x1A, 0x73, 0xE8));
        header.setBorder(new EmptyBorder(28, 30, 20, 30));

        JLabel college = new JLabel("Fr. Conceicao Rodrigues College of Engineering", SwingConstants.CENTER);
        college.setFont(new Font("Segoe UI", Font.BOLD, 14));
        college.setForeground(Color.WHITE);

        JLabel dept = new JLabel("Electronics & Computer Science Department", SwingConstants.CENTER);
        dept.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dept.setForeground(new Color(220, 230, 255));

        JLabel appTitle = new JLabel("Faculty Timetable Generator", SwingConstants.CENTER);
        appTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        appTitle.setForeground(Color.WHITE);

        header.add(college);
        header.add(dept);
        header.add(appTitle);

        // ---- Form panel ----
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(30, 40, 30, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 4, 6, 4);

        JLabel iconLabel = new JLabel("\uD83D\uDD12", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        form.add(iconLabel, gbc);

        gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 1; form.add(new JLabel("Username:"), gbc);
        usernameField = new JTextField(18);
        usernameField.putClientProperty("JTextField.placeholderText", "Enter username");
        gbc.gridx = 1; gbc.gridy = 1; form.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; form.add(new JLabel("Password:"), gbc);
        passwordField = new JPasswordField(18);
        passwordField.putClientProperty("JTextField.placeholderText", "Enter password");
        gbc.gridx = 1; gbc.gridy = 2; form.add(passwordField, gbc);

        loginButton = new JButton("Log In");
        loginButton.setBackground(new Color(0x1A, 0x73, 0xE8));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(14, 4, 4, 4);
        form.add(loginButton, gbc);

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = 4;
        form.add(statusLabel, gbc);

        // Default credentials hint
        JLabel hint = new JLabel("<html><i>Admin: admin / admin123 &nbsp;|&nbsp; Faculty: vvg / faculty123</i></html>",
                SwingConstants.CENTER);
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(Color.GRAY);
        gbc.gridy = 5;
        form.add(hint, gbc);

        root.add(header, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        setContentPane(root);

        // Actions
        ActionListener loginAction = e -> attemptLogin();
        loginButton.addActionListener(loginAction);
        passwordField.addActionListener(loginAction);
        usernameField.addActionListener(loginAction);

        getRootPane().setDefaultButton(loginButton);
        SwingUtilities.invokeLater(usernameField::requestFocusInWindow);
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter both username and password.");
            return;
        }

        loginButton.setEnabled(false);
        statusLabel.setText("Authenticating...");
        statusLabel.setForeground(Color.GRAY);

        SwingWorker<java.util.Optional<User>, Void> worker = new SwingWorker<>() {
            @Override
            protected java.util.Optional<User> doInBackground() {
                return authService.login(username, password);
            }

            @Override
            protected void done() {
                try {
                    var result = get();
                    if (result.isPresent()) {
                        dispose();
                        new MainFrame(result.get(), authService).setVisible(true);
                    } else {
                        statusLabel.setText("Invalid username or password.");
                        statusLabel.setForeground(Color.RED);
                        passwordField.setText("");
                        passwordField.requestFocusInWindow();
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                    statusLabel.setForeground(Color.RED);
                } finally {
                    loginButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }
}

