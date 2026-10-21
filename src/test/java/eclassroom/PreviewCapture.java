package eclassroom;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Optional desktop smoke test; exports the real Swing windows for the README. */
public final class PreviewCapture {
    private static TeacherClient teacher;
    private static StudentClient student;
    private static Path output;

    public static void main(String[] args) throws Exception {
        output = Path.of(args.length == 0 ? "docs/assets" : args[0]);
        Files.createDirectories(output);
        int status = 0;
        try (Server server = new Server(0)) {
            Thread runner = new Thread(() -> {
                try { server.serve(); } catch (Exception e) { throw new RuntimeException(e); }
            });
            runner.setDaemon(true);
            runner.start();
            edt(() -> {
                invokeStatic(Main.class, "createLoginLauncher");
                for (Frame frame : Frame.getFrames()) {
                    if (frame.isVisible()) { capture((JFrame) frame, "login.png"); frame.dispose(); }
                }
            });
            ClientConnection teacherConnection = ClientConnection.connect("127.0.0.1", server.getPort(), "示範老師 (老師)");
            ClientConnection studentConnection = ClientConnection.connect("127.0.0.1", server.getPort(), "示範學生 (學生)");
            edt(() -> {
                teacher = new TeacherClient("示範老師", teacherConnection);
                student = new StudentClient("示範學生", studentConnection);
                teacher.setLocation(30, 30);
                student.setLocation(60, 60);
                WhiteboardPanel board = field(teacher, "whiteboard");
                board.setCurrentColor(new Color(52, 120, 190));
                // Draw TCP, a direction arrow, and two endpoint boxes through real mouse events.
                stroke(board, new int[][]{{80,90},{140,90}});
                stroke(board, new int[][]{{110,90},{110,155}});
                stroke(board, new int[][]{{215,90},{175,90},{165,100},{165,145},{175,155},{215,155}});
                stroke(board, new int[][]{{245,155},{245,90},{280,90},{295,100},{295,115},{280,125},{245,125}});
                stroke(board, new int[][]{{80,240},{215,240},{215,335},{80,335},{80,240}});
                stroke(board, new int[][]{{430,240},{565,240},{565,335},{430,335},{430,240}});
                stroke(board, new int[][]{{245,285},{395,285},{375,265}});
                stroke(board, new int[][]{{395,285},{375,305}});
                chat(teacher, "老師透過 TCP 廣播白板，所有學生同步接收。");
                chat(student, "已收到白板！中文聊天室也可以正常使用。");
            });
            await(() -> {
                Chatbox chat = field(student, "chatbox");
                javax.swing.JTextPane pane = field(chat, "chatPane");
                return pane.getText().contains("中文聊天室");
            });
            edt(() -> {
                require(equalBoards(), "teacher/student drawing pixels differ");
                capture(teacher, "teacher-whiteboard.png");
                capture(student, "student-whiteboard.png");
                findButton(teacher, "在線學生").doClick();
            });
            await(() -> {
                JFrame frame = field(teacher, "onlineWindow");
                return frame != null && frame.isVisible() && findButton(frame, "授權") != null;
            });
            edt(() -> {
                JFrame online = field(teacher, "onlineWindow");
                findButton(online, "授權").doClick();
                capture(online, "online-students.png");
            });
            await(() -> Boolean.TRUE.equals(field(student, "isAllowed")));
            edt(() -> {
                JFrame online = field(teacher, "onlineWindow"); online.setVisible(false);
                findButton(teacher, "測驗").doClick();
                QuizTeacher quiz = field(teacher, "quizFrame");
                JButton send = field(quiz, "btnSendQuiz"); send.doClick();
                capture(quiz, "teacher-quiz.png");
            });
            await(() -> { QuizStudent quiz = field(student, "quizFrame"); return quiz != null && ((Integer) field(quiz, "currentQuestionIndex")) == 0; });
            edt(() -> {
                QuizStudent quiz = field(student, "quizFrame"); capture(quiz, "student-quiz.png");
                JPanel options = field(quiz, "optionsPanel"); ((JButton) options.getComponent(0)).doClick();
            });
            await(() -> score() == 10);
            for (int page = 1; page < 3; page++) {
                int target = page;
                edt(() -> { QuizTeacher quiz = field(teacher, "quizFrame"); JButton next = field(quiz, "btnNext"); next.doClick(); });
                await(() -> { QuizStudent quiz = field(student, "quizFrame"); return ((Integer) field(quiz, "currentQuestionIndex")) == target; });
                edt(() -> { QuizStudent quiz = field(student, "quizFrame"); JPanel options = field(quiz, "optionsPanel"); ((JButton) options.getComponent(1)).doClick(); });
                await(() -> score() == (target == 1 ? 55 : 100));
            }
            edt(() -> { QuizTeacher quiz = field(teacher, "quizFrame"); JButton end = field(quiz, "btnEnd"); end.doClick(); });
            await(() -> {
                QuizStudent quiz = field(student, "quizFrame");
                javax.swing.JTextArea body = field(quiz, "txtQuestionBody");
                return body.getText().contains("100 分");
            });
            edt(() -> { QuizStudent quiz = field(student, "quizFrame"); capture(quiz, "quiz-results.png"); });
            server.close();
            await(() -> !Boolean.TRUE.equals(field(student, "isAllowed")));
            System.out.println("PASS PreviewCapture: login, live whiteboard, chat, permissions, quiz and 100-point result");
        } catch (Throwable e) {
            e.printStackTrace();
            status = 1;
        } finally {
            edt(() -> { for (Frame frame : Frame.getFrames()) frame.dispose(); });
        }
        System.exit(status);
    }

