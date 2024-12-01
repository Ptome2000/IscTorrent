package Nodes;

import Messages.NewConnectionRequest;
import Messages.NewDisconnectionRequest;
import Messages.WordSearchMessage;
import util.Connection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class NodeListener extends Thread {

    private final Node parentNode;
    private final int port;
    private ExecutorService threadPool = Executors.newFixedThreadPool(10);


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
                System.out.println("Aguardando conexão...");
                Socket socket = serverSocket.accept();
                System.out.println("Conexão aceita de " + socket.getInetAddress() + ":" + socket.getPort());

                // Inicia uma nova thread para processar as mensagens do cliente conectado
                // TODO - Mudar para uma worker Thread?

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
                System.out.println("Objeto recebido: " + receivedObject.getClass().getName());

                if (receivedObject instanceof WordSearchMessage searchMessage) {
                    System.out.println("WordSearchMessage recebida com palavra-chave: " + searchMessage.getKeyword());
                    processSearchMessage(searchMessage);

                } else if (receivedObject instanceof NewConnectionRequest request) {
                    System.out.println("NewConnectionRequest recebida de: " + request.getAddress() + ":" + request.getPort());
                    handleNewConnection(request, socket);

                } else if (receivedObject instanceof NewDisconnectionRequest request) {
                    System.out.println("NewDisconnectionRequest recebida de: " + request.getAddress() + ":" + request.getPort());
                    handleDisconnection(request, socket);

                } else if (receivedObject instanceof List<?>) {
                    try {
                        @SuppressWarnings("unchecked")
                        List<FileSearchResult> results = (List<FileSearchResult>) receivedObject;
                        System.out.println("Lista de FileSearchResult recebida com " + results.size() + " resultados.");
                        parentNode.processSearchResults(results);
                    } catch (ClassCastException e) {
                        System.err.println("Erro ao processar os resultados da pesquisa: " + e.getMessage());
                    }
                } else {
                    System.out.println("Tipo de objeto inesperado recebido: " + receivedObject.getClass().getName());
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




    private void processSearchMessage(WordSearchMessage searchMessage) {
        // Procura arquivos locais que correspondem à pesquisa
        List<FileSearchResult> results = parentNode.searchFiles(searchMessage);
        System.out.println("Número de resultados encontrados: " + results.size());
        System.out.println(results);
        // Envia os resultados de volta ao nó
        try {
            Connection conn = parentNode.getActiveConnections().stream()
                    .filter(c -> c.getPort() == searchMessage.getSenderPort())
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Conexão não encontrada para enviar resultados de pesquisa."));
            ObjectOutputStream out = conn.getOutputStream();
            out.writeObject(results);
            out.flush();
            System.out.println("Resultados de pesquisa enviados de volta.");

        } catch (IOException e) {
            System.err.println("Erro ao enviar resultados de pesquisa: " + e.getMessage());
        }
    }

    private void handleNewConnection(NewConnectionRequest request, Socket socket) throws IOException {
        synchronized (parentNode.getActiveConnections()) {
            if (parentNode.getActiveConnections().stream().noneMatch(
                    conn -> conn.getAddress().equals(request.getAddress()) &&
                            conn.getPort() == request.getPort())) {
                System.out.println("Nova conexão recebida dexxxxxx: " + request.getAddress() + ":" + request.getPort());
                // Cria uma nova Connection com o socket recebido
                try {
                    Connection conn = new Connection(request.getAddress(), request.getPort());
                    if (!conn.isAlive()) {
                        conn.start();
                    }
                    parentNode.addActiveConnections(conn);
                    System.out.println("Conexão adicionada e iniciada: " + conn.getAddress() + ":" + conn.getPort());
                } catch (IOException e) {
                    System.err.println("Erro ao criar a conexão: " + e.getMessage());
                }

                if (!parentNode.getActiveConnections().isEmpty()) {
                for (Connection connection : parentNode.getActiveConnections()) {
                    System.out.println("Conexões ativas: " + connection.getAddress() + ":" + connection.getPort());
                }
                }
                else {
                    System.out.println("Conexão nao ha ");
                }
                //System.out.println("Conexão adicionada e iniciada: " + conn.getAddress() + ":" + conn.getPort());
            } else {
                System.out.println("Conexão já existente: " + request.getAddress() + ":" + request.getPort());
            }
        }
    }


    private void handleDisconnection(NewDisconnectionRequest request, Socket socket) throws IOException {
        System.out.println("Received disconnection request from " + request.getAddress() + ":" + request.getPort());
        synchronized (parentNode.getActiveConnections()) {
            parentNode.getActiveConnections().removeIf(
                    conn -> conn.getAddress().equals(request.getAddress()) && conn.getPort() == request.getPort()
            );
        }
        socket.close();
    }


}
