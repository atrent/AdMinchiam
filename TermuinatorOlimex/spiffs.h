#define SPIFFS_DEBUG true
#define SPIFFS_NA "N.A."

#include <FS.h>

String spiffs_getValue(String file) {
    File f = SPIFFS.open(file, "r");
    if (!f) {
        Serial.println("file open failed");
        return SPIFFS_NA;
    }
    return f.readStringUntil('\n');
}


void spiffs_getValues() {
    String tmp=spiffs_getValue("/ssid");
    if(tmp!=SPIFFS_NA) ssid=tmp;

    tmp=spiffs_getValue("/password");
    if(tmp!=SPIFFS_NA) password=tmp;

    tmp=spiffs_getValue("/broker");
    if(tmp!=SPIFFS_NA) mqtt_server=tmp;

    tmp=spiffs_getValue("/soglia");
    if(tmp!=SPIFFS_NA) tempSoglia=tmp.toInt(); // attenzione che se non e' un numero torna 0!!!

    tmp=spiffs_getValue("/isteresi");
    if(tmp!=SPIFFS_NA) finestraIsteresi=tmp.toInt(); // attenzione che se non e' un numero torna 0!!!
}
