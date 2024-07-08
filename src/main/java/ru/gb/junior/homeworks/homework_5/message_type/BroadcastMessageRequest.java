package ru.gb.junior.homeworks.homework_5.message_type;

public class BroadcastMessageRequest extends AbstractRequest {

    public static final String TYPE = "broadcastMessage";

    private String message;

    public BroadcastMessageRequest() {
        setType(TYPE);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
