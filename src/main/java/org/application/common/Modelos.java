package org.application.common;

public final class Modelos {

    private Modelos() { }

    public record Condutor(String cpf, String nome) { }

    public record Veiculo(String placa, String modelo, double valor, String cpf, int ano) { }

    public record Multa(int ano, String descricao, int pontuacao, String placa, String cpf) { }

}