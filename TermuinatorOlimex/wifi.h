///////////////////////////////////////////////////
//https://github.com/esp8266/Arduino/blob/master/doc/libraries.md#wifiesp8266wifi-library
//https://www.arduino.cc/en/Reference/WiFi

#include <ESP8266WiFi.h>
WiFiClient espClient;
IPAddress gateway;

#define WIFI_TENTATIVI 100
#define USE_GW "[gw]"



///////////////////////////////////////////////////
String wifi = "";
String ssid = "";
String password = "";




boolean wifi_setup() {
    delay(100);

    // We start by connecting to a WiFi network
    Serial.println();
    Serial.print("Connecting to ");
    Serial.println(ssid);

    WiFi.disconnect();
    WiFi.begin(ssid.c_str(), password.c_str());

    for (int i=0; (WiFi.status() != WL_CONNECTED) && (i < WIFI_TENTATIVI) && digitalRead(0)==HIGH; i++) {
        delay(500);
        Serial.print(".");
    }

    if(WiFi.status() == WL_CONNECTED) {
        Serial.println();
        Serial.println("WiFi connected");
        Serial.println("IP address: ");
        Serial.println(WiFi.localIP());

        gateway=WiFi.gatewayIP();

        if(mqtt_server.equals(USE_GW)) mqtt_server=String(gateway[0])+"."+String(gateway[1])+"."+String(gateway[2])+"."+String(gateway[3]);

        Serial.println(gateway);

        //char n[nodo.length()];
        //nomeNodo=n;
        //Serial.println(nodo);
        return true;
    }
    else {
        Serial.println("WiFi FAILED!");
        return false;
    }
}
