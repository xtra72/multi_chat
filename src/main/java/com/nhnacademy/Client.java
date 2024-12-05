package com.nhnacademy;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.IOException;
import java.net.Socket;

public class Client extends Thread {
    String host;
    int port;
    Thread receiver;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(host, port)) {
            receiver = new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    while (true) {
                        String line = reader.readLine();

                        System.out.println("< " + line);
                    }
                } catch (IOException e) {
                }
            });

            receiver.start();

            BufferedReader terminalIn = new BufferedReader(new InputStreamReader(System.in));
            PrintStream socketOut = new PrintStream(socket.getOutputStream());

            while (true) {
                String line = terminalIn.readLine();

                socketOut.println(line);
            }

            // try {
            // receiver.join();
            // } catch (InterruptedException e) {
            // Thread.currentThread().interrupt();
            // }
        } catch (IOException ignore) {
        }
    }

    public static void main(String[] args) {
        try {
            if (args.length != 2) {
                throw new IllegalArgumentException();
            }

            String host = args[0];
            int port = Integer.parseInt(args[1]);

            Client client = new Client(host, port);

            client.start();
        } catch (NumberFormatException ignore) {
            System.err.println("포트는 정수입니다.");
        } catch (IllegalArgumentException ignore) {
            System.err.println("인수가 잘못되었습니다.");
        }
    }
}
