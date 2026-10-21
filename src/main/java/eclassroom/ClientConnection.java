package eclassroom;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

/** Connect off the Swing event thread; deliver all UI updates on that thread. */
public final class ClientConnection implements Closeable {
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final AtomicBoolean closed = new AtomicBoolean();

    private ClientConnection(Socket socket, BufferedReader reader, PrintWriter writer) {
        this.socket = socket;
        this.reader = reader;
        this.writer = writer;
    }

    public static ClientConnection connect(String host, int port, String nickname) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), 5000);
            socket.setSoTimeout(5000);
            socket.setTcpNoDelay(true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8), true);
            if (!"NICK".equals(reader.readLine())) {
                throw new IOException("伺服器未回應教室登入協定。");
            }
            writer.println(nickname);
            String response = reader.readLine();
            if (!("JOINED|" + nickname).equals(response)) {
                throw new IOException(response != null && response.startsWith("ERROR|")
                        ? response.substring(6) : "伺服器未接受登入。");
            }
            socket.setSoTimeout(0);
            return new ClientConnection(socket, reader, writer);
        } catch (IOException | RuntimeException e) {
            socket.close();
            throw e;
        }
    }

    public PrintWriter writer() {
        return writer;
    }

    public void listen(Consumer<String> onMessage, Runnable onDisconnect) {
        Thread listener = new Thread(() -> {
            try {
                String message;
                while ((message = reader.readLine()) != null) {
                    String received = message;
                    SwingUtilities.invokeLater(() -> onMessage.accept(received));
                }
            } catch (IOException e) {
                if (!closed.get()) System.err.println("連線中斷：" + e.getMessage());
            } finally {
                boolean notify = !closed.get();
                close();
                if (notify) SwingUtilities.invokeLater(onDisconnect);
            }
        }, "classroom-receiver");
        listener.setDaemon(true);
        listener.start();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // Closing the window must not leave a receiver running.
            }
        }
    }
}
