# Dois portões, um comando: batente Extel Vera / Hello V e seccionado DEA Spazio 703S

Um comando de quatro botões serve dois portões de marcas diferentes:

- **Batente Extel Vera / Hello V**, placa de comando **ATV 3**, comando original **WEATEM 5** (botões A, B, C, D).
- **Seccionado DEA Spazio 703S**, central **124RRZ**, comandos originais **GTI2 / GTI4**.

O critério é **manter o rolling code completo nas duas pontas**. Cada comando é recebido por um recetor do seu
próprio fabricante, que é o único capaz de verificar a parte cifrada, e a passagem entre marcas faz-se por fio, com
contacto seco. Não se usa o modo "HCS só parte fixa" da central DEA: a parte fixa pode ser copiada por um "code
grabber", o que anularia a segurança do rolling code.

Há duas montagens, independentes e cumuláveis:

| Montagem | Comando principal | Hardware a acrescentar | Compra |
|---|---|---|---|
| **A** | DEA **GTI4** | recetor externo DEA **291/X** junto à placa Extel | à venda |
| **B** | Extel **WEATEM 5** | recetor externo Extel **TRANSMY** (ATRC 3) junto à central DEA | descontinuado, só em segunda mão |

**Recomendação:** quem já tem comandos DEA GTI4 (ou GTI2) usa a **montagem A**: só falta comprar o 291/X. A
montagem B só se justifica se o comando do dia a dia tiver de ser o WEATEM 5 e se encontrar um TRANSMY usado.

Em qualquer das montagens, os dois portões podem ainda aparecer no Home Assistant, com estado, através de um ESP32
(ESPHome) ou de módulos **Shelly Uni**. E um Shelly pode substituir o módulo de iluminação Extel.

Fontes: manual Extel VERA (CFI EXTEL 05/2011 V2, placa ATV 3), manual Extel TRANSMY, manual DEA "Spazio 702S/703S"
(central 124RRZ, Tabelas 1 e 2), manual DEA 291X/433 (I75003 Rev 00, 01/2025).

## Placa Extel ATV 3 (Vera / Hello V)

Régua de bornes, da esquerda para a direita: **ANT, ⏚, PED, STP, ⏚, STR, PHO, LED, VA, TXF, PH1, S1, ⏚, LAMP,
MOT2, MOT1**, depois BAT e o secundário do transformador.

| Função | Bornes | Tipo |
|---|---|---|
| Abertura **total** (duas folhas) | **STR** e **⏚** (massa) | contacto seco N.A. |
| Abertura **parcial** (uma folha) | **PED** e **⏚** | contacto seco N.A. |
| Paragem de emergência | **STP** e **⏚** | N.F.; vem com ponte, só se retira se houver botão |
| Alimentação de acessórios | **VA** e **⏚** | **12 V**, fusível «acessórios» de 3 A |
| Sinal para iluminação | **S1** | ativo com o portão em movimento (ia ao TRIG do WEATCR 2) |

- **JP1 retirado:** botões **A** (total) e **B** (parcial). **JP1 colocado:** C e D. Mudar o JP1 não obriga a
  reprogramar. Para a montagem B, o JP1 fica **retirado**, de modo a deixar C e D livres.
- **WE 8111 BIS** (incluído no kit): interface que transforma os 12 V da saída de trinco do intercomunicador num
  contacto seco em STR e ⏚. Modo 2 (impulso de 0,35 s) aconselhado. Qualquer relé adicional em STR fica simplesmente
  em paralelo com os dois fios brancos do WE 8111 BIS.
- O manual avisa que um comando com mais de 1 s na entrada STR bloqueia o portão: todos os impulsos devem ser
  curtos.
- Memorização de comandos: botão **P2/RADIO** até o LED vermelho acender, premir o botão do comando, o LED pisca e
  fica aceso; 10 s sem atividade termina.

