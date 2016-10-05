void util_switch(boolean on) {
    digitalWrite(RELAY,on);
    acceso=on;
}


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
    Serial.print(F("("));
    Serial.print(old);
    Serial.print(F(") "));

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

void util_printStatus() {
    /*  Serial.print("S: ");
        Serial.print(status);    */


    //SwSerial.println(nomeNodo);

    Serial.print(F("NODE: "));
    Serial.print(nomeNodo);

    Serial.print(F(", wifi enabled: "));
    Serial.print(wifi);

    Serial.print(F(", SSID: "));
    Serial.print(ssid);

    Serial.print(F(", PSK: "));
    Serial.print(password);

    Serial.print(F(", mqtt: "));
    Serial.print(mqtt_server);

    Serial.print(F(", TEMPSOGLIA: "));
    Serial.print(tempSoglia);

    Serial.print(F(", ISTERESI: "));
    Serial.print(finestraIsteresi);

    Serial.print(F(", acceso=: "));
    Serial.print(acceso);

    Serial.print(F(", Humidity: "));
    Serial.print(humidity);
    Serial.print(F("%, "));

    Serial.print(F("Temperature: "));
    Serial.print(temperature);
    Serial.print(F("C/"));
    Serial.print(fahreneit);
    Serial.print(F("F, "));

    Serial.print(F("Heat index: "));
    Serial.print(hic);
    Serial.print(F("C/"));
    Serial.print(hif);
    Serial.print(F("F, "));

    Serial.print(ESP.getFreeHeap(),DEC);
    Serial.println(F(" mem"));
}




// TODO: qualche funzione per mostrare roba su lcd

//34376
