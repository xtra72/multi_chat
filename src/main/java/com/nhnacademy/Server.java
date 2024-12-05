package com.nhnacademy;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Server extends Thread {
    int port;
    Map<String, Consumer<String[]>> commandSet = new HashMap<>();

    public Server(int port) {
        this.port = port;

        commandSet.put("show", args -> {
            if (args != null && args.length > 0) {
                if (args[0].equalsIgnoreCase("client")) {
                    for (int i = 0; i < Handler.getCount(); i++) {
                        System.out.println(String.format("%d : %s",
                                i + 1, Handler.getHandler(i).getClientId()));
                    }
                } else {
                    System.out.println(String.format("Unknown command : %s", args[0]));
                }
            }
        });
    }

    @Override
    public void run() {
        Thread console = new Thread(() -> {
            try {
                BufferedReader terminalIn = new BufferedReader(new InputStreamReader(System.in));
                while (!Thread.currentThread().isInterrupted()) {
                    String line = terminalIn.readLine();
                    String[] fields = line.split("\\s");

                    if ((fields.length != 0) && (commandSet.containsKey(fields[0]))) {
                        commandSet.get(fields[0]).accept(Arrays.copyOfRange(fields, 1, fields.length));
                    }
                }
            } catch (IOException ignore) {
            }

        });

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            console.start();

            while (true) {
                Socket socket = serverSocket.accept();

                Handler handler = new Handler(socket);

                handler.start();

            }
        } catch (IOException ignore) {
        } finally {
            console.interrupt();
        }

    }

    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(args[0]);
            Server server = new Server(port);

            server.start();
        } catch (NumberFormatException ingore) {
        }
    }
}
