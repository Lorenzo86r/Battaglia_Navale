package com.mycompany.battaglia_navale.payloads;
public class AttackPayload {
    private int x;
    private int y;
    public AttackPayload(int x, int y) { this.x = x; this.y = y; }
    public int getX() { return x; }
    public int getY() { return y; }
}