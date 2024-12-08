package Execute;

import Messages.FileBlockAnswerMessage;
import Messages.FileBlockRequestMessage;
import Nodes.FileSearchResult;
import Nodes.Node;
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
    private final Condition notEmpty = lock.newCondition();
    private final Node parentNode;
    private final String fileHash;
    private final String fileName;
    private final DownloadCompletionListener downloadCompletionListener;
    private long startTime;
    boolean downloadComplete;
    private int blocksDownloaded = 0;


    public DownloadTaskManager(FileSearchResult file, List<Connection> nodesWithFile, Node parentNode, DownloadCompletionListener listener) {
        this.parentNode = parentNode;
        this.blocksToDownload = createBlockRequests(file);
        this.fileHash = file.getHash();
        this.fileName = file.getName();
        this.nodesWithFile = nodesWithFile;
        this.threadPool = Executors.newFixedThreadPool(nodesWithFile.size());
        this.downloadCompletionListener = listener;
        this.downloadComplete = false;
        initializeNodeBlockCounts();
        this.latch = new CountDownLatch(blocksToDownload.size());

        Thread uploadThread = new Thread(new UploadFile(downloadedBlocks, fileHash, parentNode.folderPath() + "/" + fileName, latch));
        uploadThread.start();
        startDownload();
    }

    private void initializeNodeBlockCounts() {
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
                incrementNodeBlockCount(connection.getAddress(), connection.getPort());
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error requesting block: " + e.getMessage());
            }
        }
    }


    public void uploadBlock(FileBlockAnswerMessage block) {
        lock.lock();
        try {
            System.out.println("Block received: " + block.toString());
            if (!downloadedBlocks.contains(block)) {
                if (block.getData().length != block.getLength()) {
                    System.err.println("Bloco com offset " + block.getOffset() + " tem tamanho incorreto. Ignorado.");
                    return; // Ignora blocos com tamanho incorreto
                }
                downloadedBlocks.add(block);
                latch.countDown();
                if (latch.getCount() == 0) {
                    notifyDownloadComplete();
                }


            } else {
                System.out.println("Duplicate block received, ignoring block");
            }
        } finally {
            lock.unlock();
        }
    }

    private void notifyDownloadComplete() {
        long elapsedTime = (System.currentTimeMillis() - startTime);
        System.out.println(System.currentTimeMillis() + "-" + startTime + " = " + elapsedTime);
        System.out.println(System.currentTimeMillis()/1000 + "-" + startTime/1000 + " = " + elapsedTime);


        if (downloadCompletionListener != null) {
            downloadCompletionListener.onDownloadComplete(nodeBlockCounts, elapsedTime);
        }
    }



    // TODO: Implementar método para verificar se todos os blocos foram baixados

    // TODO: Implementar método para anunciar que o download foi concluído

}
