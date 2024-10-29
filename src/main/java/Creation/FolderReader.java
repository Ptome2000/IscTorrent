package Creation;

import util.Constants;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class FolderReader {

    private File[] fileList; // TODO If files are saved in map, no need for fileList
    HashMap<File, String> hashFiles = new HashMap<>();
    private String pathFolder;

    public FolderReader(String path) {
        this.pathFolder = path;
        loadContent();
        System.out.println(Arrays.toString(fileList)); //Print files inside folder

        for (File file : fileList) {
            try {
               String hash = calculateFileHash(file);
                hashFiles.put(file, hash);
            } catch (NoSuchAlgorithmException | IOException e) {
                System.err.println("Error when calculating Hashes " + e.getMessage());
            }
        }

        System.out.println(hashFiles);
    }

    public void loadContent() {
        this.fileList = new File(this.pathFolder).listFiles();
        // TODO Need for filter?
    }

    public static String calculateFileHash(File file) throws NoSuchAlgorithmException, IOException {
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
