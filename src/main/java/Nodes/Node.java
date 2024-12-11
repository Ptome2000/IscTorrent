package Nodes;

import Execute.ConnectionListener;
import Execute.DownloadsWindow;
import Messages.*;
import util.Connection;
import util.FolderReader;
import util.TorrentFile;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

public class Node {
    private ConnectionListener connectionListener;
    private final String address;
    private final int port;
    private FolderReader directory;
    private Set<Connection> activeConnections;
    private NodeListener listener;
    private Map<FileSearchResult, List<Connection>> consolidatedResults = new HashMap<>();
    private Set<DownloadTaskManager> activeDownloads = new HashSet<>();

    public Node(String address, int port, String folderPath) {
        this.address = address;
        this.port = port;
        this.directory = new FolderReader(folderPath);
        this.activeConnections = new HashSet<>();
        System.out.println(directory.getFiles());

        this.listener = new NodeListener(this, port);
        listener.start();
    }

    public synchronized Set<Connection> getActiveConnections() {
        return activeConnections;
    }

    public synchronized Map<FileSearchResult, List<Connection>> getConsolidatedResults() {
        return consolidatedResults;
    }

    public FolderReader getDirectory() {
        return directory;
    }

    public synchronized void addActiveConnections(Connection connection) {
        activeConnections.add(connection);
    }

    public void setConnectionListener(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    public String folderPath() {
        return directory.getFolderPath();
    }

    public int getPort() {
        return port;
    }

    private void notifyConnectionError(String errorMessage) {
        if (connectionListener != null) {
            connectionListener.onConnectionError(errorMessage);
        }
    }

    private void notifyConnectionSuccess(String successMessage) {
        if (connectionListener != null) {
            connectionListener.onConnectionUpdated();
            connectionListener.onConnectionEstablished(successMessage);
        }
    }

    public void notifyConnectionTerminated(String terminatedMessage) {
        if (connectionListener != null) {
            connectionListener.onConnectionUpdated();
            connectionListener.onConnectionTerminated(terminatedMessage);
        }
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

    public void startDownload(DownloadTaskManager downloads) {
        activeDownloads.add(downloads);
    }

    public void connectToNode(String address, int port) {
        if (!(this.address.equals(address) && port == this.port) && validateRequest(address, port)) {
            NewConnectionRequest request = new NewConnectionRequest(this.address, this.port); // Informações do nó remetente
            try {
                Connection conn = new Connection(address, port, this);
                conn.establishConnection(request);
                activeConnections.add(conn);
                conn.start();
                notifyConnectionSuccess("Conexão estabelecida com " + conn);
            } catch (IOException e) {
                notifyConnectionError("Erro ao estabelecer conexão: " + e.getMessage());
            }
        } else {
            notifyConnectionError("Conexão com " + address + ":" + port + " já existe ou é inválida.");
        }
    }

    @Deprecated // Metodo Unicast de envio de mensagem
    public void sendMessageToNode(String message, String address, int port) {
        for (Connection conn : activeConnections) {
            if (conn.getAddress().equals(address) && conn.getPort() == port) {
                try {
                    ObjectOutputStream out = conn.getOutputStream();
                    out.writeObject(message);
                    out.flush();
                    System.out.println("Mensagem enviada para " + address + ":" + port);
                } catch (IOException e) {
                    System.err.println("Erro ao enviar mensagem: " + e.getMessage());
                }
                break;
            }
        }
    }

    public void closeConnection(Connection connection) {
        NewDisconnectionRequest request = new NewDisconnectionRequest(this.address, this.port);
        connection.disconnect(request);
        removeConnection(connection);
    }

    public void removeConnection(Connection connection) {
        activeConnections.remove(connection);
        notifyConnectionTerminated("Conexão encerrada com " + connection);
    }

    public List<FileSearchResult> searchFiles(WordSearchMessage searchedWord) {
        List<FileSearchResult> results = new ArrayList<>();
        Set<TorrentFile> files = directory.getFiles();
        if (files != null){
            for (TorrentFile file: files) {
                String filename = file.getName();
                if (filename.contains(searchedWord.getKeyword())) {
                    results.add(
                            new FileSearchResult(searchedWord, filename,
                                    file.getFileHash(), file.getFile().length(), this.address, this.port));
                    System.out.println(file.getName());
                }
            }
        }
        return results;
    }

    // Metodo Broadcast de envio de mensagem
    public void requestSearch(String keyword) {
        WordSearchMessage searchMessage = new WordSearchMessage(keyword, this.port);

        for (Connection connection : activeConnections) {
            try {
                ObjectOutputStream out = connection.getOutputStream();
                out.writeObject(searchMessage);
                out.flush();
                System.out.println("Mensagem de pesquisa enviada para: " + connection);
            } catch (IOException e) {
                System.err.println("Erro ao enviar mensagem de pesquisa: " + e.getMessage());
            }
        }
    }

    public synchronized void processSearchResults(List<FileSearchResult> results) {
        for (FileSearchResult result : results) {
            if (!findResult(result)) {
                consolidatedResults
                        .computeIfAbsent(result, k -> new ArrayList<>())
                        .add(findConnection(result));
            }
        }
        // Atualiza a interface gráfica com os resultados consolidados
        ((DownloadsWindow) connectionListener).updateSearchResults(new HashMap<>(consolidatedResults));
    }

    private boolean findResult(FileSearchResult result) {
        for (FileSearchResult existingResult : consolidatedResults.keySet()) {
            if (existingResult.getHash().equals(result.getHash())) {
                consolidatedResults.get(existingResult).add(findConnection(result));
                return true;
            }
        }
        return false;
    }

    private Connection findConnection(FileSearchResult result) {
        for (Connection connection : activeConnections) {
            if (connection.getAddress().equals(result.getAddress()) && connection.getPort() == result.getPort()) {
                return connection;
            }
        }
        return null;
    }

    public void clearConsolidatedResults() {
        consolidatedResults.clear();
        System.out.println("Mapa consolidado de resultados limpo.");
    }

    public FileBlockAnswerMessage handleBlockRequest(FileBlockRequestMessage request) {
        return directory.getFileBlock(request);
    }

    public synchronized void handleBlockAnswer(FileBlockAnswerMessage answer) {
        for(DownloadTaskManager download : activeDownloads) {
            System.out.println("Download: " + download.getFileHash() + " Answer: " + answer.getFileHash());
            if (download.getFileHash().equals(answer.getFileHash())) {
                download.uploadBlock(answer);
                break;
            }
        }
    }

}
