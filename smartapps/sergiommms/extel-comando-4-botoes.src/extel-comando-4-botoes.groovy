/**
 *  Extel Comando 4 Botões - portão de batente Extel + portão seccionado DEA Spazio
 *
 *  Copyright 2026 sergiommms
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Ideia
 *  -----
 *  Um único comando Extel de quatro botões passa a controlar dois automatismos de marcas diferentes:
 *
 *    - Botões 1 e 2: portão de batente de duas folhas com automatismo Extel Vera / Hello V (uma folha / duas folhas).
 *      O recetor Extel continua a receber estes botões diretamente, como sempre.
 *    - Botões 3 e 4: portão seccionado (abertura vertical) com motor de teto DEA Spazio 703S.
 *      A central DEA não reconhece o rádio Extel, por isso os botões 3 e 4 são captados por um recetor/ponte
 *      de 433,92 MHz que os entrega ao SmartThings como eventos "button"; esta SmartApp traduz esses eventos
 *      em impulsos de relé (contacto seco) nas entradas da central DEA (START / passo-a-passo, PED / parcial,
 *      e opcionalmente ABRIR, FECHAR e STOP).
 *
 *  A SmartApp cria dois dispositivos-filho ("DEA Spazio Portao Seccionado" e "Extel Portao Batente") para que
 *  ambos os portões também possam ser comandados e vistos na app e usados noutras automações.
 *
 *  Estado do seccionado
 *  --------------------
 *  Com uma só entrada passo-a-passo, a central DEA percorre uma sequência fixa (por ex. ABRE-STOP-FECHA-STOP).
 *  A SmartApp segue essa sequência para saber quantos impulsos são precisos para chegar ao estado pedido
 *  (por ex. "fechar" enquanto abre = 2 impulsos na sequência de 4 passos). Os sensores de fim de curso
 *  (fechado e/ou aberto) são opcionais mas recomendados; sem eles o estado é estimado pelo tempo de curso.
 */
definition(
	name: "Extel Comando 4 Botoes",
	namespace: "sergiommms",
	author: "sergiommms",
	description: "Usa um comando Extel de 4 botões para o portão de batente Extel (uma folha / duas folhas) e para um portão seccionado com motor DEA Spazio.",
	category: "Safety & Security",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png",
	singleInstance: false,
	pausable: true
)

preferences {
	page(name: "paginaPrincipal")
	page(name: "paginaBotoes")
	page(name: "paginaSpazio")
	page(name: "paginaBatente")
}

// ---------------------------------------------------------------------------------------------------------------------
// Constantes
// ---------------------------------------------------------------------------------------------------------------------

private getACOES() {
	[
		"nada"           : "Nenhuma",
		"extel_nativo"   : "Extel nativo (o recetor Extel trata; só registar)",
		"batente_uma"    : "Batente: abrir uma folha (via relé)",
		"batente_duas"   : "Batente: abrir duas folhas (via relé)",
		"spazio_abrir"   : "Seccionado: abrir (para, se já estiver a abrir)",
		"spazio_fechar"  : "Seccionado: fechar (para, se já estiver a fechar)",
		"spazio_alternar": "Seccionado: alternar abrir / fechar",
		"spazio_passo"   : "Seccionado: passo-a-passo (um impulso)",
		"spazio_parar"   : "Seccionado: parar",
		"spazio_parcial" : "Seccionado: abertura parcial"
	]
}

private getACOES_PREDEFINIDAS() {
	[1: "extel_nativo", 2: "extel_nativo", 3: "spazio_abrir", 4: "spazio_fechar"]
}

private getSEQUENCIAS() {
	[
		"4passos": "Passo-a-passo: ABRE - STOP - FECHA - STOP",
		"2passos": "Inversão: ABRE - FECHA - ABRE (inverte durante o movimento)"
	]
}

private opcoes(Map m) {
	m.collect { k, v -> [(k): v] }
}

// ---------------------------------------------------------------------------------------------------------------------
// Páginas de configuração
// ---------------------------------------------------------------------------------------------------------------------

