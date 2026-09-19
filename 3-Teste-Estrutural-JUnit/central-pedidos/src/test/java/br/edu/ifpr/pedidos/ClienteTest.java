package br.edu.ifpr.pedidos;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClienteTest {

    @Test
    void deveCriarClienteComHistoricoZero() {
        Cliente cliente = new Cliente(false, false, 0);

        assertAll(
            () -> assertFalse(cliente.vip()),
            () -> assertFalse(cliente.bloqueado()),
            () -> assertEquals(0, cliente.comprasAnteriores())
        );
    }

    @Test
    void deveManterOsValoresInformados() {
        Cliente cliente = new Cliente(true, true, 7);

        assertAll(
            () -> assertTrue(cliente.vip()),
            () -> assertTrue(cliente.bloqueado()),
            () -> assertEquals(7, cliente.comprasAnteriores())
        );
    }

    @Test
    void deveRejeitarHistoricoNegativo() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
            () -> new Cliente(false, false, -1));
        assertEquals("Histórico inválido", erro.getMessage());
    }
}
