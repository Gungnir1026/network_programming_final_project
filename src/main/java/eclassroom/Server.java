package eclassroom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/** UTF-8, line-based TCP relay: one classroom and one teacher per server. */
public final class Server implements AutoCloseable {
    private final ServerSocket listener;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final Object roomLock = new Object();
    private final Set<String> submittedAnswers = new HashSet<>();
    private boolean quizActive;
    private int quizPage;
    private int quizQuestionCount;

    public Server(int port) throws IOException {
        listener = new ServerSocket();
        listener.setReuseAddress(true);
        listener.bind(new InetSocketAddress(port));
    }

    public int getPort() {
        return listener.getLocalPort();
    }

    public static void main(String[] args) {
        int port;
        try {
            if (args.length > 1) throw new IllegalArgumentException();
            port = args.length == 0 ? 5555 : Integer.parseInt(args[0]);
            if (port < 1 || port > 65535) throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) {
            System.err.println("用法：java -cp eclassroom.jar eclassroom.Server [1–65535 的連接埠]");
            System.exit(2);
            return;
        }
        try (Server server = new Server(port)) {
            Runtime.getRuntime().addShutdownHook(new Thread(server::close, "classroom-shutdown"));
            System.out.println("=== E-Classroom 教室伺服器 ===");
            System.out.println("本機連線：127.0.0.1:" + server.getPort());
            displayAddresses(server.getPort());
            System.out.println("READY|" + server.getPort());
            server.serve();
        } catch (IOException e) {
            System.err.println("伺服器啟動失敗：" + e.getMessage());
            System.exit(1);
        }
    }

    public void serve() throws IOException {
        while (!listener.isClosed()) {
            try {
                Socket socket = listener.accept();
                socket.setTcpNoDelay(true);
                Thread thread = new Thread(new ClientHandler(socket), "classroom-client");
                thread.setDaemon(true);
                thread.start();
            } catch (SocketException e) {
                if (!listener.isClosed()) throw e;
            }
        }
    }

