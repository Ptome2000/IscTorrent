package Execute;

import Nodes.Node;

public class Main {
    public static void main(String[] args) {
        Node n1 = new Node("localhost", 8081, "files/dl1");
        DownloadsWindow downloadsWindow = new DownloadsWindow(n1);

        new Node("localhost", 8082, "files/dl2");
        new Node("localhost", 8083, "files/dl3");
        new Node("localhost", 8084, "files/dl4");
    }

}