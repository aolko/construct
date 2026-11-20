# Auditoria da implementação Android

## Arquitetura observada

| Área | Implementação existente |
|---|---|
| Interface | Kotlin, Jetpack Compose e Material 3 em `MainActivity.kt` |
| Editor 2D | Compose `Canvas`, transformações próprias para zoom/pan, hit-testing e edição por toque |
| Runtime Android | Kotlin/JVM (`Engine.kt`), passo por frame dentro do preview Compose |
| JNI/C++ | `construct_core.cpp` reconhece o cabeçalho `Construct1.7` e copia bytes `.cap`; não implementa o runtime de jogo nem desserializa `CArchive` |
| Projeto editável | `.ccproj` em JSON; leia/escreva pelo Android Storage Access Framework |
| Recursos | Importação de múltiplas imagens e áudio; imagem é reamostrada e normalizada como PNG/Base64 |
| Integração Android | Activity Compose, configuração de tela paisagem, mídia em Base64 dentro do projeto |

## Classificação por recurso

| Recurso | Estado atual | Evidência/limite |
|---|---|---|
| Cena vazia | Funciona | Novo `EditorState` inicia sem instâncias |
| Inserção de Sprite e Text | Parcial | Instâncias editáveis; Sprite requer imagem importada para aparecer no runtime; Text tem conteúdo próprio |
| Cenas, camadas e folhas de eventos | Parcial | Instâncias, dimensões, camadas e eventos separados por ID; sem transição de cena em jogo |
| Zoom, pan, seleção, arraste e redimensionamento | Parcial | Implementados no Canvas; não foram exercitados em aparelho nesta execução |
| Painéis | Parcial | Project Bar/propriedades com alças laterais; scroll vertical próprio |
| Barra de ferramentas inferior | Parcial | Recolhe/expande por gesto em área dedicada |
| Importação de recursos | Parcial | Imagens/áudio múltiplos, renomeação e remoção; fontes e substituição ainda não suportadas |
| Animações | Parcial | Uma sequência nomeada por objeto, frames, tempo, reordenação, duplicação, remoção e playback |
| Eventos | Parcial | AND/OR, inversão, Else, repetição limitada, duplicação, grupos, subeventos, comentários, funções, teclado/toque, comparações, sobreposição, reprodução de áudio e ações listadas em `RUNTIME.md` |
| Behaviors | Parcial | Movimento/gravidade/colisão retangular simplificados; sem equivalência física ao Construct |
| Câmera | Parcial | Seguir instância, zoom e clamp retangular; sem múltiplas câmeras ou transições |
| Depuração | Parcial | FPS, contagem, variáveis, estados de condição e erros; sem pausa, step, breakpoint ou edição ao vivo |
| `.cap` | Preservação apenas | Reconhece cabeçalho e preserva bytes; não edita semanticamente projetos Classic |
| Plugins Classic/C2/C3 | Não portados em geral | Só o subconjunto indicado em `plugins-meta/plugins.json`; plugins desconhecidos não são executados |
| Physics e transições | Não implementados completamente | Física robusta e transições de cena ainda não estão disponíveis |
| Áudio | Parcial | Importação e reprodução por evento; não há editor de áudio, mixagem ou controles avançados |

## Referência de versões e decisões

O código original do Construct Classic neste repositório usa estruturas Windows/MFC e serialização
`CArchive` (por exemplo, `IDE/Structure/EventSheet.h`, `CLayout.h`, `CLayer.h`, `CObject.h` e
`CAnimation.h`). Isso difere do armazenamento `.ccproj` JSON do app e não pode ser tratado como
compatível por renomear extensões.

O manual público atual do Construct 3 documenta layouts associados a folhas de eventos, layers
associados a layouts e eventos como filtragem de instâncias; comportamentos estendem tipos de
objeto. O runtime Android adota esses conceitos, mas ainda não possui o sistema de picking de
instâncias do Construct nem a amplitude de plugins/ações do C3. A rota pública do manual do
Construct 2 consultada durante esta tarefa retornou 404; por isso não se afirma compatibilidade
detalhada com o formato/runtime do C2.

Referências:

- [Construct 3: Events](https://www.construct.net/en/make-games/manuals/construct-3/project-primitives/events)
- [Construct 3: How events work](https://www.construct.net/en/make-games/manuals/construct-3/project-primitives/events/how-events-work)
- [Construct 3: Layouts](https://www.construct.net/en/make-games/manuals/construct-3/project-primitives/layouts)
- [Construct 3: Layers](https://www.construct.net/en/make-games/manuals/construct-3/project-primitives/layers)
- [Construct 3: Object instances](https://www.construct.net/en/make-games/manuals/construct-3/project-primitives/objects/instances)
- [Construct 3: Behaviors](https://www.construct.net/en/make-games/manuals/construct-3/project-primitives/objects/behaviors)

Nenhum código do Construct 2/3 foi copiado. As implementações Android são independentes.

## Validação desta etapa

`./gradlew testDebugUnitTest assembleDebug` compila e executa testes JVM; esses testes não validam
gestos, rolagem, orientação ou renderização em dispositivo. `adb` não está disponível no ambiente,
portanto os testes físicos solicitados permanecem pendentes.
