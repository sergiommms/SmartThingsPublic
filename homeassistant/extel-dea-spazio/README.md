# Comando Extel de 4 botões: batente Extel + seccionado DEA Spazio 703S (Home Assistant)

Um único comando Extel de **quatro botões** (WEATEM 5: botões A, B, C, D de cima para baixo) controla dois
portões de marcas diferentes:

| Botão | Portão | Função | Quem atua |
|-------|--------|--------|-----------|
| A | Batente Extel **Vera / Hello V** (placa ATV 3) | Abertura **total** (duas folhas) | Recetor da placa Extel |
| B | Batente Extel **Vera / Hello V** | Abertura **parcial** (uma folha) | Recetor da placa Extel |
| C | Seccionado **DEA Spazio 703S** (central 124RRZ) | **Abrir** | Recetor da 703S *ou* recetor externo / ESP32 |
| D | Seccionado **DEA Spazio 703S** | **Fechar** | idem |

Fontes: guia de instalação Extel VERA "Type ATV 3" (V3, 04/2012) e notice 760405, e manual DEA "Spazio
702S/703S – Operating instructions and warnings" (Tabela 1, ligações; Tabela 2, parâmetros).

## O que os manuais dizem

**Comando Extel (WEATEM 5):** código **rolling**, 433,92 MHz, pilha CR2032.

**Placa Extel ATV 3:** o jumper **JP1** escolhe os botões que a placa usa:
- JP1 **retirado**: A = total, B = parcial. Os botões **C e D ficam livres**.
- JP1 colocado: C = total, D = parcial.

Mudar o JP1 não obriga a reprogramar os comandos.

**Central DEA 124RRZ (Spazio 703S):** o recetor integrado de 433,92 MHz aceita comandos **HCS** (rolling
code completo ou **só a parte fixa**) e HT12E. Guarda até 100 comandos. Um parâmetro escolhe que botão
aciona cada entrada.

## Passo 1: tentar que a 703S aprenda diretamente o comando Extel (sem hardware extra)

Em modo **"HCS fixed part only"** (P008 = 000), a central lê só a parte fixa da transmissão (número de série
e botão), que é igual em qualquer comando HCS/KeeLoq, seja qual for a marca. **Se** o comando Extel usar
esta família de codificação, a 703S consegue aprendê-lo. Os manuais não dizem que codificação o Extel usa,
por isso é preciso **testar**. O teste é gratuito e reversível.

No painel da central (teclas +/−/OK; ver "Personalização dos parâmetros" no manual):

1. Anote os valores atuais de **P008**, **P009** e **P030**, para poder voltar atrás.
2. **P008 = 000** (HCS só parte fixa).
3. **P009 = 009** (entrada START = canal 3, entrada pedonal = canal 4).
4. **P030 = 001** (a entrada PED inicia o fecho e a AP/START inicia a abertura).
5. **P005** (memorizar comandos): quando aparecer `LEAr`, carregar no **botão C** do comando Extel.
   - Se aparecer `r00x` (por exemplo `r003`), o comando foi aceite. Repita para os outros comandos Extel e
     espere 10 s para sair.
   - Se nada acontecer, a 703S não reconhece o comando Extel: siga para o passo 2.
6. Testar: **C deve abrir** e **D deve fechar**. Se os botões estiverem trocados ou um não funcionar,
   experimente outro valor de P009 na tabela do manual (por exemplo 012 = START no CH4 e pedonal no CH3).

Antes de dar o passo 1 por concluído, confirme estes pontos:

- **O P009 aplica-se a todos os comandos memorizados.** Os comandos DEA originais também passam a usar os
  canais 3 e 4. Confirme que continuam a servir, ou ajuste.
- **Os comandos DEA originais podem precisar de ser memorizados outra vez** depois de mudar o P008.
- **Segurança:** o modo "só parte fixa" é menos seguro do que o rolling code completo, porque a parte fixa
  pode ser copiada por um "code grabber". É o preço de usar um comando de outra marca.

Se o passo 1 funcionar, o comando Extel abre e fecha os dois portões sem mais nada. O passo 2 só é preciso
para ter o seccionado no Home Assistant (estado, app, automações).

## Passo 2: Home Assistant

O seccionado aparece no Home Assistant como **portão de garagem** (`cover`): aberto, fechado, a abrir ou a
fechar.

### Ligações à central 124RRZ (Tabela 1 do manual DEA)

| Borne | Tipo | Ligação |
|-------|------|---------|
| **16 START** – **21 COM** | N.A. | relé **ABRIR** (com P030 = 1, "AP" inicia a abertura) |
| **17 PEDON** – **21 COM** | N.A. | relé **FECHAR** (com P030 = 1, PED inicia o fecho) |
| **18 STOP** – **21 COM** | N.F. | contacto **NF** do relé **PARAR**, em série (no lugar da ponte). Relé desligado = funcionamento normal; um impulso para o portão |
| **5 – 6** | contacto livre, máx. 5 A | **sensor "fechado"** sem sensor magnético: com **P027 = 000** fica fechado com o portão em movimento ou aberto e só abre quando o fecho termina |
| 19 FOTOC, 20 SIC | N.F. | segurança: manter como estão (fotocélulas, banda) |
| 9 – 10 (+24 VAUX) | 24 V ⎓ | alimentação de acessórios: pode alimentar o ESP32 através de um conversor 24 V → 5 V, se o consumo total o permitir |

