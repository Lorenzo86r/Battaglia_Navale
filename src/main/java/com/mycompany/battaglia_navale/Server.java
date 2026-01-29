package com.mycompany.battaglia_navale;

import java.io.*;
import java.net.*;
import com.google.gson.Gson;
import com.mycompany.battaglia_navale.logica.Colpo;
import com.mycompany.battaglia_navale.logica.Messaggio;
import com.mycompany.battaglia_navale.logica.Risultato;
import com.mycompany.battaglia_navale.logica.Tabella;
import com.mycompany.battaglia_navale.payloads.AttackResultPayload;
import com.mycompany.battaglia_navale.payloads.GameOverPayload;

public class Server 
{
    // Porta su cui il server ascolta
    private static final int PORT = 5000;

    // Oggetto Gson per serializzare/deserializzare JSON
    private static Gson gson = new Gson();

    // Tabelle dei due giocatori (0 e 1)
    private static Tabella[] tabella = new Tabella[2];

    // Output stream per inviare messaggi ai due client
    private static PrintWriter[] outs = new PrintWriter[2];

    // Numero di giocatori che hanno inviato la loro board
    public static int readyPlayers = 0, connectedPlayers = 0;

    // Indica di chi è il turno (0 = player1, 1 = player2)
    private static int turn = 0;

    public static void main(String[] args) throws Exception 
    {
        // Avvio del server
        ServerSocket server = new ServerSocket(PORT);
        System.out.println("Server pronto...");

        // Attende la connessione di 2 giocatori
        for (int i = 0; i < 2; i++) 
        {
            Socket client = server.accept();
            System.out.println("Client " + i + " connesso");
            connectedPlayers++;

            // Salva il canale di output del client
            outs[i] = new PrintWriter(client.getOutputStream(), true);

            // Avvia un thread dedicato per gestire il giocatore
            new Thread(new PlayerHandler(client, i)).start();
        }
    }

    // ============================================================
    // THREAD CHE GESTISCE UN SINGOLO GIOCATORE
    // ============================================================
    static class PlayerHandler implements Runnable
    {
        private Socket socket;      // socket del giocatore
        private int id;             // id del giocatore (0 o 1)
        private BufferedReader in;  // input dal client

        public PlayerHandler(Socket socket, int id) throws Exception {
            this.socket = socket;
            this.id = id;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        @Override
        public void run()
        {

            boolean hit;
            boolean sunk;
            boolean gameOver;

            int opponent;

            String jsonShot;
            String risposta;

            Colpo shot;
            Messaggio result=new Messaggio();
            AttackResultPayload attackResultPayload=new AttackResultPayload();
            GameOverPayload gameOverPayload=new GameOverPayload();


            try {
                // ============================================================
                // 1. RICEZIONE DELLA BOARD DEL GIOCATORE
                // ============================================================
                String jsonBoard = in.readLine();// riceve JSON
                tabella[id] = gson.fromJson(jsonBoard, Tabella.class); // lo converte in Tabella

                // Quando entrambi i giocatori hanno inviato la board → inizia la partita
                synchronized (Server.class) {
                    readyPlayers++;

                    if (readyPlayers == 2) {
                        // Giocatore 0 inizia
                        outs[0].println("{\"message\":\"Partita iniziata! Sei il primo a tirare.\",\"yourTurn\":true}");

                        // Giocatore 1 attende
                        outs[1].println("{\"message\":\"Partita iniziata! Attendi il tuo turno.\",\"yourTurn\":false}");
                    }
                }

                // ============================================================
                // 2. LOOP DI GIOCO
                // ============================================================
                while (true)
                {

                    // Attende un colpo dal giocatore
                    jsonShot = in.readLine();
                    if (jsonShot == null) break; // client disconnesso

                    // Converte il JSON in oggetto Colpo
                    shot = gson.fromJson(jsonShot, Colpo.class);

                    // Identifica l'avversario
                    opponent = 1 - id;

                    // ============================================================
                    // CONTROLLO SE IL COLPO HA COLPITO UNA NAVE
                    // ============================================================
                    hit = tabella[opponent].isHit(shot.x, shot.y);
                    sunk = tabella[opponent].isSunk(); // nave affondata?
                    gameOver = sunk;                   // partita finita?

                    risposta=risultato(hit,sunk);

                    // ============================================================
                    // RISPOSTA AL GIOCATORE CHE HA TIRATO
                    // ============================================================

                    if(!gameOver)
                    {
                        result.setTipo("ATTACK_RESULT");

                        attackResultPayload.setX(shot.x);
                        attackResultPayload.setY(shot.y);
                        attackResultPayload.setRisultato(risposta);

                        result.setPayload(attackResultPayload);
                    }
                    else
                    {
                        result.setTipo("GAME_OVER");
                        gameOverPayload.setVincitore(String.valueOf(id));
                        result.setPayload(gameOverPayload);
                    }

                    outs[id].println(gson.toJson(result));

                    // ============================================================
                    // INVIARE ALL'AVVERSARIO CHE È STATO COLPITO
                    // ============================================================
                    if (hit) {
                        outs[opponent].println(
                            "{\"hitYou\":true,\"x\":" + shot.x + ",\"y\":" + shot.y + "}"
                        );
                    }

                    // ============================================================
                    // FINE PARTITA
                    // ============================================================
                    if (gameOver) {
                        outs[opponent].println("{\"message\":\"Hai perso!\",\"gameOver\":true}");
                        break;
                    }

                    // ============================================================
                    // CAMBIO TURNO
                    // ============================================================
                    turn = opponent;

                    outs[opponent].println("{\"message\":\"Tocca a te!\",\"yourTurn\":true}");
                }

                // Chiude la connessione del giocatore
                socket.close();

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        private  String risultato(boolean hit, boolean sunk)
        {

            if(hit)
            {
               return "HIT";
            }

            if(sunk)
            {
                return "SUNK";
            }

            return "";

        }

    }
}

