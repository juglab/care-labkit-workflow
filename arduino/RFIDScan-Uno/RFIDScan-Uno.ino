
/**
   --------------------------------------------------------------------------------------------------------------------
   Example sketch/program showing how to read data from more than one PICC to serial.
   --------------------------------------------------------------------------------------------------------------------
   This is a MFRC522 library example; for further details and other examples see: https://github.com/miguelbalboa/rfid

   Example sketch/program showing how to read data from more than one PICC (that is: a RFID Tag or Card) using a
   MFRC522 based RFID Reader on the Arduino SPI interface.

   Warning: This may not work! Multiple devices at one SPI are difficult and cause many trouble!! Engineering skill
            and knowledge are required!

   @license Released into the public domain.

   Typical pin layout used:
   -----------------------------------------------------------------------------------------
               MFRC522      Arduino       Arduino   Arduino    Arduino          Arduino
               Reader/PCD   Uno/101       Mega      Nano v3    Leonardo/Micro   Pro Micro
   Signal      Pin          Pin           Pin       Pin        Pin              Pin
   -----------------------------------------------------------------------------------------
   RST/Reset   RST          9             5         D9         RESET/ICSP-5     RST
   SPI SS 1    SDA(SS)      ** custom, take a unused pin, only HIGH/LOW required *
   SPI SS 2    SDA(SS)      ** custom, take a unused pin, only HIGH/LOW required *
   SPI MOSI    MOSI         11 / ICSP-4   51        D11        ICSP-4           16
   SPI MISO    MISO         12 / ICSP-1   50        D12        ICSP-1           14
   SPI SCK     SCK          13 / ICSP-3   52        D13        ICSP-3           15

*/

#include <SPI.h>
#include <MFRC522.h> 


// PIN Numbers : RESET + SDAs
#define RST_PIN         9
#define SS_1_PIN        8
#define SS_2_PIN        10

// Led PINs
#define GreenLed        LED_BUILTIN

int value1 = -1;
int value2 = -1;
int value3 = -1;

// List of Tags UIDs that are allowed for the first reader
byte tagarray_SS1[][4] = {
  {0x36, 0x08, 0xCD, 0x3B},
  {0xD6, 0xE3, 0x90, 0x8D},
  {0xD6, 0x60, 0xC9, 0x3B}
};

// List of Tags UIDs that are allowed for the second reader
byte tagarray_SS2[][4] = {
  {0x63, 0x6B, 0xD1, 0x83},
  {0xDA, 0x43, 0xD1, 0x83},
  {0x14, 0xDC, 0xCA, 0x83}
};

#define NR_OF_READERS   2

//byte ssPins[] = {SS_1_PIN, SS_2_PIN, SS_3_PIN, SS_4_PIN};
byte ssPins[NR_OF_READERS] = {SS_1_PIN, SS_2_PIN};

bool rfid_tag_present_prev[NR_OF_READERS] = {false, false};
bool rfid_tag_present[NR_OF_READERS] = {false, false};
int _rfid_error_counter[NR_OF_READERS] = {0, 0};
bool _tag_found[NR_OF_READERS] = {false, false};

// Create an MFRC522 instance :
MFRC522 mfrc522[NR_OF_READERS];

void setup() {

  Serial.begin(9600);           // Initialize serial communications with the PC
  while (!Serial);              // Do nothing if no serial port is opened (added for Arduinos based on ATMEGA32U4)
  
  SPI.begin();                  // Init SPI bus

  delay(1000);

  /* Initializing Inputs and Outputs */
  pinMode(GreenLed, OUTPUT);
  digitalWrite(GreenLed, LOW);

//  for (uint8_t reader = 0; reader < NR_OF_READERS; reader++) {
//    mfrc522[reader].PCD_Init(ssPins[reader], RST_PIN);
////    mfrc522[reader].PCD_SetAntennaGain(mfrc522[reader].RxGain_max);
//    delay(1000);    
//  }

  for (uint8_t reader = 0; reader < NR_OF_READERS; reader++) {
    mfrc522[reader].PCD_Init(ssPins[reader], RST_PIN);
//    mfrc522[reader].PCD_SetAntennaGain(mfrc522[reader].RxGain_max);
    delay(1000);
//    bool result = mfrc522[reader].PCD_PerformSelfTest(); // perform the test
//    if (result) {
//      String out = "R";
//      out = out + reader + "_OK";
//      Serial.println(out);
//    }
//    else {
//      String out = "R";
//      out = out + reader + "_NO";
//      Serial.println(out);
//    }
  }
  delay(1000);
  writeString("XX_GO");
}

