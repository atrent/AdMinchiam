//////// NON SERVE PIU', c'e' SPIFFS!!!


#define EEPROM_MAX 1024
#define EEPROM_WRITE false
int cursor=0; // for EEPROM

//////////////////////////////////////////
boolean eeprom_seekAbsolute(int c) {
    /*
    if(DEBUG) {
        Serial.print("[seek from ");
        Serial.print(cursor);
        Serial.print(" to ");
        Serial.print(c);
        Serial.print("]");
    }
    */

    //TOP & BOTTOM check
    if (c<0) {
        cursor=0;
        return true;
    }
    else if(c>=EEPROM_MAX) {
        cursor=EEPROM_MAX-1;  // zero-based!!!
        return true;
    }
    else
        cursor=c;

    return false;
}

boolean  eeprom_seekRelative(int c) {
    return eeprom_seekAbsolute(cursor+c);
}

boolean eeprom_seekInc() {
    return eeprom_seekRelative(1);
}

void eeprom_writeString(String s) {
    if(DEBUG) {
        Serial.print(s);
        Serial.print(" to write @");
        Serial.println(cursor);
    }

    for(int i=0; i<s.length(); i++) {
        EEPROM.put(cursor,s.charAt(i));
        eeprom_seekInc();
    }

    /*
    if(DEBUG) {
        Serial.print((char)EEPROM.read(cursor-1));
    }
    */

    EEPROM.write(cursor,0); //null char
    eeprom_seekInc();

    //NB: solo ESP8266!!!
    EEPROM.commit();
}

int eeprom_read() {
    byte b=EEPROM.read(cursor);
    /*
    if(DEBUG)
        Serial.print((char)b);
        */
    if(eeprom_seekInc()) return -1;
    return b;
}

String eeprom_readString() {
    if(DEBUG) {
        Serial.print("reading @");
        Serial.print(cursor);
        Serial.print(" ... '");
    }

    String s=String();

    char c=0;
    while(
        (c=eeprom_read())!=0
    ) {
        if(c==-1)
            break;

        s.concat(c);

    }

    if(DEBUG) {
        Serial.print(s);
        Serial.print("' read, now @");
        Serial.println(cursor);
    }

    return s;
}


boolean eeprom_seekZero() {
    return eeprom_seekAbsolute(0);
}



void eeprom_debug_readAllStrings() {
    Serial.println("ALL MEM (Strings)");
    eeprom_seekZero();
    for(int i=0; i<EEPROM_MAX; i++) {
        String s=eeprom_readString();
        if(s.length()>0) {
            Serial.print(s);
            Serial.print(",");
        }
        i=cursor; //skip
    }
    Serial.println();
}

void eeprom_debug_readAll() {
    Serial.println(F("ALL MEM"));
    eeprom_seekZero();
    for(int i=0; i<EEPROM_MAX; i++) {
        int c=eeprom_read();
        //Serial.print("(");
        Serial.print(c);
        Serial.print(",");
        Serial.print((char)c);
        Serial.print("|");
    }
    Serial.println();
}

void eeprom_zeroAll() {
    //eeprom_seekZero();
    for(int i=0; i<EEPROM_MAX; i++) {
        EEPROM.put(i,0);
    }

    //NB: solo ESP8166!!!
    EEPROM.commit();
}

/*
void eeprom_writeInt(int i) {
    EEPROM.write(cursor,i);
    EEPROM.commit();
    cursor+=s.length()+1; // null char?
}

int eeprom_readInt() {
    int i=0;
    EEPROM.get(cursor, i);
    cursor+=sizeof(int);
}
*/



void eeprom_init() {
    EEPROM.begin(EEPROM_MAX);
    eeprom_zeroAll();
}
