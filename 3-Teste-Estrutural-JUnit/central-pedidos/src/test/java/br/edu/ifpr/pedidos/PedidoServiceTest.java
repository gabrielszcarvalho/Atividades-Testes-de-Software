package br.edu.ifpr.pedidos;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import br.edu.ifpr.pedidos.PagamentoServiceTest.StubProcessador;
import static org.junit.jupiter.api.Assertions.*;

class PedidoServiceTest {
    @Test
    void deveFecharPedidoDeClienteComumComFreteDoParanaEPagamentoAprovado() {
        // 1. Preparar: cliente comum, uma compra anterior e item disponível de R$ 100,00.
        Cliente cliente = new Cliente(false, false, 1);
        ItemPedido item = new ItemPedido("LIVRO-JAVA", 10_000, 1, 5, 1_000, false);
        Pedido pedido = new Pedido(List.of(item), "PR", false, null);

        // Simula o pagamento e registra as cobranças, sem banco ou serviço externo.
        List<Long> cobrancas = new ArrayList<>();
        PedidoService service = new PedidoService(total -> {
            cobrancas.add(total);
            return true;
        });

        // 2. Executar: percorrer um caminho completo do fechamento.
        ResultadoPedido resultado = service.fechar(pedido, cliente);

        // 3. Verificar: sem desconto; frete de R$ 12,00; total de R$ 112,00.
        assertAll(
            () -> assertEquals("PAGO", resultado.status()),
            () -> assertEquals(10_000L, resultado.subtotalCentavos()),
            () -> assertEquals(0L, resultado.descontoCentavos()),
            () -> assertEquals(1_200L, resultado.freteCentavos()),
            () -> assertEquals(11_200L, resultado.totalCentavos()),
            // A lista comprova uma única cobrança, com o valor correto.
            () -> assertEquals(List.of(11_200L), cobrancas)
        );
    }

    private static final Cliente COMUM_ANTIGO = new Cliente(false, false, 1);

    private static ItemPedido item(long preco, int quantidade, int estoque, int peso) {
        return new ItemPedido("SKU", preco, quantidade, estoque, peso, false);
    }

    private static void assertResultado(ResultadoPedido r, String status, long subtotal,
                                        long desconto, long frete, long total) {
        assertAll(
            () -> assertEquals(status, r.status()),
            () -> assertEquals(subtotal, r.subtotalCentavos()),
            () -> assertEquals(desconto, r.descontoCentavos()),
            () -> assertEquals(frete, r.freteCentavos()),
            () -> assertEquals(total, r.totalCentavos())
        );
    }

    @Test
    void deveExigirProcessadorDePagamento() {
        assertThrows(NullPointerException.class, () -> new PedidoService(null));
    }

    @Test
    void deveExigirPedidoECliente() {
        StubProcessador stub = new StubProcessador(true);
        PedidoService service = new PedidoService(stub);
        Pedido pedido = new Pedido(List.of(item(1_000, 1, 1, 100)), "PR", false, null);

        assertThrows(NullPointerException.class, () -> service.fechar(null, COMUM_ANTIGO));
        assertThrows(NullPointerException.class, () -> service.fechar(pedido, null));
        assertTrue(stub.chamadas.isEmpty());
    }

    @Test
    void clienteBloqueadoRetornaBloqueadoAntesDeAvaliarItensECupom() {
        StubProcessador stub = new StubProcessador(true);
        Pedido pedido = new Pedido(List.of(), "PR", false, "CUPOM-INEXISTENTE");

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, new Cliente(true, true, 10));

