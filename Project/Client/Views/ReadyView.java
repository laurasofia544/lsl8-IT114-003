package Project.Client.Views;

import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JPanel;
import Project.Client.ClientConsole;

public class ReadyView extends JPanel {
    public ReadyView() {
        // TODO some projects may need to add other UI here for pre-session setup
        JButton readyButton = new JButton("Ready");
        readyButton.addActionListener(_ -> {
            try {
                ClientConsole.INSTANCE.sendReady();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
        this.add(readyButton);
    }
}
