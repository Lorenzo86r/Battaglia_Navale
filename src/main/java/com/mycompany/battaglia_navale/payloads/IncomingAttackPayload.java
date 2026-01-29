package com.mycompany.battaglia_navale.payloads;
public class IncomingAttackPayload {
    private int x;
    private int y;
    private String result;
    public IncomingAttackPayload(int x, int y, String result) {
        this.x = x; this.y = y; this.result = result;
    }
}