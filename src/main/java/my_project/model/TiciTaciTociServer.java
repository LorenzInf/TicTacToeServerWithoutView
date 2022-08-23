package my_project.model;

import KAGO_framework.model.abitur.netz.Server;
import my_project.control.ProgramController;

import java.util.Objects;

public class TiciTaciTociServer extends Server {

    private static String ticTacToe = ProgramController.ANSI_CYAN + "[TicTacToe] " + ProgramController.ANSI_RESET;

    private boolean gameRunning;

    private boolean xTaken;
    private String xIP;
    private int xPort;

    private boolean oTaken;
    private String oIP;
    private int oPort;

    private boolean xTurn;
    private Integer[] playingField = new Integer[9];

    public TiciTaciTociServer(int pPort) {
        super(pPort);
        gameRunning = false;
        xTaken = false;
        xTurn = true;
    }

    @Override
    public void processNewConnection(String pClientIP, int pClientPort) {
        send(pClientIP,pClientPort,gameRunning ?
                ticTacToe + ProgramController.ANSI_GREEN + "Welcome, a game is currently in progress." + ProgramController.ANSI_RESET :
                ticTacToe + ProgramController.ANSI_GREEN + "Welcome. If you want to join the game, send 'join'" + ProgramController.ANSI_RESET);

        System.out.println("[INFO] Client with IP " + pClientIP + " and port " + pClientPort + " connected");
    }

    @Override
    public void processMessage(String pClientIP, int pClientPort, String pMessage) {

        // WENN DAS SPIEL NOCH NICHT LÄUFT -----------------------------------------------------------------------------
        if(pMessage.equals("join") && !gameRunning) {

            //Wenn es noch keinen Spieler X gibt
            if (!xTaken) {
                xTaken = true;
                xIP = pClientIP;
                xPort = pClientPort;
                send(pClientIP, pClientPort, ticTacToe + "You have joined as X, please wait for another player.");
                System.out.println("[INFO] A player joined as X");

                //Wenn es X schon gibt, O aber nicht
            } else {
                if (!Objects.equals(xIP, pClientIP) || xPort != pClientPort) {
                    oIP = pClientIP;
                    oPort = pClientPort;
                    send(pClientIP, pClientPort, ticTacToe + "You have joined as O.");
                    gameRunning = true;
                    System.out.println("[INFO] A player joined as O. Game starting");
                    sendToAll(ticTacToe + "The game has started.\n" + ticTacToe + "Waiting for X to take their turn. (set <1-9>)");

                    //Wenn X noch mal versucht zu joinen
                } else {
                    send(pClientIP, pClientPort, ticTacToe + "You've already joined as X.");
                    System.out.println("xport: " + xPort + "\nxIP: " + xIP + "\npPort: " + pClientPort + "\npIP: " + pClientIP);
                }
            }

            //WENN DAS SPIEL SCHON LÄUFT -------------------------------------------------------------------------------
            //Wenn ein dritter Spieler versucht zu joinen, während das Spiel läuft
        } else if(pMessage.equals("join") && gameRunning) {
            send(pClientIP, pClientPort, ticTacToe + "You can't join right now, a game is already in progress.");

        } else if (pMessage.equals("giveUp")){ //Jemand ruft give up auf
            if(!gameRunning) { //Das Spiel läuft nicht
                send(pClientIP,pClientPort,ticTacToe + "You can't already be giving up, the game hasn't even started!");
            } else { //Das Spiel läuft
                if(Objects.equals(pClientIP,xIP) && pClientPort == xPort) { //X gibt auf
                    send(xIP,xPort,ticTacToe + "You give up, you lose. No cookies for you.");
                    send(oIP,oPort,ticTacToe + "X just gave up... that means you win!");
                } else if (Objects.equals(pClientIP,oIP) && pClientPort == oPort) { //O gibt auf
                    send(oIP,oPort,ticTacToe + "You give up, you lose. No cookies for you.");
                    send(xIP,xPort,ticTacToe + "O just gave up... that means you win!");
                }

                resetGame();
            }

        } else if(gameRunning) {
            //Wenn X eine Nachricht schickt
            if(Objects.equals(pClientIP,xIP) && pClientPort == xPort) {
                //Wenn X auch an der Reihe ist
                if(xTurn)
                    takeTurn(pClientIP,pClientPort,pMessage);
                //Wenn X aber nicht an der Reihe ist
                else
                    send(pClientIP,pClientPort,ticTacToe + "It's not your turn.");
                //Wenn O eine Nachricht schickt
            } else if (Objects.equals(pClientIP,oIP) && pClientPort == oPort) {
                //Wenn O auch an der Reihe ist
                if(!xTurn)
                    takeTurn(pClientIP,pClientPort,pMessage);
                //Wenn O aber nicht an der Reihe ist
                else
                    send(pClientIP,pClientPort,ticTacToe + "It's not your turn.");
            }
        }
    }

