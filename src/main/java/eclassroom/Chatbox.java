package eclassroom;

import java.awt.*;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import javax.swing.*;
import javax.swing.text.*;

// ===== 聊天室（角色顏色版）=====
public class Chatbox extends JPanel {

    private JTextPane chatPane;
    private StyledDocument doc;
    private JTextField inputField;
    private PrintWriter out;

    // ===== 文字樣式 =====
    private Style teacherStyle;
    private Style studentStyle;
    private Style systemStyle;
    private Style normalStyle;

    public Chatbox(PrintWriter out) {
        this.out = out;

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(230, 300));
        setBackground(new Color(245, 246, 250));

        // ===== 標題列 =====
        JLabel title = new JLabel("聊天室");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        title.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        title.setOpaque(true);
        title.setBackground(new Color(230, 233, 240));
        add(title, BorderLayout.NORTH);

        // ===== 聊天內容區（JTextPane）=====
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        chatPane.setBackground(Color.WHITE);
        chatPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        doc = chatPane.getStyledDocument();
        initStyles();

        JScrollPane scrollPane = new JScrollPane(chatPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        scrollPane.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        add(scrollPane, BorderLayout.CENTER);

        // ===== 輸入區 =====
        JPanel inputPanel = new JPanel(new BorderLayout(6, 6));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        inputPanel.setBackground(new Color(245, 246, 250));

        inputField = new JTextField();
        inputField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

        JButton sendBtn = new JButton("送出");
        sendBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setBackground(new Color(52, 120, 246));
        sendBtn.setFocusPainted(false);
        sendBtn.setOpaque(true);
        sendBtn.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        ActionListener sendAction = e -> sendMessage();
        inputField.addActionListener(sendAction);
        sendBtn.addActionListener(sendAction);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);
    }

    // ===== 初始化顏色樣式 =====
    private void initStyles() {
        teacherStyle = doc.addStyle("teacher", null);
        StyleConstants.setForeground(teacherStyle, new Color(52, 120, 246)); // 藍
        StyleConstants.setBold(teacherStyle, true);

        studentStyle = doc.addStyle("student", null);
        StyleConstants.setForeground(studentStyle, new Color(100, 180, 120)); // 綠
        StyleConstants.setBold(studentStyle, true);

        systemStyle = doc.addStyle("system", null);
        StyleConstants.setForeground(systemStyle, new Color(157, 157, 157));
        StyleConstants.setItalic(systemStyle, true);

        normalStyle = doc.addStyle("normal", null);
        StyleConstants.setForeground(normalStyle, Color.BLACK);
    }

    // ===== 設定輸出串流 =====
    public void setWriter(PrintWriter out) {
        this.out = out;
    }

    // ===== 傳送訊息 =====
    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty() && out != null) {
            out.println("CHAT|" + text);
            inputField.setText("");
        }
    }

    // ===== 顯示訊息（重點）=====
    public void appendMessage(String msg) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> appendMessage(msg));
            return;
        }
        appendOnEventThread(msg);
    }

    private void appendOnEventThread(String msg) {
        try {
            Style style = normalStyle;

            if (msg.startsWith("SYSTEM|")) {
                style = systemStyle;
                msg = msg.substring(7); // 拿掉 SYSTEM|
            }
            else if (msg.contains("(老師)")) {
                style = teacherStyle;
            }
            else if (msg.contains("(學生)")) {
                style = studentStyle;
            }

            doc.insertString(doc.getLength(), msg + "\n", style);
            chatPane.setCaretPosition(doc.getLength());

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}
