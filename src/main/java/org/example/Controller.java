package org.example;

public class Controller {
    private Model model;
    public Controller(Model model){
        this.model = model;
    }
    public String info_country(String user_country){
        model.select(user_country);
        return model.getLabel();
    }
    public String same_countries(){
        return model.getCountries();
    }
}
