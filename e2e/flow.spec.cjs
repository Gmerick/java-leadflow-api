const {test,expect}=require('@playwright/test');
test('campanha, lead, duplicidade, conversão e indicadores',async({page})=>{
 await page.goto('/');await page.getByRole('button',{name:'Nova campanha',exact:true}).click();await page.getByLabel('Nome da campanha').fill('Campanha de laboratório');await page.getByLabel('Investimento (R$)').fill('100');await page.getByRole('button',{name:'Criar campanha',exact:true}).click();
 await expect(page.getByRole('row').filter({hasText:'Campanha de laboratório'})).toBeVisible();
 await page.getByRole('button',{name:'Leads',exact:true}).click();await page.getByRole('button',{name:'Novo lead',exact:true}).click();await page.getByLabel('Nome do contato').fill('Pessoa Exemplo');await page.getByLabel('E-mail',{exact:true}).fill('ui@example.com');await page.getByLabel('Telefone com DDD').fill('(11) 90000-1234');await page.getByRole('button',{name:'Cadastrar lead'}).click();
 const row=page.getByRole('row').filter({hasText:'ui@example.com'});await expect(row).toContainText('11900001234');
 await page.getByRole('button',{name:'Novo lead',exact:true}).click();await page.getByLabel('Nome do contato').fill('Duplicado');await page.getByLabel('E-mail',{exact:true}).fill('ui@example.com');await page.getByLabel('Telefone com DDD').fill('11900001234');await page.getByRole('button',{name:'Cadastrar lead'}).click();await expect(page.locator('#form-error')).toBeVisible();await page.getByRole('button',{name:'Cancelar',exact:true}).click();
 await row.getByRole('button',{name:'Converter',exact:true}).click();await page.getByLabel('Receita da conversão (R$)').fill('300');await page.getByRole('button',{name:'Confirmar conversão'}).click();await expect(row).toContainText('Convertido');await expect(row.getByRole('button',{name:'Converter',exact:true})).toHaveCount(0);
 await page.getByRole('button',{name:'Campanhas',exact:true}).click();const campaign=page.getByRole('row').filter({hasText:'Campanha de laboratório'});await expect(campaign).toContainText('200%');await expect(campaign).toContainText('100%');
});
test('ROI sem investimento é apresentado como indefinido',async({page,request})=>{
 await request.post('/api/campaigns',{data:{name:'Indicação sem custo',channel:'INDICACAO',cost:0}});await page.goto('/');await expect(page.getByRole('row').filter({hasText:'Indicação sem custo'})).toContainText('Sem custo');
});
