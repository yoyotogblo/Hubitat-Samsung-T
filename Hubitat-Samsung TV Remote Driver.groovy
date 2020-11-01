/*	===== HUBITAT INTEGRATION VERSION =====================================================
Hubitat - Samsung TV Remote Driver
		Copyright 2020 Dave Gutheinz

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this  file
except in compliance with the License. You may obtain a copy of the License at:
		http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the
License is distributed on an  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
either express or implied. See the License for the specific language governing permissions
and limitations under the  License.
===== DISCLAIMERS =========================================================================
		THE AUTHOR OF THIS INTEGRATION IS NOT ASSOCIATED WITH SAMSUNG. THIS CODE USES
		TECHNICAL DATA DERIVED FROM GITHUB SOURCES AND AS PERSONAL INVESTIGATION.
===== APPRECIATION ========================================================================
	Hubitat user Cal for technical, test, and emotional support.
	The GitHub WebSockets personnel for node.js code "ws" used in the external server
	GitHub user Toxblh for exlempary code for numerous commands
	Hubitat users who supported validation of 2016 - 2020 models.
===== REQUIREMENTS ========================================================================
a.	For model years 2017 and later, a stand-alone node.js server installed IAW provided
	instructions and running.
b.	This driver installed and configured IAW provided instructions.
===== RELEASE NOTES =======================================================================
Beta 1.0	Initial release
Beta 1.1	Updated to work WITHOUT an external NodeJs Server (2017 + models
			1.	Changed youTube launch to Home Page.
			2.	Added Netflix key
*/
def driverVer() { return "Beta 1.1" }
import groovy.json.JsonOutput
metadata {
	definition (name: "Hubitat-Samsung Remote",
				namespace: "davegut",
				author: "David Gutheinz",
				importUrl: "https://raw.githubusercontent.com/DaveGut/Hubitat-Samsung-T/main/Hubitat-Samsung%20TV%20Remote%20Driver.groovy"
			   ){
		//	===== Basic Commands for interface ==
		command "sendKey", ["string"]	//	Allows you to experiment or use keys not defined in this interface.
										//	Format is simply key name without "KEY_", i.e., "HDMI", "TV"
		command "close"					//	Force socket close.

		//	===== Samsung Smart Remote Keys =====
		attribute "wsDeviceStatus", "string"
		capability "Switch"				//	Creates commands on and off
										//	Note: after power off, wait at least 60 seconds to try power on.
		capability "TV"					//	volumeUp/Down, channelUp/Down
		command "numericKeyPad"			//	123 (bring up numeric and color keypad)
		command "artModeOn"				//	\\
		command "artModeOff"			//	  ==Frame TV Only
		command "artModeStatus"			//	//
		attribute "artModeStatus", "string"
		command "ambientMode"			//	non-Frame TVs
		command "arrowLeft"				//	||\
		command "arrowRight"			//	||\\
		command "arrowUp"				//	||	==Center martix
		command "arrowDown"				//	||//
		command "enter"					//	||/
		command "exit"					//	Exit / Back
		command "home"					//	Opens home menu at bottom of display
		command "mute"					//	||//
		command "guide"					//	||//
		//	===== Direct access Keys =====
		command "menu"					//	Main menu with access to system settings.
		command "source"				//	Pops up home with cursor at source.  Use left/right/enter to select.
		command "info"					//	Pops up upper display of currently playing channel
		command "channelList"			//	Pops up short channel-list that allows faster navigation to favorites.
		command "TV"					//	Direct to source TV
		command "source1"				//	Direct to source 1 (one right of TV on menu)
		command "source2"				//	Direct to source 1 (two right of TV on menu)
		command "source3"				//	Direct to source 1 (three right of TV on menu)
		command "source4"				//	Direct to source 1 (ofour right of TV on menu)
		//	===== Other implemented commands =====
		command "previousChannel"		//	Goes to previous played channel.
		command "hdmi"					//	SLOW progression through available sources.
		command "fastBack"				//	Fast Back vice jump forward
		command "fastForward"			//	Fast Forward vice jump forward
		//	===== Media Commands =====
		command "browser"				//	Opens browser
		command "youTube"				//	Opens youTube.
		command "netflix"				//	Opens youTube.
		//	===== Button Interface =====
		capability "PushableButton"
		command "push", ["NUMBER"]
	}
	preferences {
		input ("deviceIp", "text", title: "Samsung TV Ip")
		def tvModes = ["AMBIENT", "ART_MODE", "TV", "Source1", "Source2", "Source3", "Source4", "none"]
		input ("tvPwrOnMode", "enum", title: "TV Startup Display", options: tvModes, defalutValue: "none")
		input ("debugLpg", "bool",  title: "Enable debug logging for 30 minutes", defaultValue: true)
		input ("infoLog", "bool",  title: "Enable description text logging", defaultValue: true)
	}
}

