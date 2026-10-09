package com.marinogneto.pokemon.domain.localpokemon;

/** Someone else changed the record since the client read it (optimistic locking). Mapped to 409. */
public class VersionConflictException extends RuntimeException {

    public VersionConflictException(Long id, long expectedVersion, Long actualVersion) {
        super("Local Pokemon " + id + " was modified concurrently: the request was based on version "
                + expectedVersion + " but it is now at version " + actualVersion);
    }

    public VersionConflictException(String message) {
        super(message);
    }
}
