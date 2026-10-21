package eclassroom;

import java.awt.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;

public class OnlineStudentsWindow extends JFrame {

    private JPanel listPanel;
    private PrintWriter out;
    private JLabel lblCount;

    private final Font titleFont  = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    private final Font normalFont = new Font(Font.SANS_SERIF, Font.PLAIN, 14);

    private final Color bgColor      = new Color(245, 247, 250);
    private final Color cardColor    = Color.WHITE;
    private final Color primaryColor = new Color(52, 120, 190);
    private java.util.HashSet<String> authorizedStudents = new java.util.HashSet<>();


    public OnlineStudentsWindow(PrintWriter out) {
        setTitle("在線學生名單");
        setSize(360, 420);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.out = out;
        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(bgColor);
        root.setBorder(new EmptyBorder(12, 14, 14, 14));
        setContentPane(root);

        // ===== 標題卡片 =====
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(cardColor);
        header.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 220, 220)),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel lblTitle = new JLabel("線上學生");
        lblTitle.setFont(titleFont);
        lblTitle.setForeground(primaryColor);

        lblCount = new JLabel("人數：0");
        lblCount.setFont(normalFont);

        header.add(lblTitle, BorderLayout.WEST);
        header.add(lblCount, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);

        // ===== 名單卡片 =====
        JPanel listCard = new JPanel(new BorderLayout());
        listCard.setBackground(cardColor);
        listCard.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 220, 220)),
                new EmptyBorder(10, 10, 10, 10)
        ));

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(new Color(250, 250, 250));

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);

        listCard.add(scroll, BorderLayout.CENTER);


        root.add(listCard, BorderLayout.CENTER);
    }

    // ===== 更新名單 =====
    public void updateList(String data) {

        listPanel.removeAll();

        if (data == null || data.isEmpty()) {
            lblCount.setText("人數：0");
            listPanel.add(new JLabel("目前沒有學生在線。"));
            return;
        }

        String[] parts = data.split(",");
        authorizedStudents.retainAll(java.util.Arrays.asList(parts));
        int count = 0;

        try {
            count = Integer.parseInt(parts[0]);
        } catch (Exception ignored) {}

        lblCount.setText("人數：" + count);

        for (int i = 1; i < parts.length; i++) {

            String name = parts[i].trim();
            if (name.isEmpty()) continue;

            // 只給學生授權
            if (!name.contains("(學生)")) continue;
            boolean isAuthorized = authorizedStudents.contains(name);

            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
            row.setBackground(cardColor);
            row.setBorder(new CompoundBorder(
                    new LineBorder(new Color(230, 230, 230)),
                    new EmptyBorder(4, 8, 4, 8)
            ));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            row.setPreferredSize(new Dimension(300, 36));
            row.setMinimumSize(new Dimension(200, 36));

            JLabel lblName = new JLabel(name);
            lblName.setFont(normalFont);

            lblName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

            JButton btnAllow = new JButton(isAuthorized ? "收回" : "授權");
            btnAllow.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            btnAllow.setPreferredSize(new Dimension(60, 24));
            btnAllow.setMaximumSize(new Dimension(60, 24));
            btnAllow.setMinimumSize(new Dimension(60, 24));

            btnAllow.setBackground(isAuthorized ? new Color(180,180,180) : primaryColor);
            btnAllow.setForeground(Color.WHITE);
            btnAllow.setFocusPainted(false);
            btnAllow.setBorderPainted(false);
            btnAllow.setBorder(new EmptyBorder(2, 6, 2, 6));
            btnAllow.setOpaque(true);
            btnAllow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            btnPanel.setOpaque(false);
            btnPanel.add(btnAllow);

            row.add(lblName);
            row.add(Box.createHorizontalGlue()); // ⭐ 把按鈕推到最右
            row.add(btnAllow);
            btnAllow.addActionListener(e -> {

                if (out == null) {
                    JOptionPane.showMessageDialog(
                        this,
                        "連線尚未就緒，無法操作",
                        "錯誤",
                        JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }

                if (!authorizedStudents.contains(name)) {
                    // ===== 授權 =====
                    out.println("ALLOW_DRAW_TO:" + name);
                    authorizedStudents.add(name);

                    btnAllow.setText("收回");
                    btnAllow.setBackground(Color.GRAY);

                    System.out.println("【老師端】已授權：" + name);

                } else {
                    // ===== 收回 =====
                    out.println("DISALLOW_DRAW_TO:" + name);
                    authorizedStudents.remove(name);

                    btnAllow.setText("授權");
                    btnAllow.setBackground(primaryColor);

                    System.out.println("【老師端】已收回權限：" + name);
                }
            });



            listPanel.add(row);
            listPanel.add(Box.createVerticalStrut(6));
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

}
