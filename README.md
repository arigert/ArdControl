# README #

#### Purpose ####

* Android app that allows you to write to or read from Arduino pins
* Version 1

#### Setup ####

* Development: Install Eclipse and Android Development Tools (or another IDE, if you prefer)
* Dependencies: appcompat-v7 (included here)
* Deployment: Install app on Android device with Bluetooth enabled, set up Arduino with a Bluetooth module on pins 0 and 1 and whatever else you want (LED, sensor, etc) on any other pin.

#### Wish list ####

* Make a progress dialog box pop up while it's searching for devices (Currently, have messages say we are searching so we don't block users from clicking a device before scanning complete)
* Add Disconnect button (Styling is not good, but button exists and does seem to disconnect...)
* Fix problems with Bluetooth not reconnecting after disconnecting (Will reconnect once, but does not continually reconnect)
* Remove a pin's icon if its mode is manually set (Done. Aisha did it! Smart girl)
* Disable illegal modes, like analogWrite on a non-PWM pin
* Maybe work on UI design, especially for Bluetooth stuff
* Maybe add additional modes (tone, Serial)
* Maybe add additional items (temp sensor, tilt sensor, RGB LED, motor, shift register)

#### To Fix List ####
Do not allow users to move pins, etc. when bluetooth is not connected?
Otherwise, state of GUI does not necessarily reflect state of actual Arduino.

#### Contact ####

* Owner: arigert