## Central DEA 124RRZ (Spazio 703S)

| Borne | Tipo | Ligação |
|---|---|---|
| **16 START** – **21 COM** | N.A. | **abrir** (com P030 = 1, START inicia a abertura) |
| **17 PEDON** – **21 COM** | N.A. | **fechar** (com P030 = 1, PED inicia o fecho) |
| **18 STOP** – **21 COM** | N.F. | parar: contacto **NF** de um relé em série, no lugar da ponte |
| **5 – 6** | contacto livre, máx. 5 A | com **P027 = 0**: fechado com o portão aberto ou em movimento, abre quando o fecho termina (sensor "fechado") |
| **9 – 10** | +24 VAUX | alimentação de acessórios (recetor TRANSMY, Shelly Uni, ESP32 via conversor 24 → 5 V) |
| 19 FOTOC, 20 SIC | N.F. | segurança: manter |

Parâmetros: **P030 = 001**, **P027 = 000**. O recetor integrado fica em rolling code completo (valor de P008 do
manual para HCS rolling). **P009** escolhe que canal do comando aciona START e PED: na montagem A tem de atribuir
START ao canal 1 e PED ao canal 2, deixando os canais 3 e 4 sem função na central, porque a 124RRZ memoriza o comando
inteiro (P005) e reagiria também aos botões 3 e 4.

Usa-se a entrada STOP para parar, e não o START, porque o STOP só bloqueia e nunca põe o portão em movimento: é
seguro mesmo com o portão parado.

## Montagem A: comando DEA GTI4 + recetor DEA 291/X na placa Extel

| Botão GTI4 | Portão | Função | Quem recebe |
|---|---|---|---|
| 1 | Seccionado DEA | START (abre; com P030 = 1 só abre) | central 124RRZ |
| 2 | Seccionado DEA | PED (fecha) | central 124RRZ |
| 3 | Batente Extel | abertura total | 291/X canal 1 → STR |
| 4 | Batente Extel | abertura parcial | 291/X canal 2 → PED |

**Comando GTI2 (dois botões):** fica dedicado a **um só portão**. A 124RRZ memoriza o comando inteiro e o canal 2
está atribuído ao PED (fechar), por isso um GTI2 memorizado na central fecharia o seccionado com o botão 2 mesmo
que esse botão estivesse também no 291/X. Escolha uma de duas:

| Uso do GTI2 | Botão 1 | Botão 2 | Memorizar em |
|---|---|---|---|
| só seccionado | abrir (START) | fechar (PED) | 124RRZ (P005) |
| só batente | total (291/X CH1 → STR) | parcial (291/X CH2 → PED) | 291/X, **não** na 124RRZ |

**Recetor DEA 291X/433** (manual I75003): aceita "todos os radiocomandos DEA System com codificação 433,92 MHz",
em Dip Switch, Fix Code, **Rolling Code** ou D.A.R.T.; 2 canais; contactos de 0,5 A a 30 V; 1000 comandos;
alimentação 12 ou 24 V AC/DC; consumo máx. 65 mA a 12 V.

Ligações (CON-1):

| Borne 291/X | Ligação |
|---|---|
| 1 GND, 2 +V | **VA** (12 V) e **⏚** da placa ATV 3 |
| 3 NO, 5 C (canal 1) | **STR** e **⏚** |
| 7 NO, 6 C (canal 2) | **PED** e **⏚** |
| 4 NC (canal 1) | não se usa |
| CON-2 | antena (fio, ou antena DEA 240 se o alcance for curto) |

Configuração:

1. **DIP 1 = ON** (12 V). **DIP 2 e 3** na posição **Rolling Code** da tabela do manual.
2. **Reset de memória**, obrigatório na primeira instalação e sempre que se mudam os DIP: P1 premido cerca de 20 s
   até os LEDs piscarem 4 vezes. Os DIP só são lidos no fim do reset.
