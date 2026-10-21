package eclassroom;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import javax.swing.SwingUtilities;

/** Regression checks without a window system or third-party test dependencies. */
public final class CoreTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        check(Main.validatePort(" 5555 ") == 5555, "default port");
        for (String invalid : new String[]{"0", "65536", "abc", "", "1.5"}) {
            rejects(() -> Main.validatePort(invalid), "invalid port " + invalid);
        }
        check(Main.validateName(" 王同學 ").equals("王同學"), "Unicode name");
        for (String invalid : new String[]{" ", "a,b", "a:b", "a;b", "a|b", "a\nb", "a".repeat(41)}) {
            rejects(() -> Main.validateName(invalid), "reserved name separators");
        }
        check(AppResources.class.getResource("/icons/pencil.png") != null, "icon packaged in JAR");
        QuizScoreboard scores = new QuizScoreboard(new int[]{0, 1}, new int[]{10, 45}, new int[]{3, 4});
        check(!scores.submit("甲", 1, 0).accepted(), "cannot answer before start");
        scores.start();
        check(!scores.submit("甲", 0, 0).accepted(), "invalid page");
        check(!scores.submit("甲", 1, 3).accepted(), "invalid option");
        check(scores.submit("甲", 1, 0).points() == 10, "correct answer");
        check(!scores.submit("甲", 1, 0).accepted(), "duplicate answer");
        check(scores.submit("乙", 1, 2).accepted(), "wrong answer accepted");
        check(scores.scores().get("乙") == 0, "zero-score student retained");
        check(!scores.submit("乙", 1, 0).accepted(), "cannot revise wrong answer");
        scores.submit("甲", 2, 1);
        check(scores.scores().get("甲") == 55, "scores accumulate");
        scores.finish();
        check(!scores.submit("乙", 2, 1).accepted(), "late answer rejected");
        scores.start();
        check(scores.scores().isEmpty(), "new quiz clears scores");
        SwingUtilities.invokeAndWait(CoreTest::whiteboard);
        System.out.println("PASS CoreTest: " + checks + " checks");
    }

    private static void whiteboard() {
        WhiteboardPanel local = new WhiteboardPanel();
        WhiteboardPanel remote = new WhiteboardPanel();
        local.setSize(240, 180);
        remote.setSize(240, 180);
        local.setCurrentColor(Color.BLUE);
        remote.setToolMode(ToolMode.ERASER);
        remote.setEraserSize(45);
        int[] moves = {0};
        local.setDrawListener(new WhiteboardPanel.DrawListener() {
            public void onDrawBegin(int x, int y, Color c, int size) { remote.beginRemoteDraw(); }
            public void onDrawMove(int x1, int y1, int x2, int y2, Color c, int size) {
                moves[0]++;
                remote.addRemoteLine(x1, y1, x2, y2, c, size);
            }
            public void onDrawEnd() { remote.endRemoteDraw(); }
            public void onEraseBegin(int size) { remote.beginRemoteErase(size); }
            public void onEraseMove(int x, int y) { remote.remoteEraseMove(x, y); }
            public void onEraseEnd() { remote.endRemoteErase(); }
        });
        int[] empty = pixels(local);
        mouse(local, MouseEvent.MOUSE_PRESSED, 30, 40);
        mouse(local, MouseEvent.MOUSE_DRAGGED, 180, 40);
        mouse(local, MouseEvent.MOUSE_RELEASED, 180, 40);
        mouse(local, MouseEvent.MOUSE_EXITED, -1, -1);
        check(moves[0] == 2, "one dot and one move; no duplicate line transmission");
        int[] drawn = pixels(local);
        check(!Arrays.equals(empty, drawn), "stroke is visible");
        check(Arrays.equals(drawn, pixels(remote)), "local and remote render identically");
        check(remote.getToolMode() == ToolMode.ERASER, "remote stroke preserves selected tool");
        check(remote.getEraserSize() == 45, "remote stroke preserves eraser settings");
        local.undo(); remote.undoRemote();
        check(Arrays.equals(empty, pixels(local)), "one undo removes one complete stroke");
        check(Arrays.equals(pixels(local), pixels(remote)), "undo stays synchronized");
        local.redo(); remote.redoRemote();
        check(Arrays.equals(drawn, pixels(remote)), "redo restores stroke");
        local.setToolMode(ToolMode.ERASER);
        local.setEraserSize(20);
        mouse(local, MouseEvent.MOUSE_PRESSED, 90, 40);
        mouse(local, MouseEvent.MOUSE_RELEASED, 90, 40);
        mouse(local, MouseEvent.MOUSE_EXITED, -1, -1);
        check(!Arrays.equals(drawn, pixels(local)), "eraser works on a click");
        check(Arrays.equals(pixels(local), pixels(remote)), "erase stays synchronized");
        local.undo(); remote.undoRemote();
        check(Arrays.equals(drawn, pixels(local)), "undo erase restores original order");
        check(Arrays.equals(pixels(local), pixels(remote)), "undo erase stays synchronized");
        local.clearAll(); remote.clearRemote();
        check(Arrays.equals(empty, pixels(remote)), "clear board");
        local.undo(); remote.undoRemote();
        check(Arrays.equals(drawn, pixels(remote)), "undo clear");
        local.setEditable(false);
        mouse(local, MouseEvent.MOUSE_PRESSED, 20, 20);
        mouse(local, MouseEvent.MOUSE_DRAGGED, 100, 100);
        mouse(local, MouseEvent.MOUSE_RELEASED, 100, 100);
        check(Arrays.equals(drawn, pixels(local)), "readonly board rejects input");
        check(moves[0] == 2, "readonly board sends no move");
    }

    private static void mouse(WhiteboardPanel board, int id, int x, int y) {
        board.dispatchEvent(new MouseEvent(board, id, System.currentTimeMillis(), 0, x, y, 1, false));
    }

    private static int[] pixels(WhiteboardPanel board) {
        BufferedImage image = new BufferedImage(240, 180, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        board.paint(graphics);
        graphics.dispose();
        return image.getRGB(0, 0, 240, 180, null, 0, 240);
    }

    private static void rejects(Runnable action, String label) {
        try { action.run(); } catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError(label);
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
