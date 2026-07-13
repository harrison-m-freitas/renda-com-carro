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
- [ ] confirmar login pela URL HTTPS do Tailscale no celular;
- [ ] confirmar que a aplicação não é alcançável pela internet pública;
- [ ] confirmar dois dispositivos administrativos confiáveis na tailnet.

## Veículos

- [x] cadastrar veículo;
- [x] editar veículo;
- [x] arquivar veículo;
- [x] manter histórico;
- [x] impedir mais de um veículo principal ativo.

## Dias e turnos

- [x] abrir dia operacional;
- [x] abrir turno;
- [x] usar Uber e 99 no mesmo turno;
- [x] calcular duração e distância;
- [x] impedir segundo turno aberto simultâneo;
- [x] impedir fechamento do dia com turno aberto;
- [x] fechar turno e dia;
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
- [x] ratear por quilometragem;
- [x] permitir percentual ou valor fixo com justificativa;
- [x] calcular quilômetros pessoais por diferença;
- [x] impedir inconsistências de odômetro;
- [ ] realizar fechamento mensal com odômetro real.

## Combustível

- [x] registrar abastecimento;
- [x] validar litros, preço e total;
- [x] calcular consumo somente entre tanques cheios;
- [x] estimar custo econômico do turno;
- [x] evitar dupla contagem entre desembolso e consumo;
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
