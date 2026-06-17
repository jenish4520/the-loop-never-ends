package seda_project.control_alt_defeat.tetris;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

public class TetrisClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<Object> onMessage;
    private Runnable onDisconnect;

    public TetrisClient(Consumer<Object> onMessage, Runnable onDisconnect) {
        this.onMessage = onMessage;
        this.onDisconnect = onDisconnect;
    }

    public void connect(String ip, int port) throws Exception {
        socket = new Socket(ip, port);
        socket.setTcpNoDelay(true);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        new Thread(() -> {
            try {
                while (true) {
                    Object msg = in.readObject();
                    onMessage.accept(msg);
                }
            } catch (Exception e) {
                onDisconnect.run();
            }
        }).start();
    }

    public void send(Object msg) {
        try {
            if (out != null) {
                out.reset();
                out.writeObject(msg);
                out.flush();
            }
        } catch (Exception e) {
            onDisconnect.run();
        }
    }

    public void close() {
        try {
            if (socket != null) socket.close();
        } catch (Exception e) {}
    }
}
