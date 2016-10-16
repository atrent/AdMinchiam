/*
	Termuinator (olimex) - Biscuolo,Trentini © 2015

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
//////////////////////////////////////////////////////////////////////////////
// per ArduinoIDE ricordarsi https://www.olimex.com/Products/IoT/ESP8266-EVB/resources/ESP8266-EVB-how-to-use-Arduino.pdf
// per i GPIO: http://www.esp8266.com/wiki/doku.php?id=esp8266_gpio_pin_allocations

// MAIN https://github.com/esp8266/Arduino
// SPIFFS https://github.com/esp8266/Arduino/blob/master/doc/filesystem.md
// SPIFFS plugin Download the tool: https://github.com/esp8266/arduino-esp8266fs-plugin/releases/download/0.2.0/ESP8266FS-0.2.0.zip.

/*
 * devices:
 * 1) attuatore efficientatore (original Termuinator!)
 * 2) rilevatore temperatura+presenza (con PIR) (*)
 */


// versione "termostato":
// TODO: modo di funzionamento estate/inverno (per usarlo anche come termostato tradizionale, e.g. Venezia
// TODO: PIR sensor (presenza ospiti, decisione attivazione termostato)
// TODO: LCD (bootstrapped), trovare elenco caratteri speciali (commands)
// TODO: bottone selezione temperatura (su,giu)

// TODO: convertire tutte le stringhe chiamando macro F()

// TODO: integrare  TaskScheduler (https://github.com/arkhipenko/TaskScheduler)
//		- blink led
//		- connessione wifi
//		- connessione mqtt (se wifi ok)
//		- misurazione temperatura/umidità/presenza
//		- pubblicazione info via mqtt
//		- aggiornamento display (se previsto)
//		- ricezione msg mqtt (per comanderia varia)
//		- attivazione ventola/termocaldaia
//		- [nel loop?] attivazione modalità config (seriale) su pressione pulsante
//		- [nel loop?] lettura bottoni up/down (se previsti)
//		- [nel setup] lettura file config e setup iniziale

//Termuinator18fe34a206e
// mosquitto_sub -v -h broker.mqtt-dashboard.com -t Termuinator18fe34a206e
// mosquitto_sub -v -h test.mosquitto.org -t Termuinator18fe34a206e

// TODO: separare hw layer (in caso si usi altro da ESP8266)

// TODO: "inglesizzare" variabili e funzioni

// TODO: DHT sensor se manca non entrare in loop

// circa DONE: board per unire ESP+SDlogger+DHT+mosfet [esp8266-evb senza sdlogger]

// [NO] JSON
// [NO] Openlog (SD) https://www.sparkfun.com/products/9530   (3.3v)
// [NO] PIR: https://www.sparkfun.com/products/13285
// DHT11 (pero' versione montata con resistenze) https://learn.adafruit.com/dht (3 to 5v)



byte mac[6];  // TODO: verificare dove lo usiamo

String nomeNodo;  //poi viene accodato il mac // TODO: check uso

int tempSoglia=26;
int finestraIsteresi=2;
float humidity,temperature,fahreneit,hif,hic;
boolean acceso=false;

#define EFFICIENTATORE	'e'
#define TERMOSTATO		't'
char modalita=EFFICIENTATORE;

#define ON	HIGH
#define OFF	LOW


//////////////////////////////////////////////////
#define DELAY_BLINK 30
#define DELAY_LOOP 5000

//////////////////////////////////////////////////
#define DEBUG true

//////////////////////////////////////////////////
//#define STATUS_CONFIG 0
//#define STATUS_NORMAL 99
//int status=STATUS_NORMAL; //default
// non c'e' bisogno di una variabile se abbiamo solo normale/setup
// TODO: gestione stato (forse no, tanto c'e' solo normale/config) [LOW_PRI]

//////////////////////////////////////////////////
//#define TESTLED 16
// 13 ok
// 14 ok
// 15 ok (non diretto sui pin che ho saldato)
// 16 ok

#define RELAY 5
#define LEDPIN 16

#define BUTTON 0

///////////////////////////////////////////////////
//#include <SoftwareSerial.h>
//#define LCD 14
//SoftwareSerial SwSerial(BUTTON, LCD, false, 128);



// TODO: ordinare le dichiarazioni variabili e gli include e i define, PULIZIA CODICE
String mqtt_server = "SBAGLIATO"; // per vedere se lo tira su dal file
#include "wifi.h"
#include "mqtt.h"
#include "spiffs.h"
#include "utils.h"
#include "dht.h"


//////////////////////////////////////////
// headers... // TODO: capire perche' non e' piu' indifferente l'ordine di definizione!!!
//void wifi_setup();
void configFromSerialToSpiffs();
int wifiAndMqttOn();
//////////////////////////////////////////




