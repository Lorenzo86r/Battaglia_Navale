package com.mycompany.battaglia_navale.payloads;

public class AttackResultPayload
{
   private int x;
   private int y;

   private String risultato;

   public AttackResultPayload()
   {
      x=0;
      y=0;
      risultato="";
   }


    public void setX(int x)
    {
        this.x = x;
    }


    public void setY(int y)
    {
        this.y = y;
    }

    public void setRisultato(String risultato)
    {
        this.risultato = risultato;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getRisultato() {
        return risultato;
    }
}
