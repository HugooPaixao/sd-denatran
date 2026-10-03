package org.application.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Function;

public final class Bus implements MqttCallbackExtended, AutoCloseable {

    public static final ObjectMapper JSON = new ObjectMapper();

    private final String id = UUID.randomUUID().toString().substring(0, 8);
    private final MqttClient client;
    private final Map<String, IMqttMessageListener> subs = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();
    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

    private Bus(String nome) throws MqttException {

        String broker = System.getenv().getOrDefault("MQTT_BROKER", "tcp://localhost:1883");
        client = new MqttClient(broker, nome + "-" + id, new MemoryPersistence());
        client.setCallback(this);
    }

    public static Bus connect(String nome) {

        try {
            Bus bus = new Bus(nome);
            MqttConnectOptions o = new MqttConnectOptions();
            o.setAutomaticReconnect(true);
            o.setCleanSession(true);

            for (int i = 1; ; i++) {
                try {
                    bus.client.connect(o);
                    break;
                }
                catch (MqttException e) {

                    if (i >= 30)
                        throw e;

                    System.out.println("[" + nome + "] aguardando broker MQTT... (" + i + ")");
                    Thread.sleep(2000);
                }
            }


            bus.subscribe("denatran/resp/" + bus.id + "/+", (t, m) -> {
                CompletableFuture<JsonNode> f = bus.pending.get(t);

                if (f != null) {
                    try {
                        f.complete(JSON.readTree(m.getPayload()));
                    }
                    catch (Exception e) { f.completeExceptionally(e); }
                }
            });

            System.out.println("[" + nome + "] conectado ao broker");

            return bus;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao conectar no MQTT", e);
        }
    }

    public void subscribe(String filtro, IMqttMessageListener l) {
        subs.put(filtro, l);

        try {
            client.subscribe(filtro, 1, l);
        }
        catch (MqttException e) {
            throw new IllegalStateException(e);
        }
    }

    public void publish(String topico, Object payload, boolean retained) {
        try {
            client.publish(topico, JSON.writeValueAsBytes(payload), 1, retained);
        }

        catch (Exception e) { throw new IllegalStateException(e); }
    }

    public void publishState(String topico, Object estado) { publish(topico, estado, true); }

    public <T> void state(String filtro, Class<T> tipo, Map<String, T> destino) {

        subscribe(filtro, (topic, m) -> {
            String chave = topic.substring(topic.lastIndexOf('/') + 1);
            try {
                destino.put(chave, JSON.readValue(m.getPayload(), tipo));
            }
            catch (Exception e) {
                System.err.println("Estado inválido em " + topic + ": " + e);
            }
        });
    }

    public void handle(String topico, Function<JsonNode, Object> fn) {

        subscribe(topico, (t, m) -> pool.submit(() -> {

            String replyTo = null;
            Map<String, Object> resp = new LinkedHashMap<>();

            try {
                JsonNode env = JSON.readTree(m.getPayload());
                replyTo = env.path("replyTo").asText(null);
                Object r = fn.apply(env.path("dados"));
                resp.put("ok", true);
                resp.put("dados", r);

            } catch (NegocioException e) {
                resp.put("ok", false); resp.put("erro", e.getMessage());

            } catch (Exception e) {
                e.printStackTrace();
                resp.put("ok", false); resp.put("erro", "Erro interno: " + e);
            }

            if (replyTo != null) publish(replyTo, resp, false);
        }));
    }

    public JsonNode request(String topico, Object dados, Duration timeout) {

        String replyTo = "denatran/resp/" + id + "/" + UUID.randomUUID();
        CompletableFuture<JsonNode> f = new CompletableFuture<>();
        pending.put(replyTo, f);

        try {
            Map<String, Object> env = new LinkedHashMap<>();
            env.put("replyTo", replyTo);
            env.put("dados", dados);
            publish(topico, env, false);
            JsonNode r = f.get(timeout.toMillis(), TimeUnit.MILLISECONDS);

            if (!r.path("ok").asBoolean()) throw new NegocioException(r.path("erro").asText("erro desconhecido"));

            return r.path("dados");

        } catch (TimeoutException e) {
            throw new IllegalStateException("Timeout aguardando resposta de " + topico);

        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);

        } finally {
            pending.remove(replyTo);
        }
    }

    @Override
    public void connectComplete(boolean reconnect, String uri) {

        if (!reconnect) return;
        pool.submit(() -> subs.forEach((f, l) -> {

            try {
                client.subscribe(f, 1, l);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }));
    }

    @Override
    public void connectionLost(Throwable c) {
        System.err.println("Conexão MQTT perdida: " + c);
    }

    @Override
    public void messageArrived(String t, MqttMessage m) { }

    @Override
    public void deliveryComplete(IMqttDeliveryToken t) {}

    @Override public void close() {
        try {
            client.disconnect(); client.close();
        } catch (MqttException e) {
            System.out.println(e.getMessage());
        }

        pool.shutdown();
    }
}