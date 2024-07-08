package ru.gb.junior.homeworks.homework_5.list;


import ru.gb.junior.homeworks.homework_5.message_type.AbstractRequest;

/**
 * {
 *   "type": "users"
 * }
 */
public class ListRequest extends AbstractRequest {
    public static final String TYPE = "listRequest";

    public ListRequest() {
        setType(TYPE);
    }
}

