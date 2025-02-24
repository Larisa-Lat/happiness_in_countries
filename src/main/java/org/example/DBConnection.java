package org.example;
import java.sql.Connection;
import java.sql.DriverManager;


public class DBConnection {
    private Connection connection;

    public DBConnection(){
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/world_happiness_2021",
                    "root", "Put your password");
        }
        catch(Exception e){
            System.out.println(e);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void closeConnection(){
        try {
            connection.close();
        }
        catch(Exception e){
            System.out.println(e);
        }
    }
}
