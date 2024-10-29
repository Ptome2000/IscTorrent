package Execute;

import Creation.NewConnectionRequest;

import javax.swing.*;
import java.awt.*;

public class ConnectionWindow extends JDialog {

    private JTextField addressInput;
    private JTextField portInput;

    private JButton cancelButton;
    private JButton okButton;

    public ConnectionWindow(DownloadsWindow downloadsWindow) {
        super(downloadsWindow, "Connect to node", true);
        this.setSize(600, 100);
        this.setLayout(new FlowLayout());
        createWindow();
        this.setLocationRelativeTo(downloadsWindow);
        listenToCommands();
    }

    private void createWindow() {
        JLabel addressLabel = new JLabel("Address:");
        this.add(addressLabel);

        this.addressInput = new JTextField("localhost", 10);
        this.add(addressInput);

        JLabel portLabel = new JLabel("Port:");
        this.add(portLabel);

        this.portInput = new JTextField("8081", 5);
        this.add(portInput);

        this.cancelButton = new JButton("Cancel");
        this.add(cancelButton);

        this.okButton = new JButton("OK");
        this.add(okButton);
    }

    private void listenToCommands() {
        this.cancelButton.addActionListener(e -> this.dispose());

        this.okButton.addActionListener(e -> {
            String address = addressInput.getText();
            String port = portInput.getText();
            System.out.println("Connected to the address: " + address + " in the port: " + port);
            NewConnectionRequest request = new NewConnectionRequest();
            this.dispose();
        });
    }

}