    private void takeTurn(String pClientIP, int pClientPort, String pMessage) {
        String[] splitMessage = pMessage.split(" ");
        if(splitMessage[0].equals("set")) {
            try {
                int mInt = Integer.parseInt(splitMessage[1]);
                if (mInt >= 1 && mInt <= 9) {
                    //Command der gesendet wurde, entspricht der syntax
                    if(playingField[mInt - 1] == null) { //Das Feld das angegeben wurde, ist leer
                        playingField[mInt - 1] = xTurn ? 0 : 1;
                        if(!checkWin()) {
                            xTurn = !xTurn;
                            sendToAll(ticTacToe + (!xTurn ? "X" : "O") + " took their turn. It's your turn, " + (xTurn ? "X." : "O."));
                            sendPlayingField();
                        }
                    } else { //In dem Feld ist schon was
                        send(pClientIP,pClientPort,ticTacToe + "That spot is taken, please choose an empty one");
                    }

                } else { //set_Zahl ist da, Zahl ist aber über 9 oder unter 1
                    send(pClientIP,pClientPort,ticTacToe + "Incorrect arguments. The syntax is 'set <1/2/3/4/5/6/7/8/9>'.");
                }
            } catch (NumberFormatException e) { //set_ ist da, aber nach dem set ist keine Zahl
                send(pClientIP,pClientPort,ticTacToe + "Incorrect arguments. The syntax is 'set <1/2/3/4/5/6/7/8/9>'.");
            }
        } else { //Komplett verkackt, nicht mal ein set_ (oder giveUp)
            send(pClientIP,pClientPort,ticTacToe + "Unknown command. Possible commands are 'set <1/2/3/4/5/6/7/8/9>' or 'giveUp'.");
        }
    }

    private void sendPlayingField() {
        //Spielfeld wird ausgegeben
        StringBuilder playingFieldString = new StringBuilder();
        playingFieldString.append(ticTacToe)
                .append(ProgramController.ANSI_GREEN).append("The current playing field:\n").append(ProgramController.ANSI_RESET);

        for(int i = 1; i <= 9; i++) {
            if(playingField[i-1] == null) {
                playingFieldString.append(" - ");
            } else if(playingField[i-1] == 0) {
                playingFieldString.append(" X ");
            } else if(playingField[i-1] == 1) {
                playingFieldString.append(" O ");
            } else {
                System.out.println(ProgramController.ANSI_RED + "Whoops, something went horribly wrong");
                sendToAll(ProgramController.ANSI_RED + "Whoops, something went horribly wrong");
            }

            if(i % 3 == 0) {
                playingFieldString.append("\n");
            } else {
                playingFieldString.append("|");
            }
        }
        sendToAll(playingFieldString.toString());
    }

    private void resetGame() {
        for(int i = 0; i < 9; i++) {
            playingField[i] = null; //Das Spielfeld zurücksetzen
        }
        //Spieler zurücksetzen
        xIP = null;
        xPort = 0;
        oIP = null;
        oPort = 0;
        xTaken = false;

        gameRunning = false;

        sendToAll(ticTacToe + ProgramController.ANSI_YELLOW + "The game has concluded. If you wish to join the next one, send 'join'." + ProgramController.ANSI_RESET);

        System.out.println("[INFO] A game concluded");
    }

    private boolean checkWin() {
        //Die Zeilen
        if (checkWinHelp(false,0)) return true;
        if (checkWinHelp(false,3)) return true;
        if (checkWinHelp(false,6)) return true;

        //Die Spalten
        if (checkWinHelp(true,0)) return true;
        if (checkWinHelp(true,1)) return true;
        if (checkWinHelp(true,2)) return true;

        //Die Diagonalen
        if(playingField[0] != null && Objects.equals(playingField[0], playingField[4]) && Objects.equals(playingField[0], playingField[8])) {
            if(playingField[0] == 0) {
                endGame("X");
            } else {
                endGame("O");
            }
            return true;
        }
        if(playingField[2] != null && Objects.equals(playingField[2], playingField[4]) && Objects.equals(playingField[2], playingField[6])) {
            if(playingField[2] == 0) {
                endGame("X");
            } else {
                endGame("O");
            }
            return true;
        }

        //Unentschieden?
        int counter = 0;
        for(int i = 0; i < 9; i++) {
            if(playingField[i] != null)
                counter++;
        }
        if(counter == 9) {
            sendPlayingField();
            sendToAll(ticTacToe + "It's a draw!");
            resetGame();
            return true;
        }

        return false;
    }

    private boolean checkWinHelp(boolean row, int n) {
        if(playingField[n] != null && Objects.equals(playingField[n], playingField[n + (row ? 3 : 1)]) && Objects.equals(playingField[n],playingField[n + (row ? 6 : 2)])) {
            if(playingField[n] == 0) {
                endGame("X");
            } else {
                endGame("O");
            }
            return true;
        }
        return false;
    }

    private void endGame(String winner) {
        sendPlayingField();
        sendToAll(ticTacToe + ProgramController.ANSI_GREEN + winner + " won the game!!!" + ProgramController.ANSI_RESET);
        resetGame();
    }

    @Override
    public void processClosingConnection(String pClientIP, int pClientPort) {
        System.out.println("[INFO] Client with IP " + pClientIP + " and port " + pClientPort + " disconnected");

        //One of the players disconnected
        if(Objects.equals(pClientIP,xIP) && xPort == pClientPort || Objects.equals(pClientIP,oIP) && oPort == pClientPort) {
            sendToAll(ticTacToe + ProgramController.ANSI_RED + ((Objects.equals(pClientIP,xIP) && pClientPort == xPort) ? "X" : "O" ) + " disconnected prematurely, the game will be aborted.");
            resetGame();
        }
    }
}
