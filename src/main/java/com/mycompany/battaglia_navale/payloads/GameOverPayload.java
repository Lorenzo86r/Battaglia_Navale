package com.mycompany.battaglia_navale.payloads;

public class GameOverPayload
{
    private String vincitore;


    public GameOverPayload()
    {
        vincitore="";
    }

    public void setVincitore(String vincitore) {
        this.vincitore = vincitore;
    }

    public String getVincitore() {
        return vincitore;
    }
}
