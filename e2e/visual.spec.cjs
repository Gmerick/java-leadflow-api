const {test,expect}=require('@playwright/test');
test('revisão visual desktop e celular',async({page,request})=>{
 for(const [name,channel,cost] of [['Novos clientes · Setembro','META',850],['Conteúdo e relacionamento','EMAIL',200],['Pesquisa de soluções','GOOGLE',1200]]){
 await request.post('/api/campaigns',{data:{name,channel,cost}});
 }
 await page.goto('/');await expect(page.locator('#workspace')).toHaveAttribute('aria-busy','false');await expect(page.locator('#error')).toBeHidden();
 await page.screenshot({path:'test-results/overview-desktop.png',fullPage:true});
 await page.setViewportSize({width:390,height:844});
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();
 await page.screenshot({path:'test-results/overview-mobile.png',fullPage:true});
});