//	===== Installation, setup and update =====
def installed() {
	state.token = "12345678"
	updateDataValue("name", "Hubitat Samsung Remote")
	updateDataValue("name64", "Hubitat Samsung Remote".encodeAsBase64().toString())
}
def updated() {
	logInfo("updated")
	unschedule()
	sendEvent(name: "numberOfButtons", value: "50")
	sendEvent(name: "wsDeviceStatus", value: "closed")
	if (debugLog) { runIn(1800, debugLogOff) }
	if (!getDataValue("uuid")) {
		def tokenSupport = getDeviceData()
		logInfo("Performing test using tokenSupport = ${tokenSupport}")
		checkInstall()
	}
}
def getDeviceData() {
	logInfo("getDeviceData: Updating Device Data.")
	def tokenSupport = "false"
	try{
		httpGet([uri: "http://${deviceIp}:8001/api/v2/", timeout: 5]) { resp ->
			updateDataValue("deviceMac", resp.data.device.wifiMac)
			def modelYear = "20" + resp.data.device.model[0..1]
			updateDataValue("modelYear", modelYear)
			def frameTv = "false"
			if (resp.data.device.FrameTVSupport) {
				frameTv = resp.data.device.FrameTVSupport
			}
			updateDataValue("frameTv", frameTv)
			if (resp.data.device.TokenAuthSupport) {
				tokenSupport = resp.data.device.TokenAuthSupport
			}
			def uuid = resp.data.device.duid.substring(5)
			updateDataValue("uuid", uuid)
			updateDataValue("tokenSupport", tokenSupport)
			logInfo("getDeviceData: year = $modelYear, frameTv = $frameTv, tokenSupport = $tokenSupport")
		} 
	} catch (error) {
		logWarn("getDeviceData: Failed.  TV may be powered off.  Error = ${error}")
	}
		
	return tokenSupport
}
def checkInstall() {
	connect("remote")
	pauseExecution(10000)
	menu()
	pauseExecution(2000)
	close()
}

//	===== Web Socket Interface Methods  ==
def connect(funct = "remote") { sendWsCmd(funct, "connect") }
def sendKey(key, cmd = "Click") {
	key = "KEY_${key.toUpperCase()}"
	def data = [method:"ms.remote.control",
				params:[Cmd:"${cmd}",
						DataOfCmd:"${key}",
						TypeOfRemote:"SendRemoteKey"]]
	sendWsCmd("remote", "sendMessage", JsonOutput.toJson(data) )
}
def close(funct = "remote") { sendWsCmd(funct, "close") }