//////////////////////////////////////////
void loop() {
    /*
        SwSerial.print("loop...");
        delay(100);
    */

    if (WiFi.status() != WL_CONNECTED) wifiAndMqttOn();

    char str_temp[6];
    char str_hic[6];
    char str_humidity[6];

    // test fs
    //Serial.println(spiffs_getValue("/ssid"));


	if(acceso)
		util_blinkLed(LEDPIN,10); // se ventola ON
	else
		util_blinkLed(LEDPIN); // tanto per dire "sono sveglio"

    if(digitalRead(BUTTON)==LOW) { // tasto -> config
        configFromSerialToSpiffs();
    }

    // Reading temperature or humidity takes about 250 milliseconds!
    // Sensor readings may also be up to 2 seconds 'old' (its a very slow sensor)
    humidity = dht.readHumidity();
    // Read temperature as Celsius (the default)
    temperature = dht.readTemperature();
    // Read temperature as Fahrenheit (isFahrenheit = true)
    fahreneit = dht.readTemperature(true);

    // Check if any reads failed and exit early (to try again).
    if (isnan(humidity) || isnan(temperature) || isnan(fahreneit)) {
        Serial.println("Failed to read from DHT sensor!");
        return;
    }

    // Compute heat index in Fahrenheit (the default)
    hif = dht.computeHeatIndex(fahreneit, humidity);
    // Compute heat index in Celsius (isFahreheit = false)
    hic = dht.computeHeatIndex(temperature, humidity, false);

    util_printStatus();

    switch(modalita) {
    case EFFICIENTATORE:
        if(temperature >= tempSoglia) util_switch(ON);
        if(temperature <= (tempSoglia-finestraIsteresi)) util_switch(OFF);
        break;

    case TERMOSTATO:
        if(temperature <= tempSoglia) util_switch(ON);
        if(temperature >= (tempSoglia+finestraIsteresi)) util_switch(OFF);
    }

    /*
      mySerial.print("ls");
      mySerial.write(13);
      output();
    */

    //mySerial.print("read TERMU.INI");
    //mySerial.write(13);


    //Serial.println(".pre-ls.");
    //Serial.print(mySerial.read());



    if (WiFi.status() == WL_CONNECTED) {

        ///////////////////////////////////////
        // MQTT
        if (!mqtt_client.connected()) {
            mqtt_reconnect();
        }


        if (mqtt_client.connected()) {
            mqtt_client.loop();

            long now = millis();
            if (now - lastMsg > DELAY_MQTT) {
                lastMsg = now;
                dtostrf(temperature, 4, 2, str_temp);
                dtostrf(hic, 4, 2, str_hic);
                dtostrf(humidity, 4, 2, str_humidity);
                snprintf (msg, MSG_LEN,
                          "{\"_type\":\"termuinator\",\"t\":%s,\"t_t\":%d,\"his\":%d,\"hum\":\"%s\",\"h_index\":%s}",
                          str_temp,tempSoglia,finestraIsteresi,str_humidity,str_hic);
                // Serial.println(msg);
                if (mqtt_client.publish(nomeNodo.c_str(), msg)) {
                    Serial.println("info: MQTT message succesfully published");
                } else {
                    Serial.println("error: MQTT publishing error (connection error or message too large)");
                }
            }
        }
    }

    delay(DELAY_LOOP);
}


int wifiAndMqttOn() {
    Serial.println("entro in net services");
    if(wifi.startsWith("y")) {

        // wifi (dip. da parametri)
        if(wifi_setup()) {


            // DONE: deve funzionare anche senza broker, quindi check se bloccante!!! fatto, nel senso che abbiamo messo la reconnect monotentativo
            // mqtt (dip. da parametri)
            mqtt_client.setServer(mqtt_server.c_str(), 1883); //TODO: porta come config in file
            //mqtt_client.setCallback(mqtt_callback);  // solo se si vuole anche ascoltare msg

            Serial.println("mqtt set");
            return 0;
        }
        Serial.println("mqtt NOT set");
        return -1;
    }
    else {
        Serial.print("wifi enabled? ");
        Serial.println(wifi);
        return -2;
    }
}


void configFromSerialToSpiffs() {
	
    util_emptySerial();

    wifi=util_input("wifi enable: ",wifi);
    ssid=util_input("ssid: ",ssid);
    password=util_input("psk: ",password);

    mqtt_server=util_input("mqtt broker: ",mqtt_server);

    modalita=util_input("modalita (e/t): ",String(modalita)).charAt(0);

    tempSoglia=util_input("t-soglia: ",String(tempSoglia)).toInt();
    finestraIsteresi=util_input("isteresi: ",String(finestraIsteresi)).toInt();

    spiffs_writeValues();

    // restart services
    wifiAndMqttOn();
}

//////////////////////////////////////////
void setup() {
    Serial.begin(115200);
    Serial.println("Booting...");

	/*
    SwSerial.begin(9600);
    SwSerial.println("booting...");
    */

    if(DEBUG)
        util_printStatus();

    util_blinkLed(LEDPIN,10);

    pinMode(RELAY, OUTPUT);
    pinMode(LEDPIN, OUTPUT);
    pinMode(DHTPIN, INPUT);
    pinMode(0, INPUT_PULLUP); //per cambio stato

    // filesystem
    SPIFFS.begin();
    spiffs_getValues();

    if(DEBUG)
        util_printStatus();

    // sensor
    dht.begin();

    //wifiAndMqttOn();
    Serial.println("MAC address: ");
    WiFi.macAddress(mac);
    nomeNodo=TOPIC;
    Serial.print("MAC=");
    for (int i=0; i<6; i++) {
        nomeNodo += String(mac[i],HEX);
        Serial.print(mac[i],HEX);
        Serial.print(":");
    }
    Serial.println(nomeNodo);
    WiFi.disconnect();

    // "ammuina" per dare feedback al boot
    util_blinkLed(LEDPIN,10);
    util_blinkLed(RELAY,10);
    util_blinkLed(LEDPIN,10);

    Serial.println("Booted!");
}
