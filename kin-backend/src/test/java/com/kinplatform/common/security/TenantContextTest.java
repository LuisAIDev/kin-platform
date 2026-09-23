package com.kinplatform.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void setAndGet_works() {
        UUID orgId = UUID.randomUUID();
        TenantContext.set(orgId);
        assertEquals(orgId, TenantContext.get());
    }

    @Test
    void clear_resetsToNull() {
        TenantContext.set(UUID.randomUUID());
        TenantContext.clear();
        assertNull(TenantContext.get());
    }

    @Test
    void isSet_returnsTrueWhenSet() {
        assertFalse(TenantContext.isSet());
        TenantContext.set(UUID.randomUUID());
        assertTrue(TenantContext.isSet());
    }

    @Test
    void getOrDefault_returnsDefaultWhenNotSet() {
        UUID defaultId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        assertEquals(defaultId, TenantContext.getOrDefault());
    }

    @Test
    void getOrDefault_returnsSetValueWhenSet() {
        UUID orgId = UUID.randomUUID();
        TenantContext.set(orgId);
        assertEquals(orgId, TenantContext.getOrDefault());
    }

    @Test
    void threadLocalIsolation_twoThreadsDifferentValues() throws InterruptedException {
        UUID org1 = UUID.randomUUID();
        UUID org2 = UUID.randomUUID();
        CountDownLatch latch = new CountDownLatch(2);
        AtomicReference<UUID> thread1Value = new AtomicReference<>();
        AtomicReference<UUID> thread2Value = new AtomicReference<>();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            try {
                TenantContext.set(org1);
                Thread.sleep(50); // Ensure both threads run concurrently
                thread1Value.set(TenantContext.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                TenantContext.set(org2);
                Thread.sleep(50);
                thread2Value.set(TenantContext.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(org1, thread1Value.get(), "Thread 1 should see its own orgId");
        assertEquals(org2, thread2Value.get(), "Thread 2 should see its own orgId");
        assertNotEquals(thread1Value.get(), thread2Value.get(), "Thread values should be isolated");
    }
}