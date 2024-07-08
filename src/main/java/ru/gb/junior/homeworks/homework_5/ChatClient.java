package ru.gb.junior.homeworks.homework_5;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.gb.junior.homeworks.homework_5.list.ListRequest;
import ru.gb.junior.homeworks.homework_5.login.LoginRequest;
import ru.gb.junior.homeworks.homework_5.login.LoginResponse;
import ru.gb.junior.homeworks.homework_5.message_type.BroadcastMessageRequest;
import ru.gb.junior.homeworks.homework_5.message_type.DisconnectRequest;
import ru.gb.junior.homeworks.homework_5.message_type.SendMessageRequest;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import static java.lang.System.out;

public class ChatClient {
    static Scanner console = new Scanner(System.in);
    static String clientLogin = console.nextLine();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try (Socket server = new Socket("localhost", 8888)) {
            out.println("Успешно подключились к серверу");

            try (PrintWriter out = new PrintWriter(server.getOutputStream(), true)) {
                Scanner in = new Scanner(server.getInputStream());

                String loginRequest = createLoginRequest(clientLogin);
                out.println(loginRequest);

                String loginResponseString = in.nextLine();
                if (!checkLoginResponse(loginResponseString)) {
                    System.out.println("Не удалось подключиться к серверу");
                    return;
                }

                new Thread(() -> {
                    while (true) {
                        String msgFromServer = in.nextLine();
                        System.out.println("Сообщение от сервера: " + msgFromServer);
                    }
                }).start();


                while (true) {
                    System.out.println("Что хочу сделать?");
                    System.out.println("1. Послать сообщение другу");
                    System.out.println("2. Послать сообщение всем");
                    System.out.println("3. Получить список логинов");
                    System.out.println("4. Покинуть чат");
                    String type = console.nextLine();

                    switch (type) {
                        case "1" -> {
                            SendMessageRequest request = new SendMessageRequest();
                            System.out.println("Введите логин пользователя, которому Вы хотите отправить сообщение: ");
                            request.setRecipient(console.nextLine());
                            System.out.println("Введите текст сообщения:");
                            request.setMessage(console.nextLine());
                            String sendMsgRequest = objectMapper.writeValueAsString(request);
                            out.println(sendMsgRequest);
                        }
                        case "2" -> {
                            BroadcastMessageRequest request = new BroadcastMessageRequest();
                            request.setMessage(console.nextLine());
                            String sendMsgRequest = objectMapper.writeValueAsString(request);
                            out.println(sendMsgRequest);
                        }
                        case "3" -> {
                            ListRequest request = new ListRequest();
                            String sendMsgRequest = objectMapper.writeValueAsString(request);
                            out.println(sendMsgRequest);
                        }
                        case "4" -> {
                            DisconnectRequest request = new DisconnectRequest();
                            request.setRecipient(clientLogin);
                            String sendMsgRequest = objectMapper.writeValueAsString(request);
                            out.println(sendMsgRequest);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка во время подключения к серверу: " + e.getMessage());
        }

        out.println("Отключились от сервера");
    }

    private static String createLoginRequest(String login) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLogin(login);

        try {
            return objectMapper.writeValueAsString(loginRequest);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка JSON: " + e.getMessage());
        }
    }

    private static boolean checkLoginResponse(String loginResponse) {
        try {
            LoginResponse resp = objectMapper.reader().readValue(loginResponse, LoginResponse.class);
            return resp.isConnected();
        } catch (IOException e) {
            System.err.println("Ошибка чтения JSON: " + e.getMessage());
            return false;
        }
    }
}
