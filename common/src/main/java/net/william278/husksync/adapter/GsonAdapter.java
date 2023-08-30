package net.william278.husksync.adapter;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public class GsonAdapter implements DataAdapter {

    private final Gson gson;

    public GsonAdapter() {
        this.gson = Converters.registerOffsetDateTime(new GsonBuilder()).create();
    }

    @Override
    public <A extends Adaptable> byte[] toBytes(@NotNull A data) throws AdaptionException {
        return this.toJson(data).getBytes(StandardCharsets.UTF_8);
    }

    @NotNull
    @Override
    public <A extends Adaptable> String toJson(@NotNull A data) throws AdaptionException {
        try {
            return gson.toJson(data);
        } catch (Throwable e) {
            throw new AdaptionException("Failed to adapt data to JSON via Gson", e);
        }
    }

    @Override
    @NotNull
    public <A extends Adaptable> A fromBytes(@NotNull byte[] data, @NotNull Class<A> type) throws AdaptionException {
        return this.fromJson(new String(data, StandardCharsets.UTF_8), type);
    }

    @Override
    @NotNull
    public <A extends Adaptable> A fromJson(@NotNull String data, @NotNull Class<A> type) throws AdaptionException {
        try {
            return gson.fromJson(data, type);
        } catch (Throwable e) {
            throw new AdaptionException("Failed to adapt data from JSON via Gson", e);
        }
    }

}
