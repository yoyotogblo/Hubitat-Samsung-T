/*
Test only.  Light the programming fire for WS Samsung Remote.  Future:
a.	Add capability to open an installed application.
b.	Add a sendKey command buffer (if testing indicates).
c.	Add text box capability (if it works).
*/
def driverVer() { return "WS V3.1.3" }
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
//		command "connect"				//	Force connect.  Required on 2016/17 models, not required on later
//										//	models except when trying to get the token.
		command "close"					//	Force socket close.

		//	===== Samsung Smart Remote Keys =====
		attribute "wsDeviceStatus", "string"
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
		command "previousChannel"		//	Goes to previous played channel.
		command "HDMI"					//	SLOW progression through available sources.
		
		//	===== Commands under test =====
		command "aArtModeOn"
		command "aArtModeOff"
		command "aArtModeStatus"
		attribute "artModeStatus", "string"
		command "aYouTube", ["string"]
		command "aLaunchBrowser", ["string"]
		command "aSource0"				//	TV
		command "aSource1"				//	one right of TV
		command "aSource2"				//	one right of TV
		command "aSource3"				//	one right of TV
		command "aSource4"				//	one right of TV
	
	}
	preferences {
		input ("deviceIp", "text", title: "Samsung TV Ip")
		input ("hubIp", "text", title: "NodeJs Hub IP (18,19,20 Models")
		input ("hubPort", "text", title: "NodeJs Hub Port (18/19/20 Models)", defaultValue: "8080")
		def tvModes = ["AMBIENT", "ART_MODE", "none"]
		input ("tvPwrOnMode", "enum", title: "TV Startup Display", options: tvModes, defalutValue: "none")
		input ("debugLog", "bool",  title: "Enable debug logging", defaultValue: true)
		input ("infoLog", "bool",  title: "Enable description text logging", defaultValue: true)
	}
}
def installed() {
	updateDataValue("name", "Hubitat Samsung Remote")
	updateDataValue("name64", "Hubitat Samsung Remote".encodeAsBase64().toString())
}
def updated() {
	state.remove("token")
	logInfo("updated: get device data and model year")
	unschedule()
	sendEvent(name: "wsDeviceStatus", value: "closed")
	pauseExecution(2000)	  
	def tokenSupport = getDeviceData()
	runIn(2, checkInstall)
}
def getDeviceData() {
	logInfo("getDeviceData: Updating Device Data.")
	try{
		httpGet([uri: "http://${deviceIp}:8001/api/v2/", timeout: 5]) { resp ->
			def device = resp.data.device
			logDebug("getDeviceData: ${device}")
			updateDataValue("deviceMac", device.wifiMac)
			def frameTv = "false"
			if (device.FrameTVSupport != null) {
				frameTv = device.FrameTVSupport
			}
			updateDataValue("frameTv", frameTv)
			def tokenSupport = "false"
			if (device.TokenAuthSupport != null) {
				tokenSupport = device.TokenAuthSupport
			}
			if (tokenSupport == "true") { state.token = "12345678" }
			updateDataValue("tokenSupport", tokenSupport)
			def uuid = device.duid.substring(5)
			updateDataValue("uuid", device.duid.substring(5))
			logInfo("getDeviceData: frameTv = ${frameTv}, tokenSupport = ${tokenSupport}")
			logDebug("getDeviceData: mac = ${device.wifiMac}, uuid = ${device.duid.substring(5)}")
		}
	} catch (error) { logWarn("getDeviceData: <b>error = </b>${error}") }
	return true
}
def checkInstall() {
	logInfo("<b>Performing test using tokenSupport = ${getDataValue("tokenSupport")}")
	connect()
	pauseExecution(10000)
	menu()
	pauseExecution(2000)
	close()
}

