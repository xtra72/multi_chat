package com.nhnacademy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

public class Handler extends Thread {
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
                        if (!line.isEmpty()) {
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
                            if (fields.length < 3) {
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

                                    if (fields.length == 3) {
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

                                        for (int i = 0; i < getCount(); i++) {
                                            Handler target = getHandler(i);

                                            if (target != this) {
                                                target.sendMessage(getClientId(), messages.toArray(new String[0]));
                                            }
                                        }
                                    } else {
                                        try {
                                            Handler target = getHandler(fields[3]);
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