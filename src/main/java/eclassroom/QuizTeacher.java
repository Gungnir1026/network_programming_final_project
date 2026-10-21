package eclassroom;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class QuizTeacher extends JFrame {
    
    // ========== 資料結構 ==========
    private List<Question> questions = new ArrayList<>();
    private QuizScoreboard scoreboard;
    private boolean quizStarted;
    private boolean quizEnded;
    private int currentQuestionIndex = 0; // 0-based index for logic
    private PrintWriter out;

    // ========== UI 元件 ==========
    private JTextArea txtQuestionPreview;
    private JTextArea txtLog; // 右側的即時訊息欄
    private JButton btnPrev, btnNext, btnEnd, btnSendQuiz;
    private JLabel lblCurrentPage;

    // ========== 內部類別：老師版題目 (包含答案) ==========
    private static class Question {
        String text;
        String[] options;
        int correctOptionIndex; // 正確答案是第幾個 (0-based)
        int points; // 這題幾分

        public Question(String text, String[] options, int correctIndex, int points) {
            this.text = text;
            this.options = options;
            this.correctOptionIndex = correctIndex;
            this.points = points;
        }
    }

    public QuizTeacher(PrintWriter out) {
        this.out = out;
        setTitle("隨堂測驗 (老師控制台)");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // 1. 初始化內建示範題庫
        loadQuestions();
        scoreboard = new QuizScoreboard(
                questions.stream().mapToInt(q -> q.correctOptionIndex).toArray(),
                questions.stream().mapToInt(q -> q.points).toArray(),
                questions.stream().mapToInt(q -> q.options.length).toArray());

        // 2. 建立 UI
        initUI();

        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (out != null) {
                    out.println("QUIZ_END");
                }
            }
        });
    }

    // ========== 內建示範題庫 ==========

    private void loadQuestions() {
        // 可在此修改題幹、選項、正確答案與配分；目前未提供題庫匯入功能。
        questions.add(new Question(
            "1+1=？",
            new String[]{"2", "3", "4"},
            0,
            10
        ));
        questions.add(new Question(
            "這堂課的名稱叫什麼？",
            new String[]{"網頁程式設計", "網路程式設計", "網路城市設計", "網頁城市設計"},
            1,
            45
        ));
        questions.add(new Question(
            "資訊工程學系的英文縮寫是？",
            new String[]{"CEE", "CSIE", "EE", "CM"},
            1,
            45
        ));

        
    }


    private void initUI() {

        // ===== 左側：題目卡片 =====
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JLabel lblLeftTitle = new JLabel("題目預覽");
        lblLeftTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        lblLeftTitle.setForeground(new Color(52, 120, 190));
        lblLeftTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        txtQuestionPreview = new JTextArea();
        txtQuestionPreview.setEditable(false);
        txtQuestionPreview.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        txtQuestionPreview.setLineWrap(true);
        txtQuestionPreview.setWrapStyleWord(true);
        txtQuestionPreview.setBackground(new Color(250, 250, 250));
        txtQuestionPreview.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        leftPanel.add(lblLeftTitle, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(txtQuestionPreview), BorderLayout.CENTER);

        // ===== 右側：即時紀錄 =====
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setPreferredSize(new Dimension(320, 0));
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JLabel lblLogTitle = new JLabel("即時答題狀況");
        lblLogTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        lblLogTitle.setForeground(new Color(52, 120, 190));
        lblLogTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 14));
        txtLog.setBackground(new Color(250, 250, 250));
        txtLog.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        rightPanel.add(lblLogTitle, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(txtLog), BorderLayout.CENTER);

        // ===== 中央分割 =====
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.65);
        splitPane.setBorder(null);

        add(splitPane, BorderLayout.CENTER);

        // ===== 下方控制列 =====
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(BorderFactory.createMatteBorder(
                1, 0, 0, 0, new Color(220, 220, 220)
        ));

        btnSendQuiz = new JButton("發送試卷");
        btnSendQuiz.setBackground(new Color(255, 228, 196));
        btnSendQuiz.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        btnSendQuiz.addActionListener(e -> sendQuizToStudents());

        btnPrev = new JButton("<< 上一題");
        btnPrev.setEnabled(false);
        btnPrev.addActionListener(e -> changePage(-1));

        btnNext = new JButton("下一題 >>");
        btnNext.setEnabled(false);
        btnNext.addActionListener(e -> changePage(1));

        btnEnd = new JButton("結束測驗");
        btnEnd.setForeground(Color.RED);
        btnEnd.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        btnEnd.addActionListener(e -> endQuiz());

        lblCurrentPage = new JLabel("尚未開始");
        lblCurrentPage.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        bottomPanel.add(btnSendQuiz);
        bottomPanel.add(Box.createHorizontalStrut(20));
        bottomPanel.add(btnPrev);
        bottomPanel.add(lblCurrentPage);
        bottomPanel.add(btnNext);
        bottomPanel.add(Box.createHorizontalStrut(20));
        bottomPanel.add(btnEnd);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    // ========== 核心功能 1：發送考卷 (Protocol Packer) ==========
    private void sendQuizToStudents() {
        if (questions.isEmpty() || quizStarted) return;
        scoreboard.start();
        quizStarted = true;

        // 格式組合: QUIZ_DATA:題數;;Q1|OptA|OptB;;Q2|...
        StringBuilder sb = new StringBuilder("QUIZ_DATA:");
        sb.append(questions.size()).append(";;");

        for (Question q : questions) {
            sb.append(q.text).append("|");
            // 組合選項
            for (int i = 0; i < q.options.length; i++) {
                sb.append(q.options[i]);
                if (i < q.options.length - 1) sb.append("|");
            }
            sb.append(";;");
        }

        // 發送給 Server (轉發給學生)
        out.println(sb.toString());
        
        log("系統: 試卷資料已發送！");
        btnSendQuiz.setEnabled(false); // 發送過就鎖住
        btnNext.setEnabled(true);
        btnPrev.setEnabled(false);
        
        // 自動切到第一題
        currentQuestionIndex = 0;
        updateViewAndNotify();
    }

    // ========== 核心功能 2：切換題目 ==========
    private void changePage(int offset) {
        int newIndex = currentQuestionIndex + offset;
        if (newIndex >= 0 && newIndex < questions.size()) {
            currentQuestionIndex = newIndex;
            updateViewAndNotify();
        }
    }

    private void updateViewAndNotify() {
        btnPrev.setEnabled(currentQuestionIndex > 0);
        btnNext.setEnabled(currentQuestionIndex + 1 < questions.size());
        Question q = questions.get(currentQuestionIndex);
        
        // 1. 更新老師預覽畫面 (包含正確答案提示)
        StringBuilder display = new StringBuilder();
        display.append("【第 ").append(currentQuestionIndex + 1).append(" 題】\n");
        display.append(q.text).append("\n\n");
        for (int i = 0; i < q.options.length; i++) {
            display.append(i == q.correctOptionIndex ? " (V) " : " ( ) "); // 老師看得到勾勾
            display.append(q.options[i]).append("\n");
        }
        display.append("\n本題分數: ").append(q.points);
        txtQuestionPreview.setText(display.toString());
        
        lblCurrentPage.setText((currentQuestionIndex + 1) + " / " + questions.size());

        // 2. 發送指令給學生: QUIZ_GOTO:頁碼(1-based)
        out.println("QUIZ_GOTO:" + (currentQuestionIndex + 1));
        log("系統: 切換至第 " + (currentQuestionIndex + 1) + " 題");
    }

    // ========== 核心功能 3：處理學生答案 (計分引擎) ==========
    // 這個方法會被 TeacherClient 呼叫
    // msg 格式: "SUBMIT_ANS:學生姓名:題號:答案Index"
    public void processStudentAnswer(String msg) {
        try {
            String[] parts = msg.split(":", -1);
            if (parts.length != 4) return;
            String studentName = parts[1];
            int page = Integer.parseInt(parts[2]);
            int option = Integer.parseInt(parts[3]);
            if (page != currentQuestionIndex + 1) return;
            QuizScoreboard.Answer answer = scoreboard.submit(studentName, page, option);
            if (!answer.accepted()) return;
            log(studentName + " 第 " + page + " 題："
                    + (answer.correct() ? "答對 (+" + answer.points() + " 分)" : "答錯 (+0 分)"));
            log("   答案：" + questions.get(page - 1).options[option]);
        } catch (NumberFormatException e) {
            log("收到無效答案，已略過。");
        }
    }

    private void endQuiz() {
        if (!quizStarted || quizEnded) return;
        quizEnded = true;
        scoreboard.finish();
        // 1. 進行排序 (Map -> List -> Sort)
        List<Map.Entry<String, Integer>> list = new ArrayList<>(scoreboard.scores().entrySet());
        
        // 使用 Comparator 進行降冪排序 (分數高的在前面)
        list.sort(Map.Entry.<String, Integer>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey()));

        // 2. 建立顯示字串 (StringBuilder)
        StringBuilder sbDisplay = new StringBuilder();
        sbDisplay.append(" 測驗排行榜 \n");
        sbDisplay.append("================\n");

        // 用來傳送給學生的簡化字串 (格式: QUIZ_RESULT:第一名,100;第二名,90...)
        StringBuilder sbProtocol = new StringBuilder("QUIZ_RESULT:");

        int rank = 1;
        for (Map.Entry<String, Integer> entry : list) {
            String name = entry.getKey();
            int score = entry.getValue();

            // 老師端顯示格式
            sbDisplay.append("第 ").append(rank).append(" 名: ")
                     .append(name).append(" (").append(score).append(" 分)\n");

            // 學生端傳輸格式 (用 ; 分隔不同人，用 , 分隔名次分數)
            // 格式範例: rank,name,score;
            sbProtocol.append(rank).append(",").append(name).append(",").append(score).append(";");
            
            rank++;
        }

        // 如果沒有人得分
        if (list.isEmpty()) {
            sbDisplay.append("(尚無學生作答)");
            sbProtocol.append("NO_DATA");
        }

        // 3. 老師端顯示 (顯示在右側 Log 區)
        log("\n" + sbDisplay.toString());

        // 4. 發送給學生
        out.println(sbProtocol.toString());
        
        // 結果保留在學生視窗；關閉老師測驗視窗時才傳送 QUIZ_END。
        
        log("系統: 成績已公佈給所有學生。");
        
        // 鎖定所有按鈕
        btnSendQuiz.setEnabled(false);
        btnPrev.setEnabled(false);
        btnNext.setEnabled(false);
        btnEnd.setEnabled(false);
    }
    
    private void log(String msg) {
        txtLog.append(msg + "\n");
        txtLog.setCaretPosition(txtLog.getDocument().getLength()); // 自動捲動到底部
    }
}
