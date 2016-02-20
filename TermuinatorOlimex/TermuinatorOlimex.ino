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

/*
 * devices:
 * 1) attuatore efficientatore (original Termuinator!)
 * 2) rilevatore temperatura+presenza (con PIR) (*)
 */

// TODO: JSON

// circa DONE: board per unire ESP+SDlogger+DHT+mosfet [esp8266-evb senza sdlogger]

// [NO] Openlog (SD) https://www.sparkfun.com/products/9530   (3.3v)
// [NO] PIR: https://www.sparkfun.com/products/13285
// DHT11 (pero' versione montata con resistenze) https://learn.adafruit.com/dht (3 to 5v)

// MQTT lib: https://github.com/adafruit/Adafruit_MQTT_Library
// circa DONE: compatibilita' MQTT
// TODO: mqtt topic patterns

//////////////////////////////////////////////////
#include "DHT.h"
//#include "FastLED.h" NON COMPATIBILE (TODO: re-check)
//#include "Adafruit_WS2801.h" NON COMPATIBILE

//////////////////////////////////////////////////
#define DELAY_BLINK 30
#define DELAY_LOOP 1000

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

#include <ESP8266WiFi.h>
#include <PubSubClient.h>  // mqtt

#include "wifi-secrets.h"

//const char* mqtt_server = "broker.mqtt-dashboard.com";
//String mqtt_server = "broker.mqtt-dashboard.com";
String mqtt_server = "test.mosquitto.org";

WiFiClient espClient;

byte mac[6];

// per mqtt
PubSubClient client(espClient);
long lastMsg = 0;

#define MSG_LEN 150
char msg[MSG_LEN];

////////////////////////////////////////////////////////
// Uncomment whatever type you're using!
#define DHTTYPE DHT11   // DHT 11
//#define DHTTYPE DHT22   // DHT 22  (AM2302), AM2321
//#define DHTTYPE DHT21   // DHT 21 (AM2301)
////////////////////////////////////////////////////////
//#define DHTPIN 0     // ESP OK in parallelo col bottone
//#define DHTPIN 12     // ESP OK corrisponde a 7 su connettore UEXT
#define DHTPIN 4     // ...
//#define DHTPIN 16     // riprovo gpio16, corrisponde a pin 13, NON FUNZIONA con DHT, pero' il LED funzia
////////////////////////////////////////////////////////
// Connect pin 1 (on the left) of the sensor to +5V
// NOTE: If using a board with 3.3V logic like an Arduino Due connect pin 1
// to 3.3V instead of 5V!
// Connect pin 2 of the sensor to whatever your DHTPIN is
// Connect pin 4 (on the right) of the sensor to GROUND
// Connect a 10K resistor from pin 2 (data) to pin 1 (power) of the sensor

// Initialize DHT sensor.
// Note that older versions of this library took an optional third parameter to
// tweak the timings for faster processors.  This parameter is no longer needed
// as the current DHT reading algorithm adjusts itself to work on faster procs.
DHT dht(DHTPIN, DHTTYPE);

/* TODO: aggiornare!!!
 * parametri config (nel file TERMU.INI) [ORA VIA SERIALE] [non in ordine]
 * 1) broker
 * 2) ssid  //forse anche metodo (WPA/etc.)?
 * 3) password
 * 4) tempSoglia
 * 5) finestraIsteresi
 */

// variabili perche' dovranno essere configurabili a runtime
#define TOPIC "Termuinator"
String nomeNodo;  //poi viene accodato il mac
int tempSoglia=26;
int finestraIsteresi=2;
float humidity,temperature,fahreneit,hif,hic;

//////////////////////////////////////////

void util_blinkLed(int i) {
    digitalWrite(i,HIGH);
    delay(DELAY_BLINK);
    digitalWrite(i,LOW);
    delay(DELAY_BLINK);
}

void util_blinkLed(int i,int repeat) {
    for(int count=0; count<repeat; count++)
        util_blinkLed(i);
}

