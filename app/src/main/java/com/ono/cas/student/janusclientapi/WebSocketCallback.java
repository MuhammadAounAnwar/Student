package com.ono.cas.student.janusclientapi;

public interface WebSocketCallback {
    void onOpen();

    void onMessage(String text);

    void onClosed();
}
