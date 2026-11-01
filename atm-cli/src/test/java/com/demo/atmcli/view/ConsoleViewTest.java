package com.demo.atmcli.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

class ConsoleViewTest {

    private static final String LINE_BREAK = System.lineSeparator();

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    private ConsoleView viewReading(String input) throws IOException {
        return new ConsoleView(
                new BufferedReader(new StringReader(input)),
                new PrintStream(output, true, "UTF-8"));
    }

    private String printedText() throws IOException {
        return output.toString("UTF-8");
    }

    @Test
    void showMenu_whenCalled_thenPrintCommandsAndExitOption() throws IOException {
        ConsoleView view = viewReading("");

        view.showMenu();

        assertEquals(
                "atm login <tarjeta>" + LINE_BREAK
                        + "atm extraer <tarjeta> <cuenta> <importe>" + LINE_BREAK
                        + "atm depositar <tarjeta> <cbu> <importe>" + LINE_BREAK
                        + "atm saldo <tarjeta> <cuenta>" + LINE_BREAK
                        + "0. Salir" + LINE_BREAK,
                printedText());
    }

    @Test
    void readCommand_whenLineIsAvailable_thenReturnLine() throws IOException {
        ConsoleView view = viewReading("atm login 1111111111111111\n");

        String command = view.readCommand();

        assertEquals("atm login 1111111111111111", command);
    }

    @Test
    void readCommand_whenInputIsAtEnd_thenReturnNull() throws IOException {
        ConsoleView view = viewReading("");

        String command = view.readCommand();

        assertNull(command);
    }

    @Test
    void readCommand_whenInputFails_thenReturnNull() throws IOException {
        BufferedReader closedInput = new BufferedReader(new StringReader("0\n"));
        closedInput.close();
        ConsoleView view = new ConsoleView(closedInput, new PrintStream(output, true, "UTF-8"));

        String command = view.readCommand();

        assertNull(command);
    }

    @Test
    void showLoginSuccess_whenCalled_thenPrintSuccessMessage() throws IOException {
        ConsoleView view = viewReading("");

        view.showLoginSuccess();

        assertEquals("Ingreso exitoso" + LINE_BREAK, printedText());
    }

    @Test
    void showLoginFailure_whenCalled_thenPrintFailureMessage() throws IOException {
        ConsoleView view = viewReading("");

        view.showLoginFailure();

        assertEquals("Ingreso no exitoso" + LINE_BREAK, printedText());
    }

    @Test
    void showInvalidCommand_whenCalled_thenPrintInvalidCommandMessage() throws IOException {
        ConsoleView view = viewReading("");

        view.showInvalidCommand();

        assertEquals("Comando inválido" + LINE_BREAK, printedText());
    }

    @Test
    void showOperationUnavailable_whenCalled_thenPrintUnavailableMessage() throws IOException {
        ConsoleView view = viewReading("");

        view.showOperationUnavailable();

        assertEquals("Operación no disponible" + LINE_BREAK, printedText());
    }
}
