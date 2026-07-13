# Checklist de aceite do MVP

Legenda:

- `[x]` validado automaticamente ou já implementado;
- `[ ]` exige validação manual no ambiente real;
- `N/A` fora do MVP.

## Aplicação e segurança

- [x] aplicação compila com Java 21;
- [x] migrações Flyway são aplicadas em PostgreSQL real via Testcontainers;
- [x] login do proprietário funciona;
- [x] acesso não autenticado redireciona para login;
- [x] senha do proprietário não é versionada;
- [x] healthcheck da aplicação responde;
- [x] valores técnicos dos enums permanecem persistidos sem aparecer nos formulários principais;
- [x] selects e badges usam rótulos amigáveis em português;
- [x] API de rascunhos exige autenticação e CSRF;
- [x] rascunhos ficam isolados pelo proprietário autenticado;
- [ ] confirmar login pela URL HTTPS do Tailscale no celular;
- [ ] confirmar que a aplicação não é alcançável pela internet pública;
- [ ] confirmar dois dispositivos administrativos confiáveis na tailnet.

## Formulários guiados e rascunhos

- [x] gastos, fechamentos de km, metas e obrigações usam seções guiadas;
- [x] formulários simples de veículo e abertura do dia permanecem compactos;
- [x] desktop mantém todas as seções visíveis;
- [x] celular exibe uma etapa por vez quando existe navegação de etapas;
- [x] botão **Continuar** aguarda validação e sincronização do rascunho;
- [x] autosave usa atraso de 1,5 segundo após a última alteração;
- [x] rascunhos são persistidos no PostgreSQL e sincronizados entre dispositivos;
- [x] gasto admite somente o rascunho `current`;
- [x] fechamento admite um rascunho por veículo e mês;
- [x] meta admite um rascunho por mês;
- [x] obrigações admitem vários rascunhos e aparecem na listagem;
- [x] expiração ocorre após sete dias sem edição e é renovada em cada salvamento;
- [x] rotina diária remove rascunhos expirados de forma idempotente;
- [x] recuperação pergunta se deve continuar ou começar novamente;
- [x] conflito de versão retorna HTTP 409 e não sobrescreve silenciosamente;
- [x] substituição da versão do servidor exige ação explícita;
- [x] cópia local emergencial é usada somente quando a sincronização falha;
- [x] campos desconhecidos e payload maior que 64 KiB são rejeitados;
- [x] envio final recalcula valores derivados;
- [x] registro definitivo e exclusão do rascunho pertencem à mesma transação;
- [x] falha no envio definitivo preserva o rascunho;
- [x] formulário completo continua enviável sem JavaScript;
- [x] valores monetários aceitam vírgula e mostram `R$`;
- [x] percentuais são informados de 0 a 100;
- [x] competências usam mês e ano e distâncias exibem `km`;
- [x] foco visível, associação de erro e alvos móveis de 44 px estão presentes;
- [ ] validar larguras de 360 px e 412 px em celular Android real;
- [ ] validar largura mínima de 1280 px em desktop real;
- [ ] iniciar no computador e continuar o mesmo rascunho no celular;
- [ ] editar offline, reconectar e confirmar a sincronização;
- [ ] provocar conflito entre computador e celular e revisar as três ações;
- [ ] validar navegação somente por teclado e leitor de tela;
- [ ] desabilitar JavaScript no navegador e concluir um envio final.

## Veículos

- [x] cadastrar veículo;
- [x] editar veículo;
- [x] arquivar veículo;
- [x] manter histórico;
- [x] impedir mais de um veículo principal ativo;
- [x] registrar data e origem da leitura atual do odômetro;
- [x] impedir leitura cronologicamente nova menor que o odômetro atual;
- [x] preservar lançamentos históricos sem reduzir o odômetro atual.

## Dias e turnos

- [x] abrir dia operacional;
- [x] abrir turno;
- [x] usar Uber e 99 no mesmo turno;
- [x] calcular duração e distância;
- [x] impedir segundo turno aberto simultâneo;
- [x] impedir fechamento do dia com turno aberto;
- [x] fechar turno e dia;
- [x] atualizar o odômetro rastreável ao fechar turno ou dia;
- [ ] executar manualmente um dia com dois turnos pelo celular.

## Receitas

