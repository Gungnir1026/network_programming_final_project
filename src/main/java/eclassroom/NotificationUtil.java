package eclassroom;

import javax.swing.*;
import java.awt.*;

public class NotificationUtil {

    public void showNotification(JFrame frame, String message) {
        JLayeredPane layeredPane = frame.getLayeredPane();

        SwingUtilities.invokeLater(() -> {

            int width = 360;
            int height = 70;

            // ===== 卡片 =====
            JPanel card = new JPanel(null) { // ⚠ 用 null layout 才能放右上角 X
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(210, 233, 255));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                }
            };
            card.setOpaque(false);
            card.setBounds(30, 90, width, height);

            // ===== 左側色條 =====
            JPanel bar = new JPanel();
            bar.setBackground(new Color(0, 90, 181));
            bar.setBounds(0, 0, 8, height);
            card.add(bar);

            // ===== 內容 =====
            JLabel content = new JLabel("<html>" + escape(message).replace("&lt;br&gt;", "<br>") + "</html>");
            content.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            content.setBounds(18, 10, width - 50, height - 20);
            card.add(content);

            // ===== 關閉按鈕（自己畫 X）=====
            JButton btnClose = new JButton() {
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setStroke(new BasicStroke(2f));
                    g2.setColor(getForeground());

                    int p = 6;
                    g2.drawLine(p, p, getWidth() - p, getHeight() - p);
                    g2.drawLine(getWidth() - p, p, p, getHeight() - p);
                }
            };
            btnClose.setBounds(width - 26, 6, 18, 18);
            btnClose.setBorderPainted(false);
            btnClose.setContentAreaFilled(false);
            btnClose.setFocusPainted(false);
            btnClose.setForeground(new Color(80, 80, 80));
            btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // hover 變紅
            btnClose.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    btnClose.setForeground(Color.RED);
                }
                public void mouseExited(java.awt.event.MouseEvent e) {
                    btnClose.setForeground(new Color(80, 80, 80));
                }
            });

            btnClose.addActionListener(e -> {
                layeredPane.remove(card);
                layeredPane.repaint();
            });

            card.add(btnClose);

            layeredPane.add(card, JLayeredPane.POPUP_LAYER);
            layeredPane.repaint();

            // ===== 10 秒後自動消失 =====
            Timer timer = new Timer(10000, e -> {
                layeredPane.remove(card);
                layeredPane.repaint();
            });
            timer.setRepeats(false);
            timer.start();
        });
    }
    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
