package org.application.cliente;

import com.fasterxml.jackson.databind.JsonNode;
import org.application.common.Bus;
import org.application.common.NegocioException;

import java.time.Duration;
import java.util.Map;
import java.util.Scanner;

import static org.application.common.Topicos.*;

public class ClienteConsole {
    private static final Scanner entrada = new Scanner(System.in);
    private static Bus bus;

    public static void main(String[] args) {
        bus = Bus.connect("console");
        boolean executando = true;

        while (executando) {

            exibirMenu();
            String opcao = ler("Opção");

            try {
                switch (opcao) {
                    case "1" -> cadastrarCondutor();
                    case "2" -> emplacarVeiculo();
                    case "3" -> calcularIpva();
                    case "4" -> transferirProprietario();
                    case "5" -> lancarMulta();
                    case "6" -> veiculosEmplacadosNoAno();
                    case "7" -> multasDoVeiculo();
                    case "8" -> multasDoCondutor();
                    case "9" -> multasDoAno();
                    case "10" -> top5Condutores();
                    case "0" -> executando = false;
                    default -> System.out.println("Opção inválida.");
                }
            } catch (NegocioException e) {
                System.out.println("Erro: " + e.getMessage());
            } catch (IllegalStateException e) {
                System.out.println("Serviço nao respondeu: " + e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println("Erro: numero invalido.");
            }
        }

        bus.close();
        System.exit(0);
    }



    private static void exibirMenu() {
        System.out.println("""
                ----- DENATRAN -------
                 1 - Cadastrar condutor
                 2 - Emplacar veículo
                 3 - Calcular IPVA
                 4 - Transferir proprietário
                 5 - Lançar multa
                 6 - Veículos emplacados em um ano
                 7 - Multas de um veículo em um ano
                 8 - Multas de um condutor em um ano
                 9 - Multas lançadas em um ano
                10 - Top 5 condutores com mais pontos
                 0 - Sair
                """);
    }

    private static void cadastrarCondutor() {
        String cpf = ler("CPF");
        String nome = ler("Nome");
        exibir(enviar(CADASTRO_CONDUTOR, Map.of("cpf", cpf, "nome", nome)));
    }

    private static void emplacarVeiculo() {
        String placa = ler("Placa");
        String modelo = ler("Modelo");
        double valor = Double.parseDouble(ler("Valor").replace(",", "."));
        String cpf = ler("CPF do condutor");

        exibir(enviar(EMPLACAMENTO_EMPLACAR, Map.of("placa", placa, "modelo",
                                                    modelo, "valor", valor, "cpf", cpf)));
    }

    private static void calcularIpva() {
        String placa = ler("Placa");
        exibir(enviar(EMPLACAMENTO_IPVA, Map.of("placa", placa)));
    }


    private static void transferirProprietario() {
        String placa = ler("Placa do veículo");
        String cpf = ler("CPF do novo dono");

        exibir(enviar(CADASTRO_TRANSFERIR, Map.of("placa", placa, "cpf", cpf)));
    }

    private static void lancarMulta() {
        int ano = Integer.parseInt(ler("Ano"));
        String descricao = ler("Descrição");
        int pontuacao = Integer.parseInt(ler("Pontuação"));
        String placa = ler("Placa do veículo");

        exibir(enviar(MULTA_LANCAR, Map.of("ano", ano, "descricao", descricao,
                                        "pontuacao", pontuacao, "placa", placa)));
    }

    private static void veiculosEmplacadosNoAno() {
        int ano = Integer.parseInt(ler("Ano"));

        exibir(enviar(EMPLACAMENTO_LISTAR_ANO, Map.of("ano", ano)));
    }

    private static void multasDoVeiculo() {
        String placa = ler("Placa do veículo");
        int ano = Integer.parseInt(ler("Ano"));

        exibir(enviar(MULTA_VEICULO, Map.of("placa", placa, "ano", ano)));
    }

    private static void multasDoCondutor() {
        String cpf = ler("CPF do condutor");
        int ano = Integer.parseInt(ler("Ano"));

        exibir(enviar(MULTA_CONDUTOR, Map.of("cpf", cpf, "ano", ano)));
    }

    private static void multasDoAno() {
        int ano = Integer.parseInt(ler("Ano"));

        exibir(enviar(MULTA_ANO, Map.of("ano", ano)));
    }

    private static void top5Condutores() {
        exibir(enviar(MULTA_TOP5, Map.of()));
    }

    private static String ler(String rotulo) {
        System.out.print(rotulo + ": ");

        return entrada.nextLine()
                .trim();
    }

    private static JsonNode enviar(String topico, Object dados) {
        return bus.request(topico, dados, Duration.ofSeconds(5));
    }

    private static void exibir(JsonNode resposta) {
        try {
            System.out.println(Bus.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(resposta));
        } catch (Exception e) {
            System.out.println(resposta);
        }





    }

}