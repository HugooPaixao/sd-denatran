package org.application.common;

import com.fasterxml.jackson.databind.JsonNode;

public final class Campos {

    private Campos() { }

    public static String texto(JsonNode dados, String campo) {
        String valor = dados.path(campo).asText("").trim();

        if (valor.isEmpty())
            throw new NegocioException("Campo obrigatorio: " + campo);

        return valor;
    }

    public static String cpf(JsonNode dados) {
        return texto(dados, "cpf");
    }

    public static String placa(JsonNode dados) {
        return texto(dados, "placa").toUpperCase();
    }

    public static int inteiro(JsonNode dados, String campo) {

        if (!dados.path(campo).isNumber())
            throw new NegocioException("Campo numerico obrigatorio: " + campo);

        return dados.path(campo).asInt();
    }

    public static int ano(JsonNode dados) {
        return inteiro(dados, "ano");
    }

    public static double valor(JsonNode dados, String campo) {
        if (!dados.path(campo).isNumber())
            throw new NegocioException("Campo numerico obrigatorio: " + campo);

        return dados.path(campo).asDouble();
    }
}