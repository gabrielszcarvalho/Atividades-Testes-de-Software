package br.edu.ifpr.boletim;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParticipacaoTest {

    private final Participacao participacao = new Participacao();

    @Test
    void deveSomarTresPontosQuandoEntregouEParticipou() {
        assertEquals(3, participacao.calcularPontos(true, true));
    }

    @Test
    void deveSomarDoisPontosQuandoSoEntregouAtividade() {
        assertEquals(2, participacao.calcularPontos(true, false));
    }

    @Test
    void deveSomarUmPontoQuandoSoParticipouDaAula() {
        assertEquals(1, participacao.calcularPontos(false, true));
    }

    @Test
    void deveRetornarZeroQuandoNaoEntregouNemParticipou() {
        assertEquals(0, participacao.calcularPontos(false, false));
    }

    @ParameterizedTest(name = "entregou={0}, participou={1} -> {2} pontos")
    @CsvSource({
            "true,  true,  3",
            "true,  false, 2",
            "false, true,  1",
            "false, false, 0"
    })
    void deveCalcularPontosEmTodasAsCombinacoes(boolean entregou, boolean participou, int esperado) {
        assertEquals(esperado, participacao.calcularPontos(entregou, participou));
    }
}