def paginaPrincipal() {
	dynamicPage(name: "paginaPrincipal", title: "Comando Extel de 4 botões", install: true, uninstall: true) {
		section("Recetor / ponte de 433 MHz que recebe o comando Extel") {
			paragraph "Escolha o dispositivo que gera eventos de botão quando se carrega no comando Extel " +
				"(por ex. uma ponte RF 433 MHz com os 4 botões aprendidos). Os botões 1 e 2 continuam memorizados " +
				"no recetor Extel; os botões 3 e 4 são os que vão comandar o seccionado DEA Spazio."
			input "comando", "capability.button", title: "Comando Extel (dispositivo de botões)", required: false
			input "debounceMs", "number", title: "Ignorar repetições do mesmo botão durante (ms)", defaultValue: 1500, range: "0..10000", required: false
		}
		section("Alternativa: um dispositivo por botão") {
			paragraph "Se a ponte criar um dispositivo separado para cada botão, escolha-os aqui (substituem o número de botão do dispositivo acima)."
			(1..4).each { n ->
				input "botaoDispositivo${n}", "capability.button", title: "Dispositivo do botão ${n}", required: false
			}
		}
		section("Configuração") {
			href "paginaBotoes", title: "O que faz cada botão", description: resumoBotoes()
			href "paginaSpazio", title: "Portão seccionado DEA Spazio", description: resumoSpazio(), state: relePasso || releAbrir ? "complete" : null
			href "paginaBatente", title: "Portão de batente Extel (duas folhas)", description: "Relés e sensor opcionais", state: (releUmaFolha || releDuasFolhas || batenteSensor) ? "complete" : null
		}
		section("Notificações") {
			input "notificar", "bool", title: "Enviar notificação quando o seccionado abre ou fecha", defaultValue: false, required: false
			input "avisoAbertoMin", "number", title: "Avisar se o seccionado ficar aberto mais de (minutos, 0 = não avisar)", defaultValue: 0, required: false
		}
		section([mobileOnly: true]) {
			label title: "Nome desta automação", required: false
		}
	}
}

def paginaBotoes() {
	dynamicPage(name: "paginaBotoes", title: "Função de cada botão") {
		section {
			paragraph "Atenção: se o recetor Extel já tiver um botão memorizado, deixe esse botão como \"Extel nativo\". " +
				"Se também acionar o relé do batente, o portão recebe duas ordens seguidas e pode parar ou inverter."
		}
		(1..4).each { n ->
			section("Botão ${n}") {
				input "acaoPremido${n}", "enum", title: "Ao premir", options: opcoes(ACOES), defaultValue: ACOES_PREDEFINIDAS[n], required: false
				input "acaoMantido${n}", "enum", title: "Ao manter premido (se a ponte o suportar)", options: opcoes(ACOES), defaultValue: "nada", required: false
			}
		}
	}
}

def paginaSpazio() {
	dynamicPage(name: "paginaSpazio", title: "Portão seccionado - motor DEA Spazio") {
		section("Central DEA Spazio 703S") {
			paragraph "Recomendado na 703S: um relé na entrada AP (abrir) e outro na entrada CHIUDE/PED (fechar), " +
				"com o parâmetro da entrada PED programado a 1 (PED fecha, AP abre). Assim cada botão dá uma ordem " +
				"direta e não é preciso seguir a sequência do START. Confirme bornes e parâmetro no manual da central."
		}
		section("Relés ligados à central DEA (contacto seco, normalmente aberto)") {
			input "relePasso", "capability.switch", title: "Relé na entrada START (passo-a-passo)", required: false
			input "releParcial", "capability.switch", title: "Relé de abertura parcial - opcional (só se a central tiver esta função)", required: false
			input "releAbrir", "capability.switch", title: "Relé na entrada AP (abrir) - opcional", required: false
			input "releFechar", "capability.switch", title: "Relé na entrada CHIUDE/PED (fechar, parâmetro PED = 1) - opcional", required: false
			input "releParar", "capability.switch", title: "Relé em série com a entrada STOP (usar o contacto NF do relé) - opcional", required: false
			input "duracaoImpulsoMs", "number", title: "Duração do impulso (ms)", defaultValue: 600, range: "100..5000", required: false
			input "intervaloImpulsosMs", "number", title: "Intervalo entre impulsos seguidos (ms)", defaultValue: 1500, range: "500..10000", required: false
			input "usarPush", "bool", title: "Os relés já são momentâneos (usar 'push' em vez de ligar/desligar)", defaultValue: false, required: false
		}
		section("Comportamento da central") {
			input "sequencia", "enum", title: "Lógica do START programada na central", options: opcoes(SEQUENCIAS), defaultValue: "4passos", required: false
			input "tempoCursoS", "number", title: "Tempo de curso completo (segundos)", defaultValue: 20, range: "3..180", required: false
			input "tempoParcialS", "number", title: "Tempo da abertura parcial (segundos)", defaultValue: 6, range: "1..120", required: false
		}
		section("Sensores de fim de curso (recomendados)") {
			input "sensorFechado", "capability.contactSensor", title: "Sensor 'fechado' (fecha o contacto com o portão em baixo)", required: false
			input "sensorAberto", "capability.contactSensor", title: "Sensor 'aberto' (fecha o contacto com o portão em cima)", required: false
		}
	}
}

