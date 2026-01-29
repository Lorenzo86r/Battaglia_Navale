package com.mycompany.battaglia_navale;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.mycompany.battaglia_navale.logica.Cella;
import com.mycompany.battaglia_navale.logica.Colpo;
import com.mycompany.battaglia_navale.logica.Messaggio;
import com.mycompany.battaglia_navale.logica.Tabella;
import com.mycompany.battaglia_navale.payloads.AttackResultPayload;
import com.mycompany.battaglia_navale.payloads.GameOverPayload;

public class Client {
    // --- COSTANTI ESTETICHE ---
    public static final String RESET = "\u001B[0m";
    public static final String ROSSO = "\u001B[31m";
    public static final String VERDE = "\u001B[32m";
    public static final String GIALLO = "\u001B[33m";
    public static final String BLU = "\u001B[34m";
    public static final String CIANO = "\u001B[36m";
    public static final String VIOLA = "\u001B[35m";
    public static final String BIANCO = "\u001B[37m";

    private static boolean isMyTurn = false;
    private static int[][] myBoard = new int[10][10];
    private static int[][] enemyBoard = new int[10][10];

    public static void main(String[] args) throws Exception {
        try (Socket socket = new Socket("localhost", 5000)) {
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader tastiera = new BufferedReader(new InputStreamReader(System.in));
            Gson gson = new Gson();

            printBanner();
            
            // 1. POSIZIONAMENTO
            System.out.println(CIANO + "\n>>> FASE DI POSIZIONAMENTO NAVI <<<" + RESET);
            printBoards(myBoard, enemyBoard);
            List<Cella> cells = askShipPlacement(tastiera, 2, myBoard);
            
            Tabella board = new Tabella();
            board.celle = cells;
            output.println(gson.toJson(board));
            
            System.out.println(GIALLO + "\n[!] Flotta schierata. In attesa dell'avversario..." + RESET);

            // 2. LOOP DI GIOCO
            while (true) {
                if (isMyTurn) {
                    System.out.println(VERDE + "\n--- TOCCA A TE! Prendi la mira ---" + RESET);
                    int[] shot = askShot(tastiera);
                    sendAttack(output, gson, shot[0], shot[1]);
                    isMyTurn = false;
                }

                String msg = input.readLine();
                if (msg == null) break;

                Messaggio m = gson.fromJson(msg, Messaggio.class);
                if (m == null) continue;

                switch (m.getTipo()) {
                    case "GAME_START" -> {
                        isMyTurn = (boolean) ((Map)m.getPayload()).get("yourTurn");
                        clearConsole();
                        printBoards(myBoard, enemyBoard);
                        System.out.println(VERDE + ">>> LA BATTAGLIA COMINCIA! <<<" + RESET);
                    }
                    case "ATTACK_RESULT" -> {
                        AttackResultPayload res = gson.fromJson(gson.toJson(m.getPayload()), AttackResultPayload.class);
                        updateEnemyBoard(res);
                        clearConsole();
                        printBoards(myBoard, enemyBoard);
                        System.out.println(GIALLO + "Ultimo colpo: " + res.getRisultato() + RESET);
                    }
                    case "INCOMING_ATTACK" -> {
                        Map p = (Map)m.getPayload();
                        updateMyBoard(p);
                        clearConsole();
                        printBoards(myBoard, enemyBoard);
                        System.out.println(ROSSO + "Attacco subito in (" + p.get("x") + "," + p.get("y") + "): " + p.get("result") + RESET);
                    }
                    case "TURN_CHANGE" -> {
                        isMyTurn = (boolean) ((Map)m.getPayload()).get("yourTurn");
                    }
                    case "GAME_OVER" -> {
                        GameOverPayload go = gson.fromJson(gson.toJson(m.getPayload()), GameOverPayload.class);
                        clearConsole();
                        printBoards(myBoard, enemyBoard);
                        System.out.println(VIOLA + "\n************************************");
                        System.out.println("       VINCITORE: " + go.getVincitore());
                        System.out.println("************************************" + RESET);
                        return;
                    }
                }
            }
        }
    }

    // --- METODI DI SUPPORTO ESTETICI ---

    private static void printBanner() {
        System.out.println(CIANO + "  ____    _  _____ _____  _    ____ _     ___    _    " + RESET);
        System.out.println(CIANO + " | __ )  / \\|_   _|_   _|/ \\  / ___| |   |_ _|  / \\   " + RESET);
        System.out.println(CIANO + " |  _ \\ / _ \\ | |   | | / _ \\| |  _| |    | |  / _ \\  " + RESET);
        System.out.println(CIANO + " | |_) / ___ \\| |   | |/ ___ \\ |_| | |___ | | / ___ \\ " + RESET);
        System.out.println(CIANO + " |____/_/   \\_\\_|   |_/_/   \\_\\____|_____|___/_/   \\_\\" + RESET);
        System.out.println(CIANO + "                                                      " + RESET);
        System.out.println(CIANO + "      _   _    _  __     __  _    _     _____       " + RESET);
        System.out.println(CIANO + "     | \\ | |  / \\ \\ \\   / / / \\  | |   | ____|      " + RESET);
        System.out.println(CIANO + "     |  \\| | / _ \\ \\ \\ / / / _ \\ | |   |  _|        " + RESET);
        System.out.println(CIANO + "     | |\\  |/ ___ \\ \\ V / / ___ \\| |___| |___       " + RESET);
        System.out.println(CIANO + "     |_| \\_/_/   \\_\\ \\_/ /_/   \\_\\_____|_____|      " + RESET);
    }

