package com.mycompany.battaglia_navale;

import java.io.*;
import java.net.*;
import java.util.*;
import com.google.gson.Gson;
import com.mycompany.battaglia_navale.logica.Cella;
import com.mycompany.battaglia_navale.logica.Colpo;
import com.mycompany.battaglia_navale.logica.Messaggio;
import com.mycompany.battaglia_navale.logica.Tabella;
import com.mycompany.battaglia_navale.payloads.AttackResultPayload;
import com.mycompany.battaglia_navale.payloads.GameOverPayload;

public class Client
{

    public static void main(String[] args) throws Exception
    {

        // Connessione al server
        Socket socket = new Socket("localhost", 5000);

        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader tastiera = new BufferedReader(new InputStreamReader(System.in));

        Gson gson = new Gson();

        String msg = "";

        int hx = 0;
        int hy = 0;

        // --- GRIGLIE DI GIOCO ---
        // 0 = vuoto
        // 2 = nave
        // 4 = nave colpita
        int[][] myBoard = new int[10][10];

        // 0 = ignoto
        // 9 = acqua
        // 1 = colpito
        // 3 = affondato
        int[][] enemyBoard = new int[10][10];

        // Mostra la griglia vuota prima del posizionamento
        clearConsole();
        System.out.println("=== TUA GRIGLIA (VUOTA) ===");
        printBoards(myBoard, enemyBoard);

        // --- POSIZIONAMENTO NAVE ---
        List<Cella> cells = askShipPlacement(tastiera, 3, myBoard);

        // Invia la nave al server
        Tabella board = new Tabella();
        board.celle = cells;
        output.println(gson.toJson(board)); // invio JSON al server

        clearConsole();
        printBoards(myBoard, enemyBoard);

        // --- LOOP DI GIOCO ---
        while (true) {

            msg = input.readLine(); // riceve messaggi dal server
            if (msg == null) break;

            // Provo a interpretare il messaggio come Messaggio (per i TUOI payload)
            Messaggio m = null;
            try {
                m = gson.fromJson(msg, Messaggio.class);
            } catch (Exception e) {
                m = null;
            }

            // ============================================================
            // 1) ATTACK_RESULT (TUO PAYLOAD)
            // ============================================================
            if (m != null && "ATTACK_RESULT".equals(m.getTipo())) {

                AttackResultPayload attackResultPayload = gson.fromJson(
                                gson.toJson(m.getPayload()),
                                AttackResultPayload.class
                        );


                if (attackResultPayload.getRisultato().equals("HIT"))
                {
                    enemyBoard[attackResultPayload.getY()][attackResultPayload.getX()] = 1;
                }
                else if (attackResultPayload.getRisultato().equals("SUNK"))
                {
                    enemyBoard[attackResultPayload.getY()][attackResultPayload.getX()] = 3;
                }
                else
                {
                    enemyBoard[attackResultPayload.getY()][attackResultPayload.getX()] = 9;
                }

                clearConsole();
                printBoards(myBoard, enemyBoard);
                continue;
            }

            // ============================================================
            // 2) GAME_OVER (TUO PAYLOAD)
            // ============================================================
            if (m != null && "GAME_OVER".equals(m.getTipo())) {

                GameOverPayload gameOverPayload =gson.fromJson(
                                gson.toJson(m.getPayload()),
                                GameOverPayload.class
                        );


                clearConsole();
                printBoards(myBoard, enemyBoard);
                System.out.println("\nPartita finita! Vincitore: " + gameOverPayload.getVincitore());
                break;
            }

            // ============================================================
            // 3) LOGICA VECCHIA (ANCORA SENZA PAYLOAD) – NON TOCCATA
            // ============================================================

            // --- SE TI HANNO COLPITO ---
            if (msg.contains("\"hitYou\":true")) {

                // Estrae coordinate del colpo subito
                hx = extractInt(msg, "\"x\":");
                hy = extractInt(msg, "\"y\":");

                if (hx >= 0 && hy >= 0) {
                    myBoard[hy][hx] = 4;  // tua nave colpita
                    enemyBoard[hy][hx] = 1; // segna colpito anche sulla board avversaria
                }

                clearConsole();
                printBoards(myBoard, enemyBoard);
                continue;
            }

            // --- SE LA PARTITA È FINITA (vecchio formato) ---
            if (msg.contains("gameOver")) {
                clearConsole();
                printBoards(myBoard, enemyBoard);
                System.out.println("\nHAI VINTO!\n");
                break;
            }

            // --- SE NON È IL TUO TURNO ---
            if (!msg.contains("yourTurn\":true"))
                continue;

            // --- INSERIMENTO COLPO ---
            int[] shot = askShot(tastiera);
            int x = shot[0];
            int y = shot[1];

            Colpo colpo = new Colpo();
            colpo.x = x;
            colpo.y = y;

            // Invia il colpo al server
            output.println(gson.toJson(colpo));

            // ATTENZIONE: niente seconda readLine qui.
            // Il risultato del colpo arriverà come ATTACK_RESULT
            // nel prossimo giro del while.
        }

        socket.close();
    }

