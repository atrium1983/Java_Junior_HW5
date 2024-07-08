package ru.gb.junior.homeworks.homework_5.message_type;

public class DisconnectRequest extends AbstractRequest {
    public static final String TYPE = "disconnectRequest";

    private String recipient;

    public DisconnectRequest() {
        setType(TYPE);
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }
}
