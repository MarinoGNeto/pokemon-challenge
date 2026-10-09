package com.marinogneto.pokemon.application.port.out;

/** The reference catalogue could not be reached or answered with an error. Mapped to 502/503 by the web layer. */
public class CatalogUnavailableException extends RuntimeException {

    public CatalogUnavailableException(String message) {
        super(message);
    }

    public CatalogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
