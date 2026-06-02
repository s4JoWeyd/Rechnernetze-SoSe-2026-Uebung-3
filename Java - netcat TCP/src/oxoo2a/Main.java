package oxoo2a;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    private static final Map<String, PrintWriter> clients = new ConcurrentHashMap<>();

    private static void fatal(String comment) {
        System.out.println(comment);
        System.exit(-1);
    }

    // ************************************************************************
    // MAIN
    // ************************************************************************
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            fatal("Usage: \"netcat -l <port>\" or \"netcat <ip> <port>\"");
        }

        int port = Integer.parseInt(args[1]);

        if (args[0].equalsIgnoreCase("-l")) {
            Server(port);
        } else {
            Client(args[0], port);
        }
    }

    // ************************************************************************
    // Server
    // ************************************************************************
    private static void Server(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("TCP-Chat-Server läuft auf Port " + port);

        while (true) {
            Socket client = serverSocket.accept();
            Thread t = new Thread(() -> serveClient(client));
            t.start();
        }
    }

    private static void serveClient(Socket clientConnection) {
        String clientName = null;

        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(clientConnection.getInputStream())
            );

            PrintWriter writer = new PrintWriter(
                    clientConnection.getOutputStream(),
                    true
            );

            writer.println("Willkommen beim TCP-Chat!");
            writer.println("Bitte registrieren mit:");
            writer.println("register <Name>");

            String line;

            while ((line = reader.readLine()) != null) {
                if (line.equalsIgnoreCase("stop")) {
                    break;
                }

                if (line.startsWith("register ")) {
                    String requestedName = line.substring("register ".length()).trim();

                    if (requestedName.isEmpty()) {
                        writer.println("Fehler: Name darf nicht leer sein.");
                    } else if (clients.containsKey(requestedName)) {
                        writer.println("Fehler: Name ist bereits vergeben.");
                    } else {
                        if (clientName != null) {
                            clients.remove(clientName);
                        }

                        clientName = requestedName;
                        clients.put(clientName, writer);

                        writer.println("Registrierung erfolgreich als " + clientName);
                        sendHelp(writer);
                        sendClientList(writer);

                        System.out.println(clientName + " hat sich registriert.");
                    }
                } else if (line.startsWith("send ")) {
                    if (clientName == null) {
                        writer.println("Fehler: Bitte zuerst registrieren mit register <Name>");
                        continue;
                    }

                    String rest = line.substring("send ".length()).trim();
                    int firstSpace = rest.indexOf(' ');

                    if (firstSpace == -1) {
                        writer.println("Fehler: Benutzung: send <Empfängername> <Nachricht>");
                        continue;
                    }

                    String receiverName = rest.substring(0, firstSpace).trim();
                    String message = rest.substring(firstSpace + 1).trim();

                    if (message.isEmpty()) {
                        writer.println("Fehler: Nachricht darf nicht leer sein.");
                        continue;
                    }

                    PrintWriter receiver = clients.get(receiverName);

                    if (receiver == null) {
                        writer.println("Fehler: Empfänger '" + receiverName + "' ist nicht verbunden.");
                    } else {
                        receiver.println("Nachricht von " + clientName + ": " + message);
                        writer.println("Nachricht an " + receiverName + " gesendet.");
                    }
                } else if (line.equalsIgnoreCase("clients")) {
                    sendClientList(writer);
                } else if (line.equalsIgnoreCase("help")) {
                    sendHelp(writer);
                } else {
                    writer.println("Unbekannter Befehl. Schreibe help für Hilfe.");
                }
            }
        } catch (IOException e) {
            System.out.println("Client-Verbindung wurde unterbrochen.");
        } finally {
            if (clientName != null) {
                clients.remove(clientName);
                System.out.println(clientName + " hat die Verbindung beendet.");
            }

            try {
                clientConnection.close();
            } catch (IOException e) {
                System.out.println("Fehler beim Schließen der Verbindung.");
            }
        }
    }

    private static void sendHelp(PrintWriter writer) {
        writer.println("Verfügbare Befehle:");
        writer.println("register <Name>");
        writer.println("send <Empfängername> <Nachricht>");
        writer.println("clients");
        writer.println("help");
        writer.println("stop");
    }

    private static void sendClientList(PrintWriter writer) {
        writer.println("Aktive Clients:");

        if (clients.isEmpty()) {
            writer.println("-");
        } else {
            for (String name : clients.keySet()) {
                writer.println("- " + name);
            }
        }
    }

    // ************************************************************************
    // Client
    // ************************************************************************
    private static void Client(String serverHost, int serverPort) throws IOException {
        InetAddress serverAddress = InetAddress.getByName(serverHost);
        Socket serverConnect = new Socket(serverAddress, serverPort);

        BufferedReader serverIn = new BufferedReader(
                new InputStreamReader(serverConnect.getInputStream())
        );

        PrintWriter serverOut = new PrintWriter(
                serverConnect.getOutputStream(),
                true
        );

        Thread receiveThread = new Thread(() -> {
            try {
                String message;

                while ((message = serverIn.readLine()) != null) {
                    System.out.println(message);
                }
            } catch (IOException e) {
                System.out.println("Die Verbindung zum Server wurde beendet.");
            }
        });

        receiveThread.start();

        String line;

        do {
            line = readString();

            if (line != null) {
                serverOut.println(line);
            }
        } while (line != null && !line.equalsIgnoreCase("stop"));

        serverConnect.close();
    }

    private static String readString() {
        boolean again = false;
        String input = null;

        do {
            try {
                if (br == null) {
                    br = new BufferedReader(new InputStreamReader(System.in));
                }

                input = br.readLine();
            } catch (Exception e) {
                System.out.printf("Exception: %s\n", e.getMessage());
                again = true;
            }
        } while (again);

        return input;
    }

    private static BufferedReader br = null;
}