    public static void printBoards(int[][] myBoard, int[][] enemyBoard) {
        System.out.println("\n" + VERDE + "    [ TUA FLOTTA ]" + RESET + "              " + ROSSO + "   [ RADAR NEMICO ]" + RESET);
        System.out.println(BIANCO + "   0 1 2 3 4 5 6 7 8 9            0 1 2 3 4 5 6 7 8 9" + RESET);
        
        for (int y = 0; y < 10; y++) {
            // Riga Tua Board
            System.out.print(BIANCO + y + "  " + RESET);
            for (int x = 0; x < 10; x++) System.out.print(getSymbol(myBoard[y][x], true));
            
            System.out.print("        ");
            
            // Riga Enemy Board
            System.out.print(BIANCO + y + "  " + RESET);
            for (int x = 0; x < 10; x++) System.out.print(getSymbol(enemyBoard[y][x], false));
            System.out.println();
        }
    }

    private static String getSymbol(int cell, boolean showShips) {
        return switch (cell) {
            case 0 -> BLU + "~ " + RESET;
            case 1 -> ROSSO + "X " + RESET;
            case 2 -> showShips ? VERDE + "O " + RESET : BLU + "~ " + RESET;
            case 3 -> GIALLO + "# " + RESET;
            case 4 -> ROSSO + "@ " + RESET;
            case 9 -> BIANCO + "* " + RESET;
            default -> "? ";
        };
    }

    public static void clearConsole() {
        System.out.print("\033[H\033[2J"); // Metodo ANSI più potente per pulire lo schermo
        System.out.flush();
    }

    // --- LOGICA DI INPUT ---

    public static List<Cella> askShipPlacement(BufferedReader tastiera, int numeroNavi, int[][] myBoard) throws Exception {
        List<Cella> allCells = new ArrayList<>();
        for (int i = 0; i < numeroNavi; i++) {
            List<Cella> currentShipCells = new ArrayList<>();
            int dim = 0;
             while (true) {
                System.out.print(CIANO + "\nTipo Nave " + (i + 1) + " (1: Destroyer-2 celle, 2: Submarine-3 celle): " + RESET);
                String scelta = tastiera.readLine(); 
                
                if (scelta == null) continue;
                scelta = scelta.trim();

                if ("1".equals(scelta)) {
                    dim = 2;
                    break; // Scelta valida, esco dal while
                } else if ("2".equals(scelta)) {
                    dim = 3;
                    break; // Scelta valida, esco dal while
                } else {
                    System.out.println(ROSSO + "Scelta invalida! Inserisci 1 o 2." + RESET);
                }
            }

            for (int j = 0; j < dim; j++) {
                while (true) {
                    if(dim == 2) System.out.print(CIANO + "Destroyer: Cella " + (j + 1) + " (x y): " + RESET);
                    else System.out.print(CIANO + "Submarine: Cella " + (j + 1) + " (x y): " + RESET);
                    String riga = tastiera.readLine();
                    String[] p = (riga != null) ? riga.trim().split("\\s+") : new String[0];
                    if (p.length < 2) { System.out.println(ROSSO + "Inserisci due coordinate!" + RESET); continue; }
                    try {
                        int x = Integer.parseInt(p[0]), y = Integer.parseInt(p[1]);
                        if (x < 0 || x > 9 || y < 0 || y > 9 || myBoard[y][x] != 0) { System.out.println(ROSSO + "Invalida!" + RESET); continue; }
                        
                        if (j > 0) {
                            Cella prev = currentShipCells.get(j-1);
                            if ((Math.abs(x - prev.x) + Math.abs(y - prev.y)) != 1) { System.out.println(ROSSO + "Non adiacente!" + RESET); continue; }
                            if (j == 2) {
                                Cella prima = currentShipCells.get(0);
                                if (y != prima.y && x != prima.x) { System.out.println(ROSSO + "Deve essere dritta!" + RESET); continue; }
                            }
                        }
                        Cella n = new Cella(x, y);
                        currentShipCells.add(n); allCells.add(n); myBoard[y][x] = 2;
                        break;
                    } catch (Exception e) { System.out.println(ROSSO + "Numeri non validi!" + RESET); }
                }
            }
            clearConsole();
            printBoards(myBoard, new int[10][10]);
        }
        return allCells;
    }

    public static int[] askShot(BufferedReader tastiera) throws Exception {
        while (true) {
            System.out.print("Mira (x y): ");
            String riga = tastiera.readLine();
            String[] p = (riga != null) ? riga.trim().split("\\s+") : new String[0];
            if (p.length < 2) continue;
            try {
                int x = Integer.parseInt(p[0]), y = Integer.parseInt(p[1]);
                if (x >= 0 && x <= 9 && y >= 0 && y <= 9) return new int[]{x, y};
            } catch (Exception e) {}
            System.out.println(ROSSO + "Coordinate errate!" + RESET);
        }
    }

    private static void sendAttack(PrintWriter out, Gson gson, int x, int y) {
        Messaggio m = new Messaggio();
        m.setTipo("ATTACK");
        Colpo c = new Colpo(); c.x = x; c.y = y;
        m.setPayload(c);
        out.println(gson.toJson(m));
    }

    private static void updateEnemyBoard(AttackResultPayload res) {
        int val = switch(res.getRisultato()) {
            case "HIT" -> 1;
            case "SUNK" -> 3;
            default -> 9;
        };
        enemyBoard[res.getY()][res.getX()] = val;
    }

    private static void updateMyBoard(Map p) {
        if (!"MISS".equals(p.get("result"))) {
            int ix = ((Double) p.get("x")).intValue();
            int iy = ((Double) p.get("y")).intValue();
            myBoard[iy][ix] = 4;
        }
    }
}