# Hubitat-Samsung-TV BETA Version 3.1
Hubitat Samsung TV driver
## Instructions:

a. Create the new driver in Hubitat using the driver file:  'Hubitat-Samsung TV Remote Driver.groovy'

b. Turn on your Samsung TV

c.  Install the device using the driver "Hubitat-Samsung Remote"

e.  In preferences, enter the TV's IP Address

f.  Select DEBUG and INFO Logging for initial installation.  This is for later installation.

g.  Open Hubitat Logging.

h.  Grab you TV remote.  You will need to allow this apps access to your tv VERY QUICKLY.

i.  SAVE PREFERENCES.  This will update data and run a menu command to cause the on-screen menu to appear.  Note, if you had to accept the access on your TV, you should see a log entry "Beta 1.0 || parse: Token updated to NNNNNNNN".  If you do not, simply "Close" then run the menu command.  Again, VERY SHORT response time for accepting control.

## Features:

1.  Supports new remote keys plus selected other function.  A list of support keys is embedded in the code (beta version)

2.  Internal button interface to support Hubitat Dashboard integration.  List of buttons/keys is embedded in the code.

## NEXT (possibilities):

A.  Adding application start functions.

B.  Use of UPnP to explicitly set volume and implementing Hubitat capability Audio Control
