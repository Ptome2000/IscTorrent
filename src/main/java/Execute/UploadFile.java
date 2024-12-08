package Execute;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import Messages.FileBlockAnswerMessage;

public class UploadFile implements Runnable {

    private final List<FileBlockAnswerMessage> downloadedBlocks;
    private final String fileHash;
    private final String outputPath;
    private final CountDownLatch latch;


    // Constructor
    public UploadFile(List<FileBlockAnswerMessage> downloadedBlocks, String fileHash, String outputPath, CountDownLatch latch) {
        this.downloadedBlocks = downloadedBlocks;
        this.fileHash = fileHash;
        this.outputPath = outputPath;
        this.latch = latch;
    }

    @Override
    public void run() {
        try {
            System.out.println("Aguardando o término de todas as threads...");
            latch.await();
            System.out.println("Starting file assembly...");
            byte[] completeFile = assembleFile();
            saveToFile(completeFile);
            System.out.println("File saved successfully: " + outputPath);
        } catch (IOException e) {
            System.err.println("Error saving file: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // Assemble the file from downloaded blocks
    private byte[] assembleFile() {
        // Ordenar os blocos pelo offset
        downloadedBlocks.sort((block1, block2) -> Long.compare(block1.getOffset(), block2.getOffset()));

        int totalSize = downloadedBlocks.stream()
                .mapToInt(block -> (int) (block.getOffset() + block.getLength()))
                .max()
                .orElse(0);

        byte[] fileData = new byte[totalSize];

        // Copiar os dados de cada bloco para a posição correta no array
        for (FileBlockAnswerMessage block : downloadedBlocks) {
            System.arraycopy(block.getData(), 0, fileData, (int) block.getOffset(), block.getLength());
        }


        return fileData;
    }


    // Save the complete file to disk
    private void saveToFile(byte[] completeFile) throws IOException {
        File outputFile = new File(outputPath);
        Files.write(outputFile.toPath(), completeFile);
    }
}
