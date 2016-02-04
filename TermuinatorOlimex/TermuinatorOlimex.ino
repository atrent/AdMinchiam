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

// TODO: board per unire ESP+SDlogger+DHT+mosfet

// Openlog (SD) https://www.sparkfun.com/products/9530   (3.3v)
// PIR: https://www.sparkfun.com/products/13285
// DHT11 (pero' versione montata con resistenze) https://learn.adafruit.com/dht (3 to 5v)

// MQTT lib: https://github.com/adafruit/Adafruit_MQTT_Library

// TODO: compatibilita' MQTT

//////////////////////////////////////////////////
#include "DHT.h"
//#include "FastLED.h" NON COMPATIBILE
//#include "Adafruit_WS2801.h" NON COMPATIBILE

//#define TESTLED 16
// 13 ok
// 14 ok
// 15 ok (non diretto sui pin che ho saldato)
// 16 ok

#define RELAY 5
#define LEDPIN 16

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
String ssid;
String pwdWifi;
int tempSoglia=26;
int finestraIsteresi=2;
float h,t,f,hif,hic;

//////////////////////////////////////////
void setup() {
    Serial.begin(115200);
    Serial.println("Booting...");

    pinMode(RELAY, OUTPUT);
    pinMode(LEDPIN, OUTPUT);
    pinMode(DHTPIN, INPUT);

    dht.begin();

	// per dare feedback al boot
    for(int i=0; i<10; i++) {
        blinkLed(RELAY);
        blinkLed(LEDPIN);
    }
}

//////////////////////////////////////////
void blinkLed(int i){
        digitalWrite(i,HIGH);
        delay(50);
        digitalWrite(i,LOW);
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

    delay(500);
}


void printStatus(){
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
