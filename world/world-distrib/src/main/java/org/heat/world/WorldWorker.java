package org.heat.world;

import lombok.extern.slf4j.Slf4j;
import org.fungsi.Either;
import org.fungsi.Unit;
import org.fungsi.concurrent.*;
import org.fungsi.function.UnsafeRunnable;
import org.fungsi.function.UnsafeSupplier;

import java.util.concurrent.Executor;

@Slf4j
public final class WorldWorker implements Worker {
    private final Executor executor;

    public WorldWorker(Executor executor) {
        this.executor = executor;
    }

    @Override
    public <T> Future<T> execute(UnsafeSupplier<Future<T>> fn) {
        Promise<T> promise = Promises.create();
        executor.execute(() -> {
            Future<T> future = Futures.flatten(fn.safelyGet());
            future.onFailure(err -> log.error("uncaught exception", err));
            future.pipeTo(promise);
        });
        return promise;
    }

    @Override
    public <T> Future<T> submit(UnsafeSupplier<T> fn) {
        Promise<T> promise = Promises.create();
        executor.execute(() -> {
            Either<T, Throwable> result = fn.safelyGet();
            result.ifRight(err -> log.error("uncaught exception", err));
            promise.set(result);
        });
        return promise;
    }

    @Override
    public Future<Unit> cast(UnsafeRunnable fn) {
        Promise<Unit> promise = Promises.create();
        executor.execute(() -> {
            Either<Unit, Throwable> result = fn.safelyRun();
            result.ifRight(err -> log.error("uncaught exception", err));
            promise.set(result);
        });
        return promise;
    }
}
