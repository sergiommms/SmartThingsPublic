/**
 *  DEA Spazio - Portão Seccionado
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
 *  Dispositivo-filho criado pela SmartApp "Extel Comando 4 Botões".
 *
 *  Representa um portão seccionado (abertura vertical) movido por um motor de teto DEA Spazio.
 *  Este dispositivo não fala diretamente com o hardware: cada comando (abrir, fechar, parar,
 *  abertura parcial, passo-a-passo) é delegado na SmartApp-mãe, que conhece os relés ligados
 *  às entradas da central DEA e mantém o estado do portão.
 */
metadata {
	definition (name: "DEA Spazio Portao Seccionado", namespace: "sergiommms", author: "sergiommms") {
		capability "Actuator"
		capability "Door Control"
		capability "Garage Door Control"
		capability "Contact Sensor"
		capability "Momentary"
		capability "Refresh"
		capability "Sensor"

		attribute "ultimaDirecao", "string"
		attribute "ultimaOrigem", "string"

		command "stop"
		command "parar"
		command "parcial"
		command "passo"
	}

	tiles(scale: 2) {
		multiAttributeTile(name: "door", type: "generic", width: 6, height: 4) {
			tileAttribute("device.door", key: "PRIMARY_CONTROL") {
				attributeState "closed",  label: "Fechado",  action: "door control.open",  icon: "st.doors.garage.garage-closed",  backgroundColor: "#00A0DC", nextState: "opening"
				attributeState "open",    label: "Aberto",   action: "door control.close", icon: "st.doors.garage.garage-open",    backgroundColor: "#e86d13", nextState: "closing"
				attributeState "opening", label: "A abrir",  action: "parar",              icon: "st.doors.garage.garage-opening", backgroundColor: "#e86d13"
				attributeState "closing", label: "A fechar", action: "parar",              icon: "st.doors.garage.garage-closing", backgroundColor: "#00A0DC"
				attributeState "unknown", label: "Parado",   action: "door control.close", icon: "st.doors.garage.garage-open",    backgroundColor: "#cccccc"
			}
			tileAttribute("device.ultimaOrigem", key: "SECONDARY_CONTROL") {
				attributeState "default", label: 'Último comando: ${currentValue}'
			}
		}
		standardTile("open", "device.door", width: 2, height: 2, decoration: "flat") {
			state "default", label: "Abrir", action: "door control.open", icon: "st.doors.garage.garage-opening"
		}
		standardTile("close", "device.door", width: 2, height: 2, decoration: "flat") {
			state "default", label: "Fechar", action: "door control.close", icon: "st.doors.garage.garage-closing"
		}
		standardTile("stop", "device.door", width: 2, height: 2, decoration: "flat") {
			state "default", label: "Parar", action: "parar", icon: "st.sonos.stop-btn"
		}
		standardTile("passo", "device.door", width: 2, height: 2, decoration: "flat") {
			state "default", label: "Passo-a-passo", action: "momentary.push", icon: "st.switches.switch.on"
		}
		standardTile("parcial", "device.door", width: 2, height: 2, decoration: "flat") {
			state "default", label: "Parcial", action: "parcial", icon: "st.doors.garage.garage-open"
		}
		standardTile("refresh", "device.door", width: 2, height: 2, decoration: "flat") {
			state "default", label: "", action: "refresh.refresh", icon: "st.secondary.refresh"
		}

		main "door"
		details(["door", "open", "close", "stop", "passo", "parcial", "refresh"])
	}
}

def installed() {
	sendEvent(name: "door", value: "unknown")
	sendEvent(name: "contact", value: "open")
	sendEvent(name: "ultimaOrigem", value: "-")
}

def parse(String description) {
	// Dispositivo virtual: não há mensagens do hub para interpretar.
	null
}

// ---- Comandos (delegados na SmartApp-mãe) ----

def open()    { parent?.spazioComando("abrir", "app") }
def close()   { parent?.spazioComando("fechar", "app") }
def stop()    { parent?.spazioComando("parar", "app") }
def parar()   { stop() }
def parcial() { parent?.spazioComando("parcial", "app") }
def passo()   { parent?.spazioComando("passo", "app") }
def push()    { passo() }
def refresh() { parent?.spazioRefresh() }

// ---- Chamado pela SmartApp-mãe para refletir o estado ----

def atualizarEstado(String door, String direcao = null, String origem = null) {
	sendEvent(name: "door", value: door, descriptionText: "${device.displayName}: ${traduzir(door)}")
	sendEvent(name: "contact", value: (door == "closed") ? "closed" : "open", displayed: false)
	if (direcao) {
		sendEvent(name: "ultimaDirecao", value: direcao, displayed: false)
	}
	if (origem) {
		sendEvent(name: "ultimaOrigem", value: origem, displayed: false)
	}
}

private traduzir(String door) {
	[open: "aberto", closed: "fechado", opening: "a abrir", closing: "a fechar", unknown: "parado"][door] ?: door
}
