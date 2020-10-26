/*
Test only.  Light the programming fire for WS Samsung Remote.  Future:
a.	Add capability to open an installed application.
b.	Add a sendKey command buffer (if testing indicates).
c.	Add text box capability (if it works).
*/
def driverVer() { return "WS V2" }
def traceLog() { return true }
import groovy.json.JsonOutput
metadata {
	definition (name: "Hubitat-Samsung Remote",
				namespace: "davegut",
				author: "David Gutheinz",
				importUrl: ""
			   ){
		//	===== Basic Commands for interface ==
		command "sendKey", ["string"]	//	Allows you to experiment or use keys not defined in this interface.
										//	Format is simply key name without "KEY_", i.e., "HDMI", "TV"
		command "connect"				//	Force connect.  Required on 2016/17 models, not required on later
										//	models except when trying to get the token.
		command "close"					//	Force socket close.

		//	===== Samsung Smart Remote Keys =====
		capability "Switch"				//	Creates commands on and off
										//	Note: after power off, wait at least 60 seconds to try power on.
		command "numericKeyPad"			//	123 (bring up numeric and color keypad)
		command "toggleArt"				//	Frame TVs.  Provided as adjunct to ambientMode
		command "ambientMode"			//	non-Frame TVs
		command "arrowLeft"				//	||\
		command "arrowRight"			//	||\\
		command "arrowUp"				//	||	==Center martix
		command "arrowDown"				//	||//
		command "enter"					//	||/
		command "exit"					//	Exit / Back
		command "home"					//	Opens home menu at bottom of display
		//command "playPause"			//	Key play/pause not currently initiated.
		command "volumeUp"				//	||\\
		command "volumeDown"			//	||	==Left toggle/push switch
		command "mute"					//	||//
		command "channelUp"				//	||\\
		command "channelDown"			//	||  ==Right toggle/push switch
		command "guide"					//	||//
		command "browser"				//	Opens browser
		
		//	===== Direct access Keys =====
		command "menu"					//	Main menu with access to system settings.
		command "source"				//	Pops up home with cursor at source.  Use left/right/enter to select.
		command "info"					//	Pops up upper display of currently playing channel
		command "channelList"			//	Pops up short channel-list that allows faster navigation to favorites.
		
		//	===== Other implemented commands =====
		command "TV"					//	Goes to TV display.
		command "previousChannel"		//	Goes to previous played channel.
		command "HDMI"					//	SLOW progression through available sources.
	}
	preferences {
		input ("deviceIp", "text", title: "Samsung TV Ip")
		input ("hubIp", "text", title: "NodeJs Hub IP (18,19,20 Models")
		input ("hubPort", "text", title: "NodeJs Hub Port (18/19/20 Models)", defaultValue: "8080")
		def tvModes = ["AMBIENT", "ART_MODE", "none"]
		input ("tvPwrOnMode", "enum", title: "TV Startup Display", options: tvModes, defalutValue: "none")
		input ("debug", "bool",  title: "Enable debug logging", defaultValue: false)
		input ("info", "bool",  title: "Enable description text logging", defaultValue: false)
	}
}
def installed() {
	state.token = "12345678"
	updateDataValue("name", "Hubitat Samsung Remote")
	updateDataValue("name64", "Hubitat Samsung Remote".encodeAsBase64().toString())
}
def updated() {
	logInfo("updated: get device data and model year")
	unschedule()
	def tokenSupport = getDeviceData()
	runIn(2, checkInstall)
}
def getDeviceData() {
	logInfo("getDeviceData: Updating Device Data.")
	def tokenSupport = "false"
	try{
		httpGet([uri: "http://${deviceIp}:8001/api/v2/", timeout: 5]) { resp ->
			updateDataValue("deviceMac", resp.data.device.wifiMac)
			def modelYear = "20" + resp.data.device.model[0..1]
			updateDataValue("modelYear", modelYear)
			def frameTv = "true"
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
			logDebug("getDeviceData: year = $modelYear, frameTv = $frameTv, tokenSupport = $tokenSupport")
			logTrace("getDeviceData: year = $modelYear, frameTv = $frameTv, tokenSupport = $tokenSupport")
		} 
	} catch (error) {  }
		
	return tokenSupport
}
def checkInstall() {
	logInfo("<b>Performing test using tokenSupport = ${getDataValue("tokenSupport")}")
	logTrace("<b>Performing test using tokenSupport = ${getDataValue("tokenSupport")}")
	connect()
	pauseExecution(10000)
	menu()
	pauseExecution(2000)
	close()
}

//	===== Basic Commands for interface ==
def connect() { sendWsCmd("connect") }
def sendKey(key, cmd = "Click") {
	key = "KEY_${key.toUpperCase()}"
	def data = """{"method":"ms.remote.control","params":{"Cmd":"${cmd}",""" +
		""""DataOfCmd":"${key}","Option":"false","TypeOfRemote":"SendRemoteKey"}}"""
	sendWsCmd("sendMessage", data)
}
def close() {
	sendWsCmd("close")
}

