package Messages;

public class FileBlockAnswerMessage {

    private final byte[] data;
    private final int length;

    public FileBlockAnswerMessage(byte[] data, int length) {
        this.data = data;
        this.length = length;
    }

    public byte[] getData() {
        return data;
    }

    public int getLength() {
        return length;
    }

}
