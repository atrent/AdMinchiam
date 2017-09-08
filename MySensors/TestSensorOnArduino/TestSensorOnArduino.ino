/**
 * The MySensors Arduino library handles the wireless radio link and protocol
 * between your home built sensors/actuators and HA controller of choice.
 * The sensors forms a self healing radio network with optional repeaters. Each
 * repeater and gateway builds a routing tables in EEPROM which keeps track of the
 * network topology allowing messages to be routed to nodes.
 *
 * Created by Henrik Ekblad <henrik.ekblad@mysensors.org>
 * Copyright (C) 2013-2015 Sensnology AB
 * Full contributor list: https://github.com/mysensors/Arduino/graphs/contributors
 *
 * Documentation: http://www.mysensors.org
 * Support Forum: http://forum.mysensors.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 *
 *******************************
 *
 * REVISION HISTORY
 * Version 1.0 - Henrik EKblad
 *
 * DESCRIPTION
 * Example sketch showing how to measue light level using a LM393 photo-resistor
 * http://www.mysensors.org/build/light
 */

// Enable debug prints to serial monitor
//#define MY_DEBUG
//#define MY_DEBUG_VERBOSE_RF24

// Enable and select radio type attached
#define MY_RADIO_NRF24
//#define MY_RADIO_RFM69

//#define MY_NODE_ID 41  // non ha senso se definito GW qui sotto?

#include <MySensors.h>
#include <ESP8266WiFi.h> //needed to switch off (or on) WiFi on ESP8266

#define CHILD_ID_LIGHT 0
#define LIGHT_SENSOR_ANALOG_PIN 0

unsigned long SLEEP_TIME = 10000; // Sleep time between reads (in milliseconds)

MyMessage msg(CHILD_ID_LIGHT, V_LIGHT_LEVEL);
//int lastLightLevel;


void presentation()
{
	// Send the sketch version information to the gateway and Controller
	sendSketchInfo("Test Sensor", "1.0");

	// Register all sensors to gateway (they will be created as child devices)
	present(CHILD_ID_LIGHT, S_LIGHT_LEVEL);
}


void setup(){
	Serial.println("begin setup");
	Serial.begin(115200);

    // Turning off ESP8266 WiFi chip
    WiFi.disconnect();
    WiFi.mode(WIFI_OFF);
    WiFi.forceSleepBegin();
    delay(1);

	Serial.println("end setup");	
}

void loop()
{
	Serial.println("loop");
    Serial.println(SLEEP_TIME);
	int lightLevel=(int)random(0,100);
	//int16_t lightLevel = (1023-analogRead(LIGHT_SENSOR_ANALOG_PIN))/10.23;
	Serial.println(lightLevel);
	send(msg.set(lightLevel));
	// the next sleep does not work on ESP8266: wiring needed?!?
	// using delay for testng purposes
	//sleep(SLEEP_TIME);
    delay(SLEEP_TIME);
}
