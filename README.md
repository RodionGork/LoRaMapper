# LoRa Mapper

This small application is intended to help measure and record coverage in `LoRaWAN` network.

It connects to LoRa module (ASR6501, but probably others will do) via BlueTooth (thus you should have
LoRa module wired to serial BlueTooth device, with proper power supply). Then it constantly checks GPS
coordinates and attempts to send insignificant data over LoRa, recording RSSI and SNR levels.
