import { test, expect, Page } from '@playwright/test';

async function calcularFrete(page: Page, cep: string, valor: string) {
  await page.goto('/frete');
  await page.getByLabel('CEP').fill(cep);
  await page.getByLabel('Valor do pedido').fill(valor);
  await page.getByRole('button', { name: 'Calcular frete' }).click();
  return page.locator('#resultado');
}

test.describe('frete - caminhos válidos', () => {
  const casos = [
    { cep: '80000000', valor: '100', esperado: 'Frete: R$ 15,00', classe: 'CEP iniciado por 8' },
    { cep: '87050000', valor: '50,00', esperado: 'Frete: R$ 15,00', classe: 'CEP do PR com vírgula decimal' },
    { cep: '01001000', valor: '100', esperado: 'Frete: R$ 25,00', classe: 'CEP não iniciado por 8' },
    { cep: '79999999', valor: '100', esperado: 'Frete: R$ 25,00', classe: 'CEP iniciado por 7 (vizinho de 8)' },
    { cep: '90000000', valor: '100', esperado: 'Frete: R$ 25,00', classe: 'CEP iniciado por 9 (vizinho de 8)' },
    { cep: '80000000', valor: '350.50', esperado: 'Frete grátis', classe: 'pedido alto com ponto decimal' },
    { cep: '01001000', valor: '1000', esperado: 'Frete grátis', classe: 'pedido alto fora do PR' },
  ];

  for (const caso of casos) {
    test(`CEP ${caso.cep}, valor ${caso.valor} - ${caso.classe}`, async ({ page }) => {
      const resultado = await calcularFrete(page, caso.cep, caso.valor);

      await expect(resultado).toBeVisible();
      await expect(resultado).toHaveText(caso.esperado);
      await expect(resultado).toHaveAttribute('role', 'status');
      await expect(resultado).toHaveClass('success');
    });
  }
});

test.describe('frete - valores-limite do frete grátis (R$ 200,00)', () => {
  const casos = [
    { cep: '80000000', valor: '199,99', esperado: 'Frete: R$ 15,00', classe: '1 centavo abaixo do limite (CEP 8)' },
    { cep: '01001000', valor: '199.99', esperado: 'Frete: R$ 25,00', classe: '1 centavo abaixo do limite (outro CEP)' },
    { cep: '80000000', valor: '200', esperado: 'Frete grátis', classe: 'exatamente no limite' },
    { cep: '01001000', valor: '200,00', esperado: 'Frete grátis', classe: 'no limite com centavos' },
    { cep: '01001000', valor: '200,01', esperado: 'Frete grátis', classe: '1 centavo acima do limite' },
  ];

  for (const caso of casos) {
    test(`valor ${caso.valor} - ${caso.classe}`, async ({ page }) => {
      const resultado = await calcularFrete(page, caso.cep, caso.valor);
      await expect(resultado).toHaveText(caso.esperado);
      await expect(resultado).toHaveAttribute('role', 'status');
    });
  }
});

test.describe('frete - valores-limite do valor mínimo', () => {
  test('menor valor positivo (0,01) é aceito', async ({ page }) => {
    const resultado = await calcularFrete(page, '80000000', '0,01');
    await expect(resultado).toHaveText('Frete: R$ 15,00');
  });

  for (const valor of ['0', '0,00']) {
    test(`valor ${valor} é rejeitado`, async ({ page }) => {
      const resultado = await calcularFrete(page, '80000000', valor);
      await expect(resultado).toHaveText('Dados inválidos');
      await expect(resultado).toHaveAttribute('role', 'alert');
    });
  }
});

test.describe('frete - classes inválidas', () => {
  const casos = [
    { cep: '8000000', valor: '100', classe: 'CEP com 7 dígitos' },
    { cep: '800000000', valor: '100', classe: 'CEP com 9 dígitos' },
    { cep: '80000-000', valor: '100', classe: 'CEP com hífen' },
    { cep: '8000000A', valor: '100', classe: 'CEP com letra' },
    { cep: '', valor: '100', classe: 'CEP vazio' },
    { cep: '80000000', valor: '', classe: 'valor vazio' },
    { cep: '80000000', valor: 'cem', classe: 'valor não numérico' },
    { cep: '80000000', valor: '-10', classe: 'valor negativo' },
    { cep: '80000000', valor: '10,999', classe: 'valor com 3 casas decimais' },
    { cep: '80000000', valor: '1.000,00', classe: 'valor com separador de milhar' },
    { cep: '', valor: '', classe: 'todos os campos vazios' },
  ];

  for (const caso of casos) {
    test(`${caso.classe} (CEP "${caso.cep}", valor "${caso.valor}")`, async ({ page }) => {
      const resultado = await calcularFrete(page, caso.cep, caso.valor);

      await expect(resultado).toBeVisible();
      await expect(resultado).toHaveText('Dados inválidos');
      await expect(resultado).toHaveAttribute('role', 'alert');
      await expect(resultado).not.toHaveClass('success');
    });
  }
});

test.describe('frete - comportamento da interface', () => {
  test('resultado fica oculto antes do primeiro cálculo', async ({ page }) => {
    await page.goto('/frete');
    await expect(page.getByRole('heading', { name: 'Calcular frete' })).toBeVisible();
    await expect(page.locator('#resultado')).toBeHidden();
  });

  test('espaços antes e depois do CEP e do valor são ignorados', async ({ page }) => {
    const resultado = await calcularFrete(page, '  80000000  ', ' 100 ');
    await expect(resultado).toHaveText('Frete: R$ 15,00');
  });

  test('um novo cálculo válido substitui a mensagem de erro anterior', async ({ page }) => {
    const resultado = await calcularFrete(page, '123', '100');
    await expect(resultado).toHaveText('Dados inválidos');

    await page.getByLabel('CEP').fill('01001000');
    await page.getByRole('button', { name: 'Calcular frete' }).click();

    await expect(resultado).toHaveText('Frete: R$ 25,00');
    await expect(resultado).toHaveAttribute('role', 'status');
  });

  test('link "Voltar ao início" leva para o login', async ({ page }) => {
    await page.goto('/frete');
    await page.getByRole('link', { name: 'Voltar ao início' }).click();
    await expect(page).toHaveURL(/\/login$/);
  });
});