    // ============================================================
    // PULIZIA CONSOLE (compatibile ovunque)
    // ============================================================
    public static void clearConsole() {
        for (int i = 0; i < 40; i++)
            System.out.println();
    }

    // ============================================================
    // POSIZIONAMENTO NAVE (senza vincoli)
    // ============================================================
    public static List<Cella> askShipPlacement(BufferedReader tastiera, int numeroNavi, int[][] myBoard) throws Exception {

        List<Cella> cells = new ArrayList<>();

        System.out.println("Inserisci " + numeroNavi + " navicelle (x y):");

        for (int i = 0; i < numeroNavi; i++) {

            while (true) {
                System.out.print("Cella " + (i + 1) + ": ");
                String riga = tastiera.readLine();

                if (riga == null) continue;

                String[] p = riga.trim().split("\\s+");
                if (p.length != 2) {
                    System.out.println("Formato non valido. Usa: x y");
                    continue;
                }

                int x, y;
                try {
                    x = Integer.parseInt(p[0]);
                    y = Integer.parseInt(p[1]);
                } catch (Exception e) {
                    System.out.println("Inserisci numeri validi.");
                    continue;
                }

                if (x < 0 || x > 9 || y < 0 || y > 9) {
                    System.out.println("Coordinate fuori range (0-9).");
                    continue;
                }

                if (myBoard[y][x] == 2) {
                    System.out.println("Cella già occupata.");
                    continue;
                }

                // Posiziona la nave
                cells.add(new Cella(x, y));
                myBoard[y][x] = 2;

                clearConsole();
                printBoards(myBoard, new int[10][10]);
                break;
            }
        }

        return cells;
    }

    // ============================================================
    // VALIDAZIONE COLPO
    // ============================================================
    public static int[] askShot(BufferedReader tastiera) throws Exception {

        while (true) {
            System.out.print("Inserisci colpo (x y): ");
            String riga = tastiera.readLine();

            if (riga == null) continue;

            String[] p = riga.trim().split("\\s+");
            if (p.length != 2) {
                System.out.println("Formato non valido.");
                continue;
            }

            try {
                int x = Integer.parseInt(p[0]);
                int y = Integer.parseInt(p[1]);

                if (x < 0 || x > 9 || y < 0 || y > 9) {
                    System.out.println("Coordinate fuori range.");
                    continue;
                }

                return new int[]{x, y};

            } catch (Exception e) {
                System.out.println("Inserisci numeri validi.");
            }
        }
    }

    // ============================================================
    // ESTRATTORE DI INTERI DA JSON
    // ============================================================
    public static int extractInt(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1) return -1;

        int start = idx + key.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);

        return Integer.parseInt(json.substring(start, end));
    }

    // ============================================================
    // STAMPA DELLE TABELLE
    // ============================================================
    public static void printBoards(int[][] myBoard, int[][] enemyBoard) {
        System.out.println("\n=== TUA GRIGLIA ===");
        printBoard(myBoard, true);

        System.out.println("\n=== GRIGLIA AVVERSARIO ===");
        printBoard(enemyBoard, false);
    }

    public static void printBoard(int[][] grid, boolean showShips) {

        System.out.println("   0 1 2 3 4 5 6 7 8 9");

        for (int y = 0; y < 10; y++) {
            System.out.print(y + "  ");

            for (int x = 0; x < 10; x++) {

                int v = grid[y][x];

                // --- GRIGLIE DI GIOCO ---
                // 0 = ignoto
                // 1 = colpito
                // 2 = nave
                // 3 = affondato
                // 4 = nave colpita
                // 9 = acqua

                switch (v) {
                    case 0:  System.out.print("~ "); break; // acqua / ignoto
                    case 1:  System.out.print("X "); break; // colpito
                    case 2:  System.out.print(showShips ? "O " : " ~"); break; // nave visibile solo sulla tua board
                    case 3:  System.out.print("# "); break; // affondato
                    case 4:  System.out.print("@ "); break; // tua nave colpita
                    case 9:  System.out.print("* "); break; // colpo a vuoto
                    default: System.out.print("? ");
                }
            }

            System.out.println();
        }
    }
}
