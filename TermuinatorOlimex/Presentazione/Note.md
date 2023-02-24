# Termuinator

"Efficientatore" e misuratore consumo caloriferi



## Inventario dell'esperimento "sparare ad un moscerino..."

- termuinator (PID climate controller, COOL mode, esphome)
- lettura consumo calorifero via esp32cam (con led programmato, tuning flash led, esphome, vibrazione dovuta al fan)
- motion (on openwrt, tuning diff!!!)
- syncthing (sync dei file)
- temp esterna (raspi + dht, logs su file)
- MQTT per campionamento info dal termuinator (su miniserver, filtrato)
- sheet log (a vista e via cam, NON automatizzato, provato OCR su immagini ma non estrae nulla di utile)
- script stats.sh (stats generazione immagini, per aiutare nel tuning)
- script ocr.sh (stats consumo Internet)
    - digressione adb
    - script screenshot.sh
- script plot.r
- consumo ventilatore circa 50W al max (era da campionare anche questo... magari via sonoff)