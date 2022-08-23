package my_project.model;

import KAGO_framework.model.abitur.netz.Server;

public class TiciTaciTociServer extends Server {

    private boolean gameRunning;

    private boolean xTaken;
    private String xIP;
    private int xPort;

    private boolean oTaken;
    private String oIP;
    private int oPort;

    private boolean xTurn;
    private String[][] field;

    public TiciTaciTociServer(int pPort) {
        super(pPort);
        gameRunning = false;
        xTaken = false;
        oTaken = false;
    }

    @Override
    public void processNewConnection(String pClientIP, int pClientPort) {
        send(pClientIP,pClientPort,gameRunning ? "Hey bro, Spiel läuft schon, warte mal ein bissl" : "Hey bro, wenn du joinen willst schick 'join' Mann");
        System.out.println("[INFO] Client connected");
    }

    @Override
    public void processMessage(String pClientIP, int pClientPort, String pMessage) {
        if(pMessage.equals("join")) {
            if(!xTaken) { //Grenzfall = X sagt join, muss gefixt werden
                xTaken = true;
                xIP = pClientIP;
                xPort = pClientPort;
                send(pClientIP, pClientPort, "Du bist jetzt X, brooo");
            } else if(!oTaken) {
                oTaken = true;
                oIP = pClientIP;
                oPort = pClientPort;
                send(pClientIP, pClientPort, "Du bist jetzt O, brooo");
                gameRunning = true;
                sendToAll("Spiel hat gestartet, let's goooo!!!! \nX du bist dran");
            } else {
                send(pClientIP,pClientPort,"Spiel läuft schon bro");
            }
        } else if(xTurn && gameRunning) {
            String[] splitMessage = pMessage.split("_");
            if(splitMessage[0].equals("set")) {
                var message = Integer.parseInt(splitMessage[1]);
                if (message >= 1 && message <= 9) {
                    send(pClientIP, pClientPort, "Boah bist du cool");
                } else {
                    send(pClientIP, pClientPort, "Syntax ist falsch king, brauche eine Zahl von 1 bis 9");
                }
            }
        }
    }

    @Override
    public void processClosingConnection(String pClientIP, int pClientPort) { }
}
