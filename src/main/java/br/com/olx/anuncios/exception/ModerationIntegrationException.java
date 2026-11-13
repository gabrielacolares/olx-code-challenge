package br.com.olx.anuncios.exception;

public class ModerationIntegrationException extends RuntimeException {
    public ModerationIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModerationIntegrationException(String message) {
        super(message);
    }
}
