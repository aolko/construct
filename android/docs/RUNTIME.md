# Runtime Android: estado e limites

## Referencias de arquitetura

O manual público do Construct descreve eventos como condições que selecionam instâncias e ações que operam sobre essas instâncias; subeventos dependem do resultado do evento pai. Layouts organizam a cena, camadas e a folha de eventos associada. Este projeto implementa um subconjunto independente desses conceitos, sem reutilizar código proprietário do Construct:

- [Construct 3: Events](https://www.construct.net/en/make-games/manuals/construct-3/project-primitives/events)
- [Construct 3: How events work](https://www.construct.net/en/make-games/manuals/construct-3/project-primitives/events/how-events-work)
- [Construct 3: Layouts](https://www.construct.net/en/make-games/manuals/construct-3/project-primitives/layouts)
- [Construct 3: Layers](https://www.construct.net/en/make-games/manuals/construct-3/project-primitives/layers)
- [Construct 3: Object instances](https://www.construct.net/en/make-games/manuals/construct-3/project-primitives/objects/instances)
- [Construct 3: Behaviors](https://www.construct.net/en/make-games/manuals/construct-3/project-primitives/objects/behaviors)
- Referências do Construct Classic neste repositório: `IDE/Structure/EventSheet.h`, `IDE/Structure/CLayout.h`, `IDE/Structure/CLayer.h`, `IDE/Structure/CObject.h` e `IDE/Structure/CAnimation.h`.

O runtime Android é Kotlin/JVM e independente da camada MFC do editor antigo. O `.cap` usa serialização binária `CArchive`; não é um projeto JSON do runtime Android.

## Recursos implementados nesta etapa

| Sistema | Estado verificado |
|---|---|
| Preview de cena/projeto em tela cheia | Implementado; usa os objetos, eventos, camadas e recursos do projeto aberto; não injeta controles/instâncias |
| Loop do runtime | Passo limitado a 50 ms, animações e eventos atualizados em cada frame |
| Eventos | `Sempre`, início da cena, intervalo periódico, tecla, toque na cena/objeto, comparação numérica, sobreposição retangular, `Senão`, repetição limitada, AND/OR e inversão |
| Ações | Movimento, posição, variáveis global/local/instância, rotação, criar instância a partir de modelo, remover instâncias, reproduzir áudio e chamada de função |
| Estrutura de eventos | Grupos, comentários, funções e subeventos |
| Expressões | Aritmética com `+`, `-`, `*`, `/`, parênteses e variáveis numéricas |
| Behaviors | 8 direções, plataforma/gravity simples, projétil, sólido, seguir câmera e limitar à cena |
| Animações | Sequência nomeada de imagens por objeto, duração por quadro, reordenação, duplicação, remoção e preview/reprodução |
| Câmera | Seguir objeto, zoom configurável (0,25–4x) e limites no retângulo da cena |
| Depuração | FPS, contagem de instâncias, variáveis globais, estado dos eventos e erro da última expressão/função |
| Assets | Importação de múltiplas imagens e áudios, renomeação e remoção no Project Bar; eventos podem reproduzir áudio importado |
| Interface Android | Orientação paisagem, Project Bar e painel de propriedades retráteis por alças, barra inferior recolhível por gesto |
| Persistência `.ccproj` | Objetos, propriedades, variáveis de instância, layers, comportamentos, quadros, eventos, variáveis e imagens Base64 serializados em JSON |
| Layouts independentes | Instâncias possuem `layoutId` e eventos possuem `eventSheetId`; o runtime filtra ambos pela cena ativa |

Arquivos `.ccproj` versionados com layouts independentes usam `formatVersion: 2`. Arquivos anteriores usam uma única cena migrada automaticamente para o layout 1 e a folha de eventos 1.

Os testes unitários de `EngineTest` cobrem movimentação direcional, eventos, expressões, entrada por toque e teclado, câmera, criação de instâncias e isolamento por cena. Os testes de transformação da cena cobrem enquadramento em retrato/paisagem e zoom por foco.

## Limitações conhecidas — não equivale ao Construct 2

- Física não usa Box2D: gravidade e colisões são uma aproximação retangular simples; não há massa, impulso, juntas, sensores nem resolução robusta de colisões.
- Colisões ignoram máscaras, polígonos e rotação. Sobreposição também usa retângulos sem rotação.
- A condição de toque em objeto usa coordenadas da cena e limites retangulares alinhados aos eixos; não considera rotação nem máscaras.
- Várias cenas possuem instâncias e folhas de eventos independentes; não há ainda transição entre layouts nem persistência de estado independente por cena durante o jogo.
- O preview aceita toque e teclado físico. Não insere controles virtuais nem objetos por padrão; não há suporte a gamepad.
- O editor de eventos implementa apenas as condições, ações e expressões listadas acima. A repetição é fixa (0–1000 vezes), sem laços condicionais; Else é avaliado contra o evento irmão anterior. Não há temporizadores nomeados nem parâmetros/retornos de funções.
- A edição de animações é uma sequência por objeto; não inclui pivôs, pontos de imagem, máscaras de colisão ou editor de imagem.
- Imagens são normalizadas para PNG e áudio é armazenado como dado Base64 no `.ccproj`; formatos de áudio reproduzíveis dependem do decodificador Android. Fontes ainda não foram portadas.
- Arquivos `.cap` válidos podem ser preservados sem perda como arquivos originais, mas o conteúdo binário MFC não é convertido em objetos/eventos editáveis. Não salvar projetos Android como `.cap`.
- A tela de depuração é observacional; não há pausa, step, breakpoint, árvore de instâncias ou edição ao vivo.

## Build e validação

Na raiz `android/`, execute:

```sh
./gradlew testDebugUnitTest assembleDebug
```

O APK debug gerado fica em `app/build/outputs/apk/debug/app-debug.apk`. Os testes são unitários JVM; eles não substituem validação em um dispositivo Android físico. Nenhum dispositivo/emulador conectado foi usado nesta validação.