//	===== Communications =====
def sendWsCmd(funct, command, data = "") {
	logDebug("sendWsCmd: function = ${funct} | command = ${command} | data = ${data}")
	def name = getDataValue("name64")
	if (command == "close") {
		unschedule(close)
	} else if (funct == "remote") {
		runIn(300, close, [data: function])
	} else if (funct == "frameArt") {
		runIn(10, close, [data: function])
	}
	def url
	if (getDataValue("tokenSupport") == "true") {
		def token = state.token
		if (funct == "remote") {
			url = "wss://${deviceIp}:8002/api/v2/channels/samsung.remote.control?name=${name}&token=${token}"
		} else if (funct == "frameArt") {
			url = "wss://${deviceIp}:8002/api/v2/channels/com.samsung.art-app?name=${name}&token=${token}"
		} else {
			logWarn("sendWsCmd: Invalid Function = ${funct}, tokenSupport = true")
		}
		if (command == "connect") {
				interfaces.webSocket.connect(url, ignoreSSLIssues: true)
		} else if (command == "sendMessage") {
			if (getDataValue("wsDeviceStatus") != "open") {
				interfaces.webSocket.connect(url, ignoreSSLIssues: true)
				pauseExecution(500)
			}
			interfaces.webSocket.sendMessage(data)
		} else if (command == "close") {
			interfaces.webSocket.close()
		} else {
			logWarn("sendWsCmd: Invalid Command = ${command}")
		}
	} else {
		if (funct == "remote") {
			url = "ws://${deviceIp}:8001/api/v2/channels/samsung.remote.control?name=${name}"
		} else if (funct == "frameArt") {
			url = "ws://${deviceIp}:8001/api/v2/channels/com.samsung.art-app?name=${name}"
		} else {
			logWarn("sendWsCmd: Invalid Function = ${funct}, tokenSupport = false")
		}
		if (command == "sendMessage") {
			if (getDataValue("wsDeviceStatus") != "open") {
				interfaces.webSocket.connect(url)
				pauseExecution(500)
			}
			interfaces.webSocket.sendMessage(data)
		} else if (command == "close") {
			interfaces.webSocket.close()
		} else {
			logWarn("sendWsCmd: Invalid Command = ${command}")
		}
	}
}

def wsHubParse(response) {
	//	===== NodeJs Return Parse =====
	def command = response.headers.command
	def hubStatus = response.headers.hubStatus
	def respType = response.headers.respType
	def message = response.headers.message
	def logMsg = "wsHubParse: command = ${command}"
	if (respType == "close" || command == "close") {
		sendEvent(name: "wsDeviceStatus", value: "closed")
		logInfo("wsHubParse: wsDeviceStatus = closed")
		logMsg += ", ws Closed. ${message}"
	} else if(respType == "open") {
		sendEvent(name: "wsDeviceStatus", value: "open")
		logInfo("wsHubParse: wsDeviceStatus = open")
		logMsg += ", message sent to parse"
		parse(parseJson(message).resp)
	} else if(respType == "message") {
		if (message != "default") {
			logMsg += ", message sent to parse"
			parse(parseJson(message).resp)
		}
	} else if(respType == "error") {
		logMsg += ", ERROR = ${message}"
	}
	logDebug(logMsg)
}

def parse(message) {
	logDebug("parse: ${message}")
	message = parseJson(message)
	def event = message.event
	def logMsg = "parse: event = ${event}"
	if (event == "ms.channel.connect") {
		logMsg += ", webSocket Open"
		def newToken = message.data.token
		if (newToken != null && newToken != state.token) {
			logMsg += ", token updated to ${newToken}"
			logInfo("parse: Token updated to ${newToken}")
			state.token = newToken
		}
	} else if (event == "ms.error") {
		logMsg += "Error Event.  Closing webSocket"
		close{}
	} else {
		logMsg += ", message = <b>${message}"
	}
	logDebug(logMsg)
}

def webSocketStatus(message) {
	logDebug("webSocketStatus: ${message}")
	if (message == "status: open") {
		sendEvent(name: "wsDeviceStatus", value: "open")
		logInfo("wsHubParse: wsDeviceStatus = open")
	} else if (message == "status: closing") {
		sendEvent(name: "wsDeviceStatus", value: "closed")
		logInfo("wsHubParse: wsDeviceStatus = closed")
	}
}

