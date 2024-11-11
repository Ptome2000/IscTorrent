package Execute;

import Nodes.FileSearchResult;
import Messages.WordSearchMessage;
import Nodes.Node;

import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) {

        //FolderReader files = new FolderReader("files");
        Node n1 = new Node("localhost", 8081, "files/N1");
        DownloadsWindow downloadsWindow = new DownloadsWindow(n1);

        Node n2 = new Node("localhost", 8082, "files/N2");
        DownloadsWindow downloadsWindow2 = new DownloadsWindow(n2);

        WordSearchMessage w = new WordSearchMessage("cat", 8081);
        n2.searchFiles(w);

    }
}