        assertResultado(r, "BLOQUEADO", 0, 0, 0, 0);
        assertTrue(stub.chamadas.isEmpty());
    }

    @Test
    void pedidoSemItensLancaExcecao() {
        StubProcessador stub = new StubProcessador(true);
        Pedido pedido = new Pedido(List.of(), "PR", false, null);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
            () -> new PedidoService(stub).fechar(pedido, COMUM_ANTIGO));
        assertEquals("Pedido sem itens ativos", erro.getMessage());
        assertTrue(stub.chamadas.isEmpty());
    }

    @Test
    void pedidoSoComItensInativosLancaExcecao() {
        StubProcessador stub = new StubProcessador(true);
        Pedido pedido = new Pedido(List.of(item(5_000, 0, 0, 100)), "PR", false, null);

        assertThrows(IllegalArgumentException.class, () -> new PedidoService(stub).fechar(pedido, COMUM_ANTIGO));
        assertTrue(stub.chamadas.isEmpty());
    }

    @Test
    void faltaDeEstoqueRetornaSemEstoqueAntesDoCupom() {
        StubProcessador stub = new StubProcessador(true);
        Pedido pedido = new Pedido(List.of(item(5_000, 3, 2, 100)), "PR", false, "CUPOM-INEXISTENTE");

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM_ANTIGO);

        assertResultado(r, "SEM_ESTOQUE", 0, 0, 0, 0);
        assertTrue(stub.chamadas.isEmpty());
    }

    @Test
    void cupomDesconhecidoInterrompeOFechamento() {
        StubProcessador stub = new StubProcessador(true);
        Pedido pedido = new Pedido(List.of(item(5_000, 1, 1, 100)), "PR", false, "PROMO");

        assertThrows(IllegalArgumentException.class, () -> new PedidoService(stub).fechar(pedido, COMUM_ANTIGO));
        assertTrue(stub.chamadas.isEmpty());
    }

    @Test
    void clienteNovoComEntregaExpressaFicaEmRevisaoSemCobranca() {
        StubProcessador stub = new StubProcessador(true);
        Pedido pedido = new Pedido(List.of(item(10_000, 1, 1, 1_000)), "PR", true, null);

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, new Cliente(false, false, 0));

        assertResultado(r, "REVISAO", 10_000, 0, 2_700, 12_700);
        assertTrue(stub.chamadas.isEmpty());
    }

    @Test
    void clienteNovoAcimaDeMilReaisFicaEmRevisao() {
        StubProcessador stub = new StubProcessador(true);
        Pedido pedido = new Pedido(List.of(item(200_000, 1, 1, 1_000)), "PR", false, null);

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, new Cliente(false, false, 0));

        assertResultado(r, "REVISAO", 200_000, 10_000, 0, 190_000);
        assertTrue(stub.chamadas.isEmpty());
    }

    @Test
    void clienteAntigoNaoVipAcimaDeCincoMilReaisFicaEmRevisao() {
        StubProcessador stub = new StubProcessador(true);
        Pedido pedido = new Pedido(List.of(item(1_000_000, 6, 6, 1)), "PR", false, null);

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM_ANTIGO);

        assertResultado(r, "REVISAO", 6_000_000, 300_000, 0, 5_700_000);
        assertTrue(stub.chamadas.isEmpty());
    }

    @Test
    void clienteVipAntigoComValorAltoEhAprovadoEPago() {
        StubProcessador stub = new StubProcessador(true);
        Pedido pedido = new Pedido(List.of(item(1_000_000, 6, 6, 1)), "PR", false, null);

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, new Cliente(true, false, 2));

        assertResultado(r, "PAGO", 6_000_000, 600_000, 0, 5_400_000);
        assertEquals(List.of(5_400_000L), stub.chamadas);
    }

    @Test
    void pagamentoRecusadoMantemValoresCalculados() {
        StubProcessador stub = new StubProcessador(false);
        Pedido pedido = new Pedido(List.of(item(10_000, 1, 1, 1_000)), "PR", false, null);

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM_ANTIGO);

        assertResultado(r, "PAGAMENTO_RECUSADO", 10_000, 0, 1_200, 11_200);
        assertEquals(List.of(11_200L), stub.chamadas);
    }

    @Test
    void pagamentoAprovadoNaTerceiraTentativa() {
        StubProcessador stub = new StubProcessador(
            new IllegalStateException(), new IllegalStateException(), true);
        Pedido pedido = new Pedido(List.of(item(10_000, 1, 1, 1_000)), "PR", false, null);

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM_ANTIGO);

        assertEquals("PAGO", r.status());
        assertEquals(List.of(11_200L, 11_200L, 11_200L), stub.chamadas);
    }

    @Test
    void tresIndisponibilidadesResultamEmPagamentoRecusado() {
        StubProcessador stub = new StubProcessador(
            new IllegalStateException(), new IllegalStateException(), new IllegalStateException(), true);
        Pedido pedido = new Pedido(List.of(item(10_000, 1, 1, 1_000)), "PR", false, null);

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM_ANTIGO);

        assertResultado(r, "PAGAMENTO_RECUSADO", 10_000, 0, 1_200, 11_200);
        assertEquals(3, stub.chamadas.size());
    }

    @Test
    void falhaInesperadaNoProcessadorInterrompeOFechamento() {
        StubProcessador stub = new StubProcessador(new RuntimeException("Falha grave"));
        Pedido pedido = new Pedido(List.of(item(10_000, 1, 1, 1_000)), "PR", false, null);

        RuntimeException erro = assertThrows(RuntimeException.class,
            () -> new PedidoService(stub).fechar(pedido, COMUM_ANTIGO));
        assertEquals("Falha grave", erro.getMessage());
        assertEquals(1, stub.chamadas.size());
    }

    @Test
    void combinaCupomExtra10PesoExcedenteEFragilEmSaoPaulo() {
        ItemPedido vaso = new ItemPedido("VASO", 15_000, 1, 1, 1_500, true);
        ItemPedido livro = new ItemPedido("LIVRO", 10_000, 1, 4, 2_000, false);
        Pedido pedido = new Pedido(List.of(vaso, livro), "SP", false, " extra10 ");
        StubProcessador stub = new StubProcessador(true);

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, new Cliente(false, false, 2));

        assertResultado(r, "PAGO", 25_000, 2_500, 3_100, 25_600);
        assertEquals(List.of(25_600L), stub.chamadas);
    }

    @Test
    void clienteNovoComBemVindoNoRioDeJaneiro() {
        Pedido pedido = new Pedido(List.of(item(12_000, 1, 1, 1_000)), "RJ", false, "BEMVINDO");
        StubProcessador stub = new StubProcessador(true);

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, new Cliente(false, false, 0));

        assertResultado(r, "PAGO", 12_000, 2_000, 2_000, 12_000);
        assertEquals(List.of(12_000L), stub.chamadas);
    }

    @Test
    void vipComFreteGratisPorValorLiquido() {
        Pedido pedido = new Pedido(List.of(item(40_000, 1, 1, 500)), "MG", false, null);
        StubProcessador stub = new StubProcessador(true);

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, new Cliente(true, false, 1));

        assertResultado(r, "PAGO", 40_000, 4_000, 0, 36_000);
    }

    @Test
    void itemInativoNaoEntraNoCalculoDoFechamento() {
        ItemPedido inativo = new ItemPedido("INATIVO", 99_999, 0, 0, 90_000, true);
        Pedido pedido = new Pedido(List.of(inativo, item(10_000, 1, 1, 1_000)), "PR", false, null);
        StubProcessador stub = new StubProcessador(true);

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM_ANTIGO);

        assertResultado(r, "PAGO", 10_000, 0, 1_200, 11_200);
    }
}