//	===== DEVELOPMENT AND TEST AREA =====
//	FRAME ART Commands
def aArtModeOn() {
	if(getDataValue("frameTv") == "true") {
		artModeData("on") 
	} else { logWarn("artModeOn: not available") }
}
def aArtModeOff() {
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
def aArtModeStatus() {
	if(getDataValue("frameTv") == "true") {
		def data = [request:"get_artmode_status",
					id: "${getDataValue("uuid")}"]
		data = JsonOutput.toJson(data)
		artModeCmd(data)
	} else { logWarn("artModeStatus: not available") }
}

//	YouTube
def aYouTube(videoId = "Ki-Ajhd83OM") {
	def launchData = "v=${videoId}"
	def url = "http://${deviceIp}:8080/ws/apps/YouTube"
	httpPost(url, launchData) { resp ->
		logDebug("youTube:  ${resp.status}  ||  ${resp.data}")
	}
}

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
def aSource0() {
	startSource()
	sendKey("ENTER")
}
def aSource1() {
	startSource()
	sendKey("RIGHT")
	pauseExecution(200)
	sendKey("ENTER")
}
def aSource2() {
	startSource()
	sendKey("RIGHT")
	pauseExecution(200)
	sendKey("RIGHT")
	pauseExecution(200)
	sendKey("ENTER")
}
def aSource3() {
	startSource()
	sendKey("RIGHT")
	pauseExecution(200)
	sendKey("RIGHT")
	pauseExecution(200)
	sendKey("RIGHT")
	pauseExecution(200)
	sendKey("ENTER")
}
def aSource4() {
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



//	===== TEST Commands do not work =====
def aGetAppsList() {
	def data = [method:"ms.channel.emit",
				params:[event: "ed.installedApp.get",
						to: "host"]]
	sendWsCmd("remote", "sendMessage", JsonOutput.toJson(data) )
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
def xxaLaunchBrowser(url = "https://community.hubitat.com/") {
	def cmdData = [appId: "org.tizen.browser",
				   action_type: "NATIVE_LAUNCH",
				   metaTag: url]
	def data = [method:"ms.channel.emit",
				params:[event: "ed.apps.launch",
						to: "host",
						data: JsonOutput.toJson(cmdData)]]
	sendWsCmd("remote", "sendMessage", JsonOutput.toJson(data) )
}
//	UPNP Set Volume
def setVolume(volume) {
	logDebug("setVolume: volume = ${volume}")
	logTrace("setVolume: volume = ${volume}")
	volume = volume.toInteger()
	if (volume <= 0 || volume >= 100) { return }
log.trace volume
	sendUpnpCmd("renderingControl",
			"SetVolume",
			["InstanceID" :0,
			 "Channel": "Master",
			 "DesiredVolume": volume])
	runIn(1, getVolume)
}
def getVolume() {
	logDebug("getVolume")
	sendUpnpCmd("renderingControl",
			"GetVolume",
			["InstanceID" :0])
}
private sendUpnpCmd(String type, String action, Map body){
	logDebug("sendIpnpCmd: upnpAction = ${action}, upnpBody = ${body}")
	def host = "${deviceIp}:9197"
	def hubCmd = new hubitat.device.HubSoapAction(
		path: "/upnp/control/RenderingControl1",
		urn: "urn:schemas-upnp-org:service:RenderingControl:1",
		action: action,
		body: body,
		headers: [Host: host,
				  CONNECTION: "close"]
	)
	sendHubCommand(hubCmd)
}
def xxparse(response) {
	def resp = parseLanMessage(response)
	log.trace resp
	log.trace resp.status
	log.trace resp.port
	log.trace resp.body
}

//	===== Web Socket Interface Methods  ==
//	===== Remote
def connect(funct = "remote") { sendWsCmd(funct, "connect") }
def sendKey(key, cmd = "Click") {
	key = "KEY_${key.toUpperCase()}"
	def data = [method:"ms.remote.control",
				params:[Cmd:"${cmd}",
						DataOfCmd:"${key}",
						TypeOfRemote:"SendRemoteKey"]]
	sendWsCmd("remote", "sendMessage", JsonOutput.toJson(data) )
}
def artModeCmd(data) {
	def cmdData = [method:"ms.channel.emit",
				   params:[data:"${data}",
						   to:"host",
						   event:"art_app_request"]]
	cmdData = JsonOutput.toJson(cmdData)
//	New sendWsCmd:  sendWsCmd(function, command, data)
//	function will define eventual url for the connect command.
//	current values are frameArt and remote
//	commands are sendMessage and close.  Connection is checked
//	prior to sending message and connects if necessary.
//	FrameArt command is one command per connect to avoid 
//	additional confusion and error chances
	if (device.currentValue("wsDeviceStatus") == "open") {
		close("remote")								//	Close existing remote websocket
	}
	pauseExecution(500)
	sendWsCmd("frameArt", "sendMessage", cmdData)	//	send command, connect is automatic.
}
def close(funct = "remote") { 
	sendEvent(name: "wsDeviceStatus", value: "closed")
	sendWsCmd(funct, "close") 
}

//	===== Communications =====
def sendWsCmd(function, command, data = "") {
	logDebug("sendWsCmd: ${function} | ${command} | ${data}")
	def name = getDataValue("name64")
	if (function == "remote") {
		runIn(300, close, [data: function])
	} else {
		runIn(10, close, [data: function])
	}
	def url
	if (getDataValue("tokenSupport") == "true") {
		def token = state.token
		if (function == "remote") {
			url = "wss://${deviceIp}:8002/api/v2/channels/samsung.remote.control?name=${name}&token=${token}"
		} else if (function == "frameArt") {
			url = "wss://${deviceIp}:8002/api/v2/channels/com.samsung.art-app?name=${name}&token=${token}"
		}
		def headers = [HOST: "${hubIp}:${hubPort}", command: command, data: data, url: url]
		sendHubCommand(new hubitat.device.HubAction([headers: headers],
												device.deviceNetworkId,
												[callback: wsHubParse]))

	} else {
		if (function == "remote") {
			url = "ws://${deviceIp}:8001/api/v2/channels/samsung.remote.control?name=${name}"
		} else if (function == "frameArt") {
			url = "ws://${deviceIp}:8001/api/v2/channels/com.samsung.art-app?name=${name}"
		}
		if (command == "sendMessage") {
			if (getDataValue("wsDeviceStatus") != "open") {
				interfaces.webSocket.connect(url)
				pauseExecution(100)
			}
			interfaces.webSocket.sendMessage(data)
		} else {
			interfaces.webSocket.close()
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
	if (cmdResponse == "{}") { return }
	//	===== Update connection status.
	if (device.currentValue("wsDeviceStatus") != wsDeviceStatus) {
		sendEvent(name: "wsDeviceStatus", value: wsDeviceStatus)
	}
	//	===== Check connect response for token update.
	try { def resp = parseJson(cmdResponse)
		def respData = parseJson(resp.cmdData)
		def newToken = respData.data.token
		if (newToken != state.token && newToken) {
			logDebug("wsHubParse: token updated to ${newToken}")
			logTrace("wsHubParse: token updated to ${newToken}")
			state.token = newToken
		}
	} catch (e) {
		logDebug("wsHubParse: <b>error</b> creating token = ${e}")
	}
}
def parse(message) {
	logDebug("parse: ${message}")
	def resp = parseJson(message)
	if (resp.event == "ms.error") {
		logDebug("parse: <b>error</b> = ${resp.data.message}")
	}
}
def webSocketStatus(message) {
	logDebug("webSocketStatus: ${message}")
	if (message == "status: open") {
		sendEvent(name: "wsDeviceStatus", value: "open")
	} else if (message == "status: closing") {
		sendEvent(name: "wsDeviceStatus", value: "closed")
	}
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
		logWarn("toggleArt only works for Frame TV's")
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
def previousChannel() { sendKey("PRECH") }
def HDMI() { sendKey("HDMI") }

//	===== Logging =====
def logTrace(msg) { 
	if (traceLog() == true) {
		log.trace "${driverVer()} || ${msg}"
	}
}
def logInfo(msg) { 
//	if (info == true) {
		log.info "${driverVer()} || ${msg}"
//	}
}
def logDebug(msg) {
//	if (debug == true) {
		log.debug "${driverVer()} || ${msg}"
//	}
}
def logWarn(msg) { log.warn "${driverVer()} || ${msg}" }