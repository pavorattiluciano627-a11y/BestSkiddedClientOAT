package dev.echo.client.config;

import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Serializes profile I/O on the original named daemon thread.
 *
 * <p>Storage belongs to {@link LocalProfileService}. Nested calls from the I/O
 * thread execute directly, avoiding submission to and waiting on the same worker.
 */
public final class ProfileIoExecutor implements AutoCloseable {
    private final LocalProfileService profiles;
    private final ExecutorService executor;
    private volatile Thread ioThread;

    /**
     * Creates the executor without performing storage I/O.
     *
     * @param profiles the root-owned local profile service
     */
    public ProfileIoExecutor(LocalProfileService profiles) {
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        executor = Executors.newSingleThreadExecutor(this::createThread);
    }

    private Thread createThread(Runnable task) {
        Thread thread = Thread.ofPlatform().name("Echo-Config-IO").daemon(true).unstarted(task);
        ioThread = thread;
        return thread;
    }

    /**
     * Queries local protocol availability on the I/O thread.
     *
     * @return whether the local protocol is available
     */
    public boolean isAvailable() {
        try {
            return Boolean.TRUE.equals(call(profiles::isAvailable));
        } catch (UnsatisfiedLinkError error) {
            return false;
        }
    }

    /**
     * Lists stored metadata through the shared storage service.
     *
     * @return the unchanged response, or null after interruption/execution failure
     */
    public JsonObject listProfiles() {
        return call(profiles::listProfiles);
    }

    /**
     * Retrieves a profile through the shared storage service.
     *
     * @param id the profile identifier, or empty for the active profile
     * @return the unchanged response, or null after interruption/execution failure
     */
    public JsonObject getProfile(String id) {
        return call(() -> profiles.getProfile(id));
    }

    /**
     * Saves a profile through the shared storage service.
     *
     * @param request the six-field save request
     * @return the unchanged response, or null after interruption/execution failure
     */
    public JsonObject saveProfile(ProfileStore.SaveRequest request) {
        return call(() -> profiles.saveProfile(request));
    }

    /**
     * Selects a profile through the shared storage service.
     *
     * @param id the profile identifier, or empty to clear selection
     * @return the unchanged response, or null after interruption/execution failure
     */
    public JsonObject selectProfile(String id) {
        return call(() -> profiles.selectProfile(id));
    }

    /**
     * Deletes a profile through the shared storage service.
     *
     * @param id the profile identifier
     * @return the unchanged response, or null after interruption/execution failure
     */
    public JsonObject deleteProfile(String id) {
        return call(() -> profiles.deleteProfile(id));
    }

    /**
     * Schedules an asynchronous read on the same serial queue.
     *
     * @return the pending profile-list response
     */
    public CompletableFuture<JsonObject> listProfilesAsync() {
        return CompletableFuture.supplyAsync(this::listProfiles, executor);
    }

    /**
     * Queues an operation after its caller has captured the required client state.
     *
     * @param operation the operation to run
     */
    public void execute(Runnable operation) {
        executor.execute(operation);
    }

    /**
     * Queues a value-producing configuration operation on the same I/O worker.
     *
     * @param operation the configuration operation
     * @param <T> the result type
     * @return the asynchronous result
     */
    public <T> CompletableFuture<T> submit(Supplier<T> operation) {
        return CompletableFuture.supplyAsync(operation, executor);
    }

    private <T> T call(Supplier<T> operation) {
        if (Thread.currentThread() == ioThread) {
            return operation.get();
        }
        try {
            return executor.submit(operation::get).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof UnsatisfiedLinkError error) {
                throw error;
            }
            exception.printStackTrace();
            return null;
        }
    }

    /** Shuts down the queue and waits at most two seconds for pending operations. */
    @Override
    public void close() {
        executor.shutdown();
        try {
            executor.awaitTermination(2L, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
