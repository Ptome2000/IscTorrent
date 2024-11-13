package Nodes;

import Messages.NewConnectionRequest;
import Messages.NewDisconnectionRequest;
import util.Connection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class NodeListener extends Thread {

    private final Node parentNode;
    private final int port;

    public NodeListener(Node parentNode, int port) {
        this.parentNode = parentNode;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            startListening();
        } catch (IOException e) {
            System.err.println("Error while listening for connections: " + e.getMessage());
        }
    }

    private void startListening() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("NodeListener is listening on port " + port);

            while (true) {
                // Aceita uma nova conexão
                Socket socket = serverSocket.accept();
                System.out.println("Connection accepted from " + socket.getInetAddress() + ":" + socket.getPort());

                // Cria um ObjectInputStream para receber mensagens
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                Object receivedObject = in.readObject();

                // Processa as mensagens recebidas
                if (receivedObject instanceof NewConnectionRequest request) {
                    handleNewConnection(request, socket);
                } else if (receivedObject instanceof NewDisconnectionRequest request) {
                    handleDisconnection(request, socket);
                }
            }

        } catch (ClassNotFoundException e) {
            System.err.println("Problem while receiving a new Connection: " + e.getMessage());
        }
    }

    private void handleNewConnection(NewConnectionRequest request, Socket socket) {
        // Verifica se a conexão já está ativa para evitar duplicação
        if (!parentNode.getActiveConnections().stream().anyMatch(
                conn -> conn.getAddress().equals(request.getAddress()) && conn.getPort() == request.getPort())) {
            System.out.println("Received connection request from " + request.getAddress() + ":" + request.getPort());
            Connection conn = new Connection(request.getAddress(), request.getPort(), socket);
            synchronized (parentNode.getActiveConnections()) {
                parentNode.getActiveConnections().add(conn);
            }
        }
    }

    private void handleDisconnection(NewDisconnectionRequest request, Socket socket) throws IOException {
        System.out.println("Received disconnection request from " + request.getAddress() + ":" + request.getPort());
        synchronized (parentNode.getActiveConnections()) {
            parentNode.getActiveConnections().removeIf(
                    conn -> conn.getAddress().equals(request.getAddress()) && conn.getPort() == request.getPort()
            );
        }
        socket.close();
    }
}
