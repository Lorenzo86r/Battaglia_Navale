package com.mycompany.battaglia_navale;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import com.google.gson.Gson;
import com.mycompany.battaglia_navale.logica.Colpo;
import com.mycompany.battaglia_navale.logica.Messaggio;
import com.mycompany.battaglia_navale.logica.Tabella;
import com.mycompany.battaglia_navale.payloads.AttackResultPayload;
import com.mycompany.battaglia_navale.payloads.GameOverPayload;
import com.mycompany.battaglia_navale.payloads.IncomingAttackPayload;
import com.mycompany.battaglia_navale.payloads.TurnChangePayload;

public class Server {
    private static final int PORT = 5000;
    private static final Gson gson = new Gson();
    private static final Tabella[] tabella = new Tabella[2];
    private static final PrintWriter[] outs = new PrintWriter[2];
    public static int readyPlayers = 0;

    // Colori per i log del server
    public static final String S_RESET = "\u001B[0m";
    public static final String S_CYAN = "\u001B[36m";
    public static final String S_VERDE = "\u001B[32m";
    public static final String S_ROSSO = "\u001B[31m";

    public static void main(String[] args) throws Exception {
        ServerSocket server = new ServerSocket(PORT);
        System.out.println(S_CYAN + "[SERVER] In ascolto sulla porta " + PORT + "..." + S_RESET);

        for (int i = 0; i < 2; i++) {
            Socket client = server.accept();
            System.out.println(S_VERDE + "[CONNESSIONE] Giocatore " + i + " collegato da " + client.getInetAddress() + S_RESET);
            outs[i] = new PrintWriter(client.getOutputStream(), true);
            new Thread(new PlayerHandler(client, i)).start();
        }
    }

    static class PlayerHandler implements Runnable {
        private Socket socket;
        private int id;
        private BufferedReader in;

        public PlayerHandler(Socket socket, int id) throws Exception {
            this.socket = socket;
            this.id = id;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        @Override
        public void run() {
            try {
                // 1. RICEZIONE BOARD
                String jsonBoard = in.readLine();
                tabella[id] = gson.fromJson(jsonBoard, Tabella.class);
                System.out.println(S_CYAN + "[LOG] Ricevuta flotta da Giocatore " + id + S_RESET);

                synchronized (Server.class) {
                    readyPlayers++;
                    if (readyPlayers == 2) {
                        System.out.println(S_VERDE + "[GAME] Entrambi pronti. Invio GAME_START." + S_RESET);
                        sendTo(0, "GAME_START", new TurnChangePayload(true));
                        sendTo(1, "GAME_START", new TurnChangePayload(false));
                    }
                }

                // 2. LOOP DI GIOCO
                while (true) {
                    String jsonShot = in.readLine();
                    if (jsonShot == null) break;

                    Messaggio msgRicevuto = gson.fromJson(jsonShot, Messaggio.class);
                    Colpo shot = gson.fromJson(gson.toJson(msgRicevuto.getPayload()), Colpo.class);
                    int opponent = 1 - id;

                    boolean hit = tabella[opponent].isHit(shot.x, shot.y);
                    boolean sunk = tabella[opponent].isSunk();
                    String risposta = (sunk) ? "SUNK" : (hit ? "HIT" : "MISS");

                    System.out.println(S_CYAN + "[MOSSA] P" + id + " spara in (" + shot.x + "," + shot.y + ") -> " + risposta + S_RESET);

                    // Risultato all'attaccante
                    if (!sunk) {
                        AttackResultPayload p = new AttackResultPayload();
                        p.setX(shot.x); p.setY(shot.y); p.setRisultato(risposta);
                        sendTo(id, "ATTACK_RESULT", p);
                    } else {
                        GameOverPayload go = new GameOverPayload();
                        go.setVincitore("Giocatore " + id);
                        sendTo(id, "GAME_OVER", go);
                        sendTo(opponent, "GAME_OVER", go);
                        System.out.println(S_VERDE + "[GAME OVER] Giocatore " + id + " vince!" + S_RESET);
                        break;
                    }

                    // Notifica al difensore
                    sendTo(opponent, "INCOMING_ATTACK", new IncomingAttackPayload(shot.x, shot.y, risposta));

                    // Cambio Turno
                    sendTo(id, "TURN_CHANGE", new TurnChangePayload(false));
                    sendTo(opponent, "TURN_CHANGE", new TurnChangePayload(true));
                }
                socket.close();
            } catch (Exception e) {
                System.out.println(S_ROSSO + "[ERRORE] Giocatore " + id + " disconnesso." + S_RESET);
            }
        }

        private void sendTo(int playerId, String type, Object payload) {
            Messaggio m = new Messaggio();
            m.setTipo(type); // Ricordati di cambiarlo in setType se rinomini il campo
            m.setPayload(payload);
            outs[playerId].println(gson.toJson(m));
        }

        private String risultato(boolean hit, boolean sunk) {
            if (sunk) return "SUNK";
            return hit ? "HIT" : "MISS";
        }
    }
}