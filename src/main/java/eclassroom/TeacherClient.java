package eclassroom;

import java.awt.*;
import java.io.PrintWriter;
import java.util.Hashtable;
import javax.swing.*;

public class TeacherClient extends JFrame {

    // ========== 常數定義 ==========
    private static final Color TEACHER_DEFAULT_COLOR = Color.BLACK;

    // ========== 成員變數 ==========
    private WhiteboardPanel whiteboard;
    private PrintWriter out;
    private final ClientConnection connection;
    private Color selectedColor = TEACHER_DEFAULT_COLOR;
    private Chatbox chatbox;
    private String teacherName;
    private StudentPicker pickWindow;
    private String currentUserListStr = ""; // 儲存目前使用者名單字串
    private NotificationUtil notificationUtil = new NotificationUtil();
    private QuizTeacher quizFrame;
    private OnlineStudentsWindow onlineWindow;
    private JToolBar toolbar;

    // ========== 建構子 ==========
    public TeacherClient(String name, ClientConnection connection) {
        this.teacherName = name + " (老師)";
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
        setTitle("E-Classroom — " + teacherName);
        setSize(1000, 680);
        setMinimumSize(new Dimension(900, 560));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent event) {
                connection.close();
                if (quizFrame != null) quizFrame.dispose();
                if (pickWindow != null) pickWindow.dispose();
                if (onlineWindow != null) onlineWindow.dispose();
            }
        });
        setLayout(new BorderLayout());
    }

    private void initializeWhiteboard() {
        whiteboard = new WhiteboardPanel();
        whiteboard.setDrawListener(new WhiteboardPanel.DrawListener(){
            // ========= 畫筆 =========
            @Override
            public void onDrawBegin(int x, int y, Color color, int size) {
                out.println("DRAW_BEGIN " + x + " " + y + " "
                    + color.getRed() + " " + color.getGreen() + " " + color.getBlue()
                    + " " + size);
            }

            @Override
            public void onDrawMove(int x1, int y1, int x2, int y2, Color color, int size) {
                out.println("DRAW_MOVE " + x1 + " " + y1 + " " + x2 + " " + y2 + " "
                    + color.getRed() + " " + color.getGreen() + " " + color.getBlue()
                    + " " + size);
            }

            @Override
            public void onDrawEnd() {
                out.println("DRAW_END");
            }

            // ========= 橡皮擦 =========
            @Override
            public void onEraseBegin(int size) {
                out.println("ERASE_BEGIN " + size);
            }

            @Override
            public void onEraseMove(int x, int y) {
                out.println("ERASE_MOVE " + x + " " + y);
            }

            @Override
            public void onEraseEnd() {
                out.println("ERASE_END");
            }
        });

        add(whiteboard, BorderLayout.CENTER);
    }

    private void initializeToolbar() {

        // 1️⃣ 建立工具列
        toolbar = ToolbarFactory.createHorizontalToolbar(
            new ToolbarFactory.ToolAction() {

                @Override
                public void onToolSelected(ToolMode mode) {
                    whiteboard.setToolMode(mode);
                    updateCursor(mode);
                }

                @Override
                public void onPenSettings() {
                    openPenToolDialog();
                }

                @Override
                public void onEraserSettings() {
                    openEraserDialog();
                }

                @Override
                public void onUndo() {
                    whiteboard.undo();
                    out.println("UNDO");
                }

                @Override
                public void onRedo() {
                    whiteboard.redo();
                    out.println("REDO");
                }

                @Override
                public void onClear() {
                    whiteboard.clearAll();
                    out.println("CLEAR");
                }
            },
            false
        );

        // 老師專用按鈕
        JButton pickBtn = createButtonWithIcon("drawing.png", "抽籤");
        pickBtn.setPreferredSize(new Dimension(64,64));
        toolbar.add(pickBtn);
        pickBtn.addActionListener(e -> {
            if (pickWindow == null || !pickWindow.isVisible()) {
                pickWindow = new StudentPicker(out, currentUserListStr);
                pickWindow.setVisible(true);
            } 
            else {
                pickWindow.toFront();
            }
            out.println("GET_USERS");
        });
        
        JButton btnAnnounce = createButtonWithIcon("announcement.png", "公告");
        btnAnnounce.setPreferredSize(new Dimension(64,64));
        toolbar.add(btnAnnounce);
        btnAnnounce.addActionListener(e -> {
            // 跳出一個輸入視窗讓老師打字
            String content = JOptionPane.showInputDialog(this, "請輸入公告內容：");
            
            // 確保老師有輸入東西，不是空的也不是按取消
            if (content != null && !content.trim().isEmpty()) {
                if (out != null) {
                    // 送出協定：ANNOUNCE:內容
                    out.println("ANNOUNCE:" + content);
                    // 老師自己也顯示一個通知確認已發送
                    notificationUtil.showNotification(this,"公告已發送：<br>" + content);
                }
            }
        });

        JButton btnOnlineUsers = createButtonWithIcon("users.png", "在線學生");
        btnOnlineUsers.setPreferredSize(new Dimension(64,64));
        toolbar.add(btnOnlineUsers);

        btnOnlineUsers.addActionListener(e -> {
            if (out == null) {
                JOptionPane.showMessageDialog(
                    this,
                    "尚未連線到伺服器，請稍候",
                    "系統提示",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            if (onlineWindow == null) onlineWindow = new OnlineStudentsWindow(out);
            onlineWindow.setVisible(true);
            out.println("GET_USERS");
        });

        JButton btnQuiz = createButtonWithIcon("quiz.png", "測驗");
        btnQuiz.setPreferredSize(new Dimension(64,64));
        toolbar.add(btnQuiz);

        btnQuiz.addActionListener(e -> {
            // 按下按鈕時，建立並顯示視窗
            if (quizFrame == null || !quizFrame.isVisible()) {
                quizFrame = new QuizTeacher(out);
                quizFrame.setVisible(true);
            } else {
                quizFrame.toFront();
            }
        });

        // 2️⃣ 聊天室「只負責右邊」
        chatbox = new Chatbox(null);

        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.add(chatbox, BorderLayout.CENTER);

        // 3️⃣ 放位置（關鍵）
        add(toolbar, BorderLayout.NORTH);   // 工具列在上
        add(chatPanel, BorderLayout.EAST);  // 聊天室在右
        setToolbarEnabled(true);
    }

    private static JButton createButtonWithIcon(String path, String fallbackText) {
        return AppResources.iconButton(path, fallbackText);
    }

    private void setToolbarEnabled(boolean enabled) {
        for (Component c : toolbar.getComponents()) {
            if (c instanceof JButton) {
                c.setEnabled(enabled);
            }
        }
    }


    // ========== 網路連線 ==========
    private void connectToServer() {
        chatbox.setWriter(out);
        connection.listen(this::processServerCommand, () -> {
            setToolbarEnabled(false);
            whiteboard.setEditable(false);
            chatbox.setWriter(null);
            if (quizFrame != null) quizFrame.dispose();
            if (pickWindow != null) pickWindow.dispose();
            if (onlineWindow != null) onlineWindow.dispose();
            notificationUtil.showNotification(this, "與伺服器斷線，請關閉視窗後重新登入。");
        });
    }

    private void processServerCommand(String msg) {
        if (WhiteboardProtocol.apply(msg, whiteboard)) return;
        if (msg.startsWith("ERROR|")) {
            notificationUtil.showNotification(this, msg.substring(6));
        } else if (msg.startsWith("USER_LIST:")) {
            currentUserListStr = msg.substring(10);
            if (pickWindow != null && pickWindow.isVisible()) pickWindow.updateList(currentUserListStr);
            if (onlineWindow != null && onlineWindow.isVisible()) onlineWindow.updateList(currentUserListStr);
        } else if (msg.startsWith("HAND_RAISED:")) {
            notificationUtil.showNotification(this, msg.substring(12) + " 舉手了！");
        } else if (msg.startsWith("SUBMIT_ANS:")) {
            if (quizFrame != null && quizFrame.isVisible()) quizFrame.processStudentAnswer(msg);
        } else {
            chatbox.appendMessage(msg);
        }
    }

    // ========== 選橡皮擦時鼠標是手 ==========
    private void updateCursor(ToolMode mode) {
        if (mode == ToolMode.ERASER) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
    }
    // ========== 畫筆設定視窗 ==========
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

    // ========= 橡皮擦設定視窗 =========
    private void openEraserDialog() {

        JDialog dialog = new JDialog(this, "橡皮擦設定", true);
        dialog.setSize(360, 250);
        dialog.setResizable(false);
        dialog.setLayout(new BorderLayout());

        Font uiFont = new Font(Font.SANS_SERIF, Font.PLAIN, 15);
        Font uiFontBold = new Font(Font.SANS_SERIF, Font.BOLD, 15);

        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        // ===== 粗細標題 =====
        JLabel sizeLabel = new JLabel("粗細：");
        sizeLabel.setFont(uiFontBold);
        sizeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        main.add(sizeLabel);
        main.add(Box.createVerticalStrut(10));

        // ===== 粗細滑桿 =====
        JSlider sizeSlider = new JSlider(1, 100, whiteboard.getEraserSize());
        sizeSlider.setMajorTickSpacing(20);
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

        // ===== 橡皮擦預覽 =====
        JPanel previewPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );

                int size = whiteboard.getEraserSize();
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                g2.setColor(Color.LIGHT_GRAY);
                g2.fillOval(x, y, size, size);

                g2.setColor(Color.DARK_GRAY);
                g2.drawOval(x, y, size, size);
            }
        };
        previewPanel.setPreferredSize(new Dimension(80, 80));
        previewPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 滑桿一動就重畫預覽
        sizeSlider.addChangeListener(e -> {
            whiteboard.setEraserSize(sizeSlider.getValue());
            previewPanel.repaint();
        });

        main.add(sizeSlider);
        main.add(Box.createVerticalStrut(20));

        // ===== 預覽標題 =====
        JLabel previewLabel = new JLabel("橡皮擦預覽：");
        previewLabel.setFont(uiFontBold);
        previewLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        main.add(previewLabel);
        main.add(Box.createVerticalStrut(6));
        main.add(previewPanel);

        dialog.add(main);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    
}