/*
   Main loop.
*/

void loop() {

  for (uint8_t reader = 0; reader < NR_OF_READERS; reader++) {

    rfid_tag_present_prev[reader] = rfid_tag_present[reader];

    _rfid_error_counter[reader] += 1;
    if(_rfid_error_counter[reader] > 2){
      _tag_found[reader] = false;
    }
  
    // Detect Tag without looking for collisions
    byte bufferATQA[2];
    byte bufferSize = sizeof(bufferATQA);
  
    // Reset baud rates
    mfrc522[reader].PCD_WriteRegister(mfrc522[reader].TxModeReg, 0x00);
    mfrc522[reader].PCD_WriteRegister(mfrc522[reader].RxModeReg, 0x00);
    // Reset ModWidthReg
    mfrc522[reader].PCD_WriteRegister(mfrc522[reader].ModWidthReg, 0x26);
  
    MFRC522::StatusCode result = mfrc522[reader].PICC_RequestA(bufferATQA, &bufferSize);
  
    if(result == mfrc522[reader].STATUS_OK){
      
      if ( ! mfrc522[reader].PICC_ReadCardSerial()) { //Since a PICC placed get Serial and continue 
        return;
      }
      _rfid_error_counter[reader] = 0;
      _tag_found[reader] = true;        
    }
    
    rfid_tag_present[reader] = _tag_found[reader];
    
    // rising edge
    if (rfid_tag_present[reader] && !rfid_tag_present_prev[reader]){
      boolean foundTag = false;
      for (int x = 0; x < tagsNum(reader) && !foundTag; x++)                  // tagarray's row
      {
        for (int i = 0; i < mfrc522[reader].uid.size; i++)        //tagarray's columns
        {
          if ( mfrc522[reader].uid.uidByte[i] != getTag(reader, x)[i])  //Comparing the UID in the buffer to the UID in the tag array.
          {
            break;
          }
          else
          {
            if (i == mfrc522[reader].uid.size - 1)                // Test if we browesed the whole UID.
            {
              foundTag = true;
              triggerSerialAdd(reader, x); 
            }
          }
        }
      }
      if(!foundTag) {
        denyTag(reader);
      }
    }
    
    // falling edge
    if (!rfid_tag_present[reader] && rfid_tag_present_prev[reader]){
//      Serial.println("Tag gone");
      triggerSerialRemove(reader);
    }
    
    delay(200);
  } //for(uint8_t reader..
}

void triggerSerialAdd(int reader, int tag) {
    String out = "R";
    out = out + reader + "_T" + tag;
    writeString(out);
}

void triggerSerialRemove(int reader) {
    String out = "R";
    out = out + reader + "_NO";
    writeString(out);
}

size_t tagsNum(int reader) {
  if(reader == 0) return sizeof(tagarray_SS1) / sizeof(tagarray_SS1[0]);
  if(reader == 1) return sizeof(tagarray_SS2) / sizeof(tagarray_SS2[0]);
}

byte* getTag(int reader, int tag) {
  if(reader == 0) return tagarray_SS1[tag];
  if(reader == 1) return tagarray_SS2[tag];
}

void denyTag(int reader) {
  String out = "R";
  out = out + reader + "_??";
  writeString(out);
}

void writeString(String stringData) {
  Serial.println(stringData);
}
