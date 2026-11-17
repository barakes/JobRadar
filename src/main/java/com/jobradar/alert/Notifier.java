package com.jobradar.alert;

// Sends a message to a recipient. The alert service doesn't know or care how it is delivered.
public interface Notifier {

    void send(String recipient, String subject, String htmlBody);
}