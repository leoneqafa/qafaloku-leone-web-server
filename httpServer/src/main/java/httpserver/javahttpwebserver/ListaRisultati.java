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
public class ListaRisultati {
    private int size;
    private ArrayList<PuntoVendita> listaRisultati;

    public void setSize(int size) {
        this.size = size;
    }

    public void setListaRisultati(ArrayList<PuntoVendita> listaRisultati) {
        this.listaRisultati = listaRisultati;
    }

    public int getSize() {
        return size;
    }

    public ArrayList<PuntoVendita> getListaRisultati() {
        return listaRisultati;
    }
}