//	===== Samsung Smart Remote Keys =====
def on() {
	logDebug("on: desired TV Mode = ${tvPwrOnMode}")
	def newMac = getDataValue("deviceMac").replaceAll(":","").replaceAll("-","")
	def result = new hubitat.device.HubAction (
		"wake on lan $newMac",
		hubitat.device.Protocol.LAN,
		null
	)
	sendHubCommand(result)
	sendEvent(name: "switch", value: "on")
	pauseExecution(5000)
	if(tvPwrOnMode == "ART_MODE") { artModeOn() }
	else { sendKey(tvPwrOnMode) }
}
def off() {
	sendEvent(name: "switch", value: "off")
	if (getDataValue("frameTv") == "false") { sendKey("POWER") }
	else {
		sendKey("POWER", "Press")
		pauseExecution(3000)
		sendKey("POWER", "Release")
	}
}
def numericKeyPad() { sendKey("MORE") }
def artModeOn() {
	if(getDataValue("frameTv") == "true") {
		artModeData("on") 
	} else { logWarn("artModeOn: not available") }
}
def artModeOff() {
	if(getDataValue("frameTv") == "true") {
		artModeData("off") 
	} else { logWarn("artModeOff: not available") }
}
def artModeData(cmd) {
	def data = [value:"${cmd}",
				request:"set_artmode_status",
				id: "${getDataValue("uuid")}"]
	data = JsonOutput.toJson(data)		//groovy version of json.stringify
	artModeCmd(data)
}
def artModeStatus() {
	if(getDataValue("frameTv") == "true") {
		def data = [request:"get_artmode_status",
					id: "${getDataValue("uuid")}"]
		data = JsonOutput.toJson(data)
		artModeCmd(data)
	} else { logWarn("artModeStatus: not available") }
}
def artModeCmd(data) {
	def cmdData = [method:"ms.channel.emit",
				   params:[data:"${data}",
						   to:"host",
						   event:"art_app_request"]]
	cmdData = JsonOutput.toJson(cmdData)
	if (device.currentValue("wsDeviceStatus") == "open") {
		close("remote")								//	Close existing remote websocket
	}
	pauseExecution(500)
	sendWsCmd("frameArt", "sendMessage", cmdData)	//	send command, connect is automatic.
}
def ambientMode() { sendKey("AMBIENT") }
def arrowLeft() { sendKey("LEFT") }
def arrowRight() { sendKey("RIGHT") }
def arrowUp() { sendKey("UP") }
def arrowDown() { sendKey("DOWN") }
def enter() { sendKey("ENTER") }
def exit() { sendKey("EXIT") }
def home() { sendKey("HOME") }
def volumeUp() { sendKey("VOLUP") }
def volumeDown() { sendKey("VOLDOWN") }
def mute() { sendKey("MUTE") }
def channelUp() { sendKey("CHUP") }
def channelDown() { sendKey("CHDOWN") }
def guide() { sendKey("GUIDE") }

//	===== Direct access Keys =====
def menu() { sendKey("MENU") }
def source() { sendKey("SOURCE") }
def info() { sendKey("INFO") }
def channelList() { sendKey("CH_LIST") }
def startSource() {
	sendKey("SOURCE")
	pauseExecution(1000)
	sendKey("LEFT")
	pauseExecution(200)
	sendKey("LEFT")
	pauseExecution(200)
	sendKey("LEFT")
	pauseExecution(200)
	sendKey("LEFT")
	pauseExecution(200)
	sendKey("LEFT")
	pauseExecution(200)
}
def TV() {
	startSource()
	sendKey("ENTER")
}
def source1() {
	startSource()
	sendKey("RIGHT")
	pauseExecution(200)
	sendKey("ENTER")
}
def source2() {
	startSource()
	sendKey("RIGHT")
	pauseExecution(200)
	sendKey("RIGHT")
	pauseExecution(200)
	sendKey("ENTER")
}
def source3() {
	startSource()
	sendKey("RIGHT")
	pauseExecution(200)
	sendKey("RIGHT")
	pauseExecution(200)
	sendKey("RIGHT")
	pauseExecution(200)
	sendKey("ENTER")
}
def source4() {
	startSource()
	sendKey("RIGHT")
	pauseExecution(200)
	sendKey("RIGHT")
	pauseExecution(200)
	sendKey("RIGHT")
	pauseExecution(200)
	sendKey("RIGHT")
	pauseExecution(200)
	sendKey("ENTER")
}

