package org.application.cadastro;

import org.application.common.*;
import org.application.common.Modelos.Condutor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.application.common.Topicos.*;

public class CadastroService {

    public static void main(String[] args) {
        Bus bus = Bus.connect("cadastro");
        Map<String, Condutor> condutores = new ConcurrentHashMap<>();
        bus.state(ESTADO_CONDUTOR + "/+", Condutor.class, condutores);
        Object lock = new Object();

        bus.handle(CADASTRO_CONDUTOR, d -> {
            String cpf = Campos.cpf(d);
            String nome = Campos.texto(d, "nome");

            synchronized (lock) {

                if (condutores.containsKey(cpf)) {
                    throw new NegocioException("Condutor ja cadastrado: " + cpf);
                }

                Condutor c = new Condutor(cpf, nome);
                condutores.put(cpf, c);
                bus.publishState(ESTADO_CONDUTOR + "/" + cpf, c);

                return c;
            }
        });

        bus.handle(CADASTRO_TRANSFERIR, dados -> {
            String placa = Campos.placa(dados);
            String cpf = Campos.cpf(dados);

            if (!condutores.containsKey(cpf)) {
                throw new NegocioException("Novo dono não cadastrado: " + cpf);
            }

            return bus.request(EMPLACAMENTO_PROPRIETARIO, Map.of("placa", placa, "cpf", cpf),
                                                                    Duration.ofSeconds(5));
        });

        System.out.println("Cadastro pronto");
    }
}