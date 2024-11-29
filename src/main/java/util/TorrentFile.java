package util;

import Messages.FileBlockAnswerMessage;
import Messages.FileBlockRequestMessage;
import Nodes.FileSearchResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TorrentFile {

    private final List<FileBlockAnswerMessage> fileBlocks;
    private final File file;
    private final String fileHash;

    public File getFile() { return file; }

    public String getFileHash() { return fileHash; }

    public List<FileBlockAnswerMessage> getFileBlocks() {
        return fileBlocks;
    }

    public String getName() {
        return this.file.getName();
    }

    public TorrentFile(File file, String fileHash) {
        this.file = file;
        this.fileHash = fileHash;
        this.fileBlocks = createBlocks();
    }

    private List<FileBlockAnswerMessage> createBlocks() {
        List<FileBlockAnswerMessage> blocks = new ArrayList<>();
        long fileSize = file.length();
        int blockSize = Constants.BLOCK_SIZE;
        long offset = 0;

        while (offset < fileSize) {
            int length = (int) Math.min(blockSize, fileSize - offset);
            blocks.add(new FileBlockAnswerMessage(new byte[length], offset, length, fileHash));
            offset += length;
        }
        return blocks;
    }

}