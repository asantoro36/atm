package com.demo.atmcli.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

public final class ConsoleView implements AtmView {

    private final BufferedReader input;
    private final PrintStream output;

    public ConsoleView(BufferedReader input, PrintStream output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public void showMenu() {
        output.println("atm login <tarjeta>");
        output.println("atm extraer <tarjeta> <cuenta> <importe>");
        output.println("atm depositar <tarjeta> <cbu> <importe>");
        output.println("atm saldo <tarjeta> <cuenta>");
        output.println("0. Salir");
    }

    @Override
    public String readCommand() {
        try {
            return input.readLine();
        } catch (IOException exception) {
            return null;
        }
    }

    @Override
    public void showLoginSuccess() {
        output.println("Ingreso exitoso");
    }

    @Override
    public void showLoginFailure() {
        output.println("Ingreso no exitoso");
    }

    @Override
    public void showInvalidCommand() {
        output.println("Comando inválido");
    }

    @Override
    public void showOperationUnavailable() {
        output.println("Operación no disponible");
    }
}
