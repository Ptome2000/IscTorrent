package Nodes;

import Execute.ConnectionListener;
import Execute.DownloadsWindow;
import Messages.NewConnectionRequest;
import Messages.NewDisconnectionRequest;
import Messages.WordSearchMessage;
import util.Connection;
import util.FolderReader;
import util.TorrentFile;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;



public class Node {
    private ConnectionListener connectionListener;
    private final String address;
    private final int port;
    private FolderReader directory;
    private Set<Connection> activeConnections;
    private NodeListener listener;

    // TODO EXTRA PELO STOR - Criar thread pool para limitar o numero de tarefas que se pode fazer (SÓ NO FINAL)

    public Node(String address, int port, String folderPath) {
        this.address = address;
        this.port = port;
        this.directory = new FolderReader(folderPath);
        this.activeConnections = new HashSet<>();
        System.out.println(directory.getFiles());

        this.listener = new NodeListener(this, port);
        listener.start();
    }

    public void setConnectionListener(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    public void notifyConnectionUpdated() {
        if (connectionListener != null) {
            connectionListener.onConnectionUpdated();
        }
    }

    private void notifyConnectionError(String errorMessage) {
        if (connectionListener != null) {
            connectionListener.onConnectionError(errorMessage);
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

    public void connectToNode(String address, int port) {
        if (!(this.address.equals(address) && port == this.port) && validateRequest(address, port)) {
            NewConnectionRequest request = new NewConnectionRequest(this.address, this.port); // Informações do nó remetente
            try {
                // Cria a conexão e armazena o Socket e o ObjectOutputStream na classe Connection
                Connection conn = new Connection(address, port);
                activeConnections.add(conn);

                // Envia a solicitação de nova conexão usando o ObjectOutputStream da Connection
                ObjectOutputStream out = conn.getOutputStream();
                out.writeObject(request);
                out.flush(); // Garante que a mensagem seja enviada imediatamente

                notifyConnectionUpdated();
                System.out.println("Conexão estabelecida e adicionada: " + address + ":" + port);
            } catch (IOException e) {
                notifyConnectionError("Erro ao estabelecer conexão: " + e.getMessage());
                System.err.println("Problema ao estabelecer conexão: " + e.getMessage());
            }
        } else {
            notifyConnectionError("Conexão com " + address + ":" + port + " já existe ou é inválida.");
            System.out.println("Não foi possível estabelecer conexão com " + address + ":" + port);
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

        //TODO EXTRA - Make a method to update the file map with the downloaded files?

        List<FileSearchResult> results = new ArrayList<>();
        Set<TorrentFile> files = directory.getFiles();
        for (TorrentFile file: files) {
            String filename = file.getName();
            if (filename.contains(searchedWord.getKeyword())) {
                results.add(
                        new FileSearchResult(searchedWord, filename,
                                file.getFileHash(), file.getFile().length(), this.address, this.port));
                System.out.println(file.getName());
            }
        }
        return results;
    }

    // Metodo Broadcast de envio de mensagem
    public void requestSearch(String keyword) {
        WordSearchMessage searchMessage = new WordSearchMessage(keyword, this.port);
        System.out.println("Enviando mensagem de pesquisa: " + keyword);

        for (Connection connection : activeConnections) {
            try {
                ObjectOutputStream out = connection.getOutputStream(); // Usa o ObjectOutputStream persistente
                out.writeObject(searchMessage);
                out.flush();
                System.out.println("Mensagem de pesquisa enviada para: " + connection.getAddress() + ":" + connection.getPort());
                // Não feche o Socket nem o ObjectOutputStream aqui, ele permanece aberto
            } catch (IOException e) {
                System.err.println("Erro ao enviar mensagem de pesquisa: " + e.getMessage());
            }
        }
    }

    public void processSearchResults(List<FileSearchResult> results) {
        ((DownloadsWindow) connectionListener).updateSearchResults(results);
    }

    public Set<Connection> getActiveConnections() {
        return activeConnections;
    }

}
