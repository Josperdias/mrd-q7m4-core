# Meridian · Estudo com direção

Agenda de provas, laboratório de documentos e memória de aprendizagem em um aplicativo que funciona com dados locais. Versão 1.0.0, preparada em 8 de outubro de 2026.

## Baixar o aplicativo Android

**[Baixar o APK mais recente](https://github.com/Josperdias/mrd-q7m4-core/releases/tag/apk-latest)**: na página, toque em `Meridian-1.0.N.apk` (a versão vem no nome do arquivo) e confira o SHA-256 na descrição.

O arquivo é gerado pelo GitHub Actions a cada atualização da `main` e assinado sempre com a mesma chave. Para instalar, permita instalar apps de fontes desconhecidas no navegador usado no download. Exporte um backup antes de desinstalar uma versão anterior.

## Comece em cinco minutos

1. Abra **Meus objetivos** e registre sua situação de inscrição em cada seleção. O app não presume que você esteja inscrito.
2. Ajuste os minutos disponíveis por dia em **Preferências**. A distribuição inicial é apenas uma sugestão.
3. Abra **Plano de estudos**, gere a semana e comece uma sessão de foco.
4. Responda algumas questões. Marque hesitação quando acertar sem segurança: isso também alimenta o caderno de erros.
5. Exporte um backup em **Preferências**. APK, navegadores e aparelhos guardam bases separadas; para transferir seu trabalho, exporte e restaure o arquivo JSON.

## O que está pronto

- Agenda com 41 eventos, fontes, horários de Brasília, filtros, eventos pessoais e exportação `.ics`.
- Quatro objetivos: Enem 2026, Vestibular UnB 2027, TCDF 2026 e TCE-GO 2026/2027.
- Aviso de conflito em 22/11: UnB e TCDF. O horário exato do TCDF ainda precisa da convocação.
- Plano semanal explicável, com disponibilidade, proximidade da prova, revisões, erros e matérias compartilhadas.
- 41 tópicos iniciais, 36 questões autorais de fundamentos e seis miniguias. O mapa inicial não substitui a leitura integral de cada conteúdo programático.
- Treino comentado, simulado com tempo, opção de penalização, importação CSV, caderno de erros e revisões espaçadas.
- 110 referências de estudo, incluindo 103 links de PDFs oficiais. O coletor encontrou 102 PDFs; isso é localização de documentos, não revisão integral de seu conteúdo.
- Três editais consolidados incluídos no pacote para leitura offline: TCDF, UnB e TCE-GO.
- Laboratório PDF/TXT/MD: extração por página, busca, notas, OCR local em português, comparação de versões, datas candidatas e criação de cartões/questões com revisão humana.
- Oficina de escrita com rascunho automático e histórico de versões, sem inventar nota de redação.
- Memória em Markdown para levar a outra conversa de IA; backup completo em JSON, tema escuro e interface responsiva.

## Datas centrais

| Objetivo | Provas | Horário de Brasília | Situação em 08/10/2026 |
| --- | --- | --- | --- |
| Enem 2026 | 08 e 15/11/2026 | 13h30–19h; 13h30–18h30 | Inscrições encerradas; conferir situação pessoal |
| UnB, Vestibular 2027 | 21 e 22/11/2026 | 13h–18h, nos dois dias | Inscrições encerradas; pagamento até 15/10, conforme retificações |
| TCDF, Analista Administrativo | 22/11/2026 | Objetiva pela manhã; discursiva à tarde; 4h cada | Exige nível superior; horários exatos na convocação |
| TCE-GO, Técnico de Controle Externo | 17/01/2027 | Turno e horários a confirmar | Ensino médio; inscrição de 05/10 a 06/11/2026 |

O TCE-GO foi adotado como provável referência ao tribunal em Goiânia. É diferente do TCM-GO. Cargos, requisitos e datas precisam ser conferidos no edital e nas retificações antes de qualquer inscrição. A agenda é datada e não confirma automaticamente novas publicações.

## Instalar no Android

Baixe `Meridian-1.0.0.apk`, abra o arquivo e autorize a instalação por esse aplicativo quando o Android solicitar. Compatível em projeto com Android 8 ou superior; mantenha o Android System WebView atualizado. O APK contém a interface, os motores PDF/OCR e os três editais, sem precisar entrar no site.

Esta distribuição usa assinatura de desenvolvimento e não é uma publicação na Play Store. O APK foi compilado e sua assinatura foi validada; não houve teste em aparelho físico. Exporte seu backup antes de desinstalar ou trocar por um pacote com outra assinatura.

## Abrir a versão web

O conteúdo de `dist/` é um site estático. Para uso local com todos os recursos:

```bash
python3 -m http.server 8080 --directory dist
```

Abra `http://localhost:8080`. Servir por HTTPS ou localhost é necessário para os recursos de segurança e instalação. Abrir `index.html` diretamente como `file://` não oferece funcionamento completo dos módulos, OCR e cache. No site hospedado, a primeira preparação offline requer conexão e espaço para os arquivos; o APK já os inclui.

## Laboratório e limites práticos

Em **Acervo**, os editais marcados como incluídos offline podem ser enviados ao laboratório pelo botão **Analisar**. Outros PDFs podem impedir leitura direta entre sites; abra a fonte, baixe o arquivo e importe-o pelo seletor.

- Até 20 MB por documento. Texto inicial extraído de até 150 páginas; páginas adicionais podem ser abertas e submetidas a OCR individualmente.
- OCR trabalha uma página por vez. Texto reconhecido e datas detectadas precisam ser conferidos no original.
- Comparação de versões é lexical: imagens, tabelas e mudança de diagramação podem exigir leitura manual.
- Backup completo aceita até 60 MB de documentos binários, antes da expansão base64. Divida acervos grandes.
- O cronômetro mede a sessão; não promete alarme com o aplicativo encerrado.
- O plano usa heurísticas transparentes. Não estima probabilidade de aprovação, não calcula TRI e não usa modelo de linguagem escondido.
- Não há sincronização automática com Drive, ChatGPT, Claude ou entre aparelhos. As exportações são intencionais e feitas por você.

## Código e automação de compilação

`dist/` é a aplicação; `android/` contém o invólucro WebView; `scripts/` reúne empacotamento, compilação e coleta. Não há servidor de dados pessoais.

```bash
node --test tests/core.test.cjs
npm install
npm run test:ui
npm run test:documents
node scripts/package.mjs
```

Para Android, instale JDK 17, Android SDK Platform 35 e Build Tools 35.0.0. Defina `ANDROID_SDK_ROOT` e execute:

```bash
bash scripts/build-android.sh
```

O resultado fica em `android/build/Meridian-1.0.0.apk`. A chave gerada fica fora do Git. Para manter a identidade de atualização, preserve privadamente `android/build/development.keystore`; a primeira compilação em outra máquina gera uma chave diferente.

Há workflows prontos em `.github/workflows/`: compilação Android por push/manual e coleta manual do catálogo. A presença desses arquivos não significa que o GitHub Actions já tenha sido executado. Para importar o projeto, envie o conteúdo descompactado, incluindo `.github/`, ao seu repositório.

```bash
python3 scripts/collector.py --output catalog-collected.json
```

Importe esse JSON em **Preferências → Importar catálogo**. O coletor não altera as datas verificadas nem publica atualizações sozinho. Ele consulta fontes públicas permitidas, limita tamanho/ritmo, observa `robots.txt` e registra falhas sem contornar bloqueios. Nesta coleta, a FCC ficou indisponível para coleta automática; seu edital foi conferido separadamente.

## Privacidade e fontes

Progresso fica em `localStorage`; documentos ficam em IndexedDB. Limpar dados do aplicativo/navegador elimina essas bases, por isso o backup é parte do fluxo. Nenhuma conversa privada, documento pessoal, token ou chave de assinatura está incluído no código público.

Um nome de repositório discreto e `noindex` não tornam um repositório público secreto. Mantenha backups de estudo e a chave Android fora dele.

Veja [FONTES_E_TECNOLOGIA.md](FONTES_E_TECNOLOGIA.md) para proveniência, componentes e critérios de verificação.