def paginaBatente() {
	dynamicPage(name: "paginaBatente", title: "Portão de batente Extel") {
		section {
			paragraph "Placa Extel Vera / Hello V: o comando Extel continua a abrir o batente diretamente pelo recetor " +
				"da placa (botão 1 = uma folha, botão 2 = duas folhas). Os relés abaixo são opcionais e servem para abrir " +
				"o batente a partir da app, ligados às entradas de botoneira da placa (confirme os bornes na notice)."
			input "criarBatente", "bool", title: "Criar dispositivo do portão de batente", defaultValue: true, required: false
			input "releUmaFolha", "capability.switch", title: "Relé na entrada 'uma folha' (pedonal)", required: false
			input "releDuasFolhas", "capability.switch", title: "Relé na entrada 'duas folhas' (total)", required: false
			input "batenteSensor", "capability.contactSensor", title: "Sensor de contacto do batente (fechado = portão fechado)", required: false
		}
	}
}

private resumoBotoes() {
	(1..4).collect { n ->
		def a = settings["acaoPremido${n}"] ?: ACOES_PREDEFINIDAS[n]
		"${n}: ${ACOES[a]}"
	}.join("\n")
}

private resumoSpazio() {
	def partes = []
	if (relePasso) partes << "START"
	if (releParcial) partes << "parcial"
	if (releAbrir) partes << "AP"
	if (releFechar) partes << "CHIUDE/PED"
	if (releParar) partes << "STOP"
	partes ? "Relés: ${partes.join(', ')}" : "Tocar para configurar"
}

// ---------------------------------------------------------------------------------------------------------------------
// Ciclo de vida
// ---------------------------------------------------------------------------------------------------------------------

def installed() {
	log.debug "Instalado com: ${settings}"
	initialize()
}

def updated() {
	log.debug "Atualizado com: ${settings}"
	unsubscribe()
	unschedule()
	initialize()
}

def uninstalled() {
	getChildDevices().each { deleteChildDevice(it.deviceNetworkId) }
}

def initialize() {
	criarFilhos()

	if (comando) {
		subscribe(comando, "button", eventoBotao)
	}
	(1..4).each { n ->
		def d = settings["botaoDispositivo${n}"]
		if (d) {
			subscribe(d, "button", eventoBotao)
		}
	}
	if (sensorFechado) {
		subscribe(sensorFechado, "contact", eventoSensorFechado)
	}
	if (sensorAberto) {
		subscribe(sensorAberto, "contact", eventoSensorAberto)
	}
	if (batenteSensor) {
		subscribe(batenteSensor, "contact", eventoSensorBatente)
	}

	state.ultimoBotao = state.ultimoBotao ?: [:]
	if (!state.ultimaDirecao) {
		state.ultimaDirecao = "fechar"
	}
	spazioRefresh()
	batenteRefresh()
}

private criarFilhos() {
	def dniSpazio = "${app.id}-spazio"
	if (!getChildDevice(dniSpazio)) {
		addChildDevice("sergiommms", "DEA Spazio Portao Seccionado", dniSpazio, null,
			[label: "Portão seccionado", completedSetup: true])
	}

	def dniBatente = "${app.id}-batente"
	def batente = getChildDevice(dniBatente)
	if (criarBatente != false && !batente) {
		addChildDevice("sergiommms", "Extel Portao Batente", dniBatente, null,
			[label: "Portão de batente", completedSetup: true])
	} else if (criarBatente == false && batente) {
		deleteChildDevice(dniBatente)
	}
}

