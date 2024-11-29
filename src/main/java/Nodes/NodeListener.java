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

public class NodeListener extends Thread {

    private final Node parentNode;
    private final int port;

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

                new Thread(() -> handleClient(socket)).start();
            }
        }
    }

    private void handleClient(Socket socket) {
        try ( ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
              ObjectInputStream in = new ObjectInputStream(socket.getInputStream()))
        {
            System.out.println("ObjectInputStream criado. Aguardando mensagens...");

            while (true) {
                Object receivedObject = in.readObject();

                //TODO Trocar por um SWITCH em vez de vários IFs
                //TODO Substituir validação de classes por ENUMS?

                switch (receivedObject) {
                    case WordSearchMessage searchMessage -> {
                        System.out.println("WordSearchMessage recebida com palavra-chave: " + searchMessage.getKeyword());
                        processSearchMessage(searchMessage, socket);
                    }
                    case NewConnectionRequest request -> {
                        System.out.println("NewConnectionRequest recebida de: " + request.getAddress() + ":" + request.getPort());
                        handleNewConnection(request, socket);
                    }
                    case List<?> objects -> {
                        try {
                            @SuppressWarnings("unchecked")
                            List<FileSearchResult> results = (List<FileSearchResult>) receivedObject;
                            System.out.println("Lista de FileSearchResult recebida com " + results.size() + " resultados.");
                            parentNode.processSearchResults(results);
                        } catch (ClassCastException e) {
                            System.err.println("Erro ao processar os resultados da pesquisa: " + e.getMessage());
                        }
                    }
                    default ->
                            System.out.println("Tipo de objeto inesperado recebido: " + receivedObject.getClass().getName());
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Erro ao ler objeto recebido: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Conexão encerrada com " + socket.getInetAddress() + ":" + socket.getPort());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar socket: " + e.getMessage());
            }
        }
    }

    private void processSearchMessage(WordSearchMessage searchMessage, Socket socket) {
        // Procura arquivos locais que correspondem à pesquisa
        List<FileSearchResult> results = parentNode.searchFiles(searchMessage);
        System.out.println("Número de resultados encontrados: " + results.size());

        // Envia os resultados de volta ao nó
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(results);
            out.flush();
            System.out.println("Resultados de pesquisa enviados de volta .");

        } catch (IOException e) {
            System.err.println("Erro ao enviar resultados de pesquisa: " + e.getMessage());
        }
    }

    private void handleNewConnection(NewConnectionRequest request, Socket socket) throws IOException {
        // Verifica se a conexão já está ativa para evitar duplicação
        if (parentNode.getActiveConnections().stream().noneMatch(
                conn -> conn.getAddress().equals(request.getAddress()) && conn.getPort() == request.getPort())) {

            // Cria a Connection com o Socket existente
            Connection conn = new Connection(request.getAddress(), request.getPort(), socket, this.parentNode);
            synchronized (parentNode.getActiveConnections()) {
                parentNode.notifyConnectionUpdated();
                parentNode.getActiveConnections().add(conn);
            }
            conn.start();
            System.out.println("Conexão adicionada: " + request.getAddress() + ":" + request.getPort());
        } else {
            System.out.println("Conexão com " + request.getAddress() + ":" + request.getPort() + " já está ativa.");
        }
    }

}
