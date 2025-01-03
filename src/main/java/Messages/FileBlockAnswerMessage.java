package Messages;

import java.io.Serializable;

public class FileBlockAnswerMessage implements Serializable {

    private final byte[] data;
    private final long offset;
    private final int length;
    private final String fileHash;
    private String adress;
    private int port;

    public FileBlockAnswerMessage(byte[] data, long offset, int length, String fileHash) {
        this.data = data;
        this.offset = offset;
        this.length = length;
        this.fileHash = fileHash;

    }

    public long getOffset() { return offset; }

    public byte[] getData() {
        return data;
    }

    public int getLength() {
        return length;
    }

    public String getFileHash() { return fileHash; }

    public String toString() {
        return "Block " + offset + " of " + fileHash;
    }

    public String getAdress() {
        return adress;
    }

    public int getPort() {
        return port;
    }

    public void setAdress(String adress) {
        this.adress = adress;
    }

    public void setPort(int port) {
        this.port = port;
    }

}
