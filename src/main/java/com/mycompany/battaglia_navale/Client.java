// package di appartenenza
package com.mycompany.battaglia_navale;

// import librerie
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

public class Client {
    private static int numNaviUser = 0;

    public static void main(String[] args) throws Exception {

        // Connessione al server
        Socket socket = new Socket("localhost", 5000);

        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader tastiera = new BufferedReader(new InputStreamReader(System.in));

        Gson gson = new Gson();

        String msg;

        int hx, hy;

        int[][] myBoard = new int[10][10];

        int[][] enemyBoard = new int[10][10];

        // Mostra la griglia vuota prima del posizionamento
        clearConsole();
        System.out.println("=== TUA GRIGLIA (VUOTA) ===");
        printBoards(myBoard, enemyBoard);

        // --- POSIZIONAMENTO NAVE ---
        List<Cella> cells = askShipPlacement(tastiera, 2, myBoard); // 2 navi 

        // Creiamo la tabella con le celle
        Tabella board = new Tabella();
        board.celle = cells;

        // Impacchettiamo tutto nel Messaggio
        Messaggio mPos = new Messaggio();
        mPos.setTipo("PLACE_SHIPS");
        mPos.setPayload(board);

        // Inviamo il messaggio JSON
        output.println(gson.toJson(mPos));

        clearConsole();
        printBoards(myBoard, enemyBoard);

        // --- LOOP DI GIOCO ---
        while (true) {
            msg = input.readLine(); // riceve messaggi dal server
            if (msg == null)
                break;

            // provo a interpretare il messaggio come Messaggio (per i TUOI payload)
            Messaggio m;
            try {
                m = gson.fromJson(msg, Messaggio.class);
            } catch (Exception e) {
                m = null;
            }

            if (m != null && "GAME_START".equals(m.getTipo())) {
                System.out.println("Entrambi i giocatori pronti! La partita inizia.");
                // Estrai dal payload se è il tuo turno
                continue;
            }

            // 1) ATTACK RESULT
            if (m != null && "ATTACK_RESULT".equals(m.getTipo())) {

                AttackResultPayload attackResultPayload = gson.fromJson(
                        gson.toJson(m.getPayload()),
                        AttackResultPayload.class);

                String result = attackResultPayload.getRisultato();
                if (result.equals("HIT"))
                    enemyBoard[attackResultPayload.getY()][attackResultPayload.getX()] = 1;
                else if (result.equals("SUNK"))
                    enemyBoard[attackResultPayload.getY()][attackResultPayload.getX()] = 3;
                else
                    enemyBoard[attackResultPayload.getY()][attackResultPayload.getX()] = 9;

                clearConsole();
                printBoards(myBoard, enemyBoard);
                continue;
            }

            // 2) GAME OVER
            if (m != null && "GAME_OVER".equals(m.getTipo())) {

                GameOverPayload gameOverPayload = gson.fromJson(
                        gson.toJson(m.getPayload()),
                        GameOverPayload.class);

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
                    myBoard[hy][hx] = 4; // tua nave colpita
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

    // pulizia console
    public static void clearConsole() {
        for (int i = 0; i < 40; i++)
            System.out.println();
    }

    // metodo per il posizionamento delle navi in console
    public static List<Cella> askShipPlacement(BufferedReader tastiera, int numeroNavi, int[][] myBoard)
            throws Exception {
        List<Cella> cells = new ArrayList<>();

        for (int i = 0; i < numeroNavi; i++) {
            System.out.println("\nNave " + (i + 1) + ":");
            System.out.print("Scegli: 1 per Destroyer (2 celle), 2 per Submarine (3 celle): ");
            String tipo = tastiera.readLine();
            int dimensione = (tipo.equals("1")) ? 2 : 3;

            for (int j = 0; j < dimensione; j++) {
                while (true) {
                    System.out.print(
                            "Cella " + (j + 1) + " per " + (dimensione == 2 ? "Destroyer" : "Submarine") + " (x y): ");
                    String riga = tastiera.readLine();
                    if (riga == null)
                        continue;
                    String[] p = riga.trim().split("\\s+");

                    try {
                        int x = Integer.parseInt(p[0]);
                        int y = Integer.parseInt(p[1]);

                        if (x >= 0 && x < 10 && y >= 0 && y < 10 && myBoard[y][x] == 0) {
                            cells.add(new Cella(x, y));
                            myBoard[y][x] = 2;
                            numNaviUser++; // Importante per la stampa della griglia
                            break;
                        } else {
                            System.out.println("Coordinate non valide o occupate!");
                        }
                    } catch (Exception e) {
                        System.out.println("Errore! Inserisci due numeri separati da spazio.");
                    }
                }
            }
            clearConsole();
            printBoards(myBoard, new int[10][10]);
        }
        return cells;
    }

    // metodo per validare un colpo
    public static int[] askShot(BufferedReader tastiera) throws Exception {

        while (true) {
            System.out.print("Inserisci colpo (x y): ");
            String riga = tastiera.readLine();

            if (riga == null)
                continue;

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

                return new int[] { x, y };

            } catch (Exception e) {
                System.out.println("Inserisci numeri validi.");
            }
        }
    }

    // estrai interi da un json
    public static int extractInt(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1)
            return -1;

        int start = idx + key.length();
        int end = json.indexOf(",", start);
        if (end == -1)
            end = json.indexOf("}", start);

        return Integer.parseInt(json.substring(start, end));
    }

    // stampa delle navi su console
    public static void printBoards(int[][] myBoard, int[][] enemyBoard) {
        System.out.println("\n=== TUA GRIGLIA ===");
        printBoard(myBoard, true);

        // griglia avversario solo quando l'utente ha messo le sue 3 navi
        if (numNaviUser == 3) {
            System.out.println("\n=== GRIGLIA AVVERSARIO ===");
            printBoard(enemyBoard, false);
        }
    }

    public static void printBoard(int[][] grid, boolean showShips) {

        System.out.println("   0 1 2 3 4 5 6 7 8 9");

        for (int y = 0; y < 10; y++) {
            System.out.print(y + "  ");

            for (int x = 0; x < 10; x++) {

                int v = grid[y][x];

                switch (v) {
                    case 0:
                        System.out.print("~ ");
                        break; // acqua / ignoto
                    case 1:
                        System.out.print("X ");
                        break; // colpito
                    case 2:
                        System.out.print(showShips ? "O " : " ~");
                        break; // nave visibile solo sulla tua board
                    case 3:
                        System.out.print("# ");
                        break; // affondato
                    case 4:
                        System.out.print("@ ");
                        break; // tua nave colpita
                    case 9:
                        System.out.print("* ");
                        break; // colpo a vuoto
                    default:
                        System.out.print("? ");
                }
            }

            System.out.println();
        }
    }
}
