package Messages;

import java.io.Serializable;

public class FileSearchResult implements Serializable {

    private WordSearchMessage keyWord;
    private String hash;
    private long length;
    private String name;
    private String address;
    private int port;

    public FileSearchResult(WordSearchMessage keyWord, String name, String hash, long length, String address, int port) {
        this.keyWord = keyWord;
        this.hash = hash;
        this.length = length;
        this.name = name;
        this.address = address;
        this.port = port;
    }

    public WordSearchMessage getKeyWord() {
        return keyWord;
    }

    public String getHash() {
        return hash;
    }

    public long getLength() {
        return length;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String toString() {
        return address + ":" + port;
    }
}
