# Extel Comando 4 Botões

Um único comando Extel de **quatro botões** passa a controlar dois automatismos de marcas diferentes:

| Botão | Automatismo | Função (predefinida) | Quem atua |
|-------|-------------|----------------------|-----------|
| 1 | Portão de batente Extel (2 folhas) | Abrir **uma folha** (pedonal) | Recetor Extel (nativo) |
| 2 | Portão de batente Extel (2 folhas) | Abrir **duas folhas** | Recetor Extel (nativo) |
| 3 | **Portão seccionado, motor DEA Spazio** | **Abrir** (volta a premir = parar) | SmartThings → relé → central DEA |
| 4 | **Portão seccionado, motor DEA Spazio** | **Fechar** (volta a premir = parar) | SmartThings → relé → central DEA |

A novidade é o seccionado. A central DEA só reconhece os comandos rádio DEA, por isso não aprende o
comando Extel. Esta SmartApp faz a ponte: os botões 3 e 4 são captados por um recetor de 433,92 MHz
ligado ao SmartThings e convertidos em impulsos de contacto seco nas entradas da central DEA.

Ficheiros:

- `smartapps/sergiommms/extel-comando-4-botoes.src/extel-comando-4-botoes.groovy`: SmartApp (mapeamento dos botões, lógica do seccionado)
- `devicetypes/sergiommms/dea-spazio-portao-seccionado.src/`: dispositivo-filho do seccionado (abrir, fechar, parar, parcial, passo-a-passo)
- `devicetypes/sergiommms/extel-portao-batente.src/`: dispositivo-filho do batente (uma folha / duas folhas, estado)

## Esquema

```
                       433,92 MHz
  Comando Extel  ─────────┬──────────────────────────────►  Recetor / central Extel  ──►  Batente (botões 1, 2)
  (4 botões)              │
                          ▼
                Ponte / recetor 433 MHz          (eventos "button" 1..4)
                ligado ao SmartThings  ───────────────────────►  SmartApp "Extel Comando 4 Botoes"
                                                                       │
                                                                       ▼  impulso (contacto seco NA)
                                                         Módulo de relé Z-Wave / Zigbee
                                                                       │
                                                                       ▼
                                                 Central DEA Spazio: START (passo-a-passo)
                                                 [opcional: PED, ABRIR, FECHAR, STOP]
                                                                       │
                                                                       ▼
                                                              Portão seccionado
  Opcional: sensor magnético "fechado" (e/ou "aberto") no seccionado  ──►  estado real na app
```

## Hardware

1. **Comando Extel de 4 botões.** Os botões 1 e 2 ficam memorizados no recetor Extel, como já estão.
   Os botões 3 e 4 **não** são memorizados no recetor Extel.
2. **Recetor de 433,92 MHz ligado ao SmartThings** que entregue os botões como eventos `button` com
   `buttonNumber` (por ex. uma ponte RF com os quatro códigos aprendidos, ou um recetor por botão).
   - Se o comando Extel for de **código fixo** (aprendido), qualquer ponte RF 433 MHz consegue aprendê-lo.
   - Se for de **código rotativo** (rolling code), a ponte genérica não reconhece toques seguidos. Nesse
     caso use um recetor externo compatível com o comando Extel, com saídas de relé, e ligue essas saídas
     a um módulo de entradas de contacto seco que gere eventos `button` (ou ligue-as diretamente à central
     DEA; ver "Alternativa sem SmartThings").
3. **Módulo de relé de contacto seco** (Z-Wave/Zigbee, alimentação compatível com o local) ligado aos
   bornes da central DEA:
   - **START / passo-a-passo**: obrigatório.
   - **PED** (abertura parcial), **ABRIR**, **FECHAR**: opcionais, se a central do Spazio os tiver.
   - **STOP**: opcional. A entrada STOP da DEA é normalmente fechada (NF), por isso use o contacto NF do
     relé ou deixe-a como está e use só o START.
   Confirme no manual da central do Spazio o nome e a posição dos bornes (o borne comum é normalmente
   partilhado com as fotocélulas e as botoneiras).
