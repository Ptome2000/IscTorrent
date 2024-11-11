package Execute;

//import Messages.NewConnectionRequest;
import javax.swing.*;
import java.awt.*;

public class ConnectionWindow extends JDialog {

    private JTextField addressInput;
    private JTextField portInput;

    private JButton cancelButton;
    private JButton okButton;

    private final DownloadsWindow downloadsWindow;

    public ConnectionWindow(DownloadsWindow downloadsWindow) {
        super(downloadsWindow, "Connect to node", true);
        this.setSize(600, 100);
        this.setLayout(new FlowLayout());
        this.downloadsWindow = downloadsWindow;
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
            this.downloadsWindow.node.connectToNode(address, Integer.parseInt(port));
            this.dispose();
        });
    }

}
