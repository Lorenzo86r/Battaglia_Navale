package com.mycompany.battaglia_navale;

import java.io.*;
import java.net.*;
import java.util.*;
import com.google.gson.Gson;

public class Client {

    public static void main(String[] args) throws Exception {

        // Connessione al server
        Socket socket = new Socket("10.102.21.13", 5000);

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader tastiera = new BufferedReader(new InputStreamReader(System.in));

        Gson gson = new Gson();

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
        out.println(gson.toJson(board));

        clearConsole();
        printBoards(myBoard, enemyBoard);

        // --- LOOP DI GIOCO ---
        while (true) {

            String msg = in.readLine();
            if (msg == null) break;

            // --- SE TI HANNO COLPITO ---
            if (msg.contains("\"hitYou\":true")) {
                int hx = extractInt(msg, "\"x\":");
                int hy = extractInt(msg, "\"y\":");

                if (hx >= 0 && hy >= 0) {
                    myBoard[hy][hx] = 4;
                    enemyBoard[hy][hx] = 1;
                }

                clearConsole();
                printBoards(myBoard, enemyBoard);
                continue;
            }

            // --- SE LA PARTITA È FINITA ---
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

            out.println(gson.toJson(colpo));

            // --- RISPOSTA DEL SERVER ---
            String risposta = in.readLine();
            if (risposta == null) break;

            // Fine partita
            if (risposta.contains("\"gameOver\":true")) {
                enemyBoard[y][x] = 3;
                clearConsole();
                printBoards(myBoard, enemyBoard);
                System.out.println("\nHAI VINTO!\n");
                break;
            }

            // Aggiorna la griglia avversaria
            enemyBoard[y][x] = risposta.contains("\"hit\":true") ? 1 : 9;

            if (risposta.contains("\"sunk\":true")) enemyBoard[y][x] = 3;

            clearConsole();
            printBoards(myBoard, enemyBoard);
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
    public static List<Cella> askShipPlacement(BufferedReader tastiera, int size, int[][] myBoard) throws Exception {

        List<Cella> cells = new ArrayList<>();

        System.out.println("Inserisci " + size + " navicelle (x y):");

        for (int i = 0; i < size; i++) {

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
                    case 0:  System.out.print("~ "); break; //acqua
                    case 1:  System.out.print("X "); break; // colpito la barca
                    case 2:  System.out.print(showShips ? "O " : " ~"); break; //posizione tua nave in tua visione o avversaria
                    case 3:  System.out.print("# "); break; //hai affondato l'ultima nave
                    case 4:  System.out.print("@ "); break; //tua nave colpita
                    case 9:  System.out.print("* "); break; //colpo a vuoto
                    default: System.out.print("? ");

                }
            }

            System.out.println();
        }
    }
}
;