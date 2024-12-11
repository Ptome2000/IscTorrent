package Nodes;

import Execute.DownloadCompletionListener;
import Execute.UploadFile;
import Messages.FileBlockAnswerMessage;
import Messages.FileBlockRequestMessage;
import Messages.FileSearchResult;
import util.Connection;
import util.Constants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
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
    private final CountDownLatch latch;
    private final Map<String, Integer> nodeBlockCounts = new HashMap<>();
    private final ExecutorService threadPool;
    private final Lock lock = new ReentrantLock();
    private final Node parentNode;
    private final String fileHash;
    private final String fileName;
    private final DownloadCompletionListener downloadCompletionListener;
    private long startTime;

    public DownloadTaskManager(FileSearchResult file, List<Connection> nodesWithFile, Node parentNode, DownloadCompletionListener listener) {
        this.parentNode = parentNode;
        this.blocksToDownload = createBlockRequests(file);
        this.fileHash = file.getHash();
        this.fileName = file.getName();
        this.nodesWithFile = nodesWithFile;
        this.threadPool = Executors.newFixedThreadPool(nodesWithFile.size());
        this.downloadCompletionListener = listener;
        initializeNodeBlockCounts();
        this.latch = new CountDownLatch(blocksToDownload.size());

        Thread uploadThread = new Thread(new UploadFile(downloadedBlocks, parentNode.folderPath() + "/" + fileName, latch, parentNode.getDirectory()));
        uploadThread.start();
        startDownload();
    }

    private synchronized void initializeNodeBlockCounts() {
        for (Connection connection : nodesWithFile) {
            String nodeKey = connection.getAddress() + ":" + connection.getPort();
            nodeBlockCounts.put(nodeKey, 0);
        }
    }
    private void incrementNodeBlockCount(String address, int port) {
        String nodeKey = address + ":" + port;
        nodeBlockCounts.put(nodeKey, nodeBlockCounts.getOrDefault(nodeKey, 0) + 1);
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
        this.startTime = System.currentTimeMillis();
        System.out.println(blocksToDownload.size() + " blocks to download");
        for (int i = 0; i < nodesWithFile.size(); i++) {
            threadPool.submit(this::downloadBlock);
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

    private synchronized void requestBlockFromNode(FileBlockRequestMessage blockRequest) {
        for (Connection connection : nodesWithFile) {
            try {
                connection.requestBlock(blockRequest);
                incrementNodeBlockCount(connection.getAddress(), connection.getPort());
                System.out.println("Block requested from " + connection.getAddress() + ":" + connection.getPort());
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error requesting block: " + e.getMessage());
            }
        }
    }

    public void uploadBlock(FileBlockAnswerMessage block) {
        lock.lock();
        try {
            System.out.println("Block received: " + block.toString());
            if (!isBlockDownloaded(block)) {
                if (block.getData().length != block.getLength()) {
                    System.err.println("Bloco com offset " + block.getOffset() + " tem tamanho incorreto. Ignorado.");
                    return; // Ignora blocos com tamanho incorreto
                }
                downloadedBlocks.add(block);
                System.out.println("falta " + latch.getCount() + " blocos");
                latch.countDown();

                if (latch.getCount() == 0) {
                    threadPool.shutdown();
                    notifyDownloadComplete();
                }
            } else {
                System.out.println("Duplicate block received, ignoring block");
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean isBlockDownloaded(FileBlockAnswerMessage block) {
        for (FileBlockAnswerMessage downloadedBlock : downloadedBlocks) {
            if (downloadedBlock.getOffset() == block.getOffset()) {
                return true;
            }
        }
        return false;
    }

    private void notifyDownloadComplete() {
        long elapsedTime = (System.currentTimeMillis() - startTime);
        System.out.println(System.currentTimeMillis() + "-" + startTime + " = " + elapsedTime);
        System.out.println(System.currentTimeMillis()/1000 + "-" + startTime/1000 + " = " + elapsedTime);

        if (downloadCompletionListener != null) {
            downloadCompletionListener.onDownloadComplete(nodeBlockCounts, elapsedTime);
        }
    }

}
