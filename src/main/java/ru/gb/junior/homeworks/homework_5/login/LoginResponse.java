package ru.gb.junior.homeworks.homework_5.login;


/**
 * {
 *   "connected": true
 * }
 */
public class LoginResponse {
    private boolean connected;

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