private getFilhoSpazio()  { getChildDevice("${app.id}-spazio") }
private getFilhoBatente() { getChildDevice("${app.id}-batente") }

// ---------------------------------------------------------------------------------------------------------------------
// Comando Extel: eventos de botão
// ---------------------------------------------------------------------------------------------------------------------

def eventoBotao(evt) {
	Integer numero = numeroDoBotao(evt)
	if (!numero || numero < 1 || numero > 4) {
		log.debug "Botão ignorado: ${evt.value} ${evt.data}"
		return
	}

	// Pontes RF costumam repetir o mesmo código várias vezes por cada toque: num motor passo-a-passo
	// isso seria abrir-parar-fechar. Ignoramos repetições dentro da janela configurada.
	Long agora = now()
	Long janela = (debounceMs != null ? debounceMs : 1500) as Long
	String chave = "${numero}-${evt.value}"
	Long anterior = state.ultimoBotao?.get(chave) as Long
	if (anterior && agora - anterior < janela) {
		log.debug "Repetição do botão ${numero} ignorada (${agora - anterior} ms)"
		return
	}
	def ultimo = state.ultimoBotao ?: [:]
	ultimo[chave] = agora
	state.ultimoBotao = ultimo

	String acao = (evt.value == "held") ?
		(settings["acaoMantido${numero}"] ?: "nada") :
		(settings["acaoPremido${numero}"] ?: ACOES_PREDEFINIDAS[numero])

	log.info "Comando Extel: botão ${numero} ${evt.value} -> ${acao}"
	executarAcao(acao, "comando (botão ${numero})")
}

private Integer numeroDoBotao(evt) {
	// 1) dispositivo dedicado a um botão
	for (n in 1..4) {
		def d = settings["botaoDispositivo${n}"]
		if (d && d.id == evt.deviceId) {
			return n
		}
	}
	// 2) número de botão nos dados do evento
	try {
		def dados = evt.jsonData
		if (dados?.buttonNumber != null) {
			return dados.buttonNumber as Integer
		}
	} catch (e) {
		log.debug "Evento sem dados JSON: ${e}"
	}
	return 1
}

private executarAcao(String acao, String origem) {
	switch (acao) {
		case "extel_nativo":
			// O recetor Extel já atuou o batente; apenas registamos.
			filhoBatente?.atualizarOrigem(origem)
			break
		case "batente_uma":
			batenteComando("umaFolha", origem)
			break
		case "batente_duas":
			batenteComando("duasFolhas", origem)
			break
		case "spazio_abrir":
			spazioComando(estadoSpazio() == "opening" ? "parar" : "abrir", origem)
			break
		case "spazio_fechar":
			spazioComando(estadoSpazio() == "closing" ? "parar" : "fechar", origem)
			break
		case "spazio_alternar":
			spazioComando(estadoSpazio() in ["closed", "closing"] ? "abrir" : "fechar", origem)
			break
		case "spazio_passo":
			spazioComando("passo", origem)
			break
		case "spazio_parar":
			spazioComando("parar", origem)
			break
		case "spazio_parcial":
			spazioComando("parcial", origem)
			break
		default:
			break
	}
}

// ---------------------------------------------------------------------------------------------------------------------
// Portão seccionado DEA Spazio
// ---------------------------------------------------------------------------------------------------------------------

private String estadoSpazio() {
	filhoSpazio?.currentValue("door") ?: "unknown"
}

/**
 * Ponto de entrada único para comandar o seccionado (usado pelos botões e pelo dispositivo-filho).
 * comando: abrir | fechar | parar | passo | parcial
 */
