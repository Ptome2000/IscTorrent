package util;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Connection {

    private final String address;
    private final int port;
    private final Socket socket;
    private final ObjectOutputStream out;

    public Connection(String address, int port) throws IOException {
        this.address = address;
        this.port = port;
        this.socket = new Socket(address, port); // Conecta uma vez ao servidor
        this.out = new ObjectOutputStream(socket.getOutputStream()); // Cria uma vez o ObjectOutputStream
    }

    public Connection(String address, int port, Socket socket) throws IOException {
        this.address = address;
        this.port = port;
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream()); // Cria o ObjectOutputStream uma vez
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

    public ObjectOutputStream getOutputStream() {
        return out;
    }

    // Método para fechar a conexão quando não for mais necessária
    public void close() {
        try {
            out.close();
            socket.close();
            System.out.println("Conexão fechada com " + address + ":" + port);
        } catch (IOException e) {
            System.err.println("Erro ao fechar a conexão: " + e.getMessage());
        }
    }

    public boolean equals(String address, int port) {
        return (address.equals(this.address) && port == this.port);
    }

    @Override
    public String toString() {
        return address + ":" + port;
    }
}
