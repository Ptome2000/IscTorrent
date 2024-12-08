package util;

import Messages.FileBlockAnswerMessage;
import Messages.FileBlockRequestMessage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class TorrentFile {

    private final List<FileBlockAnswerMessage> fileBlocks;
    private final File file;
    private final String fileHash;

    public File getFile() { return file; }

    public String getFileHash() { return fileHash; }

    public String getName() {
        return this.file.getName();
    }

    public TorrentFile(File file, String fileHash) {
        this.file = file;
        this.fileHash = fileHash;
        this.fileBlocks = createBlocks();
    }

    public FileBlockAnswerMessage getFileBlock(FileBlockRequestMessage request) {
        for (FileBlockAnswerMessage block : fileBlocks) {
            if (block.getOffset() == request.getOffset() && block.getLength() == request.getLength()) {
                return block;
            }
        }
        return null;
    }

    private List<FileBlockAnswerMessage> createBlocks() {
        List<FileBlockAnswerMessage> blocks = new ArrayList<>();
        long fileSize = file.length();
        int blockSize = Constants.BLOCK_SIZE;
        long offset = 0;

        try {
            byte[] fileContents = Files.readAllBytes(file.toPath());
            while (offset < fileSize) {
                // The length will either be the default block size or the remaining bytes
                int length = (int) Math.min(blockSize, fileSize - offset);
                byte[] blockData = new byte[length];
                // Arraycopy will copy from the fileContents array, starting at the offset, to the blockData array
                System.arraycopy(fileContents, (int) offset, blockData, 0, length);
                blocks.add(new FileBlockAnswerMessage(blockData, offset, length, fileHash));
                offset += length;
            }
        } catch (IOException e) {
            System.err.println("Error reading file contents: " + e.getMessage());
        }
        return blocks;
    }

    // TODO: Implementar método toString

}
