package org.example;

import javax.swing.*;
import java.awt.*;

public class UserView extends JFrame {
    private JTextArea text;
    private JButton sendButton;
    private JLabel info;
    private JLabel same_countries;
    private Controller controller;
    public UserView(Controller controller){
        this.controller = controller;
    }
    public void init(){
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Всемирный доклад о счастье 2021");

        //создание обьектов на панели
        text = new JTextArea();
        sendButton = new JButton("Send");
        info = new JLabel();
        same_countries = new JLabel();

        // добавление на  панель с размещение местоположения
        add(text, BorderLayout.NORTH);
        add(info, BorderLayout.WEST);
        add(same_countries, BorderLayout.PAGE_END);
        add(sendButton, BorderLayout.EAST);

        sendButton.addActionListener(e ->{

            String user_country = this.text.getText();

            String category = controller.info_country(user_country);
            info.setText("<html>Ответ:<br/>" + user_country + category + "<html>");

            String countries = controller.same_countries();
            same_countries.setText("<html>" + "Страны из той же категории: <br/>" +
                    countries.replaceAll("\n", "<br/>") + "<br/><br/><html>");
        });

        setVisible(true);
    }
}
