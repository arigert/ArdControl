# README #

#### Purpose ####

* Android app that allows you to write to or read from Arduino pins
* Version 1

#### Setup ####

* Development: Install Eclipse and Android Development Tools (or another IDE, if you prefer)
* Dependencies: appcompat-v7 (included here)
* Deployment: Install app on Android device with Bluetooth enabled, set up Arduino with a Bluetooth module on pins 0 and 1 and whatever else you want (LED, sensor, etc) on any other pin.

#### Wish list ####

* Enable searching for Bluetooth devices rather than using a hard-coded MAC address
* Make a progress dialog box pop up while it's connecting 
* Fix problems with Bluetooth not reconnecting after disconnecting
* Make a pin turn Off and have no label when you drag an icon off of it
* Disable illegal modes, like analogWrite on a non-PWM pin
* Maybe work on UI design
* Maybe add additional modes, like "play a tone"

#### Contact ####

* Owner: arigert