4. **Sensores de fim de curso** (recomendados): um sensor magnético que fecha com o portão em baixo
   (`fechado`) e, se quiser, outro com o portão em cima (`aberto`).

## Configuração da SmartApp

1. Publicar os dois device handlers (`DEA Spazio Portao Seccionado`, `Extel Portao Batente`) e a SmartApp
   no IDE do SmartThings, com o namespace `sergiommms`.
2. Instalar a SmartApp e escolher:
   - **Comando Extel**: o dispositivo de botões (ou um dispositivo por botão).
   - **O que faz cada botão**: por omissão 1 e 2 = "Extel nativo", 3 = abrir seccionado, 4 = fechar
     seccionado. Também há "alternar", "passo-a-passo", "parar" e "abertura parcial", e pode atribuir
     funções diferentes a "manter premido".
   - **Portão seccionado DEA Spazio**: os relés, a **sequência do passo-a-passo programada na central**
     (`ABRE-STOP-FECHA-STOP` ou `ABRE-FECHA`), o tempo de curso e os sensores.
   - **Portão de batente Extel** (opcional): relés nas entradas de botoeira da central Extel para abrir
     o batente a partir da app, e um sensor de contacto para mostrar o estado.
3. Ficam dois dispositivos na app: **Portão seccionado** e **Portão de batente**. Pode usá-los em cenas,
   rotinas, Alexa/Google, etc.

### Como funciona o seccionado com uma só entrada (START)

A central DEA percorre uma sequência fixa a cada impulso. A SmartApp segue essa sequência e envia o
número de impulsos necessário, com o intervalo configurado entre eles:

| Estado atual | Pedido | `ABRE-STOP-FECHA-STOP` | `ABRE-FECHA` |
|--------------|--------|------------------------|--------------|
| fechado | abrir | 1 impulso | 1 impulso |
| a abrir | fechar | 2 impulsos (para, fecha) | 1 impulso (inverte) |
| a abrir | parar | 1 impulso | só com relé STOP |
| parado a meio (depois de abrir) | fechar | 1 impulso | 1 impulso |
| aberto | fechar | 1 impulso | 1 impulso |
| fechado | fechar | nada | nada |

Se estiverem configurados relés ABRIR, FECHAR ou STOP, são usados diretamente nesses comandos.

As pontes RF repetem o código várias vezes por cada toque. Numa central passo-a-passo isso daria
abrir-parar-fechar, por isso as repetições do mesmo botão dentro de 1,5 s são ignoradas (valor
configurável).

Sem sensores, o estado aberto/fechado é estimado pelo tempo de curso. Com o sensor `fechado`, um fecho só
é confirmado quando o sensor fecha. Se o tempo acabar sem confirmação, o portão fica como "parado" (por
ex. inversão pela fotocélula). O sensor também deteta aberturas feitas sem passar pela SmartApp (comando
DEA original, botoneira).

## Segurança

- O fecho à distância de um portão seccionado só deve ser usado com as **fotocélulas e a deteção de
  obstáculos da central DEA ativas e testadas**. A SmartApp não substitui nenhum dispositivo de segurança.
- Não acione por relé um botão que o recetor Extel já tenha memorizado ("Extel nativo"): o portão
  receberia duas ordens seguidas e poderia parar ou inverter.
- As ligações na central devem ser feitas com a alimentação desligada e segundo o manual do fabricante.

## Alternativa sem SmartThings

Se só quiser os botões 3 e 4 a atuar o Spazio, sem app nem estado: um recetor externo de 2 canais
compatível com o comando Extel, com os contactos NA ligados diretamente aos bornes START (botão 3) e
PED ou START (botão 4) da central DEA. A SmartApp acrescenta o controlo pela app, o estado do portão,
os comandos abrir/fechar explícitos sobre uma única entrada passo-a-passo, as notificações e o aviso de
portão aberto.