//	===== Samsung Smart Remote Keys =====
def on() {
	def newMac = getDataValue("deviceMac").replaceAll(":","").replaceAll("-","")
	logDebug("on: sending WOL packet to ${newMac}")
	logTrace("on: sending WOL packet to ${newMac}")
	def result = new hubitat.device.HubAction (
		"wake on lan $newMac",
		hubitat.device.Protocol.LAN,
		null
	)
	sendHubCommand(result)
	sendEvent(name: "switch", value: "on")
	if(tvPwrOnMode == "ART_MODE") { toggleArt() }
	else if(tvPwrOnMode == "AMBIENT") { sendKey("AMBIENT") }
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
def toggleArt() {
	if (getDataValue("frameTv") == "true") {
		sendKey("POWER")
	} else {
		logDebug("toggleArt only works for Frame TV's")
	}
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
def browser() { sendKey("CONVERGENCE") }

//	===== Direct access Keys =====
def menu() { sendKey("MENU") }
def source() { sendKey("SOURCE") }
def info() { sendKey("INFO") }
def channelList() { sendKey("CH_LIST") }

//	===== Other implemented commands =====
def TV() { sendKey("TV") }
def previousChannel() { sendKey("PRECH") }
def HDMI() { sendKey("HDMI") }

//	===== Communications =====
def sendWsCmd(command, data = "") {
//	logDebug("sendWsCmd: ${command} | ${data}")
	logTrace("sendWsCmd: ${command} | ${data}")
	def name = getDataValue("name64")
	def token = state.token
//	def url = "wss://${deviceIp}:8002/api/v2/channels/samsung.remote.control?name=${name}&token=${token}"
	def url = "https://${deviceIp}:8002/api/v2/channels/samsung.remote.control?name=${name}&token=${token}"
	if (getDataValue("tokenSupport") == "true") {
		def headers = [HOST: "${hubIp}:${hubPort}", command: command, data: data, url: url]
		sendHubCommand(new hubitat.device.HubAction([headers: headers],
												device.deviceNetworkId,
												[callback: wsHubParse]))

	} else {
		switch(command) {
			case "connect":
				url = "ws://${deviceIp}:8001/api/v2/channels/samsung.remote.control?name=${name}"
				interfaces.webSocket.connect(url)
				break
			case "sendMessage":
				interfaces.webSocket.sendMessage(data)
				break
			case "close":
				interfaces.webSocket.close()
				break
			default:
				logWarn("sendWsCmd: Invalid command")
		}
	}
}
def wsHubParse(response) {
	//	===== NodeJs Return Parse =====
	def command = response.headers.command
	def cmdResponse = response.headers.cmdResponse
	def hubStatus = response.headers.hubStatus
	def wsDeviceStatus = response.headers.wsDeviceStatus
	def data = "command = ${command} || hubStatus = ${hubStatus} || "
	data += "wsDeviceStatus = ${wsDeviceStatus} || cmdResponse = ${cmdResponse}"
	logDebug("wsHubParse: ${data}")
	logTrace("wsHubParse: ${data}")
	if (cmdResponse == "{}") { return }
	//	===== Check connect response for token update.
	def resp = parseJson(cmdResponse)
	def respData = parseJson(resp.cmdData)
	def newToken = respData.data.token
	if (newToken != state.token && newToken) {
		logDebug("wsHubParse: token updated to ${newToken}")
		logTrace("wsHubParse: token updated to ${newToken}")
		state.token = newToken
	}
}
def parse(message) {
	logTrace("parse: ${message}")
}
def webSocketStatus(message) {
	logTrace("webSocketStatus: ${message}")
}

//	===== TEST Commands do not work =====
def aArtModeOn() { artModeSet("on") }
def aArtModeOff() { artModeSet("off") }
def artModeSet(onOff) {
	def cmdData = JsonOutput.toJson([id:getDataValue("uuid"),value:onOff,request:"set_artmode_status"])
	def data = """{"method":"ms.channel.emit","params":{"event":"art_app_request",""" +
		""""to":"host","clientIp": "${deviceIp}","deviceName":"${getDataValue("name64")}",""" +
		""""data":"${cmdData}"}}"""
	log.trace data
	sendWsCmd("sendMessage", data)
}
def aGetAppsList() {
	def data = """{"method":"ms.channel.emit","params":{"event":"ed.installedApp.get","to":"host"}}"""
	log.trace data
	sendWsCmd("sendMessage", data)
}
def aSendText(text) {
	def data = """{"method":"ms.remote.control","params":{"Cmd":"${text.encodeAsBase64().toString()}",""" +
		""""DataOfCmd":"base64","typeOfRemote":"SendInputString"}}"""
	log.trace data
	sendWsCmd("sendMessage", data)
	pauseExecution(1000)
	data = """{"method":"ms.remote.control","params":{"typeOfRemote":"SendInputString"}}"""
	sendWsCmd("sendMessage", data)
}

//	===== Logging =====
def logTrace(msg) { 
	if (traceLog() == true) {
		log.trace "${driverVer()} || ${msg}"
	}
}
def logInfo(msg) { 
	if (info == true) {
		log.info "${driverVer()} || ${msg}"
	}
}
def logDebug(msg) {
	if (debug == true) {
		log.debug "${driverVer()} || ${msg}"
	}
}
def logWarn(msg) { log.warn "${driverVer()} || ${msg}" }