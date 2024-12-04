package Messages;

import java.io.Serializable;

public class NewConnectionRequest implements Serializable {

    private final String address;
    private final int port;

    public NewConnectionRequest(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    // TODO: Implementar método toString
}
