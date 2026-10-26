package com.timestored.misc;

import lombok.Data;

@Data
class ChatMessage {
    public enum Role { SYSTEM, USER, ASSISTANT }

    private final Role role;
    private final String content;

    public static ChatMessage system(String content) {
        return new ChatMessage(Role.SYSTEM, content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(Role.USER, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(Role.ASSISTANT, content);
    }
}
