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

//#include <IniFile.h>
#include <SoftwareSerial.h>
#include "DHT.h"

////a regime altra lib////////////////////////////
/* Including ENC28J60 libraries */
#include <EtherCard.h>
#include <IPAddress.h>
/* LAN-unique MAC address for ENC28J60 controller */
static byte mymac[] = { 0x70,0x69,0x69,0x2D,0x30,0x33 };

/* TCP/IP send/receive buffer */
byte Ethernet::buffer[500];
#define ETHERCARD_ICMP 1

//////////////////////////////////////////////////

/* Including Strip-led libraries */
#include <SPI.h>
#include <Adafruit_WS2801.h>

#define DHTPIN 5     // what digital pin we're connected to
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

//const unsigned int led=7;

/* Setup strip-led stuff */
uint8_t dataPin = 2; // Yellow wire, on Adafruit-WS2801
uint8_t clockPin = 3; // Green wire, on Adafruit-WS2801

// nr. of LEDs
const int length = 17;

// config
Adafruit_WS2801 strip = Adafruit_WS2801(length, dataPin, clockPin);

/* This function return a 24bit color value from parameters r, g and b */
uint32_t Color(byte r, byte g, byte b) {
    uint32_t c;
    c = r;
    c <<= 8;
    c |= g;
    c <<= 8;
    c |= b;
    return c;
}



/*
 * parametri config (nel file TERMU.INI)
 * 1) nomeNodo
 * 2) ssid  //forse anche metodo (WPA/etc.)?
 * 3) pwdWifi
 * 4) tempSoglia
 * 5) finestraIsteresi
 */
String nomeNodo;
String ssid;
String pwdWifi;
int tempSoglia=25;
int finestraIsteresi=2;

// rompe alla ethercard, ci serviva per SD
//SoftwareSerial mySerial(10, 11); // RX, TX

//char temp[5]; // per conversione itoa

void rispondi(
    uint16_t dest_port,    ///< Port the packet was sent to
    uint8_t src_ip[4],    ///< IP address of the sender
    uint16_t src_port,    ///< Port the packet was sent from
    const char *data,   ///< UDP payload data
    uint16_t len)        ///< Length of the payload data
    {

    // DEBUG ...
    Serial.println("Received UDP packet.");
}


void setup() {
    Serial.begin(57600);
    Serial.println("Booting...");

    pinMode(PIRPIN, INPUT_PULLUP);
    pinMode(LEDPIN, OUTPUT);

    //mySerial.begin(9600);
    //mySerial.println("Inizio log");

    dht.begin();


	// sempre per SD
    //readConfig();

    /* Setup ledstrip stuff ... */
    strip.begin();
    strip.show();
    colorWipe(Color(0, 0, 0), 1);

// attenzione che se non e' collegata la ethernet aspetta un casino
    if (ether.begin(sizeof Ethernet::buffer, mymac) == 0)
        Serial.println( "Failed to access Ethernet controller");

    if (!ether.dhcpSetup()) {
        Serial.println("DHCP failed");
        // set fixed 192.168.10.10
        //ether.staticSetup({192,168,10,10},...,...);
    }

    ether.printIp("IP:  ", ether.myip);
    ether.printIp("GW:  ", ether.gwip);
    ether.printIp("DNS: ", ether.dnsip);

    /* ... and register udptoStripled() to port 57600 */
    ether.udpServerListenOnPort(&rispondi, 57600);
}

void loop() {
	ether.packetLoop(ether.packetReceive());

    // Wait a few seconds between measurements.
    delay(500);

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
    Serial.print("F, ");
    Serial.print("PIR:");
    Serial.println(!digitalRead(PIRPIN));


    strip.setPixelColor(t-15, Wheel(200*t)); // BUG R-B
    strip.show();

    if(t>=tempSoglia)
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


/*
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
    // TODO: aggiungere una modalita' di funzionamento? forse no, programma separato (*)
    nomeNodo=readSD();
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

void readSDandPrint() {
    while(mySerial.available()) {
        Serial.print((char)mySerial.read());
        delay(10);
    }
}
*/


//Input a value 0 to 255 to get a color value.
//The colours are a transition r - g -b - back to r
uint32_t Wheel(byte WheelPos)
{
    if (WheelPos < 85) {
        return Color(WheelPos * 3, 255 - WheelPos * 3, 0);
    } else if (WheelPos < 170) {
        WheelPos -= 85;
        return Color(255 - WheelPos * 3, 0, WheelPos * 3);
    } else {
        WheelPos -= 170;
        return Color(0, WheelPos * 3, 255 - WheelPos * 3);
    }
}


// fill the dots one after the other with said color
// good for testing purposes
void colorWipe(uint32_t c, uint8_t wait) {
    int i;

    for (i=0; i < strip.numPixels(); i++) {
        strip.setPixelColor(i, c);
        strip.show();
        delay(wait);
    }
}


