MQTT Termuinator setup
========================

Termuinator uses MQTT to publish data in json format, via the Arduino library:
http://pubsubclient.knolleary.net/

Topic
-----

Termuinator topic is so configured: ``/sensors/BULDING/TERMUINATOR_SN``

Where: ``BUILDING`` is the building name, ``TERMUINATOR_ID`` is the Termuinator
serial number

Published data
---------------

Data is published as a json stream with this schema:

```

{ "_type":"termuinator","ssid":"SSID","trigger_temp":T_TEMP,"histeresys":HISTER,"humidity":HUM,"temp":TEMP,"h_index":HEAT_IDX}

```

all temperatures are **in Celsius degrees**

MQTT servers for testing
-------------------------

For testing purposes you can use the ``test.mosquitto.org`` broker, just be
aware that the payload size is limited (but the limit is undocumented)
