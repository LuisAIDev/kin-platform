package com.kinplatform.kin.enterprise.web;

import com.kinplatform.kin.enterprise.progress.EnterpriseProgressEvent;
import com.kinplatform.kin.enterprise.progress.EnterpriseProgressSink;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Servicio de progreso Enterprise vía Server Sent Events (Fase 10,
 * Milestone 2J).
 *
 * <p>Implementa el puerto {@link EnterpriseProgressSink} y distribuye cada
 * {@link EnterpriseProgressEvent} a todos los clientes suscritos de una misma
 * versión {@code (projectId, version)}: múltiples clientes por versión,
 * thread-safe (mapas concurrentes y listas de copia al escribir) y sin bloquear
 * al publicador (los envíos fallidos de un cliente desconectado no afectan a
 * los demás).</p>
 *
 * <p>Cada suscripción registra un {@link SseEmitter}, programa un heartbeat
 * periódico (comentario SSE) para mantener la conexión viva y se limpia
 * automáticamente al completarse, agotar el tiempo o fallar la conexión. Los
 * estados terminales ({@code COMPLETED}/{@code FAILED}) cierran los emisores
 * para que el cliente detenga la reconexión.</p>
 */
public final class EnterpriseProgressService implements EnterpriseProgressSink {

    private static final long DEFAULT_HEARTBEAT_MILLIS = 15_000L;
    private static final String PROGRESS_EVENT_NAME = "progress";

    private final Map<ProgressKey, List<Subscription>> subscriptions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor;
    private final long heartbeatIntervalMillis;

    /**
     * Servicio con ejecutor de heartbeat en segundo plano (daemon) e intervalo
     * de 15 segundos.
     */
    public EnterpriseProgressService() {
        this(defaultHeartbeatExecutor(), DEFAULT_HEARTBEAT_MILLIS);
    }

    /**
     * @param heartbeatExecutor      ejecutor de las tareas de heartbeat
     * @param heartbeatIntervalMillis intervalo del heartbeat en milisegundos
     */
    public EnterpriseProgressService(ScheduledExecutorService heartbeatExecutor,
                                     long heartbeatIntervalMillis) {
        if (heartbeatExecutor == null) {
            throw new IllegalArgumentException("heartbeatExecutor no puede ser null");
        }
        if (heartbeatIntervalMillis <= 0) {
            throw new IllegalArgumentException("heartbeatIntervalMillis debe ser positivo");
        }
        this.heartbeatExecutor = heartbeatExecutor;
        this.heartbeatIntervalMillis = heartbeatIntervalMillis;
    }

    /**
     * Suscribe un nuevo cliente al flujo SSE de una versión.
     *
     * @param projectId identificador del proyecto de KIN origen
     * @param version   versión del proyecto empresarial
     * @return emisor SSE asociado al cliente
     * @throws IllegalArgumentException si {@code projectId} es {@code null} o la
     *                                  versión es inválida
     */
    public SseEmitter subscribe(UUID projectId, int version) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId no puede ser null");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version debe ser mayor o igual a 1");
        }
        ProgressKey key = new ProgressKey(projectId, version);
        SseEmitter emitter = new SseEmitter();
        emitter.onCompletion(() -> unsubscribe(key, emitter));
        emitter.onTimeout(() -> unsubscribe(key, emitter));
        emitter.onError(error -> unsubscribe(key, emitter));

        ScheduledFuture<?> heartbeat = heartbeatExecutor.scheduleAtFixedRate(
            () -> sendHeartbeat(emitter),
            heartbeatIntervalMillis, heartbeatIntervalMillis, TimeUnit.MILLISECONDS);

        subscriptions.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>())
            .add(new Subscription(emitter, heartbeat));
        return emitter;
    }

    /**
     * Publica un evento de progreso a todos los suscriptores de la versión.
     *
     * @param event evento de progreso (obligatorio)
     * @throws IllegalArgumentException si {@code event} es {@code null}
     */
    @Override
    public void publish(EnterpriseProgressEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event no puede ser null");
        }
        ProgressKey key = new ProgressKey(event.projectId(), event.version());
        List<Subscription> subscribers = subscriptions.get(key);
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }
        boolean terminal = event.state().isTerminal();
        for (Subscription subscription : subscribers) {
            try {
                subscription.emitter().send(SseEmitter.event()
                    .name(PROGRESS_EVENT_NAME)
                    .data(event));
                if (terminal) {
                    subscription.emitter().complete();
                }
            } catch (IOException | IllegalStateException ex) {
                subscription.cancel();
                unsubscribe(key, subscription.emitter());
            }
        }
        if (terminal) {
            subscriptions.remove(key);
        }
    }

    /**
     * Número total de suscripciones activas (para observabilidad y pruebas).
     */
    public int activeSubscriptionCount() {
        return subscriptions.values().stream().mapToInt(List::size).sum();
    }

    private void unsubscribe(ProgressKey key, SseEmitter emitter) {
        List<Subscription> subscribers = subscriptions.get(key);
        if (subscribers == null) {
            return;
        }
        subscribers.removeIf(subscription -> subscription.emitter() == emitter);
        if (subscribers.isEmpty()) {
            subscriptions.remove(key);
        }
    }

    private void sendHeartbeat(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().comment("heartbeat"));
        } catch (IOException | IllegalStateException ex) {
            // Emisor cerrado: la limpieza la realiza el callback de completado.
        }
    }

    private static ScheduledExecutorService defaultHeartbeatExecutor() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "enterprise-progress-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
    }

    private record ProgressKey(UUID projectId, int version) {
    }

    private record Subscription(SseEmitter emitter, ScheduledFuture<?> heartbeat) {

        void cancel() {
            heartbeat.cancel(true);
        }
    }
}
