package org.heat.world;

import lombok.extern.slf4j.Slf4j;
import org.fungsi.Either;
import org.fungsi.Throwables;
import org.fungsi.Unit;
import org.fungsi.concurrent.*;
import org.fungsi.function.UnsafeRunnable;
import org.fungsi.function.UnsafeSupplier;

import java.time.Duration;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public final class WorldWorker implements Worker {
    private final Executor executor;

    public WorldWorker(Executor executor) {
        this.executor = executor;
    }

    @Override
    public <T> Future<T> execute(UnsafeSupplier<Future<T>> fn) {
        Promise<T> promise = new PromiseImpl<>();
        executor.execute(() -> {
            Future<T> future = Futures.flatten(fn.safelyGet());
            future.onFailure(err -> log.error("uncaught exception", err));
            future.respond(promise::set);
        });
        return promise;
    }

    @Override
    public <T> Future<T> submit(UnsafeSupplier<T> fn) {
        Promise<T> promise = new PromiseImpl<>();
        executor.execute(() -> {
            Either<T, Throwable> result = fn.safelyGet();
            result.ifRight(err -> log.error("uncaught exception", err));
            promise.set(result);
        });
        return promise;
    }

    @Override
    public Future<Unit> cast(UnsafeRunnable fn) {
        Promise<Unit> promise = new PromiseImpl<>();
        executor.execute(() -> {
            Either<Unit, Throwable> result = fn.safelyRun();
            result.ifRight(err -> log.error("uncaught exception", err));
            promise.set(result);
        });
        return promise;
    }

    final static class PromiseImpl<T> implements Promise<T> {
        private volatile Either<T, Throwable> result;
        private final Deque<Consumer<Either<T, Throwable>>> responders = new LinkedList<>();

        private final CountDownLatch resultLatch = new CountDownLatch(1);
        private final StampedLock lock = new StampedLock();

        @Override
        public Optional<Either<T, Throwable>> poll() {
            Either<T, Throwable> result;

            long stamp = lock.tryOptimisticRead();
            result = this.result;
            if (!lock.validate(stamp)) {
                stamp = lock.readLock();
                try {
                    result = this.result;
                } finally {
                    lock.unlockRead(stamp);
                }
            }

            return Optional.ofNullable(result);
        }

        @Override
        public T get() {
            try {
                resultLatch.await();
                return Either.unsafe(result);
            } catch (InterruptedException e) {
                throw Throwables.propagate(e);
            }
        }

        @Override
        public T get(Duration timeout) {
            try {
                if (!resultLatch.await(timeout.toNanos(), TimeUnit.NANOSECONDS)) {
                    throw new TimeoutException(timeout.toString());
                }
                return Either.unsafe(result);
            } catch (InterruptedException e) {
                throw Throwables.propagate(e);
            }
        }

        @Override
        public void set(Either<T, Throwable> newResult) {
            Either<T, Throwable> result;
            boolean optimistic = true;
            long stamp = lock.tryOptimisticRead();
            try {
                result = this.result;
                if (!lock.validate(stamp)) {
                    optimistic = false;
                    stamp = lock.readLock();
                    result = this.result;
                }

                if (result != null) {
                    return;
                }

                optimistic = false;
                stamp = lock.tryConvertToWriteLock(stamp);
                if (stamp == 0L) {
                    stamp = lock.writeLock();
                }

                this.result = newResult;
                this.resultLatch.countDown();

                this.responders.forEach(fn -> fn.accept(newResult));
                this.responders.clear();
            } finally {
                if (!optimistic) {
                    lock.unlock(stamp);
                }
            }
        }

        @Override
        public void respond(Consumer<Either<T, Throwable>> fn) {
            Either<T, Throwable> result;
            boolean optimistic = true;
            long stamp = lock.tryOptimisticRead();
            try {
                result = this.result;
                if (!lock.validate(stamp)) {
                    optimistic = false;
                    stamp = lock.readLock();
                    result = this.result;
                }

                if (result != null) {
                    fn.accept(result);
                } else {
                    optimistic = false;
                    stamp = lock.tryConvertToWriteLock(stamp);
                    if (stamp == 0L) {
                        stamp = lock.writeLock();
                    }

                    responders.addLast(fn);
                }
            } finally {
                if (!optimistic) {
                    lock.unlock(stamp);
                }
            }
        }

        @Override
        public <TT> Future<TT> transform(Function<Either<T, Throwable>, Future<TT>> fn) {
            return new TransformedFuture<>(this, fn);
        }

        @Override
        public String toString() {
            return "PromiseImpl(" + poll() + ")";
        }
    }

    final static class TransformedFuture<T, R> implements Future<R> {
        private volatile Future<R> result;
        private final Deque<Consumer<Either<R, Throwable>>> responders = new LinkedList<>();

        private final CountDownLatch resultSyn = new CountDownLatch(1);
        private final StampedLock lock = new StampedLock();

        TransformedFuture(Future<T> parent, Function<Either<T, Throwable>, Future<R>> fn) {
            parent.respond(e -> {
                long stamp = lock.writeLock();
                try {
                    result = fn.apply(e);
                    resultSyn.countDown();

                    responders.forEach(result::respond);
                    responders.clear();
                } finally {
                    lock.unlockWrite(stamp);
                }
            });
        }

        @Override
        public Optional<Either<R, Throwable>> poll() {
            Future<R> result;
            long stamp = lock.tryOptimisticRead();
            try {
                result = this.result;
                if (!lock.validate(stamp)) {
                    stamp = lock.readLock();
                    result = this.result;
                }

                return result != null
                    ? result.poll()
                    : Optional.empty();
            } finally {
                lock.unlock(stamp);
            }
        }

        @Override
        public R get() {
            try {
                resultSyn.await();
                return result.get();
            } catch (InterruptedException e) {
                throw Throwables.propagate(e);
            }
        }

        @Override
        public R get(Duration timeout) {
            try {
                if (!resultSyn.await(timeout.toNanos(), TimeUnit.NANOSECONDS)) {
                    throw new TimeoutException(timeout.toString());
                }
                return result.get(timeout);
            } catch (InterruptedException e) {
                throw Throwables.propagate(e);
            }
        }

        @Override
        public void respond(Consumer<Either<R, Throwable>> fn) {
            Future<R> result;
            boolean optimistic = true;
            long stamp = lock.tryOptimisticRead();
            try {
                result = this.result;
                if (!lock.validate(stamp)) {
                    optimistic = false;
                    stamp = lock.readLock();
                    result = this.result;
                }

                if (result != null) {
                    result.respond(fn);
                } else {
                    optimistic = false;
                    stamp = lock.tryConvertToWriteLock(stamp);
                    if (stamp == 0L) {
                        stamp = lock.writeLock();
                    }

                    responders.addLast(fn);
                }
            } finally {
                if (!optimistic) {
                    lock.unlock(stamp);
                }
            }
        }

        @Override
        public <TT> Future<TT> transform(Function<Either<R, Throwable>, Future<TT>> fn) {
            return new TransformedFuture<>(this, fn);
        }
    }
}
