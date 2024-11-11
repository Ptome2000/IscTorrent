package Execute;

import javax.swing.*;
import java.awt.*;
import java.util.Set;
import util.Connection;

public class ConnectionWindow extends JDialog {

    private JTextField addressInput;
    private JTextField portInput;

    private JButton cancelButton;
    private JButton okButton;
    private JButton closeConnectionButton;

    private final DownloadsWindow downloadsWindow;

    private DefaultListModel<String> connectionsListModel; // TODO: Upgrade this (EXTRA)
    private JList<String> activeConnectionsList;

    public ConnectionWindow(DownloadsWindow downloadsWindow) {
        super(downloadsWindow, "Connect to node", true);
        this.setSize(800, 300);
        this.setLayout(new BorderLayout());
        this.downloadsWindow = downloadsWindow;
        createWindow();
        this.setLocationRelativeTo(downloadsWindow);
        listenToCommands();
    }

    private void createWindow() {
        JPanel inputPanel = new JPanel(new FlowLayout());

        JLabel addressLabel = new JLabel("Address:");
        inputPanel.add(addressLabel);

        this.addressInput = new JTextField("localhost", 10);
        inputPanel.add(addressInput);

        JLabel portLabel = new JLabel("Port:");
        inputPanel.add(portLabel);

        this.portInput = new JTextField("8081", 5);
        inputPanel.add(portInput);

        this.cancelButton = new JButton("Cancel");
        inputPanel.add(cancelButton);

        this.okButton = new JButton("OK");
        inputPanel.add(okButton);

        this.closeConnectionButton = new JButton("Close Connection");
        this.closeConnectionButton.setEnabled(false);
        inputPanel.add(closeConnectionButton);

        this.add(inputPanel, BorderLayout.NORTH);

        this.connectionsListModel = new DefaultListModel<>();
        this.activeConnectionsList = new JList<>(connectionsListModel);
        this.activeConnectionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScrollPane = new JScrollPane(activeConnectionsList);
        this.add(listScrollPane, BorderLayout.CENTER);
    }

    private void listenToCommands() {
        this.cancelButton.addActionListener(e -> this.dispose());

        this.okButton.addActionListener(e -> {
            String address = addressInput.getText();
            String port = portInput.getText();
            this.downloadsWindow.node.connectToNode(address, Integer.parseInt(port));
            updateConnectionList();
        });

        this.activeConnectionsList.addListSelectionListener(e -> {
            closeConnectionButton.setEnabled(!activeConnectionsList.isSelectionEmpty());
        });

        this.closeConnectionButton.addActionListener(e -> {
            int selectedIndex = activeConnectionsList.getSelectedIndex();
            if (selectedIndex != -1) {
                String selectedValue = connectionsListModel.getElementAt(selectedIndex);
                Connection connectionToClose = findConnectionFromString(selectedValue);
                if (connectionToClose != null) {
                    downloadsWindow.node.closeConnection(connectionToClose);
                    updateConnectionList();
                }
            }
        });
    }

    public void updateConnectionList() {
        connectionsListModel.clear();
        Set<Connection> activeConnections = downloadsWindow.node.getActiveConnections();
        for (Connection conn : activeConnections) {
            connectionsListModel.addElement(conn.toString());
        }
        closeConnectionButton.setEnabled(false);
    }

    private Connection findConnectionFromString(String connectionString) {
        for (Connection conn : downloadsWindow.node.getActiveConnections()) {
            if (conn.toString().equals(connectionString)) {
                return conn;
            }
        }
        return null;
    }
}
