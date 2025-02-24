package org.example;

import java.sql.*;
import java.util.ArrayList;

public class Model {
    private Connection connection;
    private int label;
    private ArrayList<String> Countries;
    public Model(DBConnection connection){
        this.connection = connection.getConnection();
        this.Countries = new ArrayList<String>();
    }
    public void select(String user_country){
        try {
            Statement statement = connection.createStatement();
            String query = "SELECT label FROM country_label WHERE country = " + "'" + user_country + "';"; // country из ответа пользователя
            ResultSet result_label = statement.executeQuery(query);

            while(result_label.next()) {
                this.label = result_label.getInt("label");
            }

            String get_countries = "SELECT country FROM country_label WHERE label = " + this.label + " AND country != '" + user_country + "' ORDER BY country LIMIT 10;";
            ResultSet result = statement.executeQuery(get_countries);

            Countries.clear();
            while(result.next()){
                this.Countries.add(result.getString("country"));
            }
            connection.close();
        }
        catch(Exception e){
            System.out.println(e);
        }
    }
    public String getLabel(){
        switch (label){
            case 3: return " принадлежит к категории 'несчастным' странам.";
            case 2: return " принадлежит к категории 'счастливым' странам.";
            case 1: return " принадлежит к категории 'средним' странам.";
            default: return " такой страны нет.";
        }
    }
    public String getCountries(){
        String answer = new String();
        for (String country : Countries) {
            answer += country + " \n";
        }
        return answer;
    }
}
