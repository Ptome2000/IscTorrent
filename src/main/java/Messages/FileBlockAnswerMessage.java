package Messages;

public class FileBlockAnswerMessage {

    private final byte[] data;
    private final long offset;
    private final int length;

    public FileBlockAnswerMessage(byte[] data, long offset, int length) {
        this.data = data;
        this.offset = offset;
        this.length = length;
    }

    public long getOffset() { return offset; }

    public byte[] getData() {
        return data;
    }

    public int getLength() {
        return length;
    }

}
