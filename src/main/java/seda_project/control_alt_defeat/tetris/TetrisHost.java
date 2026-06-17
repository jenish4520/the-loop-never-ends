package seda_project.control_alt_defeat.tetris;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

public class TetrisHost {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<Object> onMessage;
    public Runnable onDisconnect;
    private DatagramSocket udpSocket; // stored so close() can break blocking receive()
    private boolean disconnectFired = false;

    public TetrisHost(Consumer<Object> onMessage, Runnable onDisconnect) {
        this.onMessage = onMessage;
        this.onDisconnect = onDisconnect;
    }

    public void start(int port, String hostName) throws Exception {
        // SO_REUSEADDR lets the OS immediately reuse the port after the previous
        // session closes, preventing "address already in use" on repeated hosting.
        udpSocket = new DatagramSocket(null);
        udpSocket.setReuseAddress(true);
        udpSocket.bind(new java.net.InetSocketAddress(28081));

        Thread udpThread = new Thread(() -> {
            try {
                byte[] buf = new byte[256];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    udpSocket.receive(packet); // broken by udpSocket.close() in close()
                    String req = new String(packet.getData(), 0, packet.getLength()).trim();
                    if (req.equals("TETRIS_DISCOVER")) {
                        String resp = "TETRIS_HOST:" + hostName + ":" + port;
                        byte[] respData = resp.getBytes();
                        DatagramPacket respPacket = new DatagramPacket(
                                respData, respData.length, packet.getAddress(), packet.getPort());
                        udpSocket.send(respPacket);
                    }
                }
            } catch (Exception e) { /* closed */ }
        });
        udpThread.setDaemon(true);
        udpThread.start();

        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new java.net.InetSocketAddress(port));
        clientSocket = serverSocket.accept();
        // Client connected — stop advertising so stale hosts never appear
        if (udpSocket != null && !udpSocket.isClosed()) udpSocket.close();
        clientSocket.setTcpNoDelay(true);
        out = new ObjectOutputStream(clientSocket.getOutputStream());
        in = new ObjectInputStream(clientSocket.getInputStream());

        new Thread(() -> {
            try {
                while (true) {
                    Object msg = in.readObject();
                    onMessage.accept(msg);
                }
            } catch (Exception e) {
                fireDisconnect();
            }
        }).start();
    }

    private synchronized void fireDisconnect() {
        if (!disconnectFired) { disconnectFired = true; onDisconnect.run(); }
    }

    public void send(Object msg) {
        try {
            if (out != null) {
                out.reset();
                out.writeObject(msg);
                out.flush();
            }
        } catch (Exception e) {
            fireDisconnect();
        }
    }

    public void close() {
        try { if (udpSocket != null && !udpSocket.isClosed()) udpSocket.close(); } catch (Exception e) {}
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception e) {}
        try { if (clientSocket != null) clientSocket.close(); } catch (Exception e) {}
    }
}
