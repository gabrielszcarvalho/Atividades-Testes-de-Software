package br.edu.ifpr.pedidos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PagamentoServiceTest {

    static class StubProcessador implements ProcessadorPagamento {
        private final Deque<Object> respostas = new ArrayDeque<>();
        final List<Long> chamadas = new ArrayList<>();

        StubProcessador(Object... respostas) {
            this.respostas.addAll(List.of(respostas));
        }

        @Override
        public boolean autorizar(long totalCentavos) {
            chamadas.add(totalCentavos);
            Object resposta = respostas.removeFirst();
            if (resposta instanceof RuntimeException erro) throw erro;
            return (Boolean) resposta;
        }
    }

    private static IllegalStateException indisponivel() {
        return new IllegalStateException("Gateway indisponível");
    }

    @Test
    void deveExigirProcessador() {
        assertThrows(NullPointerException.class, () -> new PagamentoService(null));
    }

    @ParameterizedTest(name = "total {0} é inválido")
    @ValueSource(longs = {0, -1})
    void deveRejeitarTotalNaoPositivoSemChamarProcessador(long total) {
        StubProcessador stub = new StubProcessador(true);
        PagamentoService service = new PagamentoService(stub);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
            () -> service.pagar(total, 3));
        assertEquals("Total deve ser positivo", erro.getMessage());
        assertTrue(stub.chamadas.isEmpty());
    }

    @ParameterizedTest(name = "{0} tentativas é inválido")
    @ValueSource(ints = {0, 4, -1})
    void deveRejeitarLimiteDeTentativasForaDeUmATres(int tentativas) {
        StubProcessador stub = new StubProcessador(true);
        PagamentoService service = new PagamentoService(stub);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
            () -> service.pagar(1_000, tentativas));
        assertEquals("Use 1 a 3 tentativas", erro.getMessage());
        assertTrue(stub.chamadas.isEmpty());
    }

    @Test
    void aprovacaoNaPrimeiraTentativa() {
        StubProcessador stub = new StubProcessador(true);
        assertTrue(new PagamentoService(stub).pagar(11_200, 3));
        assertEquals(List.of(11_200L), stub.chamadas);
    }

    @Test
    void recusaEhDefinitivaENaoRepete() {
        StubProcessador stub = new StubProcessador(false, true);
        assertFalse(new PagamentoService(stub).pagar(5_000, 3));
        assertEquals(List.of(5_000L), stub.chamadas);
    }

    @Test
    void limiteDeUmaTentativaAceitaAprovacao() {
        StubProcessador stub = new StubProcessador(true);
        assertTrue(new PagamentoService(stub).pagar(1, 1));
        assertEquals(List.of(1L), stub.chamadas);
    }

    @Test
    void indisponibilidadeComUmaTentativaEsgotaNaPrimeira() {
        StubProcessador stub = new StubProcessador(indisponivel(), true);
        assertFalse(new PagamentoService(stub).pagar(5_000, 1));
        assertEquals(1, stub.chamadas.size());
    }

    @Test
    void indisponibilidadeSeguidaDeAprovacao() {
        StubProcessador stub = new StubProcessador(indisponivel(), true);
        assertTrue(new PagamentoService(stub).pagar(5_000, 3));
        assertEquals(List.of(5_000L, 5_000L), stub.chamadas);
    }

    @Test
    void aprovacaoNaTerceiraTentativa() {
        StubProcessador stub = new StubProcessador(indisponivel(), indisponivel(), true);
        assertTrue(new PagamentoService(stub).pagar(5_000, 3));
        assertEquals(List.of(5_000L, 5_000L, 5_000L), stub.chamadas);
    }

    @Test
    void indisponibilidadeSeguidaDeRecusa() {
        StubProcessador stub = new StubProcessador(indisponivel(), false, true);
        assertFalse(new PagamentoService(stub).pagar(5_000, 3));
        assertEquals(2, stub.chamadas.size());
    }

    @Test
    void esgotarDuasTentativasRetornaFalse() {
        StubProcessador stub = new StubProcessador(indisponivel(), indisponivel(), true);
        assertFalse(new PagamentoService(stub).pagar(5_000, 2));
        assertEquals(2, stub.chamadas.size());
    }

    @Test
    void esgotarTresTentativasRetornaFalse() {
        StubProcessador stub = new StubProcessador(indisponivel(), indisponivel(), indisponivel(), true);
        assertFalse(new PagamentoService(stub).pagar(5_000, 3));
        assertEquals(3, stub.chamadas.size());
    }

    @Test
    void outraExcecaoPropagaSemNovaTentativa() {
        RuntimeException falha = new IllegalArgumentException("Cartão inválido");
        StubProcessador stub = new StubProcessador(falha, true);
        PagamentoService service = new PagamentoService(stub);

        RuntimeException lancada = assertThrows(RuntimeException.class, () -> service.pagar(5_000, 3));
        assertSame(falha, lancada);
        assertEquals(1, stub.chamadas.size());
    }

    @Test
    void outraExcecaoDepoisDeIndisponibilidadeTambemPropaga() {
        StubProcessador stub = new StubProcessador(indisponivel(), new UnsupportedOperationException("erro"));
        PagamentoService service = new PagamentoService(stub);

        assertThrows(UnsupportedOperationException.class, () -> service.pagar(5_000, 3));
        assertEquals(2, stub.chamadas.size());
    }
}
