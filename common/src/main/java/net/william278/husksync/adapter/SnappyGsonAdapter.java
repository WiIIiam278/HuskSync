package net.william278.husksync.adapter;

import org.jetbrains.annotations.NotNull;
import org.xerial.snappy.Snappy;

import java.io.IOException;

public class SnappyGsonAdapter extends GsonAdapter {

    @NotNull
    @Override
    public <A extends Adaptable> byte[] toBytes(@NotNull A data) throws AdaptionException {
        try {
            return Snappy.compress(super.toBytes(data));
        } catch (IOException e) {
            throw new AdaptionException("Failed to compress data through Snappy", e);
        }
    }

    @NotNull
    @Override
    public <A extends Adaptable> A fromBytes(@NotNull byte[] data, @NotNull Class<A> type) throws AdaptionException {
        try {
            return super.fromBytes(Snappy.uncompress(data), type);
        } catch (IOException e) {
            throw new AdaptionException("Failed to decompress data through Snappy", e);
        }
    }

}
