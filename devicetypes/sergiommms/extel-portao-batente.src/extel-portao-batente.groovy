/**
 *  Extel - Portão de Batente (duas folhas)
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
 *  Representa o automatismo Extel de portão de batente com duas folhas. O comando Extel continua a
 *  falar diretamente com a central Extel (botão "uma folha" e botão "duas folhas"); este dispositivo
 *  serve para comandar o mesmo portão a partir da app, através de relés opcionais ligados às entradas
 *  de botoeira da central Extel, e para mostrar o estado se existir um sensor de contacto.
 */
metadata {
	definition (name: "Extel Portao Batente", namespace: "sergiommms", author: "sergiommms") {
		capability "Actuator"
		capability "Contact Sensor"
		capability "Momentary"
		capability "Refresh"
		capability "Sensor"

		attribute "ultimaOrigem", "string"

		command "abrirUmaFolha"
		command "abrirDuasFolhas"
	}

	tiles(scale: 2) {
		standardTile("contact", "device.contact", width: 6, height: 4) {
			state "closed", label: "Fechado", icon: "st.doors.garage.garage-closed", backgroundColor: "#00A0DC"
			state "open",   label: "Aberto",  icon: "st.doors.garage.garage-open",   backgroundColor: "#e86d13"
		}
		standardTile("umaFolha", "device.contact", width: 3, height: 2, decoration: "flat") {
			state "default", label: "Uma folha", action: "abrirUmaFolha", icon: "st.doors.garage.garage-opening"
		}
		standardTile("duasFolhas", "device.contact", width: 3, height: 2, decoration: "flat") {
			state "default", label: "Duas folhas", action: "abrirDuasFolhas", icon: "st.doors.garage.garage-opening"
		}
		valueTile("origem", "device.ultimaOrigem", width: 4, height: 2, decoration: "flat") {
			state "default", label: 'Último: ${currentValue}'
		}
		standardTile("refresh", "device.contact", width: 2, height: 2, decoration: "flat") {
			state "default", label: "", action: "refresh.refresh", icon: "st.secondary.refresh"
		}

		main "contact"
		details(["contact", "umaFolha", "duasFolhas", "origem", "refresh"])
	}
}

def installed() {
	sendEvent(name: "contact", value: "closed")
	sendEvent(name: "ultimaOrigem", value: "-")
}

def parse(String description) {
	null
}

def abrirUmaFolha()   { parent?.batenteComando("umaFolha", "app") }
def abrirDuasFolhas() { parent?.batenteComando("duasFolhas", "app") }
def push()            { abrirDuasFolhas() }
def refresh()         { parent?.batenteRefresh() }

def atualizarContacto(String contact) {
	sendEvent(name: "contact", value: contact)
}

def atualizarOrigem(String origem) {
	sendEvent(name: "ultimaOrigem", value: origem, displayed: false)
}
