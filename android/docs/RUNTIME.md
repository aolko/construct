# Runtime Android: estado e limites

## Referencias de arquitetura

O manual público do Construct descreve eventos como condições que selecionam instâncias e ações que operam sobre essas instâncias; subeventos dependem do resultado do evento pai. Layouts organizam a cena, camadas e a folha de eventos associada. Este projeto implementa um subconjunto independente desses conceitos, sem reutilizar código proprietário do Construct:

- [Construct 3: Events](https://www.construct.net/en/make-games/manuals/construct-3/project-primitives/events)
- [Construct 3: Layouts](https://www.construct.net/en/make-games/manuals/construct-3/project-primitives/layouts)
- Referências do Construct Classic neste repositório: `IDE/Structure/EventSheet.h`, `IDE/Structure/CLayout.h`, `IDE/Structure/CLayer.h`, `IDE/Structure/CObject.h` e `IDE/Structure/CAnimation.h`.

O runtime Android é Kotlin/JVM e independente da camada MFC do editor antigo. O `.cap` usa serialização binária `CArchive`; não é um projeto JSON do runtime Android.

## Recursos implementados nesta etapa

| Sistema | Estado verificado |
|---|---|
| Preview de cena/projeto em tela cheia, com controles de toque | Implementado; usa os objetos, eventos, camadas e recursos do projeto aberto |
| Loop do runtime | Passo limitado a 50 ms, animações e eventos atualizados em cada frame |
| Eventos | Condições `Sempre`, início da cena, tecla pressionada, comparação numérica e sobreposição retangular |
| Ações | Movimento, posição, variável global/local, rotação, remoção de instâncias e chamada de função |
| Estrutura de eventos | Grupos, comentários, funções e subeventos |
| Expressões | Aritmética com `+`, `-`, `*`, `/`, parênteses e variáveis numéricas |
| Behaviors | 8 direções, plataforma/gravity simples, projétil, sólido, seguir câmera e limitar à cena |
| Animações | Sequência de imagens por objeto, duração por quadro, reordenação e remoção; runtime troca os quadros |
| Depuração | Contagem de instâncias, variáveis globais e erro da última expressão/função |
| Persistência `.ccproj` | Objetos, propriedades, layers, comportamentos, quadros, eventos, variáveis e imagens Base64 serializados em JSON |
| Layouts independentes | Instâncias possuem `layoutId` e eventos possuem `eventSheetId`; o runtime filtra ambos pela cena ativa |

Arquivos `.ccproj` versionados com layouts independentes usam `formatVersion: 2`. Arquivos anteriores usam uma única cena migrada automaticamente para o layout 1 e a folha de eventos 1.

Os testes unitários de `EngineTest` cobrem movimentação direcional, execução de condição/expressão e evento de início único. Os testes de transformação da cena cobrem enquadramento em retrato/paisagem e zoom por foco.

## Limitações conhecidas — não equivale ao Construct 2

- Física não usa Box2D: gravidade e colisões são uma aproximação retangular simples; não há massa, impulso, juntas, sensores nem resolução robusta de colisões.
- Colisões ignoram máscaras, polígonos e rotação. Sobreposição também usa retângulos sem rotação.
- Várias cenas possuem instâncias e folhas de eventos independentes, mas ainda não há transição completa entre layouts nem persistência de estado independente por cena durante o jogo.
- O editor de eventos implementa apenas as condições, ações e expressões listadas acima. Plugins, behaviors e recursos do Construct que não estejam implementados não são executados.
- Controles disponíveis no preview são direcionais e pular; teclado, gamepad, áudio, transições e depurador com inspeção/pausa/step ainda não foram implementados.
- A edição de animações é uma sequência de quadros; não inclui pivôs, pontos de imagem, máscaras de colisão ou editor de imagem.
- Arquivos `.cap` válidos podem ser preservados sem perda como arquivos originais, mas o conteúdo binário MFC não é convertido em objetos/eventos editáveis. Não salvar projetos Android como `.cap`.
- A tela de depuração é observacional básica; não promete paridade com o debugger do Construct.

## Build e validação

Na raiz `android/`, execute:

```sh
./gradlew testDebugUnitTest assembleDebug
```

O APK debug gerado fica em `app/build/outputs/apk/debug/app-debug.apk`. Os testes são unitários JVM; eles não substituem validação em um dispositivo Android físico. Nenhum dispositivo/emulador conectado foi usado nesta validação.
