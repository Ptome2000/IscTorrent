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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NodeListener extends Thread {

    private final Node parentNode;
    private final int port;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private final Lock lock = new ReentrantLock();

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
                threadPool.execute(() -> handleClient(socket));
            }
        }
    }

    private void handleClient(Socket socket) {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            while (!socket.isClosed()) {
                Object receivedObject = in.readObject();
                System.out.println("Objeto recebido: " + receivedObject.getClass().getName());
                lock.lock();
                try {
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
                            System.out.println("FileBlockRequestMessage recebida: " + request + parentNode.getPort());
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
                        case null -> System.out.println("Objeto nulo recebido.");

                        default -> System.out.println("Objeto desconhecido foi recebido: " + receivedObject.getClass().getName());
                    }
                } finally {
                    lock.unlock();
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro ao processar cliente: " + e.getMessage());

        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar socket: " + e.getMessage());
            }
        }
    }

    private void handleSearchMessage(WordSearchMessage searchMessage) {
        List<FileSearchResult> results = parentNode.searchFiles(searchMessage);
        Connection conn = parentNode.getActiveConnections().stream()
                .filter(c -> c.getPort() == searchMessage.getSenderPort())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Conexão não encontrada para enviar resultados de pesquisa."));
        ObjectOutputStream out = conn.getOutputStream();
        sendResultsSafely(out, results);
    }

    private void handleNewConnection(NewConnectionRequest request) throws IOException {
        lock.lock();
        try {
            if (parentNode.getActiveConnections().stream().noneMatch(
                    conn -> conn.getAddress().equals(request.getAddress()) &&
                            conn.getPort() == request.getPort())) {
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
        } finally {
            lock.unlock();
        }
    }

    private void handleBlockRequest(FileBlockRequestMessage request) {
        FileBlockAnswerMessage answer = parentNode.handleBlockRequest(request);
        answer.setAdress(parentNode.getAdress());
        answer.setPort(parentNode.getPort());
        Connection conn = parentNode.getActiveConnections().stream()
                .filter(c -> c.getPort() == request.getSenderPort())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Conexão não encontrada para enviar bloco de resultado."));
        ObjectOutputStream out = conn.getOutputStream();
        sendResultsSafely(out, answer);
    }

    private void handleBlockAnswer(FileBlockAnswerMessage answer) {
        parentNode.handleBlockAnswer(answer);
    }

    private void handleDisconnection(NewDisconnectionRequest request, Socket socket) throws IOException {
        lock.lock();
        try {
            parentNode.getActiveConnections().removeIf(
                    conn -> conn.getAddress().equals(request.getAddress()) && conn.getPort() == request.getPort()
            );
        } finally {
            lock.unlock();
        }
        socket.close();
    }

    private void sendResultsSafely(ObjectOutputStream out, Object results) {
        synchronized (out) {
            try {
                out.writeObject(results);
                out.flush();
            } catch (IOException e) {
                System.err.println("Erro ao enviar resultados de pesquisa: " + e.getMessage());
            }
        }
    }
}