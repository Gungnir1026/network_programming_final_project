package eclassroom;

import java.awt.*;
import java.io.PrintWriter;
import java.util.Hashtable;
import javax.swing.*;

public class StudentClient extends JFrame {

    // ========== 常數定義 ==========
    private static final Color STUDENT_DEFAULT_COLOR = Color.BLUE;
    private static final String ALLOW_DRAW_COMMAND = "ALLOW_DRAW"; 
    private static final String DISABLE_DRAW_COMMAND = "DISABLE_DRAW";

    // ========== 成員變數 ==========
    private WhiteboardPanel whiteboard;
    private final ClientConnection connection;
    private boolean isAllowed = false;
    private JToolBar toolbar;
    private Chatbox chatbox;
    private PrintWriter out;
    private String studentName;
    private NotificationUtil notificationUtil = new NotificationUtil();
    private QuizStudent quizFrame = null;
    private Color selectedColor = STUDENT_DEFAULT_COLOR;


    // ========== 建構子 ==========
    public StudentClient(String name, ClientConnection connection) {
        this.studentName = name+" (學生)";
        this.connection = connection;
        this.out = connection.writer();

        initializeWindow();
        initializeWhiteboard();
        initializeToolbar();

        connectToServer();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ========== 初始化方法 ==========
    private void initializeWindow() {
        setTitle("E-Classroom — " + studentName);
        setSize(1000, 680);
        setMinimumSize(new Dimension(900, 560));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent event) {
                connection.close();
                if (quizFrame != null) quizFrame.dispose();
            }
        });
        setLayout(new BorderLayout());
    }

    private void initializeWhiteboard() {
        whiteboard = new WhiteboardPanel();
        whiteboard.setEditable(false);
        whiteboard.setCurrentColor(STUDENT_DEFAULT_COLOR);
        whiteboard.setDrawListener(new WhiteboardPanel.DrawListener() {

            @Override
            public void onDrawBegin(int x, int y, Color color, int size) {
                if (!isAllowed) return;
                out.println("DRAW_BEGIN " + x + " " + y + " "
                    + color.getRed() + " " + color.getGreen() + " " + color.getBlue()
                    + " " + size);
            }

            @Override
            public void onDrawMove(int x1, int y1, int x2, int y2, Color color, int size) {
                if (!isAllowed) return;
                out.println("DRAW_MOVE " + x1 + " " + y1 + " " + x2 + " " + y2 + " "
                    + color.getRed() + " " + color.getGreen() + " " + color.getBlue()
                    + " " + size);
            }

            @Override
            public void onDrawEnd() {
                if (!isAllowed) return;
                out.println("DRAW_END");
            }

            @Override
            public void onEraseBegin(int size) {
                if (!isAllowed) return;
                out.println("ERASE_BEGIN " + size);
            }

            @Override
            public void onEraseMove(int x, int y) {
                if (!isAllowed) return;
                out.println("ERASE_MOVE " + x + " " + y);
            }

            @Override
            public void onEraseEnd() {
                if (!isAllowed) return;
                out.println("ERASE_END");
            }
        });
            add(whiteboard, BorderLayout.CENTER);
        }

    private void initializeToolbar() {
        toolbar = ToolbarFactory.createHorizontalToolbar(new ToolbarFactory.ToolAction() {
            @Override
            public void onToolSelected(ToolMode mode) {
                if (isAllowed) {
                    whiteboard.setToolMode(mode);
                    System.out.println("學生切換模式: " + mode);
                } else {
                    showUnauthorizedMessage();
                }
            }

            @Override
            public void onPenSettings() {
                if (!isAllowed) {
                    showUnauthorizedMessage();
                    return;
                }
                openPenToolDialog();
            }

            @Override
            public void onEraserSettings() {
                // 學生端不需要設定
            }

            @Override
            public void onUndo() {
                if (isAllowed) {
                    whiteboard.undo();
                    out.println("UNDO");
                }
            }

            @Override
            public void onRedo() {
                if (isAllowed) {
                    whiteboard.redo();
                    out.println("REDO");
                }
            }

            @Override
            public void onClear() {
                if (isAllowed) {
                    whiteboard.clearAll();
                    out.println("CLEAR");
                }
            }
        }, true);

        JButton btnRaiseHand = createButtonWithIcon("raised_hand.png", "舉手");
        btnRaiseHand.setPreferredSize(new Dimension(64,64));
        toolbar.add(btnRaiseHand);
        btnRaiseHand.addActionListener(e -> {
            if (out != null) {
                out.println("RAISE_HAND"); // 送出舉手指令
                notificationUtil.showNotification(this, "你已舉手，請等待老師回應。");
            }
        });

        // 2️⃣ 聊天室「只負責右邊」
        chatbox = new Chatbox(null);

        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.add(chatbox, BorderLayout.CENTER);

        // 3️⃣ 放位置（關鍵）
        add(toolbar, BorderLayout.NORTH);   // 工具列在上
        add(chatPanel, BorderLayout.EAST);  // 聊天室在右
        setToolbarEnabled(false);
    }

    private static JButton createButtonWithIcon(String path, String fallbackText) {
        return AppResources.iconButton(path, fallbackText);
    }

    // ========== 網路連線 ==========
    private void connectToServer() {
        chatbox.setWriter(out);
        connection.listen(this::processServerCommand, () -> {
            isAllowed = false;
            whiteboard.setEditable(false);
            for (Component component : toolbar.getComponents()) component.setEnabled(false);
            chatbox.setWriter(null);
            if (quizFrame != null) quizFrame.dispose();
            notificationUtil.showNotification(this, "與伺服器斷線，請關閉視窗後重新登入。");
        });
    }

    // ========== 指令處理 ==========
    private void processServerCommand(String msg) {
        if (WhiteboardProtocol.apply(msg, whiteboard)) return;
        if (msg.startsWith("ERROR|")) {
            notificationUtil.showNotification(this, msg.substring(6));
        } else if (msg.equals(ALLOW_DRAW_COMMAND)) {
            isAllowed = true;
            whiteboard.setEditable(true);
            setToolbarEnabled(true);
            whiteboard.setToolMode(ToolMode.PENCIL);
            notificationUtil.showNotification(this, "老師已授權，你可以開始操作了！");
        } else if (msg.equals(DISABLE_DRAW_COMMAND)) {
            isAllowed = false;
            whiteboard.setEditable(false);
            setToolbarEnabled(false);
            whiteboard.setToolMode(ToolMode.NONE);
            notificationUtil.showNotification(this, "老師已收回權限。");
        } else if (msg.startsWith("ANNOUNCE:")) {
            notificationUtil.showNotification(this, "老師公告：<br>" + msg.substring(9));
        } else if (msg.startsWith("QUIZ_DATA:")) {
            if (quizFrame == null) quizFrame = new QuizStudent(out);
            quizFrame.setQuizData(msg);
            quizFrame.setVisible(true);
        } else if (msg.startsWith("QUIZ_GOTO:")) {
            if (quizFrame != null) {
                quizFrame.showQuestion(Integer.parseInt(msg.substring(10)));
                quizFrame.setVisible(true);
            }
        } else if (msg.equals("QUIZ_END")) {
            if (quizFrame != null) quizFrame.endQuiz();
            quizFrame = null;
        } else if (msg.startsWith("QUIZ_RESULT:")) {
            if (quizFrame != null) {
                quizFrame.showResult(msg);
                quizFrame.setVisible(true);
            }
        } else {
            chatbox.appendMessage(msg);
        }
    }

    // ========== 輔助方法 ==========
    private void setToolbarEnabled(boolean enabled) {
        for (Component comp : toolbar.getComponents()) {
            if (comp instanceof JButton) {
                String name = comp.getName();
                if (name == null) continue;

                if (name.startsWith("BTN_")) {
                    comp.setEnabled(enabled);
                }
            }
        }
    }

    private void showUnauthorizedMessage() {
        JOptionPane.showMessageDialog(this, "老師尚未授權！", "權限不足", JOptionPane.WARNING_MESSAGE);
    }

    private void openPenToolDialog() {

        JDialog dialog = new JDialog(this, "畫筆設定", true);
        dialog.setSize(360, 400);
        dialog.setResizable(false);
        dialog.setLayout(new BorderLayout());

        Font uiFont = new Font(Font.SANS_SERIF, Font.PLAIN, 15);
        Font uiFontBold = new Font(Font.SANS_SERIF, Font.BOLD, 15);

        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JLabel title = new JLabel("選擇顏色：");
        title.setFont(uiFontBold);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        main.add(title);
        main.add(Box.createVerticalStrut(12));

        JPanel colorGrid = new JPanel(new GridLayout(2, 6, 12, 12));
        colorGrid.setAlignmentX(Component.CENTER_ALIGNMENT);

        Color[] colors = {
                Color.BLACK, Color.DARK_GRAY, Color.LIGHT_GRAY, Color.RED,
                Color.PINK, Color.ORANGE, Color.YELLOW, Color.GREEN,
                new Color(0,128,0), Color.BLUE, new Color(135,206,250),
                new Color(128,0,128)
        };

        // ===== 預覽區 =====
        JPanel previewPanel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = whiteboard.getBrushSize();
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                g2.setColor(
                    whiteboard.getToolMode() == ToolMode.ERASER ? Color.WHITE : whiteboard.getCurrentColor());
                g2.fillOval(x, y, size, size);

                g2.setColor(Color.DARK_GRAY);
                g2.drawOval(x, y, size, size);
            }
        };
        previewPanel.setPreferredSize(new Dimension(80, 80));

        // ===== 顏色按鈕 =====
        for (Color c : colors) {
            JButton btn = new JButton() {

                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);

                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int w = getWidth();
                    int h = getHeight();

                    int diameter = Math.min(w, h) - 12;
                    int x = (w - diameter) / 2;
                    int y = (h - diameter) / 2;

                    g2.setColor(c);
                    g2.fillOval(x, y, diameter, diameter);

                    g2.setColor(Color.GRAY);
                    g2.drawOval(x, y, diameter, diameter);

                    if (c.equals(selectedColor)) {
                        g2.setColor(Color.LIGHT_GRAY);
                        g2.setStroke(new BasicStroke(5f));
                        g2.drawOval(x - 3, y - 3, diameter + 6, diameter + 6);
                    }
                }

                public boolean contains(int px, int py) {
                    int w = getWidth(), h = getHeight();
                    int d = Math.min(w, h) - 12;
                    int x = (w - d) / 2;
                    int y = (h - d) / 2;

                    double dx = px - (x + d / 2.0);
                    double dy = py - (y + d / 2.0);
                    return dx * dx + dy * dy <= Math.pow(d / 2.0, 2);
                }
            };

            btn.setPreferredSize(new Dimension(55, 55));
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);

            btn.addActionListener(e -> {
                selectedColor = c;
                whiteboard.setCurrentColor(c);
                dialog.repaint();
                previewPanel.repaint();
            });

            colorGrid.add(btn);
        }

        main.add(colorGrid);
        main.add(Box.createVerticalStrut(20));

        // ===== 粗細調整 =====
        JPanel sizePanel = new JPanel();
        sizePanel.setLayout(new BoxLayout(sizePanel, BoxLayout.Y_AXIS));
        sizePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sizeLabel = new JLabel("粗細：");
        sizeLabel.setFont(uiFontBold);
        sizeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSlider sizeSlider = new JSlider(1, 20, whiteboard.getBrushSize());
        sizeSlider.setMajorTickSpacing(5);
        sizeSlider.setMinorTickSpacing(1);
        sizeSlider.setPaintTicks(true);
        sizeSlider.setPaintLabels(true);
        sizeSlider.setFont(uiFont);
        sizeSlider.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ===== 自訂刻度文字 =====
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(1,  new JLabel("1"));
        labelTable.put(5,  new JLabel("5"));
        labelTable.put(10, new JLabel("10"));
        labelTable.put(15, new JLabel("15"));
        labelTable.put(20, new JLabel("20"));

        sizeSlider.setLabelTable(labelTable);

        sizeSlider.addChangeListener(e -> {
            whiteboard.setBrushSize(sizeSlider.getValue());
            previewPanel.repaint();
        });

        sizePanel.add(sizeLabel);
        sizePanel.add(Box.createVerticalStrut(10));
        sizePanel.add(sizeSlider);

        main.add(sizePanel);
        main.add(Box.createVerticalStrut(20));

        JLabel previewLabel = new JLabel("筆刷預覽：");
        previewLabel.setFont(uiFontBold);
        previewLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        main.add(previewLabel);
        main.add(Box.createVerticalStrut(6));

        previewPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        main.add(previewPanel);

        dialog.add(main);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

}
