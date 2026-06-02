package oxoo2a;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class Main {

    private static final int PACKET_SIZE = 4096;

    public static void main(String[] args) {
        if (args.length != 1) {
            return;
        }

        int localPort;

        try {
            localPort = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Der lokale Port muss eine Zahl sein.");
            return;
        }

        try {
            startChat(localPort);
        } catch (IOException e) {
            System.out.println("Fehler beim Starten des Chats: " + e.getMessage());
        }
    }

    private static void startChat(int localPort) throws IOException {
        DatagramSocket socket = new DatagramSocket(localPort);

        System.out.println("UDP-Chat gestartet auf Port " + localPort);
        System.out.println();
        Thread receiverThread = new Thread(() -> receiveMessages(socket));
        receiverThread.setDaemon(true);
        receiverThread.start();
        readAndSendMessages(socket);

        socket.close();
        System.out.println("UDP-Chat beendet.");
    }

    private static void receiveMessages(DatagramSocket socket) {
        byte[] buffer = new byte[PACKET_SIZE];

        while (!socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(
                        packet.getData(),
                        0,
                        packet.getLength(),
                        StandardCharsets.UTF_8
                );

                String senderIp = packet.getAddress().getHostAddress();
                int senderPort = packet.getPort();

                System.out.println();
                System.out.println("Nachricht von " + senderIp + ":" + senderPort);
                System.out.println(message);
                System.out.print("> ");
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    System.out.println("Fehler beim Empfangen: " + e.getMessage());
                }
            }
        }
    }

    private static void readAndSendMessages(DatagramSocket socket) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        String line;

        System.out.print("> ");

        while ((line = reader.readLine()) != null) {
            if (line.equalsIgnoreCase("stop")) {
                break;
            }

            if (line.startsWith("send ")) {
                sendMessage(socket, line);
            } else {
                System.out.println("Ungültiger Befehl.");
                System.out.println("Verwendung: send <Ziel-IP-Adresse> <Ziel-Port> <Nachricht>");
            }

            System.out.print("> ");
        }
    }

    private static void sendMessage(DatagramSocket socket, String line) {
        String[] parts = line.split(" ", 4);

        if (parts.length < 4) {
            System.out.println("Ungültiger Befehl.");
            System.out.println("Verwendung: send <Ziel-IP-Adresse> <Ziel-Port> <Nachricht>");
            return;
        }

        String targetIp = parts[1];
        String targetPortText = parts[2];
        String message = parts[3];

        try {
            int targetPort = Integer.parseInt(targetPortText);
            InetAddress targetAddress = InetAddress.getByName(targetIp);

            byte[] data = message.getBytes(StandardCharsets.UTF_8);

            DatagramPacket packet = new DatagramPacket(
                    data,
                    data.length,
                    targetAddress,
                    targetPort
            );

            socket.send(packet);

            System.out.println("Gesendet an " + targetIp + ":" + targetPort);
        } catch (NumberFormatException e) {
            System.out.println("Der Ziel-Port muss eine Zahl sein.");
        } catch (IOException e) {
            System.out.println("Fehler beim Senden: " + e.getMessage());
        }
    }
}