//	===== Other implemented commands =====
def previousChannel() { sendKey("PRECH") }
def hdmi() { sendKey("HDMI") }
def fastBack() {
	sendKey("LEFT", "Press")
	pauseExecution(1000)
	sendKey("LEFT", "Release")
}
def fastForward() {
	sendKey("RIGHT", "Press")
	pauseExecution(1000)
	sendKey("RIGHT", "Release")
}
def browser() { sendKey("CONVERGENCE") }
def youTube() {
	def url = "http://${deviceIp}:8080/ws/apps/YouTube"
	httpPost(url, "") { resp ->
		logDebug("youTube:  ${resp.status}  ||  ${resp.data}")
	}
}
def netflix() {
	def url = "http://${deviceIp}:8080/ws/apps/Netflix"
	httpPost(url, "") { resp ->
		logDebug("netflix:  ${resp.status}  ||  ${resp.data}")
	}
}
//	===== Button Interface (facilitates dashboard integration) =====
def push(pushed) {
	logDebug("push: button = ${pushed}, trigger = ${state.triggered}")
	if (pushed == null) {
		logWarn("push: pushed is null.  Input ignored")
		return
	}
	sendEvent(name: "pushed", value: pushed)
	pushed = pushed.toInteger()
	switch(pushed) {
		case 0 : close(); break
		//	===== Physical Remote Commands =====
		case 1 : on(); break
		case 2 : off(); break
		case 3 : numericKeyPad(); break
		case 4 : artModeOn(); break
		case 5 : artModeOff(); break
		case 6 : artModeStatus(); break
		case 7 : ambientMode(); break
		case 8 : arrowLeft(); break
		case 9 : arrowRight(); break
		case 10: arrowUp(); break
		case 11: arrowDown(); break
		case 12: enter(); break
		case 13: exit(); break
		case 14: home(); break
		case 15: volumeUp(); break
		case 16: volumeDown(); break
		case 17: mute(); break
		case 18: channelUp(); break
		case 19: channelDown(); break
		case 20: guide(); break
		//	===== Direct Access Functions
		case 23: menu(); break			//	Main menu with access to system settings.
		case 24: source(); break		//	Pops up home with cursor at source.  Use left/right/enter to select.
		case 25: info(); break			//	Pops up upper display of currently playing channel
		case 26: channelList(); break	//	Pops up short channel-list.
		case 27: source0(); break		//	Direct to source TV
		case 28: source1(); break		//	Direct to source 1 (one right of TV on menu)
		case 29: source2(); break		//	Direct to source 1 (two right of TV on menu)
		case 30: source3(); break		//	Direct to source 1 (three right of TV on menu)
		case 31: source4(); break		//	Direct to source 1 (ofour right of TV on menu)
		//	===== Other Commands =====
		case 34: previousChannel(); break
		case 35: hdmi(); break			//	Brings up next available source
		case 36: fastBack(); break		//	causes fast forward
		case 37: fastForward(); break	//	causes fast rewind
		case 38: browser(); break		//	Direct to source 1 (ofour right of TV on menu)
		case 39: youTube(); break
		case 40: netflix(); break
		default:
			logDebug("push: Invalid Button Number!")
			break
	}
}

//	===== Logging =====
def logInfo(msg) { 
	if (infoLog == true) {
		log.info "${driverVer()} || ${msg}"
	}
}
def debugLogOff() {
	device.updateSetting("debugLog", [type:"bool", value: false])
	logInfo("Debug logging is false.")
}
def logDebug(msg) {
	if (debugLog == true) {
		log.debug "${driverVer()} || ${msg}"
	}
}
def logWarn(msg) { log.warn "${driverVer()} || ${msg}" }