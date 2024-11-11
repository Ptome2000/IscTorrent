package util;

import java.net.Socket;

public class Connection {

    private final String address;
    private final int port;
    private final Socket socket;

    public Connection(String address, int port, Socket socket) {
        this.address = address;
        this.port = port;
        this.socket = socket;
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

    public boolean equals(String address, int port) {
        return (address.equals(this.address) && port == this.port);
    }

    @Override
    public String toString() {
        return address + ":" + port;
    }

}
