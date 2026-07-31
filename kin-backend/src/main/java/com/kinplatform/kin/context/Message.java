package com.kinplatform.kin.context;

public record Message(String role, String content) {

    public static Message user(String content) {
        return new Message("USER", content);
    }

    public static Message assistant(String content) {
        return new Message("ASSISTANT", content);
    }

    public static Message system(String content) {
        return new Message("SYSTEM", content);
    }
}
