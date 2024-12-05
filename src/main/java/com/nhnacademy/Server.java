package com.nhnacademy;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

class Handler extends Thread {
    static List<Handler> handlerList = new LinkedList<>();
    public static final int DEFAULT_INTERVAL = 100;
    public static char COMMAND_REQUEST = '#';

    int interval = DEFAULT_INTERVAL;
    Socket socket;
    PrintStream socketOut;

    String clientId = "";
    Queue<String> receiveQueue = new LinkedList<>();

    public Handler(Socket socket) {
        this.socket = socket;
    }

    public String getClientId() {
        return clientId;
    }

    @Override
    public void run() {
        handlerList.add(this);
        try {
            socketOut = new PrintStream(socket.getOutputStream());

            Thread receiver = new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    String line = reader.readLine();
                    receiveQueue.add(line);

                } catch (IOException ignore) {
                }
            });

            receiver.start();

            while (!Thread.currentThread().isInterrupted()) {
                if (!receiveQueue.isEmpty()) {
                    String line = receiveQueue.peek();

                    if (!line.isEmpty() && (line.charAt(0) == COMMAND_REQUEST)) {
                        line = receiveQueue.poll();

                        String[] fields = line.split(" ");
                        if (fields.length != 4) {
                            System.out.println("Invalid command");
                        } else {
                            if (fields[2].equals("connect")) {
                                try {
                                    getHandler(fields[3]);
                                    // 중복 등록!!
                                    response(fields[1], "deny", fields[3], null);
                                    break;
                                } catch (NoSuchElementException ignore) {
                                    clientId = fields[3];
                                    response(fields[1], "ok", fields[3], null);
                                }
                            }
                        }

                    }
                } else {
                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (!socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
        handlerList.remove(this);
    }

    public void send(String[] messages) {
        if (messages != null) {
            for (String message : messages) {
                socketOut.println(message);
            }
        }
    }

    public void response(String mid, String result, String clientId, String[] messages) {
        socketOut.println(String.format("@ %s %s %s", mid, result, clientId));
        if (messages != null) {
            for (String message : messages) {
                socketOut.println(message);
            }
        }
    }

    static int getCount() {
        return handlerList.size();
    }

    static Handler getHandler(int index) {
        return handlerList.get(index);
    }

    static Handler getHandler(String clientId) {
        for (Handler handler : handlerList) {
            if (handler.getClientId().equals(clientId)) {
                return handler;
            }
        }

        throw new NoSuchElementException();
    }

    static String[] getClientIdArray() {
        String[] clientIds = new String[handlerList.size()];

        for (int i = 0; i < handlerList.size(); i++) {
            clientIds[i] = handlerList.get(i).getClientId();
        }

        return clientIds;
    }
}

public class Server extends Thread {
    int port;

    public Server(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();

                Handler handler = new Handler(socket);

                handler.start();

                BufferedReader terminalIn = new BufferedReader(new InputStreamReader(System.in));
                while (true) {
                    String line = terminalIn.readLine();
                    String[] fields = line.split("\\s");

                    if (fields.length > 0) {
                        if (fields[0].equals("show")) {
                            if (fields.length > 1) {
                                if (fields[1].equals("client")) {
                                    for (int i = 0; i < Handler.getCount(); i++) {
                                        System.out.println(String.format("%d : %s",
                                                i + 1, Handler.getHandler(i).getClientId()));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException ignore) {
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
