package org.application;

import org.application.cadastro.CadastroService;
import org.application.cliente.ClienteConsole;
import org.application.emplacamento.EmplacamentoService;
import org.application.multa.MultaService;

public class Main {
    public static void main(String[] args) throws Exception {

        String modo = args.length > 0 ? args[0] : "";

        switch (modo) {
            case "cadastro" -> CadastroService.main(args);
            case "emplacamento" -> EmplacamentoService.main(args);
            case "multa" -> MultaService.main(args);
            case "console" -> { ClienteConsole.main(args); return; }
            default -> {
                System.err.println("Uso: java -jar app.jar <cadastro|emplacamento|multa|console>");
                System.exit(1);
            }
        }

        Thread.currentThread().join();
    }
}