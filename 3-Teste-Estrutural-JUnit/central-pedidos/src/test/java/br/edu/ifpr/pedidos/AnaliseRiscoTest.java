package br.edu.ifpr.pedidos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class AnaliseRiscoTest {

    private final AnaliseRisco risco = new AnaliseRisco();

    @Test
    void deveRejeitarTotalNegativo() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
            () -> risco.avaliar(new Cliente(false, false, 1), -1, false));
        assertEquals("Total negativo", erro.getMessage());
    }

    @Test
    void deveRejeitarTotalNegativoAntesDeVerificarBloqueio() {
        assertThrows(IllegalArgumentException.class,
            () -> risco.avaliar(new Cliente(false, true, 1), -1, false));
    }

    @Test
    void clienteBloqueadoEhRecusado() {
        assertEquals("RECUSADO", risco.avaliar(new Cliente(false, true, 3), 1_000, false));
    }

    @Test
    void clienteBloqueadoEhRecusadoMesmoSendoVipENovo() {
        assertEquals("RECUSADO", risco.avaliar(new Cliente(true, true, 0), 999_999, true));
    }

    @ParameterizedTest(name = "novo, total {0}, expresso {1} -> {2}")
    @CsvSource({
        "100000, false, APROVADO",
        "100001, false, REVISAO",
        "1000,   true,  REVISAO",
        "0,      false, APROVADO"
    })
    void clienteNovo(long total, boolean expresso, String esperado) {
        assertEquals(esperado, risco.avaliar(new Cliente(false, false, 0), total, expresso));
    }

    @Test
    void clienteNovoVipTambemVaiParaRevisao() {
        assertEquals("REVISAO", risco.avaliar(new Cliente(true, false, 0), 100_001, false));
    }

    @ParameterizedTest(name = "antigo, vip {0}, total {1} -> {2}")
    @CsvSource({
        "false, 500000, APROVADO",
        "false, 500001, REVISAO",
        "true,  500001, APROVADO",
        "false, 1000,   APROVADO"
    })
    void clienteComCompras(boolean vip, long total, String esperado) {
        assertEquals(esperado, risco.avaliar(new Cliente(vip, false, 4), total, false));
    }

    @Test
    void expressoNaoInfluenciaClienteComCompras() {
        assertEquals("APROVADO", risco.avaliar(new Cliente(false, false, 1), 1_000, true));
    }
}
