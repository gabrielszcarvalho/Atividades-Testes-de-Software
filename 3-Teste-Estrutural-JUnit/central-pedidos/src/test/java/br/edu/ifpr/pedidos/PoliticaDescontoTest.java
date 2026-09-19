package br.edu.ifpr.pedidos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PoliticaDescontoTest {

    private final PoliticaDesconto politica = new PoliticaDesconto();

    private static final Cliente COMUM_NOVO = new Cliente(false, false, 0);
    private static final Cliente COMUM_ANTIGO = new Cliente(false, false, 3);
    private static final Cliente VIP_NOVO = new Cliente(true, false, 0);
    private static final Cliente VIP_ANTIGO = new Cliente(true, false, 5);

    @Test
    void deveRejeitarSubtotalNegativo() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
            () -> politica.calcular(COMUM_ANTIGO, -1, null));
        assertEquals("Subtotal negativo", erro.getMessage());
    }

    @Test
    void deveAceitarSubtotalZero() {
        assertEquals(0L, politica.calcular(VIP_ANTIGO, 0, null));
    }

    @Test
    void vipRecebeDezPorCento() {
        assertEquals(1_000L, politica.calcular(VIP_ANTIGO, 10_000, null));
    }

    @Test
    void vipTemDescontoTruncadoParaBaixo() {
        assertEquals(1_000L, politica.calcular(VIP_ANTIGO, 10_009, null));
    }

    @ParameterizedTest(name = "cliente comum, subtotal {0} -> desconto {1}")
    @CsvSource({
        "49999, 0",
        "50000, 2500",
        "50001, 2500",
        "50020, 2501"
    })
    void clienteComumRecebeCincoPorCentoAPartirDeQuinhentosReais(long subtotal, long esperado) {
        assertEquals(esperado, politica.calcular(COMUM_ANTIGO, subtotal, null));
    }

    @ParameterizedTest(name = "cupom \"{0}\" mantém o desconto base")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void cupomNuloOuEmBrancoMantemDescontoBase(String cupom) {
        assertEquals(2_500L, politica.calcular(COMUM_ANTIGO, 50_000, cupom));
        assertEquals(1_000L, politica.calcular(VIP_ANTIGO, 10_000, cupom));
    }

    @Test
    void bemVindoSomaVinteReaisParaClienteSemComprasComSubtotalMinimo() {
        assertEquals(2_000L, politica.calcular(COMUM_NOVO, 15_000, "BEMVINDO"));
    }

    @Test
    void bemVindoNoLimiteDeCemReais() {
        assertEquals(2_000L, politica.calcular(COMUM_NOVO, 10_000, "BEMVINDO"));
    }

    @Test
    void bemVindoAbaixoDeCemReaisNaoSoma() {
        assertEquals(0L, politica.calcular(COMUM_NOVO, 9_999, "BEMVINDO"));
    }

    @Test
    void bemVindoNaoSomaParaClienteComComprasAnteriores() {
        assertEquals(0L, politica.calcular(COMUM_ANTIGO, 15_000, "BEMVINDO"));
    }

    @Test
    void cupomENormalizadoComTrimEMaiusculas() {
        assertEquals(2_000L, politica.calcular(COMUM_NOVO, 15_000, "  bemVindo  "));
    }

    @Test
    void vipComBemVindoEhLimitadoAoTeto() {
        assertEquals(2_000L, politica.calcular(VIP_NOVO, 10_000, "BEMVINDO"));
    }

    @Test
    void vipComBemVindoIgualAoTetoNaoEhCortado() {
        assertEquals(4_000L, politica.calcular(VIP_NOVO, 20_000, "BEMVINDO"));
    }

    @Test
    void comumAcimaDeQuinhentosComBemVindoAcumula() {
        assertEquals(5_000L, politica.calcular(COMUM_NOVO, 60_000, "BEMVINDO"));
    }

    @Test
    void extra10AbaixoDeDuzentosReaisNaoSoma() {
        assertEquals(0L, politica.calcular(COMUM_ANTIGO, 19_999, "EXTRA10"));
    }

    @Test
    void extra10NoLimiteDeDuzentosReais() {
        assertEquals(2_000L, politica.calcular(COMUM_ANTIGO, 20_000, "EXTRA10"));
    }

    @Test
    void extra10ComDescontoComumAcumula() {
        assertEquals(7_500L, politica.calcular(COMUM_ANTIGO, 50_000, "EXTRA10"));
    }

    @Test
    void vipComExtra10ChegaExatamenteAoTeto() {
        assertEquals(10_000L, politica.calcular(VIP_ANTIGO, 50_000, "extra10"));
    }

    @Test
    void extra10TambemTruncaCentavos() {
        assertEquals(2_000L, politica.calcular(COMUM_ANTIGO, 20_005, " Extra10 "));
    }

    @ParameterizedTest(name = "cupom \"{0}\" é desconhecido")
    @ValueSource(strings = {"DESCONTO50", "BEM VINDO", "EXTRA 10", "X"})
    void cupomDesconhecidoLancaExcecao(String cupom) {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
            () -> politica.calcular(COMUM_ANTIGO, 50_000, cupom));
        assertEquals("Cupom desconhecido", erro.getMessage());
    }
}
