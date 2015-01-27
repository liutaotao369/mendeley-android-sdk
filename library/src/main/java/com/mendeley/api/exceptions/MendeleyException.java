package com.mendeley.api.exceptions;

import java.util.Date;

/**
 * Base class for all the exceptions that are generated by the Mendeley API and should be
 * caught by the application.
 */
public class MendeleyException extends Exception {

    final Date timeStamp;

    public MendeleyException(String message) {
        super(message);
        timeStamp = new Date(System.currentTimeMillis());
    }

    public MendeleyException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
        timeStamp = new Date(System.currentTimeMillis());
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " (" + timeStamp.toString() + ") ";
    }
}