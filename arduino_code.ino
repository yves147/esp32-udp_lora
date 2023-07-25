#include <WiFi.h>
#include <WiFiUdp.h>
#include <heltec.h>

#define BAND 868E6  // you can set band here directly,e.g. 868E6,915E6

const char* ssid = "SOMEDATA";
const char* password = "SOMEDATA";
const char* hostIp = "SOMEDATA";

const int udpPort = 61283;

// as in example if still want to display it this may help
// receiver
int r_rssi = 0;
String r_packSize = "--";
String r_packet;

// sender
unsigned int s_counter = 0;
String s_rssi = "RSSI --";
String s_packSize = "--";
String s_packet;

WiFiUDP udp;
int dataToSend = 0;

const int MAX_DATA_SIZE = 255;
const int HEADER_SIZE = 7;

void LoRaData(uint8_t data[], int dataSize) {
  if (dataSize > MAX_DATA_SIZE - HEADER_SIZE) {
    return;
  }

  uint8_t packet[MAX_DATA_SIZE];
  int dataIndex = 0;

  packet[dataIndex++] = 0; // 0
  packet[dataIndex++] = random(0, 254); // 1

  packet[dataIndex++] = 0; // 2 to be implemented

  packet[dataIndex++] = r_rssi >> 8; // 3 << 8 +
  packet[dataIndex++] = r_rssi & 0x0F; // 4

  packet[dataIndex++] = dataSize >> 8; // 5 << 8 +
  packet[dataIndex++] = dataSize & 0x0F; // 6

  for (int i = 0; i < dataSize; i++) {
    packet[dataIndex++] = data[i];
  }

  udp.beginPacket(hostIp, udpPort);
  udp.write(packet, dataIndex);
  udp.endPacket();
}

void cbk(int packetSize) {
  if (packetSize > 0 && packetSize <= MAX_DATA_SIZE) {
    uint8_t receivedData[packetSize];
    for (int i = 0; i < packetSize; i++) {
      receivedData[i] = LoRa.read();
    }

    LoRaData(receivedData, packetSize);
  }
}

void setup() {
  Heltec.begin(true /*DisplayEnable Enable*/, true /*Heltec.Heltec.Heltec.LoRa Disable*/, true /*Serial Enable*/, true /*PABOOST Enable*/, BAND /*long BAND*/);

  Heltec.display->clear();
  Heltec.display->setTextAlignment(TEXT_ALIGN_LEFT);
  Heltec.display->setFont(ArialMT_Plain_10);

  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
  }

  udp.begin(udpPort);
}

// udp structure:
// byte 0: down=0/up=1/info=2
// byte 1: message id
// byte 2: ! to be implemented: potential first bytes checksum
// byte 3-4: 16 byte rssi
// byte 5-6: length (! obv check needed)
// data
void loop() {
  // get packet, prio 1
  int LORApacketSize = LoRa.parsePacket();
  if (LORApacketSize) {
    cbk(LORApacketSize);
  }

  int UDPpacketSize = udp.parsePacket();
  if (UDPpacketSize) {
    char sbuf[MAX_DATA_SIZE];
    int len = udp.read(sbuf, 255);
    char resultbuf[len - HEADER_SIZE];
    for (int i=HEADER_SIZE;i<len;i++){
      resultbuf[i - HEADER_SIZE] = sbuf[i];
    }
    if(len > 0) {
      sbuf[len] = 0;
    }
    if(sbuf[0] == 1) {
      LoRa.beginPacket();
      LoRa.setTxPower(14, RF_PACONFIG_PASELECT_PABOOST);
      LoRa.printf(resultbuf);
      LoRa.endPacket();
    } else if(sbuf[0] == 2){
      String str = "";
      for (int i = 0; i < len - HEADER_SIZE; i++) {
        str += resultbuf[i];
      }
      Heltec.display->clear();
      Heltec.display->drawString(0, 0, str);
      Heltec.display->display();
    }
  }
}

  delay(1000);
}
