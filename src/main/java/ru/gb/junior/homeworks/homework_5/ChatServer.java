package ru.gb.junior.homeworks.homework_5;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.gb.junior.homeworks.homework_5.list.ListRequest;
import ru.gb.junior.homeworks.homework_5.list.ListResponse;
import ru.gb.junior.homeworks.homework_5.login.LoginRequest;
import ru.gb.junior.homeworks.homework_5.login.LoginResponse;
import ru.gb.junior.homeworks.homework_5.message_type.AbstractRequest;
import ru.gb.junior.homeworks.homework_5.message_type.BroadcastMessageRequest;
import ru.gb.junior.homeworks.homework_5.message_type.DisconnectRequest;
import ru.gb.junior.homeworks.homework_5.message_type.SendMessageRequest;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private final static ObjectMapper objectMapper = new ObjectMapper();
    /**
     * Домашнее задание:
     *
     * 0. Осознать код, который мы написали на уроке.
     * При появлении вопросов, пишем в общий чат в телеграме.
     * 1. По аналогии с командой отправки сообщений, реализовать следующие команды:
     * 1.1 BroadcastMessageRequest - послать сообщение ВСЕМ пользователям (кроме себя)
     * 1.2 UsersRequest - получить список всех логинов, которые в данный момент есть в чате (в любом формате)
     * 1.3 DisconnectRequest - клиент оповещает сервер о том, что он отключился
     * 1.3.1 * Доп. задание: при отключении юзера, делать рассылку на остальных
     *
     * Можно сделать только один пункт из 1.1-1.3.
     */

    public static void main(String[] args) {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
        try (ServerSocket server = new ServerSocket(8888)) {
            System.out.println("Сервер запущен");

            while (true) {
                System.out.println("Ждем клиентского подключения");
                Socket client = server.accept();
                ClientHandler clientHandler = new ClientHandler(client, clients);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Ошибка во время работы сервера: " + e.getMessage());
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket client;
        private final Scanner in;
        private final PrintWriter out;
        private final Map<String, ClientHandler> clients;
        private String clientLogin;

        public ClientHandler(Socket client, Map<String, ClientHandler> clients) throws IOException {
            this.client = client;
            this.clients = clients;
            this.in = new Scanner(client.getInputStream());
            this.out = new PrintWriter(client.getOutputStream(), true);
        }

        @Override
        public void run() {
            System.out.println("Подключен новый клиент");
            try {
                String loginRequest = in.nextLine();
                LoginRequest request = objectMapper.reader().readValue(loginRequest, LoginRequest.class);
                this.clientLogin = request.getLogin();
            } catch (IOException e) {
                System.err.println("Не удалось прочитать сообщение от клиента [" + clientLogin + "]: " + e.getMessage());
                String unsuccessfulResponse = createLoginResponse(false);
                out.println(unsuccessfulResponse);
                doClose();
                return;
            }

            System.out.println("Запрос от клиента: " + clientLogin);
            if (clients.containsKey(clientLogin)) {
                String unsuccessfulResponse = createLoginResponse(false);
                out.println(unsuccessfulResponse);
                doClose();
                return;
            }

            clients.put(clientLogin, this);
            String successfulLoginResponse = createLoginResponse(true);
            out.println(successfulLoginResponse);

            label:
            while (true) {
                String msgFromClient = in.nextLine();

                final String type;
                try {
                    AbstractRequest request = objectMapper.reader().readValue(msgFromClient, AbstractRequest.class);
                    type = request.getType();
                } catch (IOException e) {
                    System.err.println("Не удалось прочитать сообщение от клиента [" + clientLogin + "]: " + e.getMessage());
                    sendMessage("Не удалось прочитать сообщение: " + e.getMessage());
                    continue;
                }

                switch (type) {
                    case SendMessageRequest.TYPE: {
                        final SendMessageRequest request;
                        try {
                            request = objectMapper.reader().readValue(msgFromClient, SendMessageRequest.class);
                        } catch (IOException e) {
                            System.err.println("Не удалось прочитать сообщение от клиента [" + clientLogin + "]: " + e.getMessage());
                            sendMessage("Не удалось прочитать сообщение SendMessageRequest: " + e.getMessage());
                            continue;
                        }

                        ClientHandler clientTo = clients.get(request.getRecipient());
                        if (clientTo == null) {
                            sendMessage("Клиент с логином [" + request.getRecipient() + "] не найден");
                            continue;
                        }
                        clientTo.sendMessage(request.getMessage());
                        break;
                    }
                    case BroadcastMessageRequest.TYPE: {
                        final BroadcastMessageRequest request;
                        try {
                            request = objectMapper.reader().readValue(msgFromClient, BroadcastMessageRequest.class);
                            List<ClientHandler> clientsTo = new ArrayList<>(clients.values());
                            for (ClientHandler client : clientsTo) {
                                if (client.clientLogin != this.clientLogin) {
                                    client.sendMessage(request.getMessage());
                                }
                            }
                        } catch (IOException e) {
                            System.err.println("Не удалось прочитать сообщение от клиента [" + clientLogin + "]: " + e.getMessage());
                            sendMessage("Не удалось прочитать сообщение BroadcastMessageRequest: " + e.getMessage());
                        }

                        break;
                    }
                    case ListRequest.TYPE:
                        final ListResponse response = new ListResponse();
                        try {
                            List<User> users = new ArrayList<>();
                            ClientHandler clientTo = clients.get(clientLogin);

                            for (String login : clients.keySet()) {
                                if (login != clientLogin) {
                                    User user = new User();
                                    user.setLogin(login);
                                    users.add(user);
                                }
                            }

                            response.setUsers(users);
                            String sendMsgRequest = objectMapper.writeValueAsString(response);
                            clientTo.sendMessage(sendMsgRequest);

                        } catch (IOException e) {
                            System.err.println("Не удалось выполнить запрос от клиента [" + clientLogin + "]: " + e.getMessage());
                            sendMessage("Не удалось прочитать сообщение ListRequest: " + e.getMessage());
                        }

                        break;
                    case DisconnectRequest.TYPE:
                        clients.remove(clientLogin);
                        String sendMsgRequest = "Клиент " + clientLogin + " вышел из чата";
                        System.out.println(sendMsgRequest);

                        for (String client : clients.keySet()) {
                            clients.get(client).sendMessage(sendMsgRequest);
                        }
                        doClose();
                        break label;
                    case null:
                    default:
                        System.err.println("Неизвестный тип сообщения: " + type);
                        sendMessage("Неизвестный тип сообщения: " + type);
                        break;
                }
            }
        }

        private void doClose() {
            try {
                in.close();
                out.close();
                client.close();
            } catch (IOException e) {
                System.err.println("Ошибка во время отключения клиента: " + e.getMessage());
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        private String createLoginResponse(boolean success) {
            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setConnected(success);
            try {
                return objectMapper.writer().writeValueAsString(loginResponse);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Не удалось создать loginResponse: " + e.getMessage());
            }
        }
    }
}