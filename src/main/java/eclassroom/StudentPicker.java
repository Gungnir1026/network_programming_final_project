package eclassroom;

import java.awt.*;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

public class StudentPicker extends JFrame {

    private List<String> studentNames = new ArrayList<>();
    private JTextArea resultArea;
    private JLabel lblTotal;

    private PrintWriter out;

    // ===== 字型 =====
    private final Font titleFont  = new Font(Font.SANS_SERIF, Font.BOLD, 20);
    private final Font normalFont = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    private final Font resultFont = new Font(Font.SANS_SERIF, Font.BOLD, 16);

    // ===== 主色系 =====
    private final Color primaryColor = new Color(52, 120, 190);
    private final Color bgColor = new Color(245, 247, 250);

    public StudentPicker(PrintWriter out, String currentListInfo) {
        this.out = out;

        setTitle("課堂抽籤系統");
        setSize(480, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initUI();
        updateList(currentListInfo);

        setVisible(true);
    }

    // ================= UI =================
    private void initUI() {

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(bgColor);
        root.setBorder(new EmptyBorder(14, 16, 16, 16));
        setContentPane(root);

        // ===== 標題卡片 =====
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 220, 220)),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel lblTitle = new JLabel("課堂抽籤系統");
        lblTitle.setFont(titleFont);
        lblTitle.setForeground(primaryColor);

        lblTotal = new JLabel("線上人數：0 人");
        lblTotal.setFont(normalFont);

        header.add(lblTitle, BorderLayout.WEST);
        header.add(lblTotal, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);

        // ===== 中央區 =====
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new EmptyBorder(14, 0, 0, 0));

        // ===== 操作卡片 =====
        JPanel controlCard = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 10));
        controlCard.setBackground(Color.WHITE);
        controlCard.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 220, 220)),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel lblNum = new JLabel("抽出人數");
        lblNum.setFont(normalFont);

        JTextField txtNumber = new JTextField("1");
        txtNumber.setFont(normalFont);
        txtNumber.setPreferredSize(new Dimension(60, 28));
        txtNumber.setHorizontalAlignment(JTextField.CENTER);

        JButton btnDraw = new JButton("開始抽籤並廣播");
        btnDraw.setFont(normalFont);
        btnDraw.setForeground(Color.WHITE);
        btnDraw.setBackground(primaryColor);
        btnDraw.setFocusPainted(false);
        btnDraw.setBorder(new EmptyBorder(6, 18, 6, 18));

        controlCard.add(lblNum);
        controlCard.add(txtNumber);
        controlCard.add(btnDraw);

        center.add(controlCard);
        center.add(Box.createVerticalStrut(14));

        // ===== 結果卡片 =====
        JPanel resultCard = new JPanel(new BorderLayout());
        resultCard.setBackground(Color.WHITE);
        resultCard.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 220, 220)),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel lblResultTitle = new JLabel("抽籤結果公告");
        lblResultTitle.setFont(normalFont);
        lblResultTitle.setBorder(new EmptyBorder(0, 0, 6, 0));

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(resultFont);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setBackground(new Color(250, 250, 250));
        resultArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(resultArea);
        scroll.setBorder(null);

        resultCard.add(lblResultTitle, BorderLayout.NORTH);
        resultCard.add(scroll, BorderLayout.CENTER);

        center.add(resultCard);
        root.add(center, BorderLayout.CENTER);

        // ===== 按鈕事件 =====
        btnDraw.addActionListener(e -> {
            try {
                int num = Integer.parseInt(txtNumber.getText());
                doDraw(num);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "請輸入正確的數字", "錯誤", JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    // ================= 名單同步 =================
    public void updateList(String rawData) {
        studentNames.clear();

        if (rawData == null || rawData.trim().isEmpty()) {
            lblTotal.setText("線上人數：0 人");
            resultArea.setText("目前沒有學生連線。");
            return;
        }

        String[] parts = rawData.split(",");

        try {
            int total = Integer.parseInt(parts[0]);
            lblTotal.setText("線上人數：" + total + " 人");
        } catch (Exception e) {
            lblTotal.setText("線上人數：讀取中...");
        }

        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].trim().isEmpty()) {
                studentNames.add(parts[i].trim());
            }
        }

        resultArea.setText(
                "名單同步完成\n" +
                "目前抽籤池共有 " + studentNames.size() + " 人\n\n" +
                "請設定抽籤人數後開始抽籤。"
        );
    }

    // ================= 抽籤邏輯 =================
    private void doDraw(int num) {

        if (studentNames.isEmpty()) {
            JOptionPane.showMessageDialog(this, "目前沒有可抽籤的學生", "錯誤", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (num <= 0 || num > studentNames.size()) {
            JOptionPane.showMessageDialog(this, "抽出人數不合法", "錯誤", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> pool = new ArrayList<>(studentNames);
        Collections.shuffle(pool);

        StringBuilder display = new StringBuilder("本次中籤名單\n\n");
        StringBuilder broadcast = new StringBuilder();

        for (int i = 0; i < num; i++) {
            String winner = pool.get(i);
            display.append(i + 1).append(". ").append(winner).append("\n");
            broadcast.append(winner).append(" ");
        }

        resultArea.setText(display.toString());

        if (out != null) {
            out.println("SYSTEM|抽籤結果公布：" + broadcast.toString());
        }
    }
}
