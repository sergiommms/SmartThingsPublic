# Comando Extel de 4 botões: batente Extel + seccionado DEA Spazio (Home Assistant)

Um único comando Extel de **quatro botões** controla dois portões de marcas diferentes:

| Botão | Portão | Função | Quem atua |
|-------|--------|--------|-----------|
| 1 | Batente Extel **Vera / Hello V** (2 folhas) | Abrir **uma folha** | Recetor da placa Extel (como hoje) |
| 2 | Batente Extel **Vera / Hello V** (2 folhas) | Abrir **duas folhas** | Recetor da placa Extel (como hoje) |
| 3 | Seccionado **DEA Spazio 703S** | **Abrir** (premir outra vez durante o movimento = parar) | ESP32 ou Home Assistant → relé → central DEA |
| 4 | Seccionado **DEA Spazio 703S** | **Fechar** (premir outra vez durante o movimento = parar) | idem |

A central da Spazio 703S só aprende comandos rádio DEA. Os botões 3 e 4 do comando Extel são captados por
um recetor à parte e transformados em impulsos de contacto seco nas entradas da central.

No Home Assistant, o seccionado aparece como um **portão de garagem** (`cover`) com estado
aberto / fechado / a abrir / a fechar. Pode ser comandado pela app e usado em automações, na Alexa,
no Google, etc.

## Três formas de montar

| | O que é | Funciona sem Wi-Fi / sem HA | Estado na app |
|---|---|---|---|
| **A. ESP32 com ESPHome** (recomendado) | `esphome/portao-seccionado.yaml` | **Sim**, a lógica corre no ESP32 | Sim |
| **B. Relés no Home Assistant** (Shelly, Z-Wave, Zigbee...) | `packages/extel_dea_spazio.yaml` | Não, depende do HA | Sim |
| **C. Sem domótica** | recetor ligado diretamente à central | Sim | Não |

### Comando Extel: código fixo ou rolling code?

Isto decide como se captam os botões 3 e 4.

- Os kits Vera / Hello V trazem comandos de 4 canais a 433,92 MHz, e os comandos de substituição
  compatíveis com Extel são vendidos como **rolling code**. Nesse caso, um recetor RF genérico não
  reconhece toques seguidos. Use um **recetor externo compatível com os comandos Extel** com 2 saídas de
  relé (canais 3 e 4), ligadas às entradas do ESP32 (A), de um módulo de entradas (B) ou diretamente à
  central (C).
- Se o comando for de **código fixo**, um recetor RF 433 MHz barato ligado ao ESP32 chega. O ficheiro
  ESPHome tem um bloco comentado (`remote_receiver`) para isso.

Para saber qual é o seu caso, ponha o ESP32 com o recetor RF em modo `dump` e carregue duas vezes no
botão 3. Se aparecer o mesmo código nas duas vezes, é código fixo.

## Ligações à DEA Spazio 703S

Entradas de comando da central (série Spazio 702S/703S; borne comum `COM`):

| Entrada | Tipo | Ligação |
|---------|------|---------|
| `AP` | NA | relé **AP**: abrir (botão 3) |
| `CHIUDE/PED` | NA | relé **CHIUDE**: fechar (botão 4). **Programar o parâmetro da entrada PED = 1** (PED fecha, AP abre) |
| `START` | NA | relé **START**: usado só para **parar** (programar a lógica do START em **passo-a-passo**) |
| `STOP` | NF | não mexer (manter a ponte ou a botoneira de STOP existente) |
| `FOTO` | NF | fotocélulas: manter ligadas e ativas |

Sensor de fim de curso (recomendado): contacto magnético que fecha com o portão em baixo.

```
 Comando Extel ──433 MHz──► recetor da placa Extel ──► batente (botões 1, 2)
       │
       └──433 MHz──► recetor externo compatível Extel (canais 3, 4)
                         │ contactos secos
                         ▼
                      ESP32 (ESPHome) ──Wi-Fi──► Home Assistant
                         │ relés (impulso 0,6 s)
                         ▼
          DEA Spazio 703S:  AP (abrir)   CHIUDE/PED (fechar)   START (parar)   COM
                         ▲
          sensor magnético "fechado" ──► ESP32
```

> Os números dos bornes mudam com a revisão da placa. Confirme no manual DEA da sua central (ligações e
> parâmetro da entrada PED) antes de ligar, e faça as ligações com a alimentação desligada.

## A. ESP32 com ESPHome (recomendado)

Material: um ESP32 DevKit, um módulo de 3 relés (contacto seco), o recetor externo compatível Extel e um
sensor magnético. Alimente o conjunto com uma fonte própria de 5 V; a saída de 24 V de acessórios da
central serve apenas se tiver um conversor 24 V → 5 V e corrente disponível.

| GPIO (predefinido) | Ligação |
|---|---|
| GPIO26 | relé AP |
| GPIO27 | relé CHIUDE |
| GPIO25 | relé START |
| GPIO32 ↔ GND | sensor magnético "fechado" |
| GPIO33 ↔ GND | saída do canal 3 do recetor externo (contacto seco) |
| GPIO14 ↔ GND | saída do canal 4 do recetor externo (contacto seco) |

1. No Home Assistant, instalar o add-on/app **ESPHome Device Builder** e criar um dispositivo novo.
2. Colar o conteúdo de `esphome/portao-seccionado.yaml` e preencher os `secrets` (Wi-Fi e chaves de
   encriptação; o ESPHome gera as chaves).
3. Ajustar em `substitutions` o **tempo de curso** (medido no local, um pouco acima do real) e, se
   preciso, os GPIO.
4. Instalar no ESP32 (a primeira vez por USB). O Home Assistant deteta o dispositivo e cria o portão.

Comportamento:

- Botão 3: abre. Se o portão já estiver a abrir, para.
- Botão 4: fecha. Se o portão já estiver a fechar, para.
- Repetições do mesmo botão durante 1,5 s contam como um só toque.
- O "parar" só envia o impulso START com o portão em movimento. Com o portão parado, um START pô-lo-ia
  a andar.
- Com o sensor magnético, "fechado" é o estado real. Sem sensor, o estado é estimado pelo tempo de curso.

## B. Relés já integrados no Home Assistant

Para quem prefere, por exemplo, um Shelly com saída livre de potencial nos bornes da 703S.

1. Em `configuration.yaml`:
   ```yaml
   homeassistant:
     packages: !include_dir_named packages
   ```
2. Copiar `packages/extel_dea_spazio.yaml` para `<config>/packages/`.
3. Substituir as entidades de exemplo (linhas com `# ALTERAR`): relés AP, CHIUDE e START, o sensor do
   portão e os dois sensores dos botões 3 e 4.
4. Os botões 3 e 4 têm de existir como `binary_sensor` que fica `on` enquanto se carrega no botão (por
   ex. as entradas de um Shelly ligadas às saídas do recetor externo). Se a sua integração der eventos em
   vez de estados (botões Zigbee, pontes RF por MQTT...), troque o `trigger` das duas automações pelo
   evento correspondente.
5. Reiniciar o Home Assistant. Aparece `cover.portao_seccionado`.

O comportamento é o mesmo da opção A, mas tudo depende de o Home Assistant e a rede estarem a funcionar.

## C. Sem domótica

As saídas do recetor externo compatível Extel ligam diretamente à 703S: canal 3 → `AP`, canal 4 →
`CHIUDE/PED` (parâmetro PED = 1). É simples e fiável, mas não dá estado nem controlo pela app.

## Batente Extel

Os botões 1 e 2 continuam memorizados no recetor da placa Extel (Vera / Hello V usam a mesma família de
placa, tipo ATV 3).

Segundo o guia da placa ATV 3 VERA (a confirmar no seu exemplar), o recetor da placa usa os canais A–D do
comando aos pares: **sem o jumper JP1**, canal A = abertura total e canal B = abertura parcial (uma folha);
**com JP1**, os canais C e D fazem esses papéis. Com JP1 retirado, os botões 3 e 4 (canais C e D) ficam
livres para o seccionado e a placa Extel não reage a eles. Se o batente abrir quando carrega no 3 ou no 4,
verifique o JP1. As entradas de botoneira da placa são contactos secos NA. O package (opção B) tem, comentados, dois scripts opcionais para abrir o batente pelo Home
Assistant através de relés nas entradas de botoneira da placa Extel.

**Não ligue esses scripts aos botões 1 e 2 do comando**: o recetor Extel já os trata, e o portão
receberia duas ordens seguidas, podendo parar ou inverter.

## Segurança

- O fecho à distância só deve ser usado com as **fotocélulas e a deteção de obstáculos da central DEA
  ativas e testadas**. Nem o ESP32 nem o Home Assistant substituem esses dispositivos.
- Com sensor: se o fecho não for confirmado a tempo (opção A: `tempo_maximo`; opção B: fim do tempo de
  curso), o estado fica "aberto". O portão pode ter invertido por causa da fotocélula ou de um obstáculo.
- Instalação elétrica conforme o manual do fabricante e com a alimentação desligada.

## Limitações conhecidas

- Os bornes e o parâmetro PED foram tirados da documentação da série 702S/703S. Confirme na sua placa.
- Sem sensor, uma abertura feita com o comando DEA original ou com a botoneira não é detetada.
- Opção A: a configuração passa na validação do ESPHome 2026.10, mas não foi compilada nem testada num
  ESP32 ligado à central.
- Opção B: o package passa no `check_config` do Home Assistant 2026.2, mas não foi testado com relés reais.
