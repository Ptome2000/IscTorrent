package util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.List;

import Messages.NewConnectionRequest;
import Messages.NewDisconnectionRequest;
import Messages.WordSearchMessage;
import Nodes.FileSearchResult;

import static java.lang.System.out;

public class Connection extends Thread {

        private final String address;
        private final int port;
        private final Socket socket;
        private final ObjectOutputStream out;
        private final ObjectInputStream in;

        public Connection(String address, int port) throws IOException {
            this.address = address;
            this.port = port;
            this.socket = new Socket(address, port); // Conecta uma vez ao servidor
            this.out = new ObjectOutputStream(socket.getOutputStream()); // Cria uma vez o ObjectOutputStream
            this.in = new ObjectInputStream(socket.getInputStream()); // Cria uma vez o ObjectInputStream
        }

    public Connection(String address, int port, Socket socket) throws IOException {
        this.address = address;
        this.port = port;
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream()); // Cria uma vez o ObjectOutputStream
        this.in = new ObjectInputStream(socket.getInputStream()); // Cria uma vez o ObjectInputStream
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

        public ObjectInputStream getInputStream() {
            return in;
        }



        // Método para fechar a conexão quando não for mais necessária
        public void close(NewDisconnectionRequest request) {
            try {
                out.close();
                socket.close();
                System.err.println("Conexão fechada com " + address + ":" + port);
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

    public void establishConnection(NewConnectionRequest request) {
        try {
            out.writeObject(request);
            out.flush();
        } catch (IOException e) {
            System.err.println("Erro ao estabelecer conexão: " + e.getMessage());
        }
    }

    public void sendMessage(Serializable message) throws IOException {
        synchronized (out) {
            out.writeObject(message);
            out.flush();
        }
    }

    public Object receiveMessage() throws IOException, ClassNotFoundException {
        return in.readObject();
    }


}



