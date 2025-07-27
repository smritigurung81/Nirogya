package com.example.nirogya.models;

public class ChatMessage {
    public static final int SENDER_USER = 0;
    public static final int SENDER_BOT = 1;

    private String message;
    private int sender;

    // Constructor
    public ChatMessage(String message, int sender) {
        this.message = message;
        this.sender = sender;
    }

    // Getters
    public String getMessage() {
        return message;
    }

    public int getSender() {
        return sender;
    }

    // Helper method to check if user sent the message
    public boolean isUser() {
        return sender == SENDER_USER;
    }

    // Helper method to check if bot sent the message
    public boolean isBot() {
        return sender == SENDER_BOT;
    }
}