Parâmetros: **P030 = 001**, **P027 = 000**. Mantenha a lógica do START (P025) como está: com P030 = 1, a
entrada START serve para abrir.

Usa-se a entrada **STOP** para parar, e não o START, porque o STOP só bloqueia e nunca põe o portão em
movimento. Por isso é seguro mesmo com o portão parado.

### Opção A: ESP32 com ESPHome (recomendado) — `esphome/portao-seccionado.yaml`

Material: ESP32 DevKit, módulo de 3 relés com contactos NA e NF.

| GPIO (predefinido) | Ligação |
|---|---|
| GPIO26 | relé ABRIR → 16-21 |
| GPIO27 | relé FECHAR → 17-21 |
| GPIO25 | relé PARAR (contacto NF) → em série em 18-21 |
| GPIO32 ↔ GND | contacto livre 5-6 da central |
| GPIO33 ↔ GND | saída do canal C do recetor externo (só no caso do passo 2 com recetor externo) |
| GPIO14 ↔ GND | saída do canal D do recetor externo (idem) |

1. No Home Assistant, instalar o **ESPHome Device Builder** e criar um dispositivo novo.
2. Colar `esphome/portao-seccionado.yaml` e preencher os `secrets` (Wi-Fi e chaves geradas pelo ESPHome).
3. Ajustar em `substitutions` o **tempo de curso** medido no local e, se preciso, os GPIO.
4. Instalar no ESP32 (a primeira vez por USB). O Home Assistant deteta o dispositivo e cria o portão.

Comportamento:

- **Botões (com recetor externo):** C abre e D fecha. Premir de novo durante o movimento para o portão.
  Repetições dentro de 1,5 s contam como um só toque.
- **Comandos de fora:** se o portão for aberto pelo recetor da própria 703S, pela botoneira ou por um
  comando DEA, o contacto 5-6 deteta-o e o estado passa a "aberto".
- **Fecho não confirmado:** se o contacto 5-6 não confirmar o fecho até `tempo_maximo` (fotocélula ou
  obstáculo), o estado fica "aberto".

### Opção B: relés já integrados no Home Assistant — `packages/extel_dea_spazio.yaml`

Para quem prefere, por exemplo, um Shelly com saídas livres de potencial nos mesmos bornes.

1. Em `configuration.yaml`:
   ```yaml
   homeassistant:
     packages: !include_dir_named packages
   ```
2. Copiar o ficheiro para `<config>/packages/` e substituir as entidades marcadas com `# ALTERAR`.
3. Reiniciar o Home Assistant. Aparece `cover.portao_seccionado`.

Funciona como a opção A, mas depende de o Home Assistant e a rede estarem a funcionar.

## Se a 703S não aprender o comando Extel

Use um **recetor externo compatível com os comandos Extel** (rolling code), com 2 saídas de relé.
Um recetor RF genérico de código fixo não serve. Há duas formas de o ligar:

- **Sem domótica:** canal C → bornes 16-21 (abrir) e canal D → bornes 17-21 (fechar), com P030 = 001.
- **Com o ESP32:** ligar as saídas às entradas `canal_c` e `canal_d` (GPIO33 e GPIO14).

## Batente Extel (placa ATV 3)

- **JP1 retirado:** os botões A e B continuam a servir o batente, e C e D ficam para o seccionado. Se o
  batente reagir ao C ou ao D, o JP1 está colocado.
- **Comandar pelo Home Assistant (opcional):** relés de contacto seco **NA** nos bornes da placa:
  - **STR – COM:** abertura total;
  - **PED – COM:** abertura parcial (uma folha).

  Para módulos de interfone, o manual recomenda o modo impulsional, com um contacto de 0,35 s. O package
  tem os dois scripts comentados.
- **Não ligue esses relés aos botões A e B do comando.** O recetor Extel já os trata, e o portão receberia
  duas ordens seguidas, podendo parar ou inverter.
- **STOP:** é normalmente fechado e vem shuntado na placa. Só retire a ponte se ligar um botão de STOP.

## Segurança

- O fecho à distância exige **fotocélulas (19 FOTOC) e deteção de obstáculos da central DEA ativas e
  testadas**. Os parâmetros de força P014 e P015 têm de cumprir a norma, e o manual inclui a tabela
  "Operating control" com o que é exigido em cada tipo de comando. O ESP32 e o Home Assistant não
  substituem estes dispositivos.
- Faça as ligações com a alimentação desligada, segundo os manuais.

## Limitações conhecidas

- Os manuais não dizem que codificação usa o comando Extel. O passo 1 é um teste e pode não resultar.
- A configuração ESPHome passa na validação do ESPHome 2026.10 e o package passa no `check_config` do
  Home Assistant 2026.2. Nenhum dos dois foi compilado nem testado com hardware real.
