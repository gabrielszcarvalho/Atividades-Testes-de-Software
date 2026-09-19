import { test, expect, Page } from '@playwright/test';

async function cadastrarSenha(page: Page, senha: string, confirmacao = senha) {
  await page.goto('/senha');
  await page.getByLabel('Nova senha').fill(senha);
  await page.getByLabel('Confirmar senha').fill(confirmacao);
  await page.getByRole('button', { name: 'Cadastrar senha' }).click();
  return page.locator('#resultado');
}

async function esperarSucesso(page: Page) {
  const resultado = page.locator('#resultado');
  await expect(resultado).toBeVisible();
  await expect(resultado).toHaveText('Senha cadastrada');
  await expect(resultado).toHaveAttribute('role', 'status');
  await expect(resultado).toHaveClass('success');
}

async function esperarErro(page: Page, mensagem: string) {
  const resultado = page.locator('#resultado');
  await expect(resultado).toBeVisible();
  await expect(resultado).toHaveText(mensagem);
  await expect(resultado).toHaveAttribute('role', 'alert');
  await expect(resultado).not.toHaveClass('success');
}

test.describe('senha - caminhos válidos', () => {
  const casos = [
    { senha: 'Senha123', classe: 'senha típica com 8 caracteres' },
    { senha: 'MinhaSenha2026', classe: 'senha com tamanho intermediário' },
    { senha: 'Abc123!@#', classe: 'senha com caracteres especiais' },
    { senha: '1aA45678', classe: 'número, minúscula e maiúscula no início' },
  ];

  for (const caso of casos) {
    test(`"${caso.senha}" - ${caso.classe}`, async ({ page }) => {
      await cadastrarSenha(page, caso.senha);
      await esperarSucesso(page);
    });
  }

  test('após cadastrar, os campos do formulário são limpos', async ({ page }) => {
    await cadastrarSenha(page, 'Senha123');
    await esperarSucesso(page);
    await expect(page.getByLabel('Nova senha')).toHaveValue('');
    await expect(page.getByLabel('Confirmar senha')).toHaveValue('');
  });
});

test.describe('senha - valores-limite do tamanho (8 a 20)', () => {
  const casos = [
    { senha: 'Abcde12', tamanho: 7, valida: false },
    { senha: 'Abcdef12', tamanho: 8, valida: true },
    { senha: 'Abcdefg12', tamanho: 9, valida: true },
    { senha: 'Abcdefghijklmnop123', tamanho: 19, valida: true },
    { senha: 'Abcdefghijklmnopq123', tamanho: 20, valida: true },
    { senha: 'Abcdefghijklmnopqr123', tamanho: 21, valida: false },
  ];

  for (const caso of casos) {
    test(`${caso.tamanho} caracteres - ${caso.valida ? 'aceita' : 'rejeitada'}`, async ({ page }) => {
      expect(caso.senha).toHaveLength(caso.tamanho);
      await cadastrarSenha(page, caso.senha);

      if (caso.valida) {
        await esperarSucesso(page);
      } else {
        await esperarErro(page, 'Senha fora do padrão');
      }
    });
  }
});

test.describe('senha - classes inválidas de formato', () => {
  const casos = [
    { senha: 'senha123', classe: 'sem letra maiúscula' },
    { senha: 'SENHA123', classe: 'sem letra minúscula' },
    { senha: 'SenhaForte', classe: 'sem número' },
    { senha: 'Senha 123', classe: 'com espaço no meio' },
    { senha: ' Senha123', classe: 'com espaço no início' },
    { senha: 'Senha123 ', classe: 'com espaço no fim' },
    { senha: '12345678', classe: 'somente números' },
    { senha: '', classe: 'senha vazia' },
  ];

  for (const caso of casos) {
    test(`${caso.classe} ("${caso.senha}")`, async ({ page }) => {
      await cadastrarSenha(page, caso.senha);
      await esperarErro(page, 'Senha fora do padrão');
    });
  }
});

test.describe('senha - confirmação', () => {
  test('confirmação diferente é rejeitada', async ({ page }) => {
    await cadastrarSenha(page, 'Senha123', 'Senha124');
    await esperarErro(page, 'As senhas não coincidem');
  });

  test('confirmação vazia é rejeitada', async ({ page }) => {
    await cadastrarSenha(page, 'Senha123', '');
    await esperarErro(page, 'As senhas não coincidem');
  });

  test('confirmação diferente só em maiúscula/minúscula é rejeitada', async ({ page }) => {
    await cadastrarSenha(page, 'Senha123', 'senha123');
    await esperarErro(page, 'As senhas não coincidem');
  });

  test('formato inválido é informado antes da divergência da confirmação', async ({ page }) => {
    await cadastrarSenha(page, 'curta', 'outra');
    await esperarErro(page, 'Senha fora do padrão');
  });

  test('erro não limpa os campos digitados', async ({ page }) => {
    await cadastrarSenha(page, 'Senha123', 'Senha124');
    await expect(page.getByLabel('Nova senha')).toHaveValue('Senha123');
    await expect(page.getByLabel('Confirmar senha')).toHaveValue('Senha124');
  });
});

test.describe('senha - comportamento da interface', () => {
  test('campos são do tipo password (texto oculto)', async ({ page }) => {
    await page.goto('/senha');
    await expect(page.getByLabel('Nova senha')).toHaveAttribute('type', 'password');
    await expect(page.getByLabel('Confirmar senha')).toHaveAttribute('type', 'password');
  });

  test('resultado fica oculto antes do envio', async ({ page }) => {
    await page.goto('/senha');
    await expect(page.getByRole('heading', { name: 'Criar senha' })).toBeVisible();
    await expect(page.locator('#resultado')).toBeHidden();
  });

  test('correção após erro permite cadastrar a senha', async ({ page }) => {
    await cadastrarSenha(page, 'senha123');
    await esperarErro(page, 'Senha fora do padrão');

    await page.getByLabel('Nova senha').fill('Senha123');
    await page.getByLabel('Confirmar senha').fill('Senha123');
    await page.getByRole('button', { name: 'Cadastrar senha' }).click();

    await esperarSucesso(page);
  });
});