3. Memorizar: toque curto em **P1** → LED CH1 aceso → premir o **botão 3** do GTI4 (o LED apaga 1 s a confirmar).
   Novo toque em P1 → LED CH2 → premir o **botão 4**. Termina sozinho ao fim de 10 s. Repetir para cada GTI4 e,
   no caso de um GTI2 "só batente", com os botões 1 e 2.
4. Na 124RRZ, memorizar o GTI4 (P005) e confirmar P009 (START = canal 1, PED = canal 2).

O recetor tem de ficar numa caixa com proteção adequada (caixa DEA BOX ou equivalente); a caixa 100F que vem com o
291/X é para interior. Dentro do armário do motor Extel serve.

## Montagem B: comando Extel WEATEM 5 + recetor Extel TRANSMY na central DEA

| Botão WEATEM 5 | Portão | Função | Quem recebe |
|---|---|---|---|
| A | Batente Extel | total | placa ATV 3 (JP1 retirado) |
| B | Batente Extel | parcial | placa ATV 3 |
| C | Seccionado DEA | abrir | TRANSMY canal 1 → 16-21 |
| D | Seccionado DEA | fechar | TRANSMY canal 2 → 17-21 |

**TRANSMY** (Extel 750536, EAN 3345117505367): kit com recetor tipo **ATRC 3** a 433,92 MHz e dois comandos tipo
ATEM 5, a mesma família do WEATEM 5. Recetor de 2 canais, saídas **NO / COM** de "tipo botão de pressão sem corrente"
(contacto fechado enquanto o botão está premido), alimentação **12 a 24 V, contínua ou alternada**, 30 comandos,
alcance 30 m. **JP1 do recetor: não mexer.**

Ligações: alimentação em **9-10** da 124RRZ (24 V); CH1 NO/COM em **16 START** e **21 COM**; CH2 NO/COM em
**17 PEDON** e **21 COM**; P030 = 001. Em alternativa, as saídas ligam às entradas `canal_c` e `canal_d` do ESP32
(ver abaixo), e é o ESP32 que atua os relés.

Programação: **SW1** premido pelo menos 2 s → LED1 → premir o **botão C**. **SW1 duas vezes** → LED2 → premir o
**botão D**. Para apagar tudo: desligar a alimentação e voltar a ligar com SW1 premido (o LED pisca).

Disponibilidade: a Extel/Avidsen já não vende o TRANSMY nem nenhum outro recetor que aprenda o WEATEM 5 (confirmado
em novembro de 2026 nos catálogos Avidsen e Extel Umii). Só em segunda mão, pela referência 750536.

## Home Assistant: o seccionado como portão de garagem (`cover`)

Independente das montagens A e B. O estado vem do contacto 5-6 da central; o comando é por impulsos em 16, 17 e 18.

Que módulos servem: só os que têm **contacto seco** (saída sem tensão) podem ir aos bornes de comando.

| Módulo | Contacto seco | Alimentação | Uso aqui |
|---|---|---|---|
| **Shelly Uni** | sim, 2 saídas (máx. 100 mA, 36 V DC) | 12–36 V DC ou 12–24 V AC | comando e estado de um portão |
| **Shelly 1** (Gen1) / **Plus 1** | sim, 1 saída | 110–240 V AC, 24–60 V DC ou 12 V DC (Gen1) | um comando, ou a iluminação |
| Shelly 1PM | **não**, a saída leva a tensão de alimentação | 230 V AC ou 24 V DC | só a luz de 230 V |
| Shelly 2.5 / 2PM | **não**, idem | 230 V AC | **não ligar** aos portões |

### Opção 1: ESP32 com ESPHome — `esphome/portao-seccionado.yaml`

Material: ESP32 DevKit, módulo de 3 relés com contactos NA e NF, alimentação a partir de 9-10 (24 V) por conversor
24 → 5 V.

