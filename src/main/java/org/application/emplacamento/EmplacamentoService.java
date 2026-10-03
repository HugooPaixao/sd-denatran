package org.application.emplacamento;

import org.application.common.*;
import org.application.common.Modelos.*;

import java.time.Year;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.application.common.Topicos.*;

public class EmplacamentoService {

    static final double ALIQUOTA_IPVA = 0.02;

    public static void main(String[] args) {

        Bus bus = Bus.connect("emplacamento");
        Map<String, Veiculo> veiculos = new ConcurrentHashMap<>();
        Map<String, Condutor> condutores = new ConcurrentHashMap<>();
        bus.state(ESTADO_VEICULO + "/+", Veiculo.class, veiculos);
        bus.state(ESTADO_CONDUTOR + "/+", Condutor.class, condutores);
        Object lock = new Object();

        bus.handle(EMPLACAMENTO_EMPLACAR, d -> {
            String placa = Campos.placa(d);
            String modelo = Campos.texto(d, "modelo");
            double valor = Campos.valor(d, "valor");
            String cpf = Campos.cpf(d);
            int ano = d.hasNonNull("ano") ? Campos.ano(d) : Year.now().getValue();

            synchronized (lock) {
                if (!condutores.containsKey(cpf)) {
                    throw new NegocioException("Condutor não cadastrado: " + cpf);
                }

                if (veiculos.containsKey(placa)) {
                    throw new NegocioException("Placa já emplacada: " + placa);
                }

                Veiculo v = new Veiculo(placa, modelo, valor, cpf, ano);
                veiculos.put(placa, v);

                bus.publishState(ESTADO_VEICULO + "/" + placa, v);

                return v;
            }
        });

        bus.handle(EMPLACAMENTO_IPVA, d -> {
            Veiculo v = buscar(veiculos, Campos.placa(d));
            double ipva = Math.round(v.valor() * ALIQUOTA_IPVA * 100.0) / 100.0;

            Map<String, Object> r = new LinkedHashMap<>();
            r.put("placa", v.placa()); r.put("valor", v.valor());
            r.put("aliquota", ALIQUOTA_IPVA); r.put("ipva", ipva);

            return r;
        });

        bus.handle(EMPLACAMENTO_LISTAR_ANO, d -> {
            int ano = Campos.ano(d);

            return veiculos.values().stream().filter(v -> v.ano() == ano)
                    .sorted(Comparator.comparing(Veiculo::placa)).toList();
        });

        bus.handle(EMPLACAMENTO_PROPRIETARIO, d -> {
            String placa = Campos.placa(d);
            String cpf = Campos.cpf(d);

            synchronized (lock) {
                Veiculo v = buscar(veiculos, placa);
                if (!condutores.containsKey(cpf)) {
                    throw new NegocioException("Condutor não cadastrado: " + cpf);
                }

                if (v.cpf().equals(cpf)) {
                    throw new NegocioException("Veículo já pertence a este condutor");
                }

                Veiculo novo = new Veiculo(v.placa(), v.modelo(), v.valor(), cpf, v.ano());
                veiculos.put(placa, novo);
                bus.publishState(ESTADO_VEICULO + "/" + placa, novo);

                return novo;
            }
        });

        System.out.println("[emplacamento] pronto");
    }

    public static Veiculo buscar(Map<String, Veiculo> m, String placa) {
        Veiculo v = m.get(placa);

        if (v == null) {
            throw new NegocioException("Veiculo nao encontrado: " + placa);
        }

        return v;
    }
}