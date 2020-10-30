# Hubitat-Samsung-TV BETA Version
Hubitat Samsung TV driver with associated node.js application

## Instructions:

a. Create the new driver in Hubitat using the driver file:  'Hubitat-Samsung TV Remote Driver.groovy'

b.  For 2018 and later TV's you will require a Node.JS server.  The server is installed using the instructions in 'Windows Hubitat-Websocket Install.pdf'

c. Turn on your Samsung TV

d.  Start the node.js server app

d.  Install the device using the driver "Hubitat-Samsung Remote"

e.  In preferences, enter the TV's IP Address

f.  For 2017 and later, enter the server's IP Address

g.  Select DEBUG and INFO Logging for initial installation.  This is for later installation.

h.  Open Hubitat Logging.

i.  Grab you TV remote.  You will need to allow this apps access to your tv VERY QUICKLY.

j.  SAVE PREFERENCES.  This will update data and run a menu command to cause the on-screen menu to appear.  Note, if you had to accept the access on your TV, you should see a log entry "Beta 1.0 || parse: Token updated to NNNNNNNN".  If you do not, simply "Close" then run the menu command.  Again, VERY SHORT response time for accepting control.

k.  Most Common Errors:

    1)  Did not install the "ws" capability on the node.js server setup.
    
    2)  Did not start the node.js server and leave it running.

## Features:

1.  Supports new remote keys plus selected other function.  A list of support keys is embedded in the code (beta version)

2.  Internal button interface to support Hubitat Dashboard integration.  List of buttons/keys is embedded in the code.

## NEXT (possibilities):

A.  Adding application start functions.

B.  Use of UPnP to explicitly set volume and implementing Hubitat capability Audio Control
