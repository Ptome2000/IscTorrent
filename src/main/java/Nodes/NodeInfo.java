package Nodes;

public class NodeInfo {
    private final String address;
    private final int port;

    public NodeInfo(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeInfo nodeInfo = (NodeInfo) o;
        return port == nodeInfo.port && address.equals(nodeInfo.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode() * 31 + port;
    }

    @Override
    public String toString() {
        return address + ":" + port;
    }
}
