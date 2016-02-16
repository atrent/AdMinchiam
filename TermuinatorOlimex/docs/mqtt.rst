MQTT Termuinator setup
========================

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


