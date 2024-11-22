package Execute;

import Messages.FileBlockAnswerMessage;
import Messages.FileBlockRequestMessage;
import util.TorrentFile;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// Shared resource for Threads
public class DownloadTaskManager {

    public List<FileBlockRequestMessage> blocksToDownload;
    public List<FileBlockAnswerMessage> downloadedBlocks;
    private final ExecutorService threadPool;
    private final Lock lock;
    private final Condition downloadComplete;

    public DownloadTaskManager(TorrentFile file, int numThreads, Condition notEmpty) {
        this.blocksToDownload = file.getFileBlocks();
        this.lock = new ReentrantLock();
        this.downloadComplete = lock.newCondition();
        this.threadPool = Executors.newFixedThreadPool(numThreads);
    }

    public FileBlockRequestMessage downloadBlock() throws InterruptedException {
        lock.lock();
        try {
            while (!isDownloadComplete()) {
                downloadComplete.await();
            }
            return blocksToDownload.removeFirst();
        } finally {
            lock.unlock();
        }
    }

    public void uploadBlock(FileBlockRequestMessage block) {
        lock.lock();
        try {
            blocksToDownload.add(block);
            downloadComplete.signal();
        } finally {
            lock.unlock();
        }
    }

    private boolean isDownloadComplete() {
        return blocksToDownload.size() == downloadedBlocks.size();
    }



}
