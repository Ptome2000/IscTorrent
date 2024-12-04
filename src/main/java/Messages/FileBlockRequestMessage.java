package Messages;

import util.Constants;
import java.io.Serializable;

public class FileBlockRequestMessage implements Serializable {

    private final int blockSize = Constants.BLOCK_SIZE;

    private final String fileHash;
    private final long offset;
    private final int length;
    private final int senderPort;

    public FileBlockRequestMessage(String fileHash, long offset, int length, int senderPort) {
        this.fileHash = fileHash;
        this.offset = offset;
        this.length = length;
        this.senderPort = senderPort;
    }

    public int getSenderPort() {
        return senderPort;
    }

    public String getFileHash() {
        return fileHash;
    }

    public long getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    // TODO: Implementar método toString

}
