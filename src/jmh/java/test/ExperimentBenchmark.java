package test;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;


@BenchmarkMode({Mode.Throughput})
@Warmup(iterations = 2, time = 30, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, batchSize = 1, time = 120, timeUnit = TimeUnit.SECONDS) // add 120 and more iterations
@Fork(value = 2)

// @BenchmarkMode({Mode.Throughput})
// @Warmup(iterations = 1, time = 1)
// @Measurement(iterations = 1, batchSize = 1, time = 1)
// @Fork(value = 1)

@State(Scope.Benchmark)
@Threads(1)
public class ExperimentBenchmark {

    private static final int REQESTS = 100;


//     @Param({"100"})
//     public int accessFactor;

//    @Param({"1000000"})
//    public int concurrency;

//    @Param({"64"})
//    public int values;

    @Param({"0", "1"})
    public long consumeCpu;

    @Param({"3", "100"})
    public int accessFactor;

    @Param({"1000", "100000", "1000000"})
    public int concurrency;

    @Param({"6", "16", "64"})
    public int values;

    @Setup
    public void setup(Blackhole blackhole) {
        MultithreadExperiment.init(values, concurrency / REQESTS, accessFactor, consumeCpu, blackhole::consume, Blackhole::consumeCPU);
    }

    @Benchmark
    public void threadLocalsManual() throws ExecutionException, InterruptedException {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < REQESTS; i++) {
                executor.submit(() -> {
                    MultithreadExperiment.testThreadLocalsManualInheritance();
                    return null;
                });
            }
        }
    }

    @Benchmark
    public void threadLocals() throws InterruptedException {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < REQESTS; i++) {
                executor.submit(() -> {
                    MultithreadExperiment.testThreadLocals();return null;
                });
            }
        }
    }

   @Benchmark
    public void scopedValues() throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < REQESTS; i++) {
                executor.submit(() -> {
                    MultithreadExperiment.testScopedValues();return null;
                });
            }
        }
    }

//     // @Benchmark
//     public void scopedValuesPreSet() throws Exception {
//         MultithreadExperiment.testScopedValuesPreSet();
//     }

//     // @Benchmark
//     public void scopedValuesRussianDoll() throws Exception {
//         MultithreadExperiment.testScopedValuesRussianDoll();
//     }

    @Benchmark
    public void scopedValuesMixed() throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < REQESTS; i++) {
                executor.submit(() -> {
                    MultithreadExperiment.testScopedValuesMixed();return null;
                });
            }
        }
    }
}
