# Hubitat-Samsung-TV
Hubitat Samsung TV driver with associated node.js application

### This is a test implementation and is exclusively for Hubitat integration.  It is designed to work with 2016 and later Samsung TV's

Instructions:

a. Create the new driver in Hubitat using the driver file:  'Hubitat-Samsung TV Remote Driver.groovy'

b.  For 2018 and later TV's you will require a Node.JS server.  The server is installed using the instructions in 'Windows Hubitat-Websocket Install.pdf'

c. Turn on your Samsung TV

d.  Start the node.js server app

d.  Install the device using the driver "Hubitat-Samsung Remote"

e.  In preferences, enter the TV's IP Address

f.  For 2018 and later, enter the server's IP Address

g.  Do not select debub nor info logging.  This is for later installation.

h.  Open Hubitat Logging.

i.  SAVE PREFERENCES.

### A listing of supported keys and functions is contained in the metadata section of the drier.
