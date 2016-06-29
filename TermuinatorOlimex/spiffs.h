#define SPIFFS_DEBUG true
#define SPIFFS_NA "N.A."

#include <FS.h>

// TODO: passare a file "properties" (stile .ini ma senza stanze)

// TODO: caratteri spurii nella stringa

String spiffs_getValue(String file) {
    File f = SPIFFS.open(file, "r");
    if (!f) {
        Serial.println("file open failed");
        return SPIFFS_NA;
    }
    return f.readStringUntil('\n');
}



int spiffs_writeValue(String file,String value) {
    File f = SPIFFS.open(file, "w");
    if (!f) {
        Serial.println("file open failed");
        return -2;
    }
    return f.print(value);
}

int spiffs_writeValue(String file,int value) {
    spiffs_writeValue(file, String(value));
}


void spiffs_getValues() {
    String tmp=spiffs_getValue("/wifi");
    if(tmp!=SPIFFS_NA) wifi=tmp;

    tmp=spiffs_getValue("/ssid");
    if(tmp!=SPIFFS_NA) ssid=tmp;

    tmp=spiffs_getValue("/password");
    if(tmp!=SPIFFS_NA) password=tmp;

    tmp=spiffs_getValue("/broker");
    if(tmp!=SPIFFS_NA) mqtt_server=tmp;

    tmp=spiffs_getValue("/modalita");
    if(tmp!=SPIFFS_NA){
		if(tmp.charAt(0)==EFFICIENTATORE) modalita=EFFICIENTATORE;
		if(tmp.charAt(0)==TERMOSTATO) modalita=TERMOSTATO;
	 }

    tmp=spiffs_getValue("/soglia");
    if(tmp!=SPIFFS_NA) tempSoglia=tmp.toInt(); // attenzione che se non e' un numero torna 0!!!

    tmp=spiffs_getValue("/isteresi");
    if(tmp!=SPIFFS_NA) finestraIsteresi=tmp.toInt(); // attenzione che se non e' un numero torna 0!!!
}


void spiffs_writeValues() {
    spiffs_writeValue("/wifi",wifi);
    spiffs_writeValue("/ssid",ssid);
    spiffs_writeValue("/password",password);
    spiffs_writeValue("/broker",mqtt_server);
    spiffs_writeValue("/modalita",String(modalita));
    spiffs_writeValue("/soglia",tempSoglia);
    spiffs_writeValue("/isteresi",finestraIsteresi);
}
