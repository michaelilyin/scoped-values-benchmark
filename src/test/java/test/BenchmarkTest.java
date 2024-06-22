package test;

import org.junit.jupiter.api.*;
import java.util.concurrent.Executors;

public class BenchmarkTest {

    @BeforeAll
    static void beforeAll() {
        MultithreadExperiment.init(4, 2, 1, 0, System.out::println, l -> {});
    }

    @Test
    void test() {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 10; i++) {
                executor.submit(() -> {
                    MultithreadExperiment.testThreadLocalsManualInheritance();
                    return null;
                });
            }
        }
    }
}
