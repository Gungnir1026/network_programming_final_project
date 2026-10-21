package eclassroom;

import javax.swing.*;
import java.awt.*;

public class Main {

    // 表單 Label 與輸入框尺寸設定
    private static final int LABEL_WIDTH = 70;
    private static final int FIELD_WIDTH = 160;
    private static final int FIELD_HEIGHT = 32;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createLoginLauncher);
    }

    private static void createLoginLauncher() {

        // ===== 主視窗 =====
        JFrame frame = new JFrame("電子白板 - 登入");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(420, 520);
        frame.setResizable(false);

        // ===== 深色背景 =====
        JPanel background = new JPanel(new GridBagLayout());
        background.setBackground(new Color(30, 30, 70));
        frame.setContentPane(background);

        GridBagConstraints gbc = new GridBagConstraints();

        // ===== 白色圓角登入卡片 =====
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );

                g2.setColor(Color.WHITE);
                g2.fillRoundRect(
                    0, 0,
                    getWidth(), getHeight(),
                    20, 20  
                );
            }
        };
        card.setPreferredSize(new Dimension(320, 420));
        card.setBackground(Color.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // ===== 標題 =====
        JLabel title = new JLabel("電子白板系統");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        title.setForeground(new Color(40, 40, 40));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Teacher / Student Login");
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        subtitle.setForeground(new Color(120, 120, 120));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(25));

        // ===== 輸入欄位 =====
        JTextField txtName = new JTextField();
        JTextField txtIP   = new JTextField("127.0.0.1");
        JTextField txtPort = new JTextField("5555");

        card.add(createFormRow("姓名 :", txtName));
        card.add(Box.createVerticalStrut(14));

        card.add(createFormRow("Server IP :", txtIP));
        card.add(Box.createVerticalStrut(14));

        card.add(createFormRow("Port :", txtPort));
        card.add(Box.createVerticalStrut(26));
        
        // ===== 身分選擇按鈕 =====
        JButton btnTeacher = createMainButton("我是老師", new Color(52, 120, 246));
        JButton btnStudent = createMainButton("我是學生", new Color(100, 180, 120));

        btnTeacher.addActionListener(e -> login(frame, txtName, txtIP, txtPort,
                btnTeacher, btnStudent, true));
        btnStudent.addActionListener(e -> login(frame, txtName, txtIP, txtPort,
                btnTeacher, btnStudent, false));
        frame.getRootPane().setDefaultButton(btnStudent);

        card.add(btnTeacher);
        card.add(Box.createVerticalStrut(10));
        card.add(btnStudent);

        // 將卡片置中放入背景
        gbc.gridx = 0;
        gbc.gridy = 0;
        background.add(card, gbc);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ===== 建立主要按鈕（老師 / 學生）=====
    private static JButton createMainButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setPreferredSize(new Dimension(240, 40));
        btn.setMaximumSize(new Dimension(240, 40));
        return btn;
    }

    // ===== 建立「Label + TextField」橫列 =====
    private static JPanel createFormRow(String labelText, JTextField field) {

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ===== Label =====
        JLabel label = new JLabel(labelText);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        label.setForeground(new Color(90, 90, 90));
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setPreferredSize(new Dimension(LABEL_WIDTH, FIELD_HEIGHT));
        label.setMinimumSize(new Dimension(LABEL_WIDTH, FIELD_HEIGHT));
        label.setMaximumSize(new Dimension(LABEL_WIDTH, FIELD_HEIGHT));

        // ===== TextField =====
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        field.setMaximumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        field.setBorder(BorderFactory.createMatteBorder(
                0, 0, 1, 0,
                new Color(180, 180, 180)
        ));
        field.setBackground(Color.WHITE);
        field.setForeground(new Color(40, 40, 40));

        row.add(label);
        row.add(Box.createHorizontalStrut(12));
        row.add(field);

        return row;
    }
    static String validateName(String rawName) {
        String name = rawName.trim();
        if (name.isEmpty() || name.length() > 40 || name.matches("(?s).*[,;:|\\r\\n].*")) {
            throw new IllegalArgumentException("姓名須為 1–40 個字，且不可包含 , ; : | 或換行。");
        }
        return name;
    }

    static int validatePort(String rawPort) {
        try {
            int port = Integer.parseInt(rawPort.trim());
            if (port >= 1 && port <= 65535) return port;
        } catch (NumberFormatException ignored) {}
        throw new IllegalArgumentException("Port 請輸入 1–65535 的整數。");
    }

    private static void login(JFrame frame, JTextField nameField, JTextField hostField,
                              JTextField portField, JButton teacherButton,
                              JButton studentButton, boolean teacher) {
        final String name;
        final String host;
        final int port;
        try {
            name = validateName(nameField.getText());
            port = validatePort(portField.getText());
            host = hostField.getText().trim();
            if (host.isEmpty()) throw new IllegalArgumentException("請輸入伺服器 IP 或主機名稱。");
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage(), "登入資料", JOptionPane.WARNING_MESSAGE);
            return;
        }
        teacherButton.setEnabled(false);
        studentButton.setEnabled(false);
        String nickname = name + (teacher ? " (老師)" : " (學生)");
        new SwingWorker<ClientConnection, Void>() {
            @Override protected ClientConnection doInBackground() throws Exception {
                return ClientConnection.connect(host, port, nickname);
            }

            @Override protected void done() {
                try {
                    ClientConnection connection = get();
                    if (teacher) new TeacherClient(name, connection);
                    else new StudentClient(name, connection);
                    frame.dispose();
                } catch (Exception e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    JOptionPane.showMessageDialog(frame,
                            "無法登入 " + host + ":" + port + "\n" + cause.getMessage(),
                            "連線失敗", JOptionPane.ERROR_MESSAGE);
                    teacherButton.setEnabled(true);
                    studentButton.setEnabled(true);
                }
            }
        }.execute();
    }
}
