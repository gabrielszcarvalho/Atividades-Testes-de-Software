package br.edu.ifpr.pedidos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PedidoTest {

    private static ItemPedido item(String sku, long preco, int quantidade, int estoque, int peso, boolean fragil) {
        return new ItemPedido(sku, preco, quantidade, estoque, peso, fragil);
    }

    private static ItemPedido ativo(long preco, int quantidade, int peso) {
        return item("A", preco, quantidade, quantidade, peso, false);
    }

    private static Pedido pedido(ItemPedido... itens) {
        return new Pedido(List.of(itens), "PR", false, null);
    }

    @Test
    void deveRejeitarListaNula() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
            () -> new Pedido(null, "PR", false, null));
        assertEquals("Lista inválida", erro.getMessage());
    }

    @Test
    void deveAceitarCemLinhas() {
        List<ItemPedido> itens = Collections.nCopies(100, ativo(100, 1, 1));
        assertEquals(100, new Pedido(itens, "PR", false, null).itens().size());
    }

    @Test
    void deveRejeitarMaisDeCemLinhas() {
        List<ItemPedido> itens = Collections.nCopies(101, ativo(100, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Pedido(itens, "PR", false, null));
    }

    @Test
    void deveAceitarListaVaziaNaConstrucao() {
        assertTrue(new Pedido(List.of(), "PR", false, null).itens().isEmpty());
    }

    @Test
    void deveLancarNullPointerParaElementoNulo() {
        List<ItemPedido> itens = Arrays.asList(ativo(100, 1, 1), null);
        assertThrows(NullPointerException.class, () -> new Pedido(itens, "PR", false, null));
    }

    @Test
    void deveCopiarListaDefensivamente() {
        List<ItemPedido> original = new ArrayList<>();
        original.add(ativo(1_000, 1, 1));
        Pedido pedido = new Pedido(original, "PR", false, null);

        original.add(ativo(5_000, 1, 1));

        assertEquals(1, pedido.itens().size());
        assertEquals(1_000L, pedido.subtotalCentavos());
        assertThrows(UnsupportedOperationException.class, () -> pedido.itens().add(ativo(1, 1, 1)));
    }

    @ParameterizedTest(name = "UF \"{0}\" é inválida")
    @NullSource
    @ValueSource(strings = {"", "P", "PRR", "pr", "Pr", "P1", "P ", "ÁB"})
    void deveRejeitarUfInvalida(String uf) {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
            () -> new Pedido(List.of(), uf, false, null));
        assertEquals("UF inválida", erro.getMessage());
    }

    @ParameterizedTest(name = "UF \"{0}\" é aceita")
    @ValueSource(strings = {"PR", "SP", "RJ", "ZZ"})
    void deveAceitarQualquerUfComDuasLetrasMaiusculas(String uf) {
        assertEquals(uf, new Pedido(List.of(), uf, false, null).uf());
    }

    @Test
    void deveManterExpressoECupom() {
        Pedido pedido = new Pedido(List.of(), "SP", true, "EXTRA10");
        assertTrue(pedido.expresso());
        assertEquals("EXTRA10", pedido.cupom());
    }

    @Test
    void deveTerSubtotalZeroSemItens() {
        assertEquals(0L, pedido().subtotalCentavos());
    }

    @Test
    void deveSomarUmaLinha() {
        assertEquals(6_000L, pedido(ativo(2_000, 3, 100)).subtotalCentavos());
    }

    @Test
    void deveSomarVariasLinhas() {
        assertEquals(8_500L, pedido(ativo(2_000, 3, 100), ativo(2_500, 1, 100)).subtotalCentavos());
    }

    @Test
    void deveIgnorarLinhaInativaNoSubtotal() {
        ItemPedido inativo = item("X", 9_999, 0, 0, 100, false);
        assertEquals(2_000L, pedido(inativo, ativo(2_000, 1, 100)).subtotalCentavos());
    }

    @Test
    void deveTerSubtotalZeroSeTodasAsLinhasEstaoInativas() {
        ItemPedido inativo = item("X", 9_999, 0, 0, 100, false);
        assertEquals(0L, pedido(inativo, inativo).subtotalCentavos());
    }

    @Test
    void deveTerPesoZeroSemItens() {
        assertEquals(0, pedido().pesoGramas());
    }

    @Test
    void deveMultiplicarPesoPelaQuantidadeESomarLinhas() {
        assertEquals(2_700, pedido(ativo(100, 3, 500), ativo(100, 1, 1_200)).pesoGramas());
    }

    @Test
    void deveIgnorarPesoDeItemInativo() {
        ItemPedido inativo = item("X", 100, 0, 0, 50_000, false);
        assertEquals(500, pedido(inativo, ativo(100, 1, 500)).pesoGramas());
    }

    @Test
    void naoDeveTerFragilSemItens() {
        assertFalse(pedido().temFragil());
    }

    @Test
    void naoDeveTerFragilQuandoNenhumItemEFragil() {
        assertFalse(pedido(ativo(100, 1, 1), ativo(100, 2, 1)).temFragil());
    }

    @Test
    void deveDetectarFragilAtivo() {
        assertTrue(pedido(item("VASO", 100, 1, 1, 1, true)).temFragil());
    }

    @Test
    void deveDetectarFragilNoFimDaLista() {
        assertTrue(pedido(ativo(100, 1, 1), item("VASO", 100, 1, 1, 1, true)).temFragil());
    }

    @Test
    void deveIgnorarFragilInativo() {
        assertFalse(pedido(item("VASO", 100, 0, 0, 1, true), ativo(100, 1, 1)).temFragil());
    }

    @Test
    void deveTerEstoqueSuficienteSemItens() {
        assertTrue(pedido().estoqueSuficiente());
    }

    @Test
    void deveTerEstoqueSuficienteQuandoTodasAsLinhasEstaoDisponiveis() {
        assertTrue(pedido(ativo(100, 2, 1), item("B", 100, 5, 5, 1, false)).estoqueSuficiente());
    }

    @Test
    void deveDetectarFaltaDeEstoqueNoInicio() {
        ItemPedido semEstoque = item("B", 100, 3, 2, 1, false);
        assertFalse(pedido(semEstoque, ativo(100, 1, 1)).estoqueSuficiente());
    }

    @Test
    void deveDetectarFaltaDeEstoqueNoFim() {
        ItemPedido semEstoque = item("B", 100, 3, 2, 1, false);
        assertFalse(pedido(ativo(100, 1, 1), ativo(100, 1, 1), semEstoque).estoqueSuficiente());
    }

    @Test
    void deveAvaliarEstoquePorLinhaMesmoComSkuRepetido() {
        ItemPedido linha = item("MESMO", 100, 3, 5, 1, false);
        assertTrue(pedido(linha, linha).estoqueSuficiente());
    }
}
