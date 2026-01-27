/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.battaglia_navale.logica;

/**
 *
 * @author Lorenzo
 */
public class Risultato 
{
    public boolean colpito;
    public boolean affondato;
    public boolean gameOver;
    public String messaggio;
    public boolean tuoTurno;

    public Risultato(boolean colpito, boolean affondato, boolean gameOver, String messaggio, boolean tuoTurno) {
        this.colpito = colpito;
        this.affondato = affondato;
        this.gameOver = gameOver;
        this.messaggio = messaggio;
        this.tuoTurno= tuoTurno;
    }
}

