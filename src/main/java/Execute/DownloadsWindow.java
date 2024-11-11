package Execute;

import Nodes.Node;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DownloadsWindow extends JFrame {

    private final ConnectionWindow connectionWindow;
    private JPanel searchHeader;
    private JScrollPane filesPane;
    private JPanel buttonPanel;

    private JButton searchButton;
    private JButton downloadButton;
    private JButton connectButton;

    final Node node;

    public DownloadsWindow(Node node) {
        super("Search Files");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(800, 600);
        this.setLayout(new BorderLayout());
        this.node = node;

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

        JTextField searchInput = new JTextField();
        searchHeader.add(searchInput, BorderLayout.CENTER);

        this.searchButton = new JButton("Search");
        searchHeader.add(searchButton, BorderLayout.EAST);
    }

    private void createList() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        listModel.addElement("no-copyright-music-2024_no-copyright-music-191794-um.mp3");
        listModel.addElement("short-adventurous-intro-1-117090-um.mp3");
        JList<String> fileList = new JList<>(listModel);
        this.filesPane = new JScrollPane(fileList);
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
    }

    private void listenToCommands() {
        this.searchButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Search Button pressed");
            }
        });

        this.downloadButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Download Button pressed");
            }
        });

        this.connectButton.addActionListener(e -> showConnectionWindow());

    }

}