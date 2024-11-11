package Nodes;

import Messages.NewConnectionRequest;
import Messages.NewDisconnectionRequest;
import util.Connection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class NodeListener extends Thread {

    private ObjectInputStream in;
    private Node parentNode;

    @Override
    public void run() {

    }

    private void startListening() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while(true) {
                Socket socket = serverSocket.accept();
                this.in = new ObjectInputStream(socket.getInputStream());
                Object receivedObject = in.readObject();

                if (!activeConnections.contains(socket) && receivedObject instanceof NewConnectionRequest request) {
                    System.out.println("Received connection request from " + request.getAddress() + ":" + request.getPort());
                    Connection conn = new Connection(request.getAddress(), request.getPort(), socket);
                    activeConnections.add(conn);
                }
                if (receivedObject instanceof NewDisconnectionRequest request) {
                    System.out.println("Received disconnection request from " + request.getAddress() + ":" + request.getPort());
                    activeConnections.removeIf(conn -> conn.getAddress().equals(request.getAddress()) && conn.getPort() == request.getPort());
                    socket.close();
                }
            }

        } catch (ClassNotFoundException e) {
            System.err.println("Problem while receiving a new Connection: " + e.getMessage());
        }
    }

}