    private static int score() throws Exception {
        QuizTeacher quiz = field(teacher, "quizFrame");
        QuizScoreboard scores = field(quiz, "scoreboard");
        return scores.scores().getOrDefault("示範學生 (學生)", 0);
    }

    private static void chat(Object client, String text) throws Exception {
        Chatbox box = field(client, "chatbox");
        JTextField input = field(box, "inputField"); input.setText(text); input.postActionEvent();
    }

    private static boolean equalBoards() throws Exception {
        WhiteboardPanel left = field(teacher, "whiteboard");
        WhiteboardPanel right = field(student, "whiteboard");
        BufferedImage a = paint(left), b = paint(right);
        return a.getWidth() == b.getWidth() && a.getHeight() == b.getHeight()
                && java.util.Arrays.equals(a.getRGB(0, 0, a.getWidth(), a.getHeight(), null, 0, a.getWidth()),
                                          b.getRGB(0, 0, b.getWidth(), b.getHeight(), null, 0, b.getWidth()));
    }

    private static void stroke(WhiteboardPanel board, int[][] points) {
        mouse(board, MouseEvent.MOUSE_PRESSED, points[0]);
        for (int i = 1; i < points.length; i++) mouse(board, MouseEvent.MOUSE_DRAGGED, points[i]);
        mouse(board, MouseEvent.MOUSE_RELEASED, points[points.length - 1]);
        mouse(board, MouseEvent.MOUSE_EXITED, new int[]{-1, -1});
    }

    private static void mouse(WhiteboardPanel board, int id, int[] point) {
        board.dispatchEvent(new MouseEvent(board, id, System.currentTimeMillis(), 0, point[0], point[1], 1, false));
    }

    private static JButton findButton(Container root, String label) {
        for (Component component : root.getComponents()) {
            if (component instanceof JButton button && (label.equals(button.getText()) || label.equals(button.getToolTipText()))) return button;
            if (component instanceof Container container) {
                JButton nested = findButton(container, label);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static BufferedImage paint(Component component) {
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics(); component.printAll(graphics); graphics.dispose(); return image;
    }

    private static void capture(JFrame frame, String filename) throws Exception {
        frame.validate();
        layout(frame.getRootPane());
        ImageIO.write(paint(frame.getRootPane()), "png", output.resolve(filename).toFile());
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component component : container.getComponents()) {
            if (component instanceof Container child) layout(child);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return (T) field.get(object);
    }

    private static void invokeStatic(Class<?> type, String name) throws Exception {
        Method method = type.getDeclaredMethod(name); method.setAccessible(true); method.invoke(null);
    }

    private static void await(Callable<Boolean> condition) throws Exception {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < deadline) {
            boolean[] ready = {false}; edt(() -> ready[0] = condition.call());
            if (ready[0]) return;
            Thread.sleep(20);
        }
        throw new AssertionError("Timed out waiting for the desktop workflow.");
    }

    @FunctionalInterface private interface Task { void run() throws Exception; }
    private static void edt(Task task) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try { task.run(); } catch (Exception e) { throw new RuntimeException(e); }
        });
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
