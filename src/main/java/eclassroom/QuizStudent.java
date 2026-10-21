package eclassroom;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class QuizStudent extends JFrame {

    // ========== UI 元件 ==========
    private JLabel lblQuestionInfo;
    private JTextArea txtQuestionBody;
    private JPanel optionsPanel;
    private JLabel lblStatus;

    // ========== 資料與連線 ==========
    private PrintWriter out;
    private List<QuestionObj> questionList = new ArrayList<>();
    private int currentQuestionIndex = -1;
    private boolean hasAnsweredCurrent = false;
    private final java.util.Set<Integer> answeredPages = new java.util.HashSet<>();

    private static class QuestionObj {
        String questionText;
        String[] options;
        public QuestionObj(String q, String[] opts) {
            this.questionText = q;
            this.options = opts;
        }
    }

    public QuizStudent(PrintWriter out) {
        this.out = out;

        setTitle("隨堂測驗（學生端）");
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());

        getContentPane().setBackground(new Color(245, 247, 250));

        initUI();

        setLocationRelativeTo(null);
        
        showWaitingScreen();
    }

    private void initUI() {

        // ===== 標題列 =====
        lblQuestionInfo = new JLabel("測驗準備中...");
        lblQuestionInfo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        lblQuestionInfo.setHorizontalAlignment(SwingConstants.CENTER);
        lblQuestionInfo.setOpaque(true);
        lblQuestionInfo.setBackground(Color.WHITE);
        lblQuestionInfo.setForeground(new Color(52, 120, 190));
        lblQuestionInfo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(12, 0, 12, 0)
        ));
        add(lblQuestionInfo, BorderLayout.NORTH);

        // ===== 中央區 =====
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        txtQuestionBody = new JTextArea();
        txtQuestionBody.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        txtQuestionBody.setLineWrap(true);
        txtQuestionBody.setWrapStyleWord(true);
        txtQuestionBody.setEditable(false);
        txtQuestionBody.setBackground(Color.WHITE);
        txtQuestionBody.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));

        JScrollPane qScroll = new JScrollPane(txtQuestionBody);
        qScroll.setBorder(null);
        centerPanel.add(qScroll, BorderLayout.CENTER);

        optionsPanel = new JPanel();
        optionsPanel.setOpaque(false);
        optionsPanel.setLayout(new GridLayout(4, 1, 12, 12));
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        centerPanel.add(optionsPanel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // ===== 狀態列 =====
        lblStatus = new JLabel("等待老師發卷...");
        lblStatus.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
        lblStatus.setOpaque(true);
        lblStatus.setBackground(Color.WHITE);
        lblStatus.setBorder(BorderFactory.createMatteBorder(
                1, 0, 0, 0, new Color(220, 220, 220)
        ));
        add(lblStatus, BorderLayout.SOUTH);
    }

    // ================= 題庫與作答流程 =================

    public void setQuizData(String rawData) {
        try {
            questionList.clear();
            answeredPages.clear();
            hasAnsweredCurrent = false;
            currentQuestionIndex = -1;
            txtQuestionBody.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
            showWaitingScreen();
            String content = rawData.substring("QUIZ_DATA:".length());
            String[] parts = content.split(";;");

            for (int i = 1; i < parts.length; i++) {
                String[] items = parts[i].split("\\|");
                if (items.length >= 2) {
                    String[] opts = new String[items.length - 1];
                    System.arraycopy(items, 1, opts, 0, opts.length);
                    questionList.add(new QuestionObj(items[0], opts));
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "試卷解析失敗");
        }
    }

    public void showQuestion(int pageIndex) {
        if (questionList.isEmpty()) return;

        int realIndex = pageIndex - 1;
        if (realIndex >= 0 && realIndex < questionList.size()) {

            currentQuestionIndex = realIndex;
            hasAnsweredCurrent = answeredPages.contains(realIndex);

            QuestionObj q = questionList.get(realIndex);

            lblQuestionInfo.setText("第 " + pageIndex + " 題 / 共 " + questionList.size() + " 題");
            txtQuestionBody.setText(q.questionText);

            optionsPanel.removeAll();
            optionsPanel.setLayout(new GridLayout(q.options.length, 1, 12, 12));

            for (int i = 0; i < q.options.length; i++) {
                JButton btn = new JButton(q.options[i]);
                btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
                btn.setFocusPainted(false);
                btn.setBackground(Color.WHITE);
                btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(200, 200, 200)),
                        BorderFactory.createEmptyBorder(12, 16, 12, 16)
                ));

                int myAnswerIndex = i;
                btn.addActionListener(e -> submitAnswer(myAnswerIndex));

                optionsPanel.add(btn);
            }

            if (hasAnsweredCurrent) disableAllButtons();
            lblStatus.setText(hasAnsweredCurrent ? "本題已作答，等待下一題..." : "請作答...");
            lblStatus.setForeground(new Color(52, 120, 190));

            optionsPanel.revalidate();
            optionsPanel.repaint();
        }
    }

    private void submitAnswer(int answerIndex) {
        if (hasAnsweredCurrent) return;

        hasAnsweredCurrent = true;
        answeredPages.add(currentQuestionIndex);
        out.println("SUBMIT_ANS:" + (currentQuestionIndex + 1) + ":" + answerIndex);

        disableAllButtons();
        lblStatus.setText("已送出答案！等待下一題...");
        lblStatus.setForeground(new Color(0, 128, 0));
    }

    private void disableAllButtons() {
        for (Component c : optionsPanel.getComponents()) {
            if (c instanceof JButton) {
                JButton b = (JButton) c;
                b.setEnabled(false);
                b.setBackground(new Color(230, 230, 230));
                b.setForeground(Color.DARK_GRAY);
            }
        }
    }

    private void showWaitingScreen() {
        txtQuestionBody.setText("試卷下載完成。\n\n請等待老師切換題目。");
        optionsPanel.removeAll();
        optionsPanel.revalidate();
        optionsPanel.repaint();
    }

    public void endQuiz() {
        dispose();
    }

    public void showResult(String resultProtocol) {
        hasAnsweredCurrent = true;
        lblQuestionInfo.setText("測驗結束 - 成績公佈");
        lblStatus.setText("測驗已結束");
        lblStatus.setForeground(Color.RED);
        optionsPanel.removeAll();
        optionsPanel.revalidate();
        optionsPanel.repaint();

        try {
            String data = resultProtocol.substring("QUIZ_RESULT:".length());
            if (data.equals("NO_DATA")) {
                txtQuestionBody.setText("本次測驗尚無學生作答。");
                return;
            }

            StringBuilder sb = new StringBuilder();

            sb.append(String.format("%-6s %-20s %s\n", "名次", "姓名", "分數"));
            sb.append("------------------------------------\n");

            for (String row : data.split(";")) {
                if (!row.trim().isEmpty()) {
                    String[] c = row.split(",");
                    sb.append(String.format(
                        "%-6s %-17s %s 分\n",
                        c[0],   // 名次
                        c[1],   // 姓名
                        c[2]    // 分數
                    ));
                }
            }

            txtQuestionBody.setFont(new Font("Monospaced", Font.BOLD, 14));
            txtQuestionBody.setText(sb.toString());

        } catch (Exception e) {
            txtQuestionBody.setText("成績解析錯誤");
        }
    }
}