- [x] registrar valor líquido obrigatório;
- [x] registrar bruto e taxa opcional;
- [x] registrar bônus e gorjeta;
- [x] separar competência e recebimento;
- [x] evitar referência externa duplicada;
- [x] associar receita a plataforma e turno;
- [ ] conferir valores reais de um extrato Uber/99.

## Gastos e quilometragem

- [x] registrar gasto profissional;
- [x] registrar gasto pessoal;
- [x] registrar gasto misto;
- [x] mostrar somente os campos aplicáveis à classificação e ao método de rateio;
- [x] ratear por quilometragem;
- [x] permitir percentual ou valor fixo com justificativa;
- [x] calcular quilômetros pessoais por diferença;
- [x] impedir inconsistências de odômetro;
- [x] gerar prévia automática do fechamento mensal;
- [x] inferir odômetro inicial por fechamento anterior ou primeiro registro confiável;
- [x] inferir odômetro final pela leitura cronologicamente mais recente do mês;
- [x] somar apenas turnos fechados como quilometragem profissional;
- [x] mostrar origens, quantidades de registros, avisos e bloqueios;
- [x] impedir confirmação com dia ou turno aberto;
- [x] detectar regressão cronológica entre leituras do mês;
- [x] manter os valores calculados bloqueados por padrão;
- [x] exigir justificativa ao corrigir qualquer valor inferido;
- [x] persistir valores inferidos e confirmados para auditoria;
- [x] atualizar o odômetro do veículo após a confirmação;
- [ ] realizar fechamento mensal com odômetro e dados reais.

## Combustível

- [x] registrar abastecimento;
- [x] validar litros, preço e total;
- [x] calcular consumo somente entre tanques cheios;
- [x] estimar custo econômico do turno;
- [x] evitar dupla contagem entre desembolso e consumo;
- [x] atualizar o odômetro rastreável quando a leitura for atual;
- [ ] validar consumo com dois abastecimentos reais de tanque cheio.

## Metas

- [x] criar meta mensal;
- [x] cadastrar dias planejados;
- [x] excluir domingos por padrão;
- [x] calcular necessidade diária restante;
- [x] exibir projeção e status.

## Obrigações financeiras

- [x] criar empréstimo familiar flexível;
- [x] criar financiamento estruturado;
- [x] gerar cronograma;
- [x] registrar pagamento;
- [x] registrar amortização extraordinária;
- [x] recalcular saldo;
- [x] manter dívida separada do custo operacional;
- [ ] cadastrar os termos reais do acordo familiar.

## Anexos

- [x] aceitar JPEG, PNG e PDF;
- [x] rejeitar arquivo vazio, tipo inválido e tamanho excedente;
- [x] armazenar com nome interno aleatório;
- [x] gerar checksum SHA-256;
- [x] exigir autenticação para download;
- [ ] enviar e baixar comprovante real pelo celular.

## Dashboard

- [x] receita do dia;
- [x] margem operacional;
- [x] resultado econômico;
- [x] meta e necessidade restante;
- [x] horas e quilômetros;
- [x] receita por hora e quilômetro;
- [x] saldo devedor;
- [x] estados com turno ativo, dia aberto e nenhum dia;
- [x] ratear gasto misto usando o fechamento mensal confirmado;
- [ ] revisar responsividade em pelo menos um celular Android real.

## Infraestrutura e recuperação

- [x] Dockerfile multi-stage;
- [x] build automatizado de imagem `linux/arm64`;
- [x] Compose validado;
- [x] PostgreSQL sem porta publicada;
- [x] aplicação publicada somente em `127.0.0.1`;
- [x] limites de memória configurados;
- [x] backup Restic executado em CI;
- [x] restauração de banco e anexos executada em CI;
- [x] retenção diária/semanal/mensal configurada;
- [x] timer systemd fornecido;
- [ ] concluir teste destrutivo e SMART do SSD;
- [ ] concluir soak test de 24–48 horas no Raspberry;
- [ ] configurar remote Google Drive no rclone;
- [ ] executar backup no Google Drive;
- [ ] executar restauração isolada a partir do Google Drive;
- [ ] confirmar inicialização após reboot do Raspberry;
- [ ] confirmar acesso somente via Tailscale.

## Condição para produção pessoal

O código pode ser considerado MVP verificável quando a CI estiver verde. A operação real só deve começar depois que todos os itens manuais de infraestrutura, Tailscale, SSD e restauração externa forem marcados.
