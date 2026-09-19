package br.edu.ifpr.boletim;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoletimTest {

    private final Boletim boletim = new Boletim();

    @Test
    void deveAprovarAlunoComMediaOito() {
        // Preparar: criar o objeto que será testado.
        Boletim boletim = new Boletim();

        // Executar: chamar um único método com uma entrada conhecida.
        String resultado = boletim.verificarSituacao(8);

        // Verificar: comparar o resultado esperado com o resultado obtido.
        assertEquals("APROVADO", resultado);
    }

    @Test
    void deveColocarEmRecuperacaoAlunoComMediaCinco() {
        assertEquals("RECUPERACAO", boletim.verificarSituacao(5));
    }

    @Test
    void deveReprovarAlunoComMediaDois() {
        assertEquals("REPROVADO", boletim.verificarSituacao(2));
    }

    @ParameterizedTest(name = "média {0} -> {1}")
    @CsvSource({
            "0,    REPROVADO",
            "3.99, REPROVADO",
            "4,    RECUPERACAO",
            "4.01, RECUPERACAO",
            "6.99, RECUPERACAO",
            "7,    APROVADO",
            "7.01, APROVADO",
            "10,   APROVADO"
    })
    void deveClassificarNosLimitesDasFaixas(double media, String esperado) {
        assertEquals(esperado, boletim.verificarSituacao(media));
    }

    @Test
    void deveCalcularMediaIgualCinco() {
        assertEquals(5, boletim.calcularMedia(5, 5), 0.0001);
    }

    @Test
    void deveCalcularMediaComParteDecimal() {
        assertEquals(7.5, boletim.calcularMedia(7, 8), 0.0001);
    }

    @Test
    void deveCalcularMediaSemArredondar() {
        assertEquals(6.85, boletim.calcularMedia(6.2, 7.5), 0.0001);
    }

    @Test
    void deveCalcularMediaComNotasExtremas() {
        assertEquals(0, boletim.calcularMedia(0, 0), 0.0001);
        assertEquals(10, boletim.calcularMedia(10, 10), 0.0001);
        assertEquals(5, boletim.calcularMedia(0, 10), 0.0001);
    }

    @Test
    void deveRetornarZeroParaArrayVazio() {
        assertEquals(0, boletim.contarAprovados(new double[] {}));
    }

    @Test
    void deveContarUmAprovadoComUmElemento() {
        assertEquals(1, boletim.contarAprovados(new double[] {8}));
    }

    @Test
    void deveContarZeroComUmElementoNaoAprovado() {
        assertEquals(0, boletim.contarAprovados(new double[] {5}));
    }

    @Test
    void deveContarAprovadosEntreVariasMedias() {
        assertEquals(2, boletim.contarAprovados(new double[] {8, 5, 7}));
    }

    @Test
    void deveContarMediaSeteComoAprovadaEMediaQuaseSeteNao() {
        assertEquals(1, boletim.contarAprovados(new double[] {6.99, 7}));
    }

    @Test
    void deveContarTodosQuandoTodosAprovados() {
        assertEquals(4, boletim.contarAprovados(new double[] {7, 8, 9.5, 10}));
    }

    @Test
    void deveContarZeroQuandoNenhumAprovado() {
        assertEquals(0, boletim.contarAprovados(new double[] {0, 3.5, 6.99}));
    }
}
