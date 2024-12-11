package Nodes;

import Messages.*;
import util.Connection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NodeListener extends Thread {

    private final Node parentNode;
    private final int port;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public NodeListener(Node parentNode, int port) {
        this.parentNode = parentNode;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            startListening();
        } catch (IOException e) {
            System.err.println("Error while listening for connections: " + e.getMessage());
        }
    }

    private void startListening() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("NodeListener is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                //new Thread(() -> handleClient(socket)).start();
                threadPool.execute(() -> handleClient(socket));
            }
        }
    }

    private void handleClient(Socket socket) {
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
                while (true) {
                    Object receivedObject = in.readObject();

                    switch (receivedObject) {
                        case WordSearchMessage searchMessage -> {
                            System.out.println("WordSearchMessage recebida: " + searchMessage);
                            handleSearchMessage(searchMessage);
                        }
                        case NewConnectionRequest request -> {
                            System.out.println("NewConnectionRequest recebida de: " + request);
                            handleNewConnection(request);
                        }
                        case NewDisconnectionRequest request -> {
                            System.out.println("NewDisconnectionRequest recebida de: " + request);
                            handleDisconnection(request, socket);
                        }
                        case FileBlockRequestMessage request -> {
                            System.out.println("FileBlockRequestMessage recebida: " + request);
                            handleBlockRequest(request);
                        }
                        case FileBlockAnswerMessage request -> {
                            System.out.println("FileBlockAnswerMessage recebida: " + request);
                            handleBlockAnswer(request);
                        }
                        case List<?> results -> {
                            try {
                                @SuppressWarnings("unchecked")
                                List<FileSearchResult> fileSearchResults = (List<FileSearchResult>) results;
                                System.out.println("Lista de FileSearchResult recebida com " + fileSearchResults.size() + " resultados.");
                                parentNode.processSearchResults(fileSearchResults);
                            } catch (ClassCastException e) {
                                System.err.println("Erro ao processar os resultados da pesquisa: " + e.getMessage());
                            }
                        }
                        default -> System.out.println("Objeto desconhecido foi recebido: " + receivedObject.getClass().getName());
                    }
            }
        } catch (IOException e) {
            System.err.println("Erro ao processar cliente: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar socket: " + e.getMessage());
            }
        }
    }

    private void handleSearchMessage(WordSearchMessage searchMessage) {
        // Procura arquivos locais que correspondem à pesquisa
        List<FileSearchResult> results = parentNode.searchFiles(searchMessage);
        // Envia os resultados de volta ao nó
        try {
            Connection conn = parentNode.getActiveConnections().stream()
                    .filter(c -> c.getPort() == searchMessage.getSenderPort())
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Conexão não encontrada para enviar resultados de pesquisa."));
            ObjectOutputStream out = conn.getOutputStream();
            out.writeObject(results);
            out.flush();
        } catch (IOException e) {
            System.err.println("Erro ao enviar resultados de pesquisa: " + e.getMessage());
        }
    }

    private void handleNewConnection(NewConnectionRequest request) throws IOException {
        synchronized (parentNode.getActiveConnections()) {
            if (parentNode.getActiveConnections().stream().noneMatch(
                    conn -> conn.getAddress().equals(request.getAddress()) &&
                            conn.getPort() == request.getPort())) {
                // Cria uma nova Connection com o socket recebido
                try {
                    Connection conn = new Connection(request.getAddress(), request.getPort(), this.parentNode);
                    if (!conn.isAlive()) {
                        conn.start();
                    }
                    parentNode.addActiveConnections(conn);
                    System.out.println("Conexão adicionada e iniciada: " + conn);
                } catch (IOException e) {
                    System.err.println("Erro ao criar a conexão: " + e.getMessage());
                }

                if (!parentNode.getActiveConnections().isEmpty()) {
                    for (Connection connection : parentNode.getActiveConnections()) {
                        System.out.println("Conexões ativas: " + connection);
                    }
                }
            } else {
                System.out.println("Conexão já existente: " + request);
            }
        }
    }

    private void handleBlockRequest(FileBlockRequestMessage request) {
        FileBlockAnswerMessage answer = parentNode.handleBlockRequest(request);
        try {
            Connection conn = parentNode.getActiveConnections().stream()
                    .filter(c -> c.getPort() == request.getSenderPort())
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Conexão não encontrada para enviar bloco de resultado."));
            ObjectOutputStream out = conn.getOutputStream();
            out.writeObject(answer);
            out.flush();
        } catch (IOException e) {
            System.err.println("Erro ao enviar bloco: " + e.getMessage());
        }
    }

    private void handleBlockAnswer(FileBlockAnswerMessage answer) {
        parentNode.handleBlockAnswer(answer);
    }

    private void handleDisconnection(NewDisconnectionRequest request, Socket socket) throws IOException {
        synchronized (parentNode.getActiveConnections()) {
            parentNode.getActiveConnections().removeIf(
                    conn -> conn.getAddress().equals(request.getAddress()) && conn.getPort() == request.getPort()
            );
        }
        socket.close();
    }

}
