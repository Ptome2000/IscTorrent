package util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import Messages.*;
import Nodes.Node;

public class Connection {

    private final String address;
    private final int port;
    private final Socket socket;
    private final ObjectOutputStream out;

    public Connection(String address, int port, Node parentNode) throws IOException {
        this.address = address;
        this.port = port;
        this.socket = new Socket(address, port); // Conecta uma vez ao servidor
        this.out = new ObjectOutputStream(socket.getOutputStream()); // Cria uma vez o ObjectOutputStream
    }

    @Override
    public String toString() {
        return address + ":" + port;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public synchronized ObjectOutputStream getOutputStream() {
        return out;
    }

    public void requestBlock(FileBlockRequestMessage blockRequest) throws IOException, ClassNotFoundException {
        out.writeObject(blockRequest);
        out.flush();
    }

    public void disconnect(NewDisconnectionRequest request) {
        try {
            out.writeObject(request);
            out.flush();
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