void util_emptySerial() {
    while (Serial.available()!=0) { // wait for input
        delay(1);
        Serial.read();
    }
}

String util_input(String msg, String old) {
    Serial.print("(");
    Serial.print(old);
    Serial.print(") ");

    Serial.print(msg);				// prompt
    while (Serial.available()==0) { // wait for input
        delay(5);
    }
    String newS = Serial.readString();     // read input
    newS.trim();

    /*
    Serial.print("input:");
    Serial.print(newS);
    Serial.print(" di lunghezza: ");
    Serial.println(newS.length());
    */

    if(newS.length()==0) {
        Serial.println(old);
        util_emptySerial();
        return old;
    }
    Serial.println(newS);
    return newS;
}


void node_config() {
    // TODO: uso eeprom per salvare i config data
    // BEWARE:   https://github.com/esp8266/Arduino/blob/master/doc/libraries.md#eeprom

    util_emptySerial();

    ssid=util_input("ssid: ",ssid);
    password=util_input("psk: ",password);

    mqtt_server=util_input("mqtt broker: ",mqtt_server);

    tempSoglia=util_input("t-soglia: ",String(tempSoglia)).toInt();
    finestraIsteresi=util_input("isteresi: ",String(finestraIsteresi)).toInt();

    // reboot
    setup();
}

void util_printStatus() {
    /*  Serial.print("S: ");
        Serial.print(status);    */

    Serial.print("NODE: ");
    Serial.print(nomeNodo);

    Serial.print(", SSID: ");
    Serial.print(ssid);

    //Serial.print(", PWD: ");
    //Serial.print(pwdWifi);

    Serial.print(", TEMPSOGLIA: ");
    Serial.print(tempSoglia);

    Serial.print(", ISTERESI: ");
    Serial.print(finestraIsteresi);

    Serial.print(", Humidity: ");
    Serial.print(humidity);
    Serial.print("%, ");

    Serial.print("Temperature: ");
    Serial.print(temperature);
    Serial.print("C/");
    Serial.print(fahreneit);
    Serial.print("F, ");

    Serial.print("Heat index: ");
    Serial.print(hic);
    Serial.print("C/");
    Serial.print(hif);
    Serial.print("F, ");

    Serial.print(ESP.getFreeHeap(),DEC);
    Serial.println(" mem");
}


void mqtt_reconnect() {
    // TODO: nr. tentativi???  [LOW_PRI]

    // Loop until we're reconnected
    while (!client.connected()) {
        if(digitalRead(0)==LOW) { // tasto -> esce
            break;
        }
        Serial.print("Attempting MQTT connection...");
        // Attempt to connect
        if (client.connect(nomeNodo.c_str())) {
            Serial.println("connected");
            // Once connected, publish an announcement...
            client.publish(nomeNodo.c_str(), "first msg.");
            // ... and resubscribe
            //client.subscribe("termuinator2");
        } else {
            Serial.print("failed, rc=");
            Serial.print(client.state());
            Serial.println(" try again in 5 seconds");
            // Wait 5 seconds before retrying
            delay(5000);
        }
    }
}

