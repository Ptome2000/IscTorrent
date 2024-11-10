package Creation;

import util.Constants;

public class FileBlockRequestMessage {

    private final int blockSize = Constants.BLOCK_SIZE;

    private final String fileHash;
    private final int offset;
    private final int length;

    public FileBlockRequestMessage(String fileHash, int offset, int length) {
        this.fileHash = fileHash;
        this.offset = offset;
        this.length = length;
    }



}
