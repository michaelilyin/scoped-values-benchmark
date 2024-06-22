package test;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

interface ScopeConsumer {
    void consume(Callable<Void> callable) throws Exception;
}

public class MultithreadExperiment {
    static int VALUES = 1;
    static int CONCURRENCY = 1;
    static int ACCESS_FACTOR = 5;
    static long CPU_CONSUME_SLOTS = 1L;

    private static Consumer<String> consumer;
    private static Consumer<Long> cpuConsumer;

    private static ThreadLocal<String>[] threadLocalsManual;
    private static ThreadLocal<String>[] inheritableThreadLocals;
    private static ScopedValue<String>[] scopedValues;

    private static ScopedValue.Carrier preSetCarrier;

    public static void init(int values, int concurrency, int accessFactor, long consumeSlots, Consumer<String> consumer, Consumer<Long> cpuConsumer) {
        MultithreadExperiment.consumer = consumer;

        ACCESS_FACTOR = accessFactor;
        VALUES = values;
        CONCURRENCY = concurrency;
        CPU_CONSUME_SLOTS = consumeSlots;


        threadLocalsManual = new ThreadLocal[VALUES];
        inheritableThreadLocals = new InheritableThreadLocal[VALUES];
        scopedValues = new ScopedValue[VALUES];

        for (int i = 0; i < VALUES; i++) {
            inheritableThreadLocals[i] = new InheritableThreadLocal<>();
            threadLocalsManual[i] = new ThreadLocal<>();
            scopedValues[i] = ScopedValue.newInstance();
        }

        preSetCarrier = ScopedValue.where(scopedValues[0], "value-0");
        for (int i = 1; i < VALUES; i++) {
            preSetCarrier = preSetCarrier.where(scopedValues[i], STR."value-\{i}");
        }
    }

    static void testThreadLocalsManualInheritance() throws InterruptedException {
        for (int i = 0; i < VALUES; i++) {
            threadLocalsManual[i].set(STR."value-\{i}");
        }

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < CONCURRENCY; i++) {
                executor.submit(new DecoratedCallable(() -> {
                    for (int i1 = 0; i1 < VALUES * ACCESS_FACTOR; i1++) {
                        String val = threadLocalsManual[i1 % VALUES].get();
                        consumer.accept(val);
                        if (CPU_CONSUME_SLOTS > 0) {
                            cpuConsumer.accept(CPU_CONSUME_SLOTS);
                        }
                    }
                    return null;
                }));
            }
        }
    }

    static void testThreadLocals() throws InterruptedException {
        for (int i = 0; i < VALUES; i++) {
            inheritableThreadLocals[i].set(STR."value-\{i}");
        }

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < CONCURRENCY; i++) {
                executor.submit(() -> {
                    for (int i1 = 0; i1 < VALUES * ACCESS_FACTOR; i1++) {
                        String val = inheritableThreadLocals[i1 % VALUES].get();
                        consumer.accept(val);
                        if (CPU_CONSUME_SLOTS > 0) {
                            cpuConsumer.accept(CPU_CONSUME_SLOTS);
                        }
                    }
                    return null;
                });
            }
        }
    }

    static void testScopedValues() throws Exception {
        var carrier = ScopedValue.where(scopedValues[0], "value-0");
        for (int i = 1; i < VALUES; i++) {
            carrier = carrier.where(scopedValues[i], STR."value-\{i}");
        }

        carrier.call(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                for (int i = 0; i < CONCURRENCY; i++) {
                    scope.fork(() -> {
                        for (int i1 = 0; i1 < VALUES * ACCESS_FACTOR; i1++) {
                            String val = scopedValues[i1 % VALUES].get();
                            consumer.accept(val);
                            if (CPU_CONSUME_SLOTS > 0) {
                                cpuConsumer.accept(CPU_CONSUME_SLOTS);
                            }
                        }
                        return null;
                    });
                }
                scope.join();
            }
            return null;
        });
    }

    static void testScopedValuesPreSet() throws Exception {
        preSetCarrier.call(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                for (int i = 0; i < CONCURRENCY; i++) {
                    scope.fork(() -> {
                        for (int i1 = 0; i1 < VALUES * ACCESS_FACTOR; i1++) {
                            String val = scopedValues[i1 % VALUES].get();
                            consumer.accept(val);
                            if (CPU_CONSUME_SLOTS > 0) {
                                cpuConsumer.accept(CPU_CONSUME_SLOTS);
                            }
                        }
                        return null;
                    });
                }
                scope.join();
            }
            return null;
        });
    }

    static void testScopedValuesRussianDoll() throws Exception {
        ScopeConsumer russianDollCarrier = r -> ScopedValue.callWhere(scopedValues[0], "value-0", r);
        for (int i = 1; i < VALUES; i++) {
            var ii = i;
            var currentConsumer = russianDollCarrier;
            russianDollCarrier = r -> ScopedValue.callWhere(scopedValues[ii], STR."value-\{ii}", () -> {
                currentConsumer.consume(r);
                return null;
            });
        }
        russianDollCarrier.consume(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                for (int i = 0; i < CONCURRENCY; i++) {
                    scope.fork(() -> {
                        for (int i1 = 0; i1 < VALUES * ACCESS_FACTOR; i1++) {
                            String val = scopedValues[i1 % VALUES].get();
                            consumer.accept(val);
                            if (CPU_CONSUME_SLOTS > 0) {
                                cpuConsumer.accept(CPU_CONSUME_SLOTS);
                            }
                        }
                        return null;
                    });
                }
                scope.join();
            }
            return null;
        });
    }

    static void testScopedValuesMixed() throws Exception {
        ScopeConsumer russianDollCarrier = r -> ScopedValue.where(scopedValues[0], "value-0")
                .where(scopedValues[1], "value-1")
                .call(r);

        for (int i = 2; i < VALUES; i += 2) {
            var ii = i;
            var currentConsumer = russianDollCarrier;
            russianDollCarrier = r -> ScopedValue
                    .where(scopedValues[ii], STR."value-\{ii}")
                    .where(scopedValues[ii + 1], STR."value-\{ii + 1}")
                    .call(() -> {
                        currentConsumer.consume(r);
                        return null;
                    });
        }
        russianDollCarrier.consume(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                for (int i = 0; i < CONCURRENCY; i++) {
                    scope.fork(() -> {
                        for (int i1 = 0; i1 < VALUES * ACCESS_FACTOR; i1++) {
                            String val = scopedValues[i1 % VALUES].get();
                            consumer.accept(val);
                            if (CPU_CONSUME_SLOTS > 0) {
                                cpuConsumer.accept(CPU_CONSUME_SLOTS);
                            }
                        }
                        return null;
                    });
                }
                scope.join();
            }
            return null;
        });
    }

    static class DecoratedCallable implements Callable<Void> {
        private String[] values = new String[MultithreadExperiment.VALUES];
        private final Callable<Void> delegate;

        public DecoratedCallable(Callable<Void> delegate) {
            this.delegate = delegate;
            for (int i = 0; i < MultithreadExperiment.VALUES; i++) {
                values[i] = MultithreadExperiment.threadLocalsManual[i].get();
            }
        }

        @Override
        public Void call() throws Exception {
            for (int i = 0; i < MultithreadExperiment.threadLocalsManual.length; i++) {
                MultithreadExperiment.threadLocalsManual[i].set(values[i]);
            }
            return delegate.call();
        }
    }
}
