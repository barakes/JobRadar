package com.jobradar.source;

// Thrown when an external job source cannot be read, for example because it is down or returned bad data.
public class SourceFetchException extends RuntimeException {

    public SourceFetchException(String message) {
        super(message);
    }

    public SourceFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}