//////////////////////////////////////////
void loop() {
    char str_temp[6];
    char str_hic[6];
    char str_humidity[6];

    util_blinkLed(LEDPIN); // tanto per dire "sono sveglio"

    if(digitalRead(0)==LOW) { // tasto -> config
        node_config();
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

    if(temperature>=tempSoglia)
        digitalWrite(RELAY,HIGH);

    if(temperature<=(tempSoglia-finestraIsteresi))
        digitalWrite(RELAY,LOW);

    /*
      mySerial.print("ls");
      mySerial.write(13);
      output();
    */

    //mySerial.print("read TERMU.INI");
    //mySerial.write(13);


    //Serial.println(".pre-ls.");
    //Serial.print(mySerial.read());


    ///////////////////////////////////////
    // MQTT
    if (!client.connected()) {
        mqtt_reconnect();
    }
    client.loop();

    long now = millis();
    if (now - lastMsg > 2000) {
        lastMsg = now;
        dtostrf(temperature, 4, 2, str_temp);
        dtostrf(hic, 4, 2, str_hic);
        dtostrf(humidity, 4, 2, str_humidity);
        snprintf (msg, MSG_LEN, 
          "{\"_type\":\"termuinator\",\"t\":%s,\"t_t\":%d,\"his\":%d,\"hum\":\"%s\",\"h_index\":%s}",
            str_temp,tempSoglia,finestraIsteresi,str_humidity,str_hic);
        // Serial.println(msg);
        if (client.publish(nomeNodo.c_str(), msg)){
         Serial.println("info: MQTT message succesfully published");
        } else { 
          Serial.println("error: MQTT publishing error (connection error or message too large)");
        };
    }
    ///////////////////////////////////////



    delay(DELAY_LOOP);
}



















/*
 Basic ESP8266 MQTT example

 This sketch demonstrates the capabilities of the pubsub library in combination
 with the ESP8266 board/library.

 It connects to an MQTT server then:
  - publishes "hello world" to the topic "outTopic" every two seconds
  - subscribes to the topic "inTopic", printing out any messages
    it receives. NB - it assumes the received payloads are strings not binary
  - If the first character of the topic "inTopic" is an 1, switch ON the ESP Led,
    else switch it off

 It will reconnect to the server if the connection is lost using a blocking
 reconnect function. See the 'mqtt_reconnect_nonblocking' example for how to
 achieve the same result without blocking the main loop.

 To install the ESP8266 board, (using Arduino 1.6.4+):
  - Add the following 3rd party board manager under "File -> Preferences -> Additional Boards Manager URLs":
       http://arduino.esp8266.com/stable/package_esp8266com_index.json
  - Open the "Tools -> Board -> Board Manager" and click install for the ESP8266"
  - Select your ESP8266 in "Tools -> Board"

*/



void wifi_setup() {
    delay(10);

    // We start by connecting to a WiFi network
    Serial.println();
    Serial.print("Connecting to ");
    Serial.println(ssid);

    WiFi.begin(ssid.c_str(), password.c_str());

    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }

    Serial.println();
    Serial.println("WiFi connected");
    Serial.println("IP address: ");
    Serial.println(WiFi.localIP());
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

    //char n[nodo.length()];
    //nomeNodo=n;
    //Serial.println(nodo);
}

/*
void mqtt_callback(char* topic, byte* payload, unsigned int length) {
    Serial.print("Message arrived [");
    Serial.print(topic);
    Serial.print("] ");
    for (int i = 0; i < length; i++) {
        Serial.print((char)payload[i]);
    }
    Serial.println();

    // Switch on the LED if an 1 was received as first character
    if ((char)payload[0] == '1') {
        digitalWrite(BUILTIN_LED, LOW);   // Turn the LED on (Note that LOW is the voltage level
        // but actually the LED is on; this is because
        // it is acive low on the ESP-01)
    } else {
        digitalWrite(BUILTIN_LED, HIGH);  // Turn the LED off by making the voltage HIGH
    }

}
*/

//////////////////////////////////////////
void setup() {
    util_blinkLed(LEDPIN,10);

    Serial.begin(115200);
    Serial.println("Booting...");

    pinMode(RELAY, OUTPUT);
    pinMode(LEDPIN, OUTPUT);
    pinMode(DHTPIN, INPUT);
    pinMode(0, INPUT_PULLUP); //per cambio stato

    dht.begin();

    wifi_setup();

    client.setServer(mqtt_server.c_str(), 1883);
    //client.setCallback(mqtt_callback);

    // per dare feedback al boot
    util_blinkLed(LEDPIN,10);
    util_blinkLed(RELAY,10);
    util_blinkLed(LEDPIN,10);

    /*
    Serial.println(eeprom_readString());
    Serial.println(eeprom_readString());
    */

    Serial.println("Booted!");
}