def spazioComando(String comando, String origem = "app") {
	String atual = estadoSpazio()
	String direcao = state.ultimaDirecao ?: "fechar"
	log.debug "Spazio: '${comando}' pedido (estado ${atual}, última direção ${direcao}, origem ${origem})"

	switch (comando) {
		case "passo":
			if (!relePasso) { avisar("Não há relé START configurado"); return }
			impulsos(relePasso, 1)
			def r = proximoPasso(atual, direcao)
			aplicarEstado(r.estado, r.direcao, origem)
			return

		case "parcial":
			if (!releParcial) { avisar("Não há relé PED (parcial) configurado"); return }
			impulsos(releParcial, 1)
			aplicarEstado("opening", "abrir", "${origem}, parcial", (tempoParcialS ?: 6) as Integer, "open")
			return

		case "parar":
			if (releParar) {
				impulsos(releParar, 1)
				aplicarEstado(atual in ["opening", "closing"] ? "unknown" : atual, direcao, origem)
				return
			}
			break

		case "abrir":
			if (releAbrir) {
				impulsos(releAbrir, 1)
				aplicarEstado("opening", "abrir", origem)
				return
			}
			break

		case "fechar":
			if (releFechar) {
				impulsos(releFechar, 1)
				aplicarEstado("closing", "fechar", origem)
				return
			}
			break

		default:
			log.warn "Comando desconhecido para o seccionado: ${comando}"
			return
	}

	// Sem relé dedicado: usar a entrada START e seguir a sequência passo-a-passo da central.
	if (!relePasso) {
		avisar("Não há relé START configurado para '${comando}'")
		return
	}
	def plano = planearPassos(atual, direcao, comando)
	if (plano == null) {
		avisar("Não é possível '${comando}' só com o passo-a-passo na sequência ${SEQUENCIAS[sequencia ?: '4passos']} (estado ${atual}). Configure o relé STOP.")
		return
	}
	if (plano.impulsos == 0) {
		log.debug "Spazio já está no estado pedido (${atual})"
		return
	}
	impulsos(relePasso, plano.impulsos)
	aplicarEstado(plano.estado, plano.direcao, origem)
}

/**
 * Próximo estado da central DEA após um impulso na entrada START.
 * "unknown" = parado a meio do curso.
 */
private Map proximoPasso(String estado, String direcao) {
	boolean inverte = (sequencia == "2passos")
	switch (estado) {
		case "closed":  return [estado: "opening", direcao: "abrir"]
		case "open":    return [estado: "closing", direcao: "fechar"]
		case "opening": return inverte ? [estado: "closing", direcao: "fechar"] : [estado: "unknown", direcao: "abrir"]
		case "closing": return inverte ? [estado: "opening", direcao: "abrir"] : [estado: "unknown", direcao: "fechar"]
		default:        return (direcao == "abrir") ? [estado: "closing", direcao: "fechar"] : [estado: "opening", direcao: "abrir"]
	}
}

private boolean objetivoAtingido(String estado, String comando) {
	switch (comando) {
		case "abrir":  return estado in ["opening", "open"]
		case "fechar": return estado in ["closing", "closed"]
		case "parar":  return estado in ["unknown", "open", "closed"]
		default:       return false
	}
}

/** Simula até 4 impulsos e devolve quantos são precisos e o estado final, ou null se impossível. */
private Map planearPassos(String estado, String direcao, String comando) {
	String e = estado
	String d = direcao
	for (int n = 0; n <= 4; n++) {
		if (objetivoAtingido(e, comando)) {
			return [impulsos: n, estado: e, direcao: d]
		}
		def r = proximoPasso(e, d)
		e = r.estado
		d = r.direcao
	}
	return null
}

private impulsos(rele, int quantos) {
	Integer duracao = (duracaoImpulsoMs ?: 600) as Integer
	Integer intervalo = (intervaloImpulsosMs ?: 1500) as Integer
	for (int i = 0; i < quantos; i++) {
		Integer t = i * intervalo
		if (usarPush && rele.hasCommand("push")) {
			t ? rele.push([delay: t]) : rele.push()
		} else {
			t ? rele.on([delay: t]) : rele.on()
			rele.off([delay: t + duracao])
		}
	}
	log.debug "Spazio: ${quantos} impulso(s) em ${rele.displayName}"
}

/**
 * Atualiza o dispositivo-filho e agenda o fim de curso quando o portão fica em movimento.
 */
private aplicarEstado(String estado, String direcao, String origem, Integer tempo = null, String estadoFinal = null) {
	state.ultimaDirecao = direcao
	filhoSpazio?.atualizarEstado(estado, direcao, origem)
	unschedule("spazioFimCurso")
	if (estado in ["opening", "closing"]) {
		state.estadoFinalEsperado = estadoFinal ?: (estado == "opening" ? "open" : "closed")
		runIn((tempo ?: (tempoCursoS ?: 20)) as Integer, "spazioFimCurso")
	}
	if (estado in ["opening", "closing"] && notificar) {
		sendPush("${filhoSpazio?.displayName ?: 'Portão seccionado'}: ${estado == 'opening' ? 'a abrir' : 'a fechar'} (${origem})")
	}
	agendarAvisoAberto(estado)
}

