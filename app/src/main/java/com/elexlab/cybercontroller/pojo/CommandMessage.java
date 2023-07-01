package com.elexlab.cybercontroller.pojo;

public class CommandMessage {
    public static interface CommandType{
        // trans用到了hms的包，这里用不了，全部删除
//        int TRANS = 1;
        int LOCK = 2;
        int INPUT = 3;
        int KEY_EVENT = 4;

    }
    private int command;
    private String message;

    public CommandMessage() {
    }

    public CommandMessage(int command, String message) {
        this.command = command;
        this.message = message;
    }

    public int getCommand() {
        return command;
    }

    public void setCommand(int command) {
        this.command = command;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
