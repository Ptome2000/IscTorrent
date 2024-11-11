package Execute;

import Messages.FileBlockRequestMessage;
import util.Constants;

import java.util.ArrayList;
import java.util.List;

// Shared resource for Threads
public class DownloadTaskManager {

    private final List<FileBlockRequestMessage> fileBlocks;

    public DownloadTaskManager(String fileHash, int fileSize) {
        this.fileBlocks = createBlockRequest(fileHash, fileSize);
    }

    private List<FileBlockRequestMessage> createBlockRequest(String fileHash, int fileSize) {
        List<FileBlockRequestMessage> requests = new ArrayList<>();
        int blockSize = Constants.BLOCK_SIZE;
        int offSet = 0;

        while (offSet < fileSize) {
            int length = (int) Math.min(blockSize, fileSize - offSet);
            requests.add(new FileBlockRequestMessage(fileHash, offSet, length));
            offSet += length;
        }

        return requests;
    }

    public List<FileBlockRequestMessage> getFileBlocks() {
        return fileBlocks;
    }

}