| GPIO (predefinido) | Ligação |
|---|---|
| GPIO26 | relé ABRIR (NA) → 16-21 |
| GPIO27 | relé FECHAR (NA) → 17-21 |
| GPIO25 | relé PARAR (contacto NF) → em série em 18-21 |
| GPIO32 ↔ GND | contacto livre 5-6 da central |
| GPIO33 ↔ GND | canal 1 do TRANSMY (botão C), só na montagem B via ESP32 |
| GPIO14 ↔ GND | canal 2 do TRANSMY (botão D), idem |

1. No Home Assistant, instalar o **ESPHome Device Builder** e criar um dispositivo novo.
2. Colar `esphome/portao-seccionado.yaml` e preencher os `secrets` (Wi-Fi e chaves geradas pelo ESPHome).
3. Ajustar em `substitutions` o **tempo de curso** medido no local e, se preciso, os GPIO.
4. Instalar no ESP32 (a primeira vez por USB). O Home Assistant deteta o dispositivo e cria o portão.

Comportamento:

- **Botões C e D via ESP32:** C abre e D fecha; premir de novo durante o movimento para o portão. Repetições dentro
  de 1,5 s contam como um só toque.
- **Comandos de fora:** se o portão for aberto pelo GTI, pela botoneira ou pelo TRANSMY ligado diretamente à
  central, o contacto 5-6 deteta-o e o estado passa a "aberto".
- **Fecho não confirmado:** se o contacto 5-6 não confirmar o fecho até `tempo_maximo` (fotocélula ou obstáculo), o
  estado fica "aberto".

É a única opção com a **paragem** (STOP) a partir do Home Assistant, porque precisa de um relé com contacto NF.

### Opção 2: Shelly Uni (ou outros relés já integrados) — `packages/extel_dea_spazio.yaml`

Um **Shelly Uni** por portão, alimentado pela própria placa. Sem soldaduras nem ESP32.

| Shelly Uni | Seccionado DEA (124RRZ) | Batente Extel (ATV 3) |
|---|---|---|
| Alimentação | **9 – 10** (24 V), com a polaridade do manual | **VA** (+12 V) e **⏚** |
| Saída 1 | **16 – 21**, abrir | **STR – ⏚**, total |
| Saída 2 | **17 – 21**, fechar | **PED – ⏚**, parcial |
| Entrada 1 | contacto **5-6** (portão não fechado) | reed switch na folha (folha fechada) |
| Entrada 2 | reed switch de portão aberto (opcional) | **S1** (portão em movimento), ver Iluminação |
| Sensor | até 3 DS18B20 ou 1 DHT22 (temperatura) | idem |

- **Entradas:** as do Uni leem tensão (2,2–36 V DC), não um contacto sozinho. Um contacto seco (5-6, reed switch)
  liga entre o **positivo da alimentação do Uni** e a entrada.
- **Impulsos:** o script do package faz impulsos de 0,6 s. Configure também no Uni um "auto-off" curto como
  proteção. No batente nenhum impulso pode chegar a 1 s, porque a placa ATV 3 bloqueia.
- **Sem paragem:** o Uni não tem contacto NF, portanto o STOP (18-21) fica só nos comandos e na botoneira. No
  package, apague `stop_cover` e o script `portao_seccionado_parar`, e, na montagem A, as automações dos botões C
  e D.
- **Segurança da rede:** o Shelly passa a abrir o portão. Palavra-passe no Shelly, nuvem desligada se só usar o
  Home Assistant, e de preferência numa rede Wi-Fi separada para equipamentos.

Instalação do package:

1. Em `configuration.yaml`:
   ```yaml
   homeassistant:
     packages: !include_dir_named packages
   ```
2. Copiar o ficheiro para `<config>/packages/` e substituir as entidades marcadas com `# ALTERAR` pelas do Shelly
   (saídas como `switch`, entradas como `binary_sensor`; com P027 = 0 a entrada 1 fica "on" com o portão aberto).
