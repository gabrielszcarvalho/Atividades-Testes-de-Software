package br.edu.ifpr.pedidos;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class CalculadoraFreteTest {

    private final CalculadoraFrete calculadora = new CalculadoraFrete();

    private static final Cliente COMUM = new Cliente(false, false, 1);
    private static final Cliente VIP = new Cliente(true, false, 1);

    private static Pedido pedido(String uf, int pesoGramas, boolean expresso, boolean fragil) {
        ItemPedido item = new ItemPedido("ITEM", 1_000, 1, 1, pesoGramas, fragil);
        return new Pedido(List.of(item), uf, expresso, null);
    }

    private static Pedido normal(String uf, int pesoGramas) {
        return pedido(uf, pesoGramas, false, false);
    }

    @Test
    void deveRejeitarValorLiquidoNegativo() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
            () -> calculadora.calcular(normal("PR", 1_000), COMUM, -1));
        assertEquals("Valor líquido negativo", erro.getMessage());
    }

    @ParameterizedTest(name = "UF {0} -> base {1}")
    @CsvSource({
        "PR, 1200",
        "SP, 2000",
        "RJ, 2000",
        "MG, 3000",
        "ZZ, 3000"
    })
    void deveAplicarTarifaBasePorUf(String uf, long esperado) {
        assertEquals(esperado, calculadora.calcular(normal(uf, 1_000), COMUM, 10_000));
    }

    @ParameterizedTest(name = "{0} g -> frete {1} (PR)")
    @CsvSource({
        "2000, 1200",
        "2001, 1500",
        "3000, 1500",
        "3001, 1800",
        "5500, 2400",
        "10000, 3600"
    })
    void deveCobrarTresReaisPorKgOuFracaoAcimaDeDoisKg(int peso, long esperado) {
        assertEquals(esperado, calculadora.calcular(normal("PR", peso), COMUM, 10_000));
    }

    @Test
    void pesoConsideraQuantidadeDeCadaLinha() {
        ItemPedido item = new ItemPedido("ITEM", 1_000, 3, 3, 1_000, false);
        Pedido pedido = new Pedido(List.of(item), "SP", false, null);
        assertEquals(2_300L, calculadora.calcular(pedido, COMUM, 10_000));
    }

    @Test
    void liquidoUmCentavoAbaixoDoLimitePagaFrete() {
        assertEquals(1_200L, calculadora.calcular(normal("PR", 1_000), COMUM, 29_999));
    }

    @Test
    void liquidoNoLimiteZeraFrete() {
        assertEquals(0L, calculadora.calcular(normal("PR", 1_000), COMUM, 30_000));
    }

    @Test
    void freteGratisZeraTambemOAdicionalDePeso() {
        assertEquals(0L, calculadora.calcular(normal("MG", 9_000), COMUM, 50_000));
    }

    @Test
    void entregaExpressaNaoGanhaFreteGratis() {
        assertEquals(2_700L, calculadora.calcular(pedido("PR", 1_000, true, false), COMUM, 30_000));
    }

    @Test
    void vipPagaMetadeDaBase() {
        assertEquals(600L, calculadora.calcular(normal("PR", 1_000), VIP, 10_000));
    }

    @Test
    void vipPagaMetadeDaBaseMaisPeso() {
        assertEquals(1_650L, calculadora.calcular(normal("MG", 2_500), VIP, 10_000));
    }

    @Test
    void vipComFreteGratisContinuaZero() {
        assertEquals(0L, calculadora.calcular(normal("RJ", 1_000), VIP, 30_000));
    }

    @Test
    void expressoSomaQuinzeReais() {
        assertEquals(3_500L, calculadora.calcular(pedido("SP", 1_000, true, false), COMUM, 10_000));
    }

    @Test
    void expressoNaoEhDivididoParaVip() {
        assertEquals(2_100L, calculadora.calcular(pedido("PR", 1_000, true, false), VIP, 10_000));
    }

    @Test
    void fragilSomaCincoReais() {
        assertEquals(1_700L, calculadora.calcular(pedido("PR", 1_000, false, true), COMUM, 10_000));
    }

    @Test
    void fragilIncideMesmoComFreteGratis() {
        assertEquals(500L, calculadora.calcular(pedido("PR", 1_000, false, true), COMUM, 30_000));
    }

    @Test
    void fragilEhCobradoUmaUnicaVez() {
        ItemPedido vaso = new ItemPedido("VASO", 1_000, 1, 1, 500, true);
        ItemPedido taca = new ItemPedido("TACA", 1_000, 2, 2, 200, true);
        Pedido pedido = new Pedido(List.of(vaso, taca), "PR", false, null);
        assertEquals(1_700L, calculadora.calcular(pedido, COMUM, 10_000));
    }

    @Test
    void fragilInativoNaoSoma() {
        ItemPedido vasoInativo = new ItemPedido("VASO", 1_000, 0, 0, 500, true);
        ItemPedido livro = new ItemPedido("LIVRO", 1_000, 1, 1, 500, false);
        Pedido pedido = new Pedido(List.of(vasoInativo, livro), "PR", false, null);
        assertEquals(1_200L, calculadora.calcular(pedido, COMUM, 10_000));
    }

    @Test
    void todosOsAdicionaisCombinados() {
        assertEquals(3_800L, calculadora.calcular(pedido("MG", 3_500, true, true), VIP, 50_000));
    }
}
