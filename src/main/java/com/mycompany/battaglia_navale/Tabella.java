/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.battaglia_navale;

import java.util.List;

public class Tabella 
{
    public List<Cella> celle;

    public boolean isHit(int x, int y) 
    {
        for (Cella c : celle) 
        {
            if (c.x == x && c.y == y) 
            {
                c.hit = true;
                return true;
            }
        }
        
        return false;
    }

    public boolean isSunk() 
    {
        return celle.stream().allMatch(c -> c.hit);
    }
}
