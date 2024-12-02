package Execute;

import Nodes.FileSearchResult;
import Nodes.Node;
import Nodes.NodeInfo;
import util.Connection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.Map;

public class DownloadsWindow extends JFrame implements ConnectionListener {

    private final ConnectionWindow connectionWindow;
    private JPanel searchHeader;
    private JScrollPane filesPane;
    private JPanel buttonPanel;

    private JButton searchButton;
    private JButton downloadButton;
    private JButton connectButton;

    private JTextField searchInput;  // Movido para ser uma variável de instância

    private JList<String> searchResultsList;
    private DefaultListModel<String> searchResultsModel;

    final Node node;

    public DownloadsWindow(Node node) {
        super("Search Files");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(800, 600);
        this.setLayout(new BorderLayout());
        this.node = node;
        node.setConnectionListener(this);

        createHeader();
        createList();
        createButtons();

        this.add(searchHeader, BorderLayout.NORTH);
        this.add(filesPane, BorderLayout.CENTER);
        this.add(buttonPanel, BorderLayout.EAST);

        this.connectionWindow = new ConnectionWindow(this);

        this.setVisible(true);
        listenToCommands();
    }

    private void createHeader() {
        this.searchHeader = new JPanel(new BorderLayout());

        JLabel searchLabel = new JLabel("Search Files");
        searchHeader.add(searchLabel, BorderLayout.WEST);

        this.searchInput = new JTextField();  // Inicializada corretamente
        searchHeader.add(searchInput, BorderLayout.CENTER);

        this.searchButton = new JButton("Search");
        searchHeader.add(searchButton, BorderLayout.EAST);
    }

    private void createList() {
        this.searchResultsModel = new DefaultListModel<>();
        this.searchResultsList = new JList<>(searchResultsModel);
        this.searchResultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.filesPane = new JScrollPane(searchResultsList);
    }

    private void createButtons() {
        this.buttonPanel = new JPanel(new GridLayout(2, 1));

        this.downloadButton = new JButton("Download");
        buttonPanel.add(downloadButton);

        this.connectButton = new JButton("Connect to Node");
        buttonPanel.add(connectButton);
    }

    private void showConnectionWindow() {
        this.connectionWindow.setVisible(true);
        this.connectionWindow.updateConnectionList();
    }

    private void listenToCommands() {
        this.searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String keyword = searchInput.getText(); // Lê o texto do campo de entrada correto
                if (!keyword.isEmpty()) {
                    node.clearConsolidatedResults();
                    System.out.println("Texto digitado: " + keyword);
                    node.requestSearch(keyword);
                } else {
                    JOptionPane.showMessageDialog(DownloadsWindow.this, "Digite uma palavra-chave para pesquisa.", "Atenção", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        this.downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = searchResultsList.getSelectedIndex();
                if (selectedIndex != -1) {
                    String selectedValue = searchResultsModel.getElementAt(selectedIndex);
                    FileSearchResult fileToDownload = findFileFromString(selectedValue);
                    if (fileToDownload != null) {
                        List<Connection> nodesWithFile = node.getConsolidatedResults().get(fileToDownload);
                        DownloadTaskManager downloadTaskManager = new DownloadTaskManager(fileToDownload, nodesWithFile, node);
                    }
                }
            }
        });

        this.connectButton.addActionListener(e -> showConnectionWindow());
    }

    private FileSearchResult findFileFromString(String connectionString) {
        for (FileSearchResult file : node.getConsolidatedResults().keySet()) {
            if (connectionString.contains(file.getName())) {
                return file;
            }
        }
        return null;
    }

    @Override
    public void onConnectionUpdated() {
        connectionWindow.updateConnectionList();
    }

    @Override
    public void onConnectionError(String errorMessage) {
        connectionWindow.showErrorMessage(errorMessage);
    }
/*
    public void updateSearchResults(List<FileSearchResult> results) {
        searchResultsModel.clear();

        for (FileSearchResult result : results) {
            searchResultsModel.addElement(result.getName() + " - " + result.getAddress() + ":" + result.getPort());
        }
    }

 */

    public synchronized void updateSearchResults(Map<FileSearchResult, List<Connection>> consolidatedResults) {
        searchResultsModel.clear(); // Limpa os resultados anteriores

        for (Map.Entry<FileSearchResult, List<Connection>> entry : consolidatedResults.entrySet()) {
            String fileName = entry.getKey().getName();
            List<Connection> nodes = entry.getValue();

            // Cria a string para exibição
            StringBuilder displayString = new StringBuilder(fileName + " - ");
            for (Connection node : nodes) {
                displayString.append(node.toString()).append(", ");
            }

            // Remove a vírgula extra no final
            if (displayString.length() > 2) {
                displayString.setLength(displayString.length() - 2);
            }

            // Adiciona o resultado à interface gráfica
            searchResultsModel.addElement(displayString.toString());
        }
    }



}