def spazioFimCurso() {
	String atual = estadoSpazio()
	if (!(atual in ["opening", "closing"])) {
		return
	}
	String esperado = state.estadoFinalEsperado ?: (atual == "opening" ? "open" : "closed")

	// Com sensores, só confirmamos o fim de curso se o sensor concordar.
	if (esperado == "closed" && sensorFechado && sensorFechado.currentValue("contact") != "closed") {
		log.warn "Spazio: tempo de curso esgotado mas o sensor não indica fechado (obstáculo / fotocélula?)"
		filhoSpazio?.atualizarEstado("unknown", state.ultimaDirecao, "fim de curso não confirmado")
		agendarAvisoAberto("unknown")
		return
	}
	filhoSpazio?.atualizarEstado(esperado, state.ultimaDirecao, null)
	agendarAvisoAberto(esperado)
}

def eventoSensorFechado(evt) {
	String atual = estadoSpazio()
	if (evt.value == "closed") {
		unschedule("spazioFimCurso")
		state.ultimaDirecao = "fechar"
		filhoSpazio?.atualizarEstado("closed", "fechar", null)
		agendarAvisoAberto("closed")
		if (notificar) sendPush("${filhoSpazio?.displayName ?: 'Portão seccionado'}: fechado")
	} else if (atual == "closed") {
		// Abriu sem passar por esta SmartApp (comando DEA original, botoneira, desbloqueio manual...).
		aplicarEstado("opening", "abrir", "externo")
	}
}

def eventoSensorAberto(evt) {
	if (evt.value == "closed") {
		unschedule("spazioFimCurso")
		state.ultimaDirecao = "abrir"
		filhoSpazio?.atualizarEstado("open", "abrir", null)
		agendarAvisoAberto("open")
	} else if (estadoSpazio() == "open") {
		aplicarEstado("closing", "fechar", "externo")
	}
}

def spazioRefresh() {
	if (sensorFechado?.currentValue("contact") == "closed") {
		filhoSpazio?.atualizarEstado("closed", "fechar", null)
	} else if (sensorAberto?.currentValue("contact") == "closed") {
		filhoSpazio?.atualizarEstado("open", "abrir", null)
	} else if (sensorFechado || sensorAberto) {
		if (!(estadoSpazio() in ["opening", "closing"])) {
			filhoSpazio?.atualizarEstado(sensorFechado ? "unknown" : estadoSpazio(), state.ultimaDirecao, null)
		}
	}
}

private agendarAvisoAberto(String estado) {
	unschedule("avisoAberto")
	Integer min = (avisoAbertoMin ?: 0) as Integer
	if (min > 0 && estado in ["open", "unknown"]) {
		runIn(min * 60, "avisoAberto")
	}
}

def avisoAberto() {
	if (estadoSpazio() in ["open", "unknown"]) {
		sendPush("${filhoSpazio?.displayName ?: 'Portão seccionado'} está aberto há mais de ${avisoAbertoMin} minutos")
	}
}

// ---------------------------------------------------------------------------------------------------------------------
// Portão de batente Extel
// ---------------------------------------------------------------------------------------------------------------------

/** comando: umaFolha | duasFolhas */
def batenteComando(String comando, String origem = "app") {
	def rele = (comando == "umaFolha") ? releUmaFolha : releDuasFolhas
	if (!rele) {
		avisar("Não há relé configurado para o batente '${comando == 'umaFolha' ? 'uma folha' : 'duas folhas'}'")
		return
	}
	impulsos(rele, 1)
	filhoBatente?.atualizarOrigem("${origem}, ${comando == 'umaFolha' ? 'uma folha' : 'duas folhas'}")
}

def eventoSensorBatente(evt) {
	filhoBatente?.atualizarContacto(evt.value)
}

def batenteRefresh() {
	if (batenteSensor) {
		filhoBatente?.atualizarContacto(batenteSensor.currentValue("contact") ?: "closed")
	}
}

// ---------------------------------------------------------------------------------------------------------------------

private avisar(String msg) {
	log.warn msg
}