3. Reiniciar o Home Assistant. Aparece `cover.portao_seccionado`.

Funciona como a opção 1, mas a lógica corre no Home Assistant: depende de ele e da rede estarem a funcionar.

### Batente Extel no Home Assistant (opcional)

Shelly Uni (tabela acima) ou outros relés de contacto seco NA em **STR – ⏚** (total) e **PED – ⏚** (parcial), em
paralelo com o WE 8111 BIS e com o 291/X. Impulsos curtos (0,35 a 0,6 s). O package tem os dois scripts comentados.
**Não** associe estes relés a botões que um recetor já trate: o portão receberia duas ordens seguidas e podia parar
ou inverter.

## Iluminação: Shelly no lugar do módulo WEATCR 2

O WEATCR 2 liga a **S1** (TRIG), **⏚** (GND) e **VA** (Vin, 12 V) e acende uma luz de 230 V durante 60 s, tempo
fixo, quando o portão se move. Há duas formas de o substituir por Shelly.

**Com o Shelly Uni do batente (se já o tiver):** S1 na **entrada 2** do Uni e um **Shelly 1PM** (ou 1) na luz, a
230 V. Na entrada 2 do Uni, uma ação local de URL liga o 1PM com temporizador, por exemplo
`http://<ip-do-1pm>/relay/0?turn=on&timer=120`. Funciona sem o Home Assistant, só com a rede Wi-Fi.

**Com um Shelly 1 / Plus 1 a 12 V, junto à placa:**

| Shelly 1 (Gen1) ou Plus 1 | Ligação |
|---|---|
| Alimentação 12 V DC | **VA** e **⏚** |
| Entrada SW | **S1** |
| Saída (contacto seco 16 A) | fase da luz de 230 V |

- O temporizador passa a ser o "auto-off" do Shelly, afinável. O Home Assistant pode acender a luz por outras
  razões e usar a entrada SW como sensor de **"batente em movimento"**.
- **Confirmar a polaridade do S1** com um multímetro, entre S1 e ⏚, com o portão em movimento, nas duas formas:
  a entrada SW do Shelly 1 e a entrada do Uni disparam com tensão positiva. Se o S1 puxar à massa, é preciso um
  pequeno relé ou um transístor a inverter.
- Como comuta 230 V, o Shelly fica, como o WEATCR 2, numa caixa de classe II IP44 dentro do armário do motor.
- Um módulo de luz Extel Casa (433 MHz) **não** serve: só responde a interruptores e comandos Casa, e a placa ATV 3
  não os emite.

## Segurança

- O fecho à distância do seccionado exige **fotocélulas (19 FOTOC) e deteção de obstáculos da central DEA ativas e
  testadas**. Os parâmetros de força P014 e P015 têm de cumprir a norma; o manual DEA tem a tabela "Operating control"
  com o que é exigido em cada tipo de comando. O ESP32 e o Home Assistant não substituem estes dispositivos.
- No batente, o manual Extel exige as fotocélulas em PHO validadas por aprendizagem da placa.
- Faça as ligações com a alimentação desligada, segundo os manuais.

## Limitações conhecidas

- A montagem B depende de encontrar um TRANSMY usado. Não há recetor novo que aprenda o WEATEM 5.
- Um GTI2 só serve um dos portões (ver montagem A).
- Com Shelly Uni não há paragem a partir do Home Assistant, e o "auto-off" mínimo do Uni deve ser confirmado na
  aplicação Shelly.
- O valor exato de P009 para "START = canal 1, PED = canal 2" e o valor de P008 para rolling completo devem ser
  lidos na Tabela 2 do manual DEA; o README não os fixa.
- A configuração ESPHome passa na validação do ESPHome 2026.10 e o package passa no `check_config` do
  Home Assistant 2026.2. Nenhum dos dois foi compilado nem testado com hardware real.
