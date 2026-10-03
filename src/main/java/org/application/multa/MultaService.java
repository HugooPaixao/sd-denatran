package org.application.multa;

import org.application.common.*;
import org.application.common.Modelos.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import static org.application.common.Topicos.*;

public class MultaService {

    public record Detalhada(Multa multa, Condutor condutor) { }

    public record Ranking(String cpf, String nome, int pontos) { }

    public static void main(String[] args) {

        Bus bus = Bus.connect("multa");
        Map<String, Veiculo> veiculos = new ConcurrentHashMap<>();
        Map<String, Condutor> condutores = new ConcurrentHashMap<>();
        List<Multa> multas = new CopyOnWriteArrayList<>();
        bus.state(ESTADO_VEICULO + "/+", Veiculo.class, veiculos);
        bus.state(ESTADO_CONDUTOR + "/+", Condutor.class, condutores);

        /*
        * Tudo bem parecido
        *
        * */
        bus.handle(MULTA_LANCAR, d -> {

            int ano = Campos.ano(d);
            String desc = Campos.texto(d, "descricao");
            int pontos = Campos.inteiro(d, "pontuacao");

            if (pontos <= 0) {
                throw new NegocioException("Pontuaçao deve ser positiva");
            }


            String placa = Campos.placa(d);
            Veiculo v = veiculos.get(placa);

            if (v == null) {
                throw new NegocioException("Veiculo não encontrado: " + placa);
            }

            Multa m = new Multa(ano, desc, pontos, placa, v.cpf());
            multas.add(m);

            return detalhar(m, condutores);
        });


        bus.handle(MULTA_VEICULO, d -> {

            String placa = Campos.placa(d);
            Predicate<Multa> filtro = m -> m.placa().equals(placa);

            if (d.hasNonNull("ano")) {
                int ano = Campos.ano(d);
                filtro = filtro.and(m -> m.ano() == ano);
            }

            return listar(multas, filtro, condutores);
        });

        bus.handle(MULTA_CONDUTOR, d -> {

            String cpf = Campos.cpf(d);
            int ano = Campos.ano(d);

            return listar(multas, m -> m.cpf().equals(cpf) && m.ano() == ano, condutores);
        });

        bus.handle(MULTA_ANO, d -> {
            int ano = Campos.ano(d);
            return listar(multas, m -> m.ano() == ano, condutores);
        });

        bus.handle(MULTA_TOP5, d -> {

            Map<String, Integer> soma = new HashMap<>();
            multas.forEach(m -> soma.merge(m.cpf(), m.pontuacao(), Integer::sum));

            return soma.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .map(e -> {

                        Condutor c = condutores.get(e.getKey());

                        return new Ranking(e.getKey(), c == null ? null : c.nome(), e.getValue());
                    }).toList();
        });

        System.out.println("Multa pronto");
    }

    public static Detalhada detalhar(Multa m, Map<String, Condutor> condutores) {
        return new Detalhada(m, condutores.get(m.cpf()));
    }

    public static List<Detalhada> listar(List<Multa> multas, Predicate<Multa> f, Map<String, Condutor> condutores) {
        return multas.stream().filter(f).map(m -> detalhar(m, condutores)).toList();
    }


}