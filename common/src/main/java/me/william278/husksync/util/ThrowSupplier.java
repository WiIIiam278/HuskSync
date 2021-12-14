package me.william278.husksync.util;

public interface ThrowSupplier<T> {
    T get() throws Exception;

    static <A> A get(ThrowSupplier<A> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