    private static void displayAddresses(int port) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) return;
            while (interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                if (!network.isUp() || network.isLoopback()) continue;
                Enumeration<InetAddress> addresses = network.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address) {
                        System.out.println("網路介面 " + network.getName() + "："
                                + address.getHostAddress() + ":" + port);
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("無法列出網路介面：" + e.getMessage());
        }
    }

    private void broadcast(String message, ClientHandler excluded) {
        for (ClientHandler client : clients) {
            if (client != excluded) client.send(message);
        }
    }

    private void tellTeachers(String message) {
        for (ClientHandler client : clients) {
            if (client.teacher) client.send(message);
        }
    }

    @Override
    public void close() {
        try {
            listener.close();
        } catch (IOException ignored) {}
        for (ClientHandler client : clients) client.disconnect();
    }

    private final class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private String nickname;
        private boolean teacher;
        private boolean canDraw;
        private boolean registered;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (Socket connection = socket;
                 BufferedReader in = new BufferedReader(new InputStreamReader(
                         connection.getInputStream(), StandardCharsets.UTF_8))) {
                out = new PrintWriter(new OutputStreamWriter(
                        connection.getOutputStream(), StandardCharsets.UTF_8), true);
                connection.setSoTimeout(5000);
                send("NICK");
                nickname = in.readLine();
                if (!register()) return;
                connection.setSoTimeout(0);
                String message;
                while ((message = in.readLine()) != null) {
                    synchronized (roomLock) {
                        route(message);
                    }
                }
            } catch (IOException e) {
                if (registered && !listener.isClosed()) {
                    System.err.println("連線結束：" + nickname + "（" + e.getMessage() + "）");
                }
            } finally {
                synchronized (roomLock) {
                    if (registered) {
                        clients.remove(this);
                        if (teacher) {
                            quizActive = false;
                            broadcast("QUIZ_END", this);
                            for (ClientHandler client : clients) {
                                client.canDraw = false;
                                client.send("DISABLE_DRAW");
                            }
                        }
                        broadcast("SYSTEM|" + nickname + " 離開了教室！", this);
                    }
                }
            }
        }

        private boolean register() {
            if (nickname == null) return false;
            teacher = nickname.endsWith(" (老師)");
            String suffix = teacher ? " (老師)" : " (學生)";
            String name = nickname.endsWith(suffix)
                    ? nickname.substring(0, nickname.length() - suffix.length()) : "";
            if (name.isBlank() || name.length() > 40 || nickname.matches("(?s).*[,;:|\\r\\n].*")) {
                send("ERROR|姓名須為 1–40 個字，且不可包含 , ; : | 或換行。");
                return false;
            }
            synchronized (roomLock) {
                for (ClientHandler client : clients) {
                    if (nickname.equals(client.nickname) || (teacher && client.teacher)) {
                        send("ERROR|同名角色已登入，或教室已有老師。");
                        return false;
                    }
                }
                canDraw = teacher;
                registered = true;
                clients.add(this);
                send("JOINED|" + nickname);
                broadcast("SYSTEM|" + nickname + " 加入了教室！", this);
                System.out.println("加入：" + nickname);
                return true;
            }
        }

        private void route(String message) {
            if (message.equals("GET_USERS")) {
                StringBuilder names = new StringBuilder();
                int count = 0;
                for (ClientHandler client : clients) {
                    if (!client.teacher) {
                        names.append(client.nickname).append(',');
                        count++;
                    }
                }
                send("USER_LIST:" + count + "," + names);
            } else if (message.equals("RAISE_HAND")) {
                if (!teacher) tellTeachers("HAND_RAISED:" + nickname);
            } else if (message.startsWith("ALLOW_DRAW_TO:") || message.startsWith("DISALLOW_DRAW_TO:")) {
                if (!requireTeacher()) return;
                boolean allow = message.startsWith("ALLOW_DRAW_TO:");
                String target = message.substring(message.indexOf(':') + 1);
                for (ClientHandler client : clients) {
                    if (!client.teacher && client.nickname.equals(target)) {
                        client.canDraw = allow;
                        client.send(allow ? "ALLOW_DRAW" : "DISABLE_DRAW");
                        return;
                    }
                }
                send("ERROR|該學生已離線，請重新整理名單。");
            } else if (isBoardCommand(message)) {
                if (!canDraw) {
                    send("ERROR|老師尚未授權白板操作。");
                } else if (validBoardCommand(message)) {
                    // The sender already applied the operation locally.
                    broadcast(message, this);
                } else {
                    send("ERROR|白板指令格式錯誤。");
                }
            } else if (message.startsWith("SUBMIT_ANS:")) {
                if (teacher || !quizActive) return;
                try {
                    String[] answer = message.substring(11).split(":", -1);
                    if (answer.length != 2) return;
                    int page = Integer.parseInt(answer[0]);
                    int option = Integer.parseInt(answer[1]);
                    if (page != quizPage || option < 0) return;
                    String key = nickname + ":" + page;
                    if (submittedAnswers.add(key)) {
                        tellTeachers("SUBMIT_ANS:" + nickname + ":" + page + ":" + option);
                    }
                } catch (NumberFormatException ignored) {}
            } else if (message.startsWith("ANNOUNCE:") || message.startsWith("QUIZ_")) {
                if (!requireTeacher()) return;
                if (message.startsWith("QUIZ_DATA:")) {
                    try {
                        quizQuestionCount = Integer.parseInt(message.substring(10).split(";;", 2)[0]);
                        if (quizQuestionCount < 1) return;
                    } catch (NumberFormatException e) { return; }
                    submittedAnswers.clear();
                    quizActive = true;
                    quizPage = 0;
                } else if (message.startsWith("QUIZ_GOTO:")) {
                    try {
                        int page = Integer.parseInt(message.substring(10));
                        if (!quizActive || page < 1 || page > quizQuestionCount) return;
                        quizPage = page;
                    } catch (NumberFormatException e) { return; }
                } else if (message.equals("QUIZ_END") || message.startsWith("QUIZ_RESULT:")) {
                    quizActive = false;
                } else if (!message.startsWith("ANNOUNCE:")) {
                    return;
                }
                broadcast(message, this);
            } else if (message.startsWith("CHAT|")) {
                String text = message.substring(5).trim();
                if (!text.isEmpty()) broadcast(nickname + ": " + text, null);
            } else if (message.startsWith("SYSTEM|抽籤結果公布：")) {
                if (requireTeacher()) broadcast(message, null);
            } else {
                send("ERROR|無法識別的指令。");
            }
        }

        private boolean requireTeacher() {
            if (!teacher) send("ERROR|此操作需要老師身分。");
            return teacher;
        }

        private void send(String message) {
            out.println(message);
        }

        private void disconnect() {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private static boolean isBoardCommand(String message) {
        return message.startsWith("DRAW_") || message.startsWith("ERASE_")
                || message.equals("UNDO") || message.equals("REDO") || message.equals("CLEAR");
    }

    private static boolean validBoardCommand(String message) {
        String[] fields = message.split(" ", -1);
        int numbers;
        switch (fields[0]) {
            case "DRAW_BEGIN": numbers = 6; break;
            case "DRAW_MOVE": numbers = 8; break;
            case "ERASE_BEGIN": numbers = 1; break;
            case "ERASE_MOVE": numbers = 2; break;
            case "DRAW_END": case "ERASE_END": case "UNDO": case "REDO": case "CLEAR":
                return fields.length == 1;
            default: return false;
        }
        if (fields.length != numbers + 1) return false;
        try {
            for (int i = 1; i < fields.length; i++) {
                int value = Integer.parseInt(fields[i]);
                if (Math.abs((long) value) > 10000) return false;
            }
            if (fields[0].startsWith("DRAW_")) {
                for (int i = fields.length - 4; i < fields.length - 1; i++) {
                    int color = Integer.parseInt(fields[i]);
                    if (color < 0 || color > 255) return false;
                }
            }
            if (fields[0].equals("ERASE_BEGIN") || fields[0].startsWith("DRAW_")) {
                int size = Integer.parseInt(fields[fields.length - 1]);
                return size >= 1 && size <= 100;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
