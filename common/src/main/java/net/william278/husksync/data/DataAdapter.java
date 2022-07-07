package net.william278.husksync.data;

import org.jetbrains.annotations.NotNull;

/**
 * An adapter that adapts {@link UserData} to and from a portable byte array.
 */
public interface DataAdapter {

    /**
     * Converts {@link UserData} to a byte array
     *
     * @param data The {@link UserData} to adapt
     * @return The byte array.
     * @throws DataAdaptionException If an error occurred during adaptation.
     */
    byte[] toBytes(@NotNull UserData data) throws DataAdaptionException;

    /**
     * Serializes {@link UserData} to a JSON string.
     *
     * @param data   The {@link UserData} to serialize
     * @param pretty Whether to pretty print the JSON.
     * @return The output json string.
     * @throws DataAdaptionException If an error occurred during adaptation.
     */
    @NotNull
    String toJson(@NotNull UserData data, boolean pretty) throws DataAdaptionException;

    /**
     * Converts a byte array to {@link UserData}.
     *
     * @param data The byte array to adapt.
     * @return The {@link UserData}.
     * @throws DataAdaptionException If an error occurred during adaptation, such as if the byte array is invalid.
     */
    @NotNull
    UserData fromBytes(final byte[] data) throws DataAdaptionException;

}
