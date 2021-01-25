/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httpserver.javahttpwebserver;

import java.util.ArrayList;
/**
 *
 * @author Qafaloku Leone
 */
public class Elenco {
    private ArrayList<Nominativo> nominativi;

    public Elenco(ArrayList<Nominativo> nomi) {
        nominativi=nomi;
    }

    public ArrayList<Nominativo> getNominativi() {
        return nominativi;
    }

    public void setNominativi(ArrayList<Nominativo> nominativi) {
        this.nominativi = nominativi;
    }
}
