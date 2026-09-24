# Roteiro de apresentação

## Preparação

Execute `mvn clean verify` e inicie o JAR. Abra `LeadService.report`, `normalizePhone`, `LeadRepository.convert`, schema.sql e LeadApiTest na IDE. Deixe `python scripts/demo.py` pronto em outro terminal.

## Demonstração de 5 minutos

| Tempo | O que mostrar |
| --- | --- |
| 0–1 min | Problema: duplicidades distorcem volumes e receita de campanhas |
| 1–2 min | Execute o demo: campanha 100, dois leads e conversão 300 |
| 2–3 min | Erros 409 de duplicidade e reconversão; relatório 50% / 50 / 200% |
| 3–4 min | `normalizePhone`, constraints UNIQUE e UPDATE condicionado |
| 4–5 min | `report`: groupingBy, BigDecimal e tratamento de divisões por zero |

Para 10 minutos, crie uma campanha de custo zero sem leads: CPL e ROI são null. Mostre o SQL equivalente em `examples/queries.sql` e explique quando migraria as agregações para o banco.

## Fala de apoio — adapte após estudar

“Este projeto conecta Java à organização de leads e análise de campanhas. Ele cadastra campanhas, valida contatos e impede duplicidades no banco. Cada lead só pode ser convertido uma vez, para não somar a receita novamente. Calculei conversão, custo por lead e ROI usando Collections e BigDecimal. É um modelo didático: usa dados fictícios e não está conectado a uma plataforma real.”

## Perguntas prováveis

**Por que BigDecimal?** Dinheiro precisa de precisão decimal e arredondamento explícito. double pode introduzir aproximações binárias.

**Por que validar na aplicação e usar UNIQUE?** A validação prepara e explica o dado; UNIQUE protege inclusive duas requisições concorrentes. Um SELECT antes do INSERT, sozinho, não garante unicidade.

**Por que e-mail e telefone únicos globalmente?** É a regra simplificada escolhida: um contato entra uma vez e pertence a uma campanha. Em marketing real, atribuição multicanal exigiria separar contatos e participações em campanhas.

**Por que ROI null quando custo é zero?** A divisão por zero não é definida. Dizer 0% seria uma conclusão incorreta. Sem leads, CPL também é null.

**Por que agrupar em Java?** Para exercitar Map, List e Streams. Em bases grandes, eu usaria GROUP BY no banco e retornaria só os agregados.

**Normalizar é validar a pessoa?** Não. Só padroniza o formato. Não comprova que o número existe nem que pertence a alguém.

**Como evita contar duas conversões?** `UPDATE ... WHERE id=? AND status='NEW'`. Apenas a primeira atualização tem sucesso; as demais retornam conflito e não mudam a receita.

## Exercício de domínio

Implemente filtro por canal no relatório. Teste duas campanhas de canais diferentes e uma consulta sem resultados. Explique como faria a mesma operação usando SQL.
