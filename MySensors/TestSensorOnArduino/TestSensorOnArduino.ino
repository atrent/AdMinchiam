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
#define MY_DEBUG
//#define MY_DEBUG_VERBOSE_RF24

// Enable and select radio type attached
#define MY_RADIO_NRF24
//#define MY_RADIO_RFM69

//#define MY_NODE_ID 41  // non ha senso se definito GW qui sotto?

#include <MySensors.h>
//#include <ESP8266WiFi.h> //needed to switch off (or on) WiFi on ESP8266

#define CHILD_ID_LIGHT 0
#define LIGHT_SENSOR_ANALOG_PIN 0

//const unsigned int SLEEP_TIME PROGMEM = 60000; // Sleep time between reads (in milliseconds)
const unsigned int SLEEP_TIME PROGMEM= 10000; // Sleep time between reads (in milliseconds)

#define ID_S_TEMP              6


MyMessage msg(CHILD_ID_LIGHT, V_LIGHT_LEVEL);
MyMessage msg_S_TEMP(ID_S_TEMP,V_TEMP);
//int lastLightLevel;


#include <Adafruit_Sensor.h>
#include <DHT.h>
#include <DHT_U.h>
#define DHTPIN            4         // Pin which is connected to the DHT sensor.
#define DHTPOWER            5         // Pin which is connected to the DHT sensor.
#define DHTTYPE           DHT11     // DHT 11 
// See guide for details on sensor wiring and usage:
//   https://learn.adafruit.com/dht/overview
DHT_Unified dht(DHTPIN, DHTTYPE);
#define BOOTDHT 10000

void presentation()
{
    // Send the sketch version information to the gateway and Controller
    sendSketchInfo("Test Sensor", "1.0");

    // Register all sensors to gateway (they will be created as child devices)
    present(CHILD_ID_LIGHT, S_LIGHT_LEVEL);
    present(ID_S_TEMP, S_TEMP,"Temperature");
}

void setup() {
    Serial.println("begin setup");
    Serial.begin(115200);

    pinMode(DHTPOWER,OUTPUT);
    digitalWrite(DHTPOWER,HIGH);

    // Turning off ESP8266 WiFi chip
    /*
    WiFi.disconnect();
    WiFi.mode(WIFI_OFF);
    WiFi.forceSleepBegin();
    delay(1);
    */

    dht.begin();
    Serial.println("DHTxx Unified Sensor Example");
    // Print temperature sensor details.
    sensor_t sensor;
    dht.temperature().getSensor(&sensor);
    Serial.println("------------------------------------");
    Serial.println("Temperature");
    Serial.print  ("Sensor:       ");
    Serial.println(sensor.name);
    Serial.print  ("Driver Ver:   ");
    Serial.println(sensor.version);
    Serial.print  ("Unique ID:    ");
    Serial.println(sensor.sensor_id);
    Serial.print  ("Max Value:    ");
    Serial.print(sensor.max_value);
    Serial.println(" *C");
    Serial.print  ("Min Value:    ");
    Serial.print(sensor.min_value);
    Serial.println(" *C");
    Serial.print  ("Resolution:   ");
    Serial.print(sensor.resolution);
    Serial.println(" *C");
    Serial.println("------------------------------------");
    // Print humidity sensor details.
    dht.humidity().getSensor(&sensor);
    Serial.println("------------------------------------");
    Serial.println("Humidity");
    Serial.print  ("Sensor:       ");
    Serial.println(sensor.name);
    Serial.print  ("Driver Ver:   ");
    Serial.println(sensor.version);
    Serial.print  ("Unique ID:    ");
    Serial.println(sensor.sensor_id);
    Serial.print  ("Max Value:    ");
    Serial.print(sensor.max_value);
    Serial.println("%");
    Serial.print  ("Min Value:    ");
    Serial.print(sensor.min_value);
    Serial.println("%");
    Serial.print  ("Resolution:   ");
    Serial.print(sensor.resolution);
    Serial.println("%");
    Serial.println("------------------------------------");
    // Set delay between sensor readings based on sensor details.
    //delayMS = sensor.min_delay / 1000;

    Serial.println("end setup");
}

void loop()
{
    //Serial.print("(delay) loop delay: ");
    Serial.print("(sleep) loop delay: ");
    Serial.println(SLEEP_TIME);
    int lightLevel=(int)random(0,100);
    //int16_t lightLevel = (1023-analogRead(LIGHT_SENSOR_ANALOG_PIN))/10.23;
    Serial.println(lightLevel);
    send(msg.set(lightLevel));

	// Get temperature event and print its value.
    sensors_event_t event;
    dht.temperature().getEvent(&event);
    if (isnan(event.temperature)) {
        Serial.println("Error reading temperature!");
    }
    else {
        Serial.print("Temperature: ");
        Serial.print(event.temperature);
        Serial.println(" *C");
        send(msg_S_TEMP.set((int)event.temperature));
    }

    // the next sleep does not work on ESP8266: wiring needed?!?
    // using delay for testng purposes
    digitalWrite(DHTPOWER,LOW);
    sleep(SLEEP_TIME);
    //delay(SLEEP_TIME);
    digitalWrite(DHTPOWER,HIGH);
    delay(BOOTDHT);
}
