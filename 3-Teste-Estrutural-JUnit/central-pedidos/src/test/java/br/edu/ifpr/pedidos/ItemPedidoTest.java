package br.edu.ifpr.pedidos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ItemPedidoTest {

    private static ItemPedido item(long preco, int quantidade, int estoque, int peso) {
        return new ItemPedido("SKU-1", preco, quantidade, estoque, peso, false);
    }

    @Test
    void deveCriarItemValido() {
        ItemPedido item = new ItemPedido("CANECA", 2_500, 3, 10, 400, true);

        assertAll(
            () -> assertEquals("CANECA", item.sku()),
            () -> assertEquals(2_500L, item.precoCentavos()),
            () -> assertEquals(3, item.quantidade()),
            () -> assertEquals(10, item.estoque()),
            () -> assertEquals(400, item.pesoGramas()),
            () -> assertTrue(item.fragil())
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void deveRejeitarSkuNuloOuEmBranco(String sku) {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
            () -> new ItemPedido(sku, 100, 1, 1, 1, false));
        assertEquals("SKU obrigatório", erro.getMessage());
    }

    @ParameterizedTest(name = "preço {0} centavos é inválido")
    @ValueSource(longs = {-1, 0, 1_000_001})
    void deveRejeitarPrecoForaDoIntervalo(long preco) {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
            () -> item(preco, 1, 1, 1));
        assertEquals("Preço inválido", erro.getMessage());
    }

    @ParameterizedTest(name = "preço {0} centavos é válido")
    @ValueSource(longs = {1, 1_000_000})
    void deveAceitarPrecoNosLimites(long preco) {
        assertEquals(preco, item(preco, 1, 1, 1).precoCentavos());
    }

    @ParameterizedTest(name = "quantidade {0} é inválida")
    @ValueSource(ints = {-1, 101})
    void deveRejeitarQuantidadeForaDoIntervalo(int quantidade) {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
            () -> item(100, quantidade, 200, 1));
        assertEquals("Quantidade inválida", erro.getMessage());
    }

    @ParameterizedTest(name = "quantidade {0} é válida")
    @ValueSource(ints = {0, 1, 100})
    void deveAceitarQuantidadeNosLimites(int quantidade) {
        assertEquals(quantidade, item(100, quantidade, 200, 1).quantidade());
    }

    @Test
    void deveRejeitarEstoqueNegativo() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
            () -> item(100, 0, -1, 1));
        assertEquals("Estoque inválido", erro.getMessage());
    }

    @Test
    void deveAceitarEstoqueZero() {
        assertEquals(0, item(100, 0, 0, 1).estoque());
    }

    @ParameterizedTest(name = "peso {0} g é inválido")
    @ValueSource(ints = {-1, 0, 100_001})
    void deveRejeitarPesoForaDoIntervalo(int peso) {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
            () -> item(100, 1, 1, peso));
        assertEquals("Peso inválido", erro.getMessage());
    }

    @ParameterizedTest(name = "peso {0} g é válido")
    @ValueSource(ints = {1, 100_000})
    void deveAceitarPesoNosLimites(int peso) {
        assertEquals(peso, item(100, 1, 1, peso).pesoGramas());
    }

    @Test
    void deveMultiplicarPrecoPelaQuantidade() {
        assertEquals(7_500L, item(2_500, 3, 3, 1).totalCentavos());
    }

    @Test
    void deveRetornarZeroParaItemInativo() {
        assertEquals(0L, item(2_500, 0, 0, 1).totalCentavos());
    }

    @Test
    void deveCalcularTotalMaximoSemEstourarInteiro() {
        assertEquals(100_000_000L, item(1_000_000, 100, 100, 1).totalCentavos());
    }

    @ParameterizedTest(name = "quantidade {0}, estoque {1} -> disponível = {2}")
    @CsvSource({
        "2, 5, true",
        "5, 5, true",
        "6, 5, false",
        "0, 0, true"
    })
    void deveVerificarDisponibilidade(int quantidade, int estoque, boolean esperado) {
        assertEquals(esperado, item(100, quantidade, estoque, 1).disponivel());
    }
}
