package eclassroom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/** Real TCP clients exercise routing, the login race, permissions and quiz flow. */
public final class ServerIntegrationTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        try (Server server = new Server(0)) {
            Thread thread = new Thread(() -> {
                try { server.serve(); } catch (IOException e) { throw new RuntimeException(e); }
            }, "integration-server");
            thread.setDaemon(true);
            thread.start();
            int port = server.getPort();
            try (Peer teacher = new Peer(port, "老師甲 (老師)");
                 Peer alice = new Peer(port, "小明 (學生)");
                 Peer bob = new Peer(port, "小美 (學生)");
                 Peer pending = new Peer(port, null)) {
                // A connection that has not sent its nickname must not appear in lists.
                teacher.send("GET_USERS");
                teacher.expect("USER_LIST:2,小明 (學生),小美 (學生),");
                try (Peer duplicate = new Peer(port, null)) {
                    duplicate.send("小明 (學生)");
                    duplicate.expectPrefix("ERROR|");
                }
                try (Peer secondTeacher = new Peer(port, null)) {
                    secondTeacher.send("老師乙 (老師)");
                    secondTeacher.expectPrefix("ERROR|");
                }
                try (Peer invalid = new Peer(port, null)) {
                    invalid.send("姓名,逗號 (學生)");
                    invalid.expectPrefix("ERROR|");
                }
                alice.send("CHAT|中文提問：TCP 與 UDP？");
                String chat = "小明 (學生): 中文提問：TCP 與 UDP？";
                teacher.expect(chat); alice.expect(chat); bob.expect(chat);
                alice.silent();
                alice.send("RAISE_HAND");
                teacher.expect("HAND_RAISED:小明 (學生)");
                bob.silent();
                alice.send("ANNOUNCE:假公告"); alice.expectPrefix("ERROR|"); bob.silent();
                alice.send("ALLOW_DRAW_TO:小美 (學生)"); alice.expectPrefix("ERROR|"); bob.silent();
                String begin = "DRAW_BEGIN 10 10 0 0 255 5";
                String move = "DRAW_MOVE 10 10 50 50 0 0 255 5";
                alice.send(begin); alice.expectPrefix("ERROR|"); teacher.silent();
                teacher.send("ALLOW_DRAW_TO:小明 (學生)"); alice.expect("ALLOW_DRAW"); bob.silent();
                for (String operation : new String[]{begin, move, "DRAW_END", "UNDO", "REDO", "CLEAR"}) {
                    alice.send(operation);
                    teacher.expect(operation); bob.expect(operation); alice.silent();
                }
                alice.send("DRAW_MOVE 1 1 2 2 999 0 0 5"); alice.expectPrefix("ERROR|"); teacher.silent();
                teacher.send("DISALLOW_DRAW_TO:小明 (學生)"); alice.expect("DISABLE_DRAW");
                alice.send("CLEAR"); alice.expectPrefix("ERROR|"); teacher.silent();
                teacher.send("ANNOUNCE:上課囉"); alice.expect("ANNOUNCE:上課囉"); bob.expect("ANNOUNCE:上課囉");
                teacher.silent();
                String quiz = "QUIZ_DATA:2;;1+1？|2|3;;TCP？|可靠|不可靠;;";
                teacher.send(quiz); alice.expect(quiz); bob.expect(quiz);
                teacher.send("QUIZ_GOTO:1"); alice.expect("QUIZ_GOTO:1"); bob.expect("QUIZ_GOTO:1");
                alice.send("SUBMIT_ANS:2:0"); teacher.silent();
                alice.send("SUBMIT_ANS:1:0"); teacher.expect("SUBMIT_ANS:小明 (學生):1:0"); bob.silent();
                alice.send("SUBMIT_ANS:1:0"); teacher.silent();
                teacher.send("QUIZ_GOTO:2"); alice.expect("QUIZ_GOTO:2"); bob.expect("QUIZ_GOTO:2");
                alice.send("SUBMIT_ANS:2:0"); teacher.expect("SUBMIT_ANS:小明 (學生):2:0");
                String result = "QUIZ_RESULT:1,小明 (學生),100;";
                teacher.send(result); alice.expect(result); bob.expect(result);
                bob.send("SUBMIT_ANS:2:1"); teacher.silent();
                teacher.close();
                alice.expect("QUIZ_END"); alice.expect("DISABLE_DRAW");
                bob.expect("QUIZ_END"); bob.expect("DISABLE_DRAW");
                // A replacement teacher can join the existing classroom.
                try (Peer replacement = new Peer(port, "老師乙 (老師)")) {
                    replacement.send("GET_USERS");
                    replacement.expect("USER_LIST:2,小明 (學生),小美 (學生),");
                }
            }
        }
        System.out.println("PASS ServerIntegrationTest: " + checks + " checks");
    }

    private static final class Peer implements AutoCloseable {
        private final Socket socket;
        private final BufferedReader reader;
        private final PrintWriter writer;

        Peer(int port, String nickname) throws Exception {
            socket = new Socket("127.0.0.1", port);
            socket.setSoTimeout(2000);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            expect("NICK");
            if (nickname != null) {
                send(nickname);
                expect("JOINED|" + nickname);
            }
        }

        void send(String message) { writer.println(message); }

        String next() throws Exception {
            String line;
            do { line = reader.readLine(); } while (line != null && line.startsWith("SYSTEM|"));
            return line;
        }

        void expect(String expected) throws Exception {
            String actual = next();
            checks++;
            if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + ", got " + actual);
        }

        void expectPrefix(String prefix) throws Exception {
            String actual = next();
            checks++;
            if (actual == null || !actual.startsWith(prefix)) throw new AssertionError("Expected " + prefix + ", got " + actual);
        }

        void silent() throws Exception {
            socket.setSoTimeout(100);
            try {
                String unexpected = next();
                throw new AssertionError("Unexpected message: " + unexpected);
            } catch (SocketTimeoutException expected) {
                checks++;
            } finally {
                socket.setSoTimeout(2000);
            }
        }

        @Override public void close() throws IOException { socket.close(); }
    }
}
