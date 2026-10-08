# Fontes, implementação e verificação

## Agenda oficial

Conferência em 8 de outubro de 2026. Cada evento de `dist/data.js` guarda a fonte e a data da conferência. Eventos condicionais são identificados como tal; inscrições individuais começam com situação desconhecida.

- **TCDF:** [Cebraspe](https://www.cebraspe.org.br/concursos/TC_DF_26_ANALISTA), edital consolidado e API pública do evento `TC_DF_26_ANALISTA`.
- **UnB:** [Vestibular 2027](https://www.cebraspe.org.br/vestibulares/VESTUNB_27), edital consolidado, cronograma e retificações até o edital 6. “Vestibular 2027” identifica o processo de ingresso; as provas ocorrem em 2026.
- **Enem:** [Inep](https://www.gov.br/inep/pt-br/areas-de-atuacao/avaliacao-e-exames-educacionais/enem), notícias oficiais da aplicação 2026, editais e [arquivo de provas e gabaritos 2025](https://www.gov.br/inep/pt-br/areas-de-atuacao/avaliacao-e-exames-educacionais/enem/provas-e-gabaritos/2025).
- **TCE-GO:** [FCC](https://www.concursosfcc.com.br/concursos/tcego125/index.html), edital de abertura consolidado com retificações. Cargo Técnico de Controle Externo, Administração ou Tecnologia da Informação.

Não foi encontrada necessidade de uma API comercial para o pacote inicial. As APIs públicas do Cebraspe, os portais oficiais e os arquivos publicados dão origem ao catálogo. Disponibilidade e estrutura desses endpoints podem mudar. Materiais de cursos pagos não foram coletados.

## O que foi coletado

`dist/catalog.json` preserva relatórios das fontes, hashes de respostas e indicação de conteúdo a conferir. Foram localizadas 102 referências PDF na execução: quatro do TCDF, oito da UnB e 90 do arquivo Enem 2025. A fusão com a seleção conferida, removendo duplicatas por URL, resultou em 110 referências, das quais 103 apontam diretamente a PDFs.

Os três editais incluídos em `dist/resources/` têm URL original, tamanho e SHA-256 em `provenance.json`. Os PDFs do Enem permanecem referenciados por URL: o servidor de download retornou erro 502 durante a tentativa de obtenção. Não são apresentados como baixados ou revisados integralmente.

## Componentes

| Componente | Versão | Função e origem |
| --- | --- | --- |
| PDF.js | 6.4.299, legacy build | Extração e renderização. [Mozilla](https://mozilla.github.io/pdf.js/), pacote oficial `pdfjs-dist` |
| Tesseract.js | 7.0.0 | OCR local. [Projeto oficial](https://github.com/naptha/tesseract.js) |
| Tesseract.js core | 7.0.0, LSTM WASM | Motor de reconhecimento, versão compatível com o wrapper |
| Dados de português | `por`, tessdata 4.0.0 | Modelo de idioma disponibilizado pelo projeto Tesseract.js |
| JavaScript/HTML/CSS | Sem framework | Interface, estado e heurísticas executados no aparelho |
| IndexedDB / localStorage | APIs do navegador | Documentos binários e registros de estudo |
| WebView Android | SDK alvo 35; mínimo 26 | APK local, seletor de documentos e exportação pelo sistema |
| Python | Biblioteca padrão | Coletor de metadados, sem login, sem solver de bloqueios |

PDF.js, Tesseract.js e seu core usam Apache-2.0; os avisos e licenças dos componentes distribuídos acompanham `dist/vendor/`. Fontes e CMaps incluem seus próprios avisos. Os PDFs oficiais conservam autoria e direitos de seus emissores e de conteúdos de terceiros. As questões e os miniguias identificados como autorais não são questões oficiais.

O tarball PDF.js usado tem SHA-256 `86269b40170eb41740ea05ad2102d71d33de4f122b34eb2f478be0ea7779c415`. Origem: `https://registry.npmjs.org/pdfjs-dist/-/pdfjs-dist-6.4.299.tgz`.

## Decisões de aprendizagem

O plano combina urgência, estágio autodeclarado, revisão vencida, erros e compartilhamento entre objetivos; a interface mostra os motivos da prioridade. A distribuição respeita o orçamento de minutos e evita os dias de prova.

Revisão espaçada é uma heurística simples, não uma implementação de FSRS ou validação científica de domínio individual. Falha reduz o intervalo; hesitação mantém retorno próximo; acerto seguro permite intervalos maiores. Uma única resposta correta não prova domínio.

A memória entre IAs exporta objetivo, tópicos, evidências e próximos passos em Markdown. O usuário decide quando e com quem compartilhar. OCR e comparação documental são processamento local, sem inferência remota, cobrança de API ou upload automático.

## Verificação executada

- 14 testes do núcleo: datas no fuso de Brasília, calendário RFC 5545, colisões, intervalos de revisão, pontuação, orçamento de estudo, busca por páginas, datas impossíveis, CSV e validação de backup.
- Integração DOM das 14 telas: criação/revisão de cartões, plano, registro de sessão, resposta de questão, evento e recarga de estado persistido.
- PDF.js: abertura de edital oficial de 27 páginas, extração de texto e renderização da primeira página.
- OCR: motor WASM e dados `por` incluídos reconheceram uma imagem sintética em português, com confiança interna 95. Essa medida do motor não é taxa de acerto em documentos reais.
- APK: compilação Java/DEX, empacotamento, alinhamento e verificação da assinatura Android.

Não houve teste visual em navegador, emulador ou aparelho físico. Os testes DOM não substituem esse teste. O APK é uma entrega de desenvolvimento; notificações em segundo plano, sincronização em nuvem, revisão semântica por IA e publicação na Play Store não fazem parte desta versão.
