package Nodes;

import Messages.NewConnectionRequest;
import Messages.NewDisconnectionRequest;
import Messages.WordSearchMessage;
import util.Connection;
import util.FolderReader;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Node {

    private final String address;
    private final int port;
    private FolderReader directory;
    private Set<Connection> activeConnections;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private NodeListener listener;


    public Node(String address, int port, String folderPath) {
        this.address = address;
        this.port = port;
        this.directory = new FolderReader(folderPath);
        this.activeConnections = new HashSet<>();
        System.out.println(directory.getHashedFiles());

        this.listener = new NodeListener(this, port);
        listener.start();
    }

    // Validate if a connection already exists with the address and port combo
    private boolean validateRequest(String address, int port) {
        if (activeConnections.isEmpty()) return true;
        for (Connection connection : activeConnections) {
            if (connection.equals(address, port)) {
                return false;
            }
        }
        return true;
    }

    public void connectToNode(String address, int port) {
        if (!(this.address.equals(address) && port == this.port) && validateRequest(address, port)) {
            NewConnectionRequest request = new NewConnectionRequest(this.address, this.port); // The info sent is from the sender Node
            try {
                Socket socket = new Socket(address, port);
                this.out = new ObjectOutputStream(socket.getOutputStream());
                Connection conn = new Connection(address, port, socket);
                activeConnections.add(conn);
                out.writeObject(request);
            } catch (IOException e) {
                System.err.println("Problem while establishing connection: " + e.getMessage());
            }
            System.out.println(activeConnections);
        } else {
            // TODO: Remove and add an alert to the connection window
            System.out.println("Could not establish connection to " + address + ":" + port);
        }
    }

    public void closeConnection(Connection connection) {
        Socket socket = connection.getSocket();
        NewDisconnectionRequest request = new NewDisconnectionRequest(this.address, this.port);
        try {
            // TODO: With Bugs, fix later
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(request);
            activeConnections.remove(connection);
            socket.close();
        } catch (IOException e) {
            System.err.println("Problem while closing connection: " + e.getMessage());
        }
    }

    public List<FileSearchResult> searchFiles(WordSearchMessage searchedWord) {

        // TODO - Create thread to deal with incoming requests

        List<FileSearchResult> results = new ArrayList<>();
        HashMap<File, String> files = directory.getHashedFiles();
        for (File file : files.keySet()) {
            if (file.getName().contains(searchedWord.getKeyword())) {
                String fileHash = files.get(file);
                results.add(
                        new FileSearchResult(searchedWord, file.getName(),
                                fileHash, file.length(), this.address, this.port));
                System.out.println(file.getName());
            }
        }

        return results;
    }

    private void requestSearch(String keyWord) {
        WordSearchMessage searchedWord = new WordSearchMessage(keyWord, this.port);
        // TODO: Send the message for the out
    }

    public Set<Connection> getActiveConnections() {
        return activeConnections;
    }

}
