package com.nhnacademy;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.function.Consumer;

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

    public void sendMessage(String from, String[] messages) {
        socketOut.println(String.format("@ 1 message %s", from));
        for (String message : messages) {
            socketOut.println(message);
        }
    }

    @Override
    public void run() {
        handlerList.add(this);
        try {
            Queue<String> tempQueue = new LinkedList<>();
            socketOut = new PrintStream(socket.getOutputStream());

            Thread receiver = new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    while (!Thread.currentThread().isInterrupted()) {
                        String line = reader.readLine();
                        if (line.length() != 0) {
                            tempQueue.add(line);
                        } else {
                            synchronized (receiveQueue) {
                                while (!tempQueue.isEmpty()) {
                                    receiveQueue.add(tempQueue.poll());
                                }
                            }
                        }
                    }

                } catch (IOException ignore) {
                }
            });

            receiver.start();

            while (!Thread.currentThread().isInterrupted()) {
                synchronized (receiveQueue) {
                    if (!receiveQueue.isEmpty()) {
                        String line = receiveQueue.peek();

                        if (!line.isEmpty() && (line.charAt(0) == COMMAND_REQUEST)) {
                            line = receiveQueue.poll();
                            System.out.println("Received : " + line);
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
                                } else if (fields[2].equals("message")) {
                                    System.out.println("Command : message - " + receiveQueue.size());
                                    // # <mid> message <target_client_id>
                                    // hello, world!
                                    //
                                    try {
                                        Handler target = getHandler(fields[3]);
                                        // 대상 클라이언트가 존재합니다!
                                        // try {
                                        // System.out.println("Command : message - 타겟을 찾았습니다.");
                                        // // Thread.sleep(5000);
                                        // } catch (InterruptedException e) {
                                        // // TODO Auto-generated catch block
                                        // e.printStackTrace();
                                        // }
                                        System.out.println("Command : message - " + receiveQueue.size());
                                        List<String> messages = new LinkedList<>();
                                        while (!receiveQueue.isEmpty()) {
                                            String message = receiveQueue.poll();

                                            if (!message.isEmpty()) {
                                                messages.add(message);
                                            } else {
                                                break;
                                            }
                                        }

                                        target.sendMessage(getClientId(), messages.toArray(new String[0]));

                                    } catch (NoSuchElementException ignore) {
                                        System.out.println("Command : message - 타겟을 찾을 수 없습니다");
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
    Map<String, Consumer<String[]>> commandSet = new HashMap<>();

    public Server(int port) {
        this.port = port;

        commandSet.put("show", args -> {
            if (args.length > 1) {
                if (args[0].equals("client")) {
                    for (int i = 0; i < Handler.getCount(); i++) {
                        System.out.println(String.format("%d : %s",
                                i + 1, Handler.getHandler(i).getClientId()));
                    }
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
