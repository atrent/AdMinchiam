# Termuinator

"Efficientatore" (versione full), "fregatore" (versione mini) e misuratore consumo caloriferi


## Inventario dell'esperimento "sparare ad un moscerino..."

(foto!)

+ termuinator (DHT + PID climate controller, COOL mode, esphome)
+ lettura consumo calorifero via esp32cam (con led programmato, tuning flash led, esphome, vibrazione dovuta al fan)

+ motion (on openwrt, tuning diff!!!)
+ syncthing (sync dei file)
+ script stats.sh (stats generazione immagini, per aiutare nel tuning motion+flashing)

+ script ocr.sh (stats consumo Internet)
    - digressione adb
    - script screenshot.sh

+ temp esterna (raspi + dht, logs su file)

+ MQTT per campionamento info dal termuinator (su miniserver, filtrato)

+ sheet log (a vista e via cam, NON automatizzato, provato OCR su immagini ma non estrae nulla di utile)

+ script plot.r

+ consumo ventilatore circa 50W al max


## Migliorie possibili

- posizionare temperatura esterna a nord

- mettere altri sensori temp in lab per confronto e check solare

- BME280 invece di DHT

- sensore temp calorifero ad appoggio (sonda), es. LM35

- wireguard e PULL (scp dei log, chirurgico) invece di PUSH (syncthing)

- nodered per processare direttamente i msg invece di passare per i csv (dallo stream mqtt)

- lettura RF del contacalorie (provata con tesista qualche anno fa, senza successo, ho la board, ma complicata)

- calcolare/prevedere direttamente i consumi in funzione della temperatura misurata al calorifero (i.e., clono la funzionalità del contacalorie: integrazione temperatura/dT)

- lettura consumo fan (magari via sonoff)

- si può scorporare le varie componenti: avere solo un termometro e solo una fan comandabile, che si parlano in MQTT, logica distribuita o centralizzata (HomeAssistant?)