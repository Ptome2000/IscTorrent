package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class FolderReader {

    private HashMap<File, String> hashedFiles = new HashMap<>();
    private String pathFolder;

    public FolderReader(String path) {
        this.pathFolder = path;
        File[] fileList = new File(this.pathFolder).listFiles(); // TODO Need for filter?

        for (File file : fileList) {
            try {
               String hash = calculateFileHash(file);
               hashedFiles.put(file, hash);
            } catch (NoSuchAlgorithmException | IOException e) {
                System.err.println("Error when calculating Hashes " + e.getMessage());
            }
        }
    }

    public HashMap<File, String> getHashedFiles() {
        return hashedFiles;
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

}
