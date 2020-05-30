import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.ServerBinding;
import client.Client;
import server.HttpServer;
import server.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static List<ActorRef> clientsList = new ArrayList<>();
    final static ActorSystem system = ActorSystem.create("local_system");
    private final static ActorRef server = system.actorOf(Props.create(Server.class), "server");
    private final static int clientsCount = 3;
    private static final HttpServer httpServer = new HttpServer(system);
    private static Thread httpServerThread;

    public static void main(String[] args) {
        createClients();
        startHttpServer();
        System.out.println("Started app");
        parseInput();
        // finish
        unbindServer();
        system.terminate();
    }

    private static void createClients() {
        for (int i = 0; i < clientsCount; i++) {
            ActorRef client = system.actorOf(Props.create(Client.class, i, server), "client" + i);
            clientsList.add(client);
        }
    }

    static void startHttpServer() {
        httpServerThread = new Thread(httpServer);
        httpServerThread.start();
    }

    private static void parseInput() {
        System.out.println(String.format("Type client NUMBER [0-%d] and product NAME to compare its price or 'q' to QUIT", clientsCount - 1));

        // read line & send to server
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String[] args;
            String line;
            try {
                line = br.readLine();
                if ("q".equals(line)) {
                    break;
                }
                args = line.split(" ");

                int clientIndex = Integer.parseInt(args[0]);
                if (args.length == 2 && clientIndex < clientsList.size()) {
                    String productName = args[1];
                    clientsList.get(clientIndex).tell(productName, null);
                } else {
                    System.out.println("Incorrect input format. Try [CLIENT NUMBER] [PRODUCT NAME] format.");
                }
            } catch (IOException | NumberFormatException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private static void unbindServer() {
        try {
            httpServerThread.join();
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        }
        httpServer.getBinding()
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done
        System.out.println("Unbinded HTTP server. Closing app...");
    }
}