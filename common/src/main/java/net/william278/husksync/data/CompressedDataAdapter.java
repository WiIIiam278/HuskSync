package net.william278.husksync.data;

import org.jetbrains.annotations.NotNull;
import org.xerial.snappy.Snappy;

import java.io.IOException;

public class CompressedDataAdapter extends JsonDataAdapter {

    @Override
    public byte[] toBytes(@NotNull UserData data) throws DataAdaptionException {
        try {
            return Snappy.compress(super.toBytes(data));
        } catch (IOException e) {
            throw new DataAdaptionException("Failed to compress data", e);
        }
    }

    @Override
    public @NotNull UserData fromBytes(byte[] data) throws DataAdaptionException {
        try {
            return super.fromBytes(Snappy.uncompress(data));
        } catch (IOException e) {
            throw new DataAdaptionException("Failed to decompress data", e);
        }
    }
}
