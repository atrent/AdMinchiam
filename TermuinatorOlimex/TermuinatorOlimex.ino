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


// TODO: uso eeprom per salvare i config data

// TODO: interfaccia seriale per config


// circa DONE: board per unire ESP+SDlogger+DHT+mosfet [esp8266-evb senza sdlogger]

// Openlog (SD) https://www.sparkfun.com/products/9530   (3.3v)
// PIR: https://www.sparkfun.com/products/13285
// DHT11 (pero' versione montata con resistenze) https://learn.adafruit.com/dht (3 to 5v)

// MQTT lib: https://github.com/adafruit/Adafruit_MQTT_Library

// TODO: compatibilita' MQTT

//////////////////////////////////////////////////
#include "DHT.h"
//#include "FastLED.h" NON COMPATIBILE
//#include "Adafruit_WS2801.h" NON COMPATIBILE



//////////////////////////////////////////////////

#define STATUS_CONFIG 0
#define STATUS_NORMAL 99
int status=STATUS_NORMAL; //default

//////////////////////////////////////////////////
//#define TESTLED 16
// 13 ok
// 14 ok
// 15 ok (non diretto sui pin che ho saldato)
// 16 ok

#define RELAY 5
#define LEDPIN 16

#include <ESP8266WiFi.h>
#include <PubSubClient.h>

// Update these with values suitable for your network.
#include "wifi-xelera.h"
const char* mqtt_server = "broker.mqtt-dashboard.com";

WiFiClient espClient;
PubSubClient client(espClient);
long lastMsg = 0;
char msg[50];
//int value = 0;



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
 * parametri config (nel file TERMU.INI)
 * 1) nomeNodo
 * 2) ssid  //forse anche metodo (WPA/etc.)?
 * 3) pwdWifi
 * 4) tempSoglia
 * 5) finestraIsteresi
 */

// variabili perche' dovranno essere configurabili a runtime
String nomeNodo;
//String ssid;
String pwdWifi;
int tempSoglia=26;
int finestraIsteresi=2;
float h,t,f,hif,hic;


//////////////////////////////////////////
void blinkLed(int i) {
    digitalWrite(i,HIGH);
    delay(50);
    digitalWrite(i,LOW);
    delay(50);
}

void blinkLed(int i,int repeat) {
    for(int count=0; count<repeat; count++)
        blinkLed(i);
}


void printStatus() {
    Serial.print("NODE: ");
    Serial.print(nomeNodo);
    Serial.print(", SSID: ");
    Serial.print(ssid);
    Serial.print(", PWD: ");
    Serial.print(pwdWifi);
    Serial.print(", TEMPSOGLIA: ");
    Serial.print(tempSoglia);
    Serial.print(", ISTERESI: ");
    Serial.print(finestraIsteresi);
    Serial.print(", Humidity: ");
    Serial.print(h);
    Serial.print("%, ");
    Serial.print("Temperature: ");
    Serial.print(t);
    Serial.print("C/");
    Serial.print(f);
    Serial.print("F, ");
    Serial.print("Heat index: ");
    Serial.print(hic);
    Serial.print("C/");
    Serial.print(hif);
    Serial.println("F, ");
}


void reconnect() {
    // Loop until we're reconnected
    while (!client.connected()) {
        Serial.print("Attempting MQTT connection...");
        // Attempt to connect
        if (client.connect("termuinator2-ESP8266Client")) {
            Serial.println("connected");
            // Once connected, publish an announcement...
            client.publish("termuinator2", "hello world");
            // ... and resubscribe
            client.subscribe("termuinator2");
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
    blinkLed(LEDPIN); // tanto per dire "sono sveglio"

    // Reading temperature or humidity takes about 250 milliseconds!
    // Sensor readings may also be up to 2 seconds 'old' (its a very slow sensor)
    h = dht.readHumidity();
    // Read temperature as Celsius (the default)
    t = dht.readTemperature();
    // Read temperature as Fahrenheit (isFahrenheit = true)
    f = dht.readTemperature(true);

    // Check if any reads failed and exit early (to try again).
    if (isnan(h) || isnan(t) || isnan(f)) {
        Serial.println("Failed to read from DHT sensor!");
        return;
    }

    // Compute heat index in Fahrenheit (the default)
    hif = dht.computeHeatIndex(f, h);
    // Compute heat index in Celsius (isFahreheit = false)
    hic = dht.computeHeatIndex(t, h, false);

    printStatus();

    if(t>=tempSoglia)
        digitalWrite(RELAY,HIGH);

    if(t<=(tempSoglia-finestraIsteresi))
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


    if (!client.connected()) {
        reconnect();
    }
    client.loop();

    long now = millis();
    if (now - lastMsg > 2000) {
        lastMsg = now;
        //++value;
        snprintf (msg, 75, "temp: %.2f", t);
        Serial.print("Publish message: ");
        Serial.println(msg);
        client.publish("termuinator2", msg);
    }

    delay(500);
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

    WiFi.begin(ssid, password);

    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }

    Serial.println("");
    Serial.println("WiFi connected");
    Serial.println("IP address: ");
    Serial.println(WiFi.localIP());
}

void callback(char* topic, byte* payload, unsigned int length) {
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





//////////////////////////////////////////
void setup() {
    Serial.begin(115200);
    Serial.println("Booting...");

    pinMode(RELAY, OUTPUT);
    pinMode(LEDPIN, OUTPUT);
    pinMode(DHTPIN, INPUT);

    dht.begin();

    wifi_setup();
    client.setServer(mqtt_server, 1883);
    client.setCallback(callback);

    // per dare feedback al boot
    blinkLed(LEDPIN,10);
    blinkLed(RELAY,10);
    blinkLed(LEDPIN,10);

    Serial.println("Booted!");
}
