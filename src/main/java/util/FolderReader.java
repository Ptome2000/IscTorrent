package util;

import Messages.FileBlockAnswerMessage;
import Messages.FileBlockRequestMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

public class FolderReader {

    private Set<TorrentFile> files;
    private final String path;

    public FolderReader(String path) {
        this.path = path;
        File[] fileList = new File(path).listFiles(); // TODO Need for filter?
        assert fileList != null;
        this.files = new HashSet<>();
        for (File file : fileList) {
            try {
               String hash = calculateFileHash(file);
               TorrentFile tf = new TorrentFile(file, hash);
               files.add(tf);
            } catch (NoSuchAlgorithmException | IOException e) {
                System.err.println("Error when calculating Hashes " + e.getMessage());
            }
        }
    }

    public Set<TorrentFile> getFiles() {
        return files;
    }

    public FileBlockAnswerMessage getFileBlock(FileBlockRequestMessage request) {
        for (TorrentFile file : files) {
            if (file.getFileHash().equals(request.getFileHash())) {
                return file.getFileBlock(request);
            }
        }
        return null;
    }

    private static String calculateFileHash(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest messageDigester = MessageDigest.getInstance("SHA-256");
        try (FileInputStream stream = new FileInputStream(file)) {
            byte[] byteArray = new byte[Constants.BLOCK_SIZE];
            int bytesRead;

            while ((bytesRead = stream.read(byteArray)) != -1) {
                messageDigester.update(byteArray, 0, bytesRead);
            }
        }

        byte[] hashBytes = messageDigester.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    public String getFolderPath() {
        return this.path;
    }

    public void updateFiles(File file) {
        try {
            String hash = calculateFileHash(file);
            TorrentFile tf = new TorrentFile(file, hash);
            files.add(tf);
        } catch (NoSuchAlgorithmException | IOException e) {
            System.err.println("Error when calculating Hashes " + e.getMessage());
        }
    }

}
