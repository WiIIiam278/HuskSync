package net.william278.husksync.data;

/**
 * Indicates an error occurred during {@link UserData} adaptation to and from (compressed) json.
 */
public class DataAdaptionException extends RuntimeException {
    protected DataAdaptionException(String message, Throwable cause) {
        super(message, cause);
    }

}
