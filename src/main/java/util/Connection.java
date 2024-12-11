package util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import Messages.*;
import Nodes.Node;

public class Connection extends Thread {

    private final String address;
    private final int port;
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final Node parentNode;

    public Connection(String address, int port, Node parentNode) throws IOException {
        this.address = address;
        this.port = port;
        this.socket = new Socket(address, port); // Conecta uma vez ao servidor
        this.out = new ObjectOutputStream(socket.getOutputStream()); // Cria uma vez o ObjectOutputStream
        this.in = new ObjectInputStream(socket.getInputStream()); // Cria uma vez o ObjectInputStream
        this.parentNode = parentNode;
    }

    public Connection(String address, int port, Socket socket, Node parentNode) throws IOException {
        this.address = address;
        this.port = port;
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream()); // Cria uma vez o ObjectOutputStream
        this.in = new ObjectInputStream(socket.getInputStream()); // Cria uma vez o ObjectInputStream
        this.parentNode = parentNode;
    }

    @Override
    public String toString() {
        return address + ":" + port;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Object receivedMessage = in.readObject();
                System.out.println("Mensagem recebida do tipo: " + receivedMessage.getClass().getName());
                if (receivedMessage instanceof NewDisconnectionRequest message) {
                    System.out.println("NewDisconnectionRequest recebida de: " + message.getAddress() + ":" + message.getPort());
                    handleDisconnection(message);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro ao receber mensagem: " + e.getMessage());
        }
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public Socket getSocket() {
        return socket;
    }

    public synchronized ObjectOutputStream getOutputStream() {
        return out;
    }

    public ObjectInputStream getInputStream() {
        return in;
    }

    public void requestBlock(FileBlockRequestMessage blockRequest) throws IOException, ClassNotFoundException {
        out.writeObject(blockRequest);
        out.flush();
    }

    private void handleDisconnection(NewDisconnectionRequest request) throws IOException {
        this.parentNode.removeConnection(this);
        socket.close();
        this.interrupt();
    }

    public void disconnect(NewDisconnectionRequest request) {
        try {
            out.writeObject(request);
            out.flush();
            this.interrupt();
        } catch (IOException e) {
            System.err.println("Erro ao fechar a conexão: " + e.getMessage());
        }
    }

    public boolean equals(String address, int port) {
        return (address.equals(this.address) && port == this.port);
    }

    public void establishConnection(NewConnectionRequest request) {
        try {
            out.writeObject(request);
            out.flush();
        } catch (IOException e) {
            System.err.println("Erro ao estabelecer conexão: " + e.getMessage());
        }
    }

}



