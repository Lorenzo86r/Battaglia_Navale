package com.mycompany.battaglia_navale.logica;



public class Messaggio //classe generica messaggio
{
    private String tipo;
    private Object payload;


    public Messaggio()
    {
        tipo="";
        payload=null;
    }

    public void setTipo(String tipo)
    {
        this.tipo = tipo;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public String getTipo() {
        return tipo;
    }

    public Object getPayload() {
        return payload;
    }
}
