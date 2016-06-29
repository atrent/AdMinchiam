MQTT Termuinator setup
========================

Termuinator uses MQTT to publish data in json format, via the Arduino library:
http://pubsubclient.knolleary.net/

Brokers can be bridged (federated), topics can be mapped, there could be a hierarchy of brokers, each composing the complete path of a node

Info about the MQTT broker to connect to can be sent via dhcp info (see http://www.networksorcery.com/enp/protocol/bootp/options.htm)


Topic
-----

Termuinator topic is so configured:

	``TLD/CITY/.../BUILDING/.../VARIABLE``

Where:
	``TLD`` is a generic domain name (not necessarily Internet), a context
	``CITY/.../BUILDING/...`` is the location pathname
	``VARIABLE`` is the name of a declarative value that must be shared (published)

Example:

	``atrent/Venice/CalleMinelli/Fenice/LivingRoom/Temperature``: 20
      ^^^^^^
      TLD
             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
             WHERE
                                                   ^^^^^^^^^^^
                                                   WHAT
                                                                  ^^^^^
                                                                  VALUE


TODO: (reflection) service list




Published data
---------------

Simple values... e.g., '20' (Celsius), '366' (PPM CO2), etc.

Can be JSON serialization of status


.. TODO: no more json, keeping this just in case
Data is published as a json stream with this schema:

```
{ "_type":"termuinator","ssid":"SSID","trigger_temp":T_TEMP,"histeresys":HISTER,"humidity":HUM,"temp":TEMP,"h_index":HEAT_IDX}
```

all temperatures are **in Celsius degrees**



Workflow of a node
------------------

Initial info: hostname of MQTT broker (optionally with password)




MQTT servers for testing
-------------------------

For testing purposes you can use the ``test.mosquitto.org`` broker, just be
aware that the payload size is limited (but the limit is undocumented)
