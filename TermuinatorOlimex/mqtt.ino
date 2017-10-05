// MQTT lib: https://github.com/adafruit/Adafruit_MQTT_Library
// circa DONE: compatibilita' MQTT

// TODO: mqtt topic patterns

//TODO auth MQTT con username e password 

//////////////////////////////////////////////////
//const char* mqtt_server = "broker.mqtt-dashboard.com";
//String mqtt_server = "broker.mqtt-dashboard.com";
//String mqtt_server = "test.mosquitto.org";
//String mqtt_server = "SBAGLIATO"; // per vedere se lo tira su dal file

// mosquitto_sub -v -h test.mosquitto.org -t Termuinator18fe34a206e


#define TOPIC "Termuinator"  //TODO: renderlo configurabile

#define DELAY_MQTT 9000



///////////////////////////////////////////////////
#include <PubSubClient.h>  // mqtt
PubSubClient mqtt_client(espClient);
long lastMsg = 0;
#define MSG_LEN 150
char msg[MSG_LEN];



void mqtt_reconnect() {
    // TODO: nr. tentativi???  per forza perche' se non trova mqtt server non fa piu' nulla

    // TODO: condizionare invio (altrove)

    // Loop until we're reconnected
    if (!mqtt_client.connected()) {

        Serial.print("Attempting MQTT connection...");
        // Attempt to connect
        if (mqtt_client.connect(nomeNodo.c_str())) {
            Serial.println("connected");
            // Once connected, publish an announcement...
            mqtt_client.publish(nomeNodo.c_str(), "first msg.");
            // ... and resubscribe
            mqtt_client.subscribe("TermuinatorConfig/#");
        } else {
            Serial.print("failed, rc=");
            Serial.print(mqtt_client.state());
            Serial.println(" skipping MQTT...");
            // Wait 5 seconds before retrying
            //delay(5000);
        }
    }
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
