package Messages;

import java.io.Serializable;

public class WordSearchMessage implements Serializable {

    private String keyword;
    private int senderPort;

    public WordSearchMessage(String keyword, int senderPort) {
        this.keyword = keyword;
        this.senderPort = senderPort;
    }

    public String getKeyword() {
        return keyword;
    }

    public int getSenderPort() {
        return senderPort;
    }

}

