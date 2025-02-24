package org.example;

import javax.swing.*;
import java.awt.*;

public class Main extends JFrame {
    public Main(){
        DBConnection connection = new DBConnection();
        Model model = new Model(connection);
        Controller controller = new Controller(model);
        UserView userView = new UserView(controller);
        SwingUtilities.invokeLater(() -> {
            userView.init();
        });
    }
    public static void main(String[] args) {
        new Main();
    }
}