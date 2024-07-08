package ru.gb.junior.homeworks.homework_5.login;


/**
 * {
 *   "type": "login",
 *   "login": "nagibator"
 * }
 */
public class LoginRequest {
    private String login;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }
}
