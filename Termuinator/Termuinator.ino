/*
	Termuinator - Biscuolo,Trentini © 2015

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

// TODO: board per unire ESP+SDlogger+DHT

// Openlog (SD) https://www.sparkfun.com/products/9530   (3.3v)
// PIR: https://www.sparkfun.com/products/13285
// DHT11 (pero' versione montata con resistenze) https://learn.adafruit.com/dht (3 to 5v)

// MQTT lib: https://github.com/adafruit/Adafruit_MQTT_Library

//#include <IniFile.h>
#include <SoftwareSerial.h>
#include "DHT.h"

#define DHTPIN 2     // what digital pin we're connected to
#define PIRPIN 4
#define LEDPIN 13
#define SEPA ','

// Uncomment whatever type you're using!
#define DHTTYPE DHT11   // DHT 11
//#define DHTTYPE DHT22   // DHT 22  (AM2302), AM2321
//#define DHTTYPE DHT21   // DHT 21 (AM2301)

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

/*
 * parametri config (nel file TERMU.INI)
 * 1) ssid  //forse anche metodo (WPA/etc.)?
 * 2) pwdWifi
 * 3) tempSoglia
 * 4) finestraIsteresi
 */
String ssid;
String pwdWifi;
int tempSoglia=25;
int finestraIsteresi=2;

SoftwareSerial mySerial(10, 11); // RX, TX

//char temp[5]; // per conversione itoa


void setup() {
    Serial.begin(9600);
    Serial.println("Booting...");

    pinMode(PIRPIN, INPUT_PULLUP);
    pinMode(LEDPIN, OUTPUT);

    mySerial.begin(9600);
    //mySerial.println("Inizio log");

    dht.begin();

    readConfig();
}

void loop() {
    // Wait a few seconds between measurements.
    delay(1000);

    // Reading temperature or humidity takes about 250 milliseconds!
    // Sensor readings may also be up to 2 seconds 'old' (its a very slow sensor)
    float h = dht.readHumidity();
    // Read temperature as Celsius (the default)
    float t = dht.readTemperature();
    // Read temperature as Fahrenheit (isFahrenheit = true)
    float f = dht.readTemperature(true);

    // Check if any reads failed and exit early (to try again).
    if (isnan(h) || isnan(t) || isnan(f)) {
        Serial.println("Failed to read from DHT sensor!");
        return;
    }

    // Compute heat index in Fahrenheit (the default)
    float hif = dht.computeHeatIndex(f, h);
    // Compute heat index in Celsius (isFahreheit = false)
    float hic = dht.computeHeatIndex(t, h, false);

    //mySerial.println(itoa(t,temp,10));

    Serial.print("SSID: ");
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
    Serial.print("F, ");
    Serial.print("PIR:");
    Serial.println(!digitalRead(PIRPIN));

    if(t>tempSoglia)
        digitalWrite(LEDPIN,HIGH);

    if(t<=(tempSoglia-finestraIsteresi))
        digitalWrite(LEDPIN,LOW);

    /*
      mySerial.print("ls");
      mySerial.write(13);
      output();
    */

    //mySerial.print("read TERMU.INI");
    //mySerial.write(13);

    //readSDandPrint();

    //Serial.println(".pre-ls.");
    //Serial.print(mySerial.read());
}

String readSD() {
    //Serial.println("in readSD");
    String out="";
    while(mySerial.available()) {
        //Serial.println("in while");
        char carattere=(char)mySerial.read();

        if(carattere==SEPA) break;
        if(carattere=='\n') break;

        //Serial.print(carattere);
        out.concat(carattere);
        delay(10);
    }
    return out;
}

void readConfig() {
    //Serial.println("in readConfig");
    //goComando();
    mySerial.print("read TERMU.INI");
    mySerial.write(13);
    delay(100);

    readSD();
    ssid=readSD();
    pwdWifi=readSD();
    tempSoglia=readSD().toInt();
    finestraIsteresi=readSD().toInt();
}

// TODO: modalita' comando/logger  (in futuro per loggare eventi e misure)
// TODO: funzioncina 'command'
void goComando() {
    mySerial.write(26);
    mySerial.write(26);
    mySerial.write(26);
    delay(1000);
    mySerial.write(13);
}

/*
void readSDandPrint() {
    while(mySerial.available()) {
        Serial.print((char)mySerial.read());
        delay(10);
    }
}
*/
