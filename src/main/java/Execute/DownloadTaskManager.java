package Execute;

import Messages.FileBlockAnswerMessage;
import Messages.FileBlockRequestMessage;
import Nodes.FileSearchResult;
import Nodes.Node;
import util.Connection;
import util.Constants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// Shared resource for Threads
public class DownloadTaskManager {

    private List<FileBlockRequestMessage> blocksToDownload;
    private List<FileBlockAnswerMessage> downloadedBlocks = new ArrayList<>();
    private List<Connection> nodesWithFile;
    private final ExecutorService threadPool;
    private final Lock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Node parentNode;
    private final String fileHash;

    public DownloadTaskManager(FileSearchResult file, List<Connection> nodesWithFile, Node parentNode) {
        this.parentNode = parentNode;
        this.blocksToDownload = createBlockRequests(file);
        this.fileHash = file.getHash();
        this.nodesWithFile = nodesWithFile;
        this.threadPool = Executors.newFixedThreadPool(nodesWithFile.size());
        startDownload();
    }

    public String getFileHash() {
        return fileHash;
    }

    private List<FileBlockRequestMessage> createBlockRequests(FileSearchResult file) {
        List<FileBlockRequestMessage> blockRequests = new ArrayList<>();
        long fileSize = file.getLength();
        String fileHash = file.getHash();

        for (long offset = 0; offset < fileSize; offset += Constants.BLOCK_SIZE) {
            int length = (int) Math.min(Constants.BLOCK_SIZE, fileSize - offset);
            blockRequests.add(new FileBlockRequestMessage(fileHash, offset, length, parentNode.getPort()));
        }
        return blockRequests;
    }

    public void startDownload() {
        for (int i = 0; i < nodesWithFile.size(); i++) {
            threadPool.submit(this::downloadBlock); // TODO: Transformar o método em um Runnable?
        }
    }

    private void downloadBlock() {
        try {
            while (true) {
                FileBlockRequestMessage blockRequest = getNextBlock();
                if (blockRequest == null) {
                    break;
                }
                requestBlockFromNode(blockRequest);
            }
        } catch (InterruptedException e) {
            System.out.println("Download task interrupted");
            Thread.currentThread().interrupt();
        }
    }

    private FileBlockRequestMessage getNextBlock() throws InterruptedException {
        lock.lock();
        try {
            return blocksToDownload.removeLast();
        } finally {
            lock.unlock();
        }
    }

    private void requestBlockFromNode(FileBlockRequestMessage blockRequest) {
        for (Connection connection : nodesWithFile) {
            try {
                connection.requestBlock(blockRequest);
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error requesting block: " + e.getMessage());
            }
        }
    }

    public void uploadBlock(FileBlockAnswerMessage block) {
        lock.lock();
        try {
            if (!downloadedBlocks.contains(block)) {
                downloadedBlocks.add(block);
                System.out.println("Block with Offset: " + block.getOffset() + " added to downloaded blocks");
            } else {
                System.out.println("Duplicate block received, ignoring block");
            }
        } finally {
            lock.unlock();
        }
    }

    // TODO: Implementar método para verificar se todos os blocos foram baixados

    // TODO: Implementar método com barreira para juntar os blocos baixados e salvar o arquivo (chamar a classe UploadFile)

    // TODO: Implementar método para anunciar que o download foi concluído

}
