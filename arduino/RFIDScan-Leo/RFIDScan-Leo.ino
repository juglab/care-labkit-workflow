
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

#include <Adafruit_NeoPixel.h>
#ifdef __AVR__
 #include <avr/power.h> // Required for 16 MHz Adafruit Trinket
#endif

// PIN Numbers : RESET + SDAs
#define RST_PIN         13
#define SS_1_PIN        10

// Led PINs
#define GreenLed        LED_BUILTIN
#define CIRCLE1_PIN        A3
#define CIRCLE2_PIN        A4
#define CIRCLE3_PIN        A5
#define NUMPIXELS 32

// Slider PINs
#define SLIDER1_PIN A1
#define SLIDER2_PIN A0

int slider1Val = 0;
int slider2Val = 0;
float lightness = 0.2;

int value1 = -1;
int value2 = -1;
int value3 = -1;

// List of Tags UIDs that are allowed for the first reader
byte tagarray_SS1[][4] = {
  {0xC9, 0x35, 0x17, 0xD3},
  {0xEA, 0x60, 0x17, 0xD3},
  {0xF5, 0x30, 0x16, 0xD3}
};

#define NR_OF_READERS   1
byte ssPins[] = {SS_1_PIN};
int ssRemoved[] = {0};

// Create an MFRC522 instance :
MFRC522 mfrc522[NR_OF_READERS];

Adafruit_NeoPixel _pixels1(NUMPIXELS, CIRCLE1_PIN, NEO_GRB + NEO_KHZ800);
Adafruit_NeoPixel _pixels2(NUMPIXELS, CIRCLE2_PIN, NEO_GRB + NEO_KHZ800);
Adafruit_NeoPixel _pixels3(NUMPIXELS, CIRCLE3_PIN, NEO_GRB + NEO_KHZ800);
Adafruit_NeoPixel pixels[3] = {_pixels1, _pixels2, _pixels3};

bool rfid_tag_present_prev[NR_OF_READERS] = {false};
bool rfid_tag_present[NR_OF_READERS] = {false};
int _rfid_error_counter[NR_OF_READERS] = {0};
bool _tag_found[NR_OF_READERS] = {false};

int col1[] = {int(255*lightness), int(0*lightness), int(90*lightness)}; //255, 49, 117
int col2[] = {int(170*lightness), int(70*lightness), int(0*lightness)}; //197, 49, 255
int col3[] = {int(0*lightness), int(10*lightness), int(205*lightness)}; //49, 193, 255

char fromUno[4];

void setup() {

  Serial.begin(9600);           // Initialize serial communications with the PC
  //while (!Serial);            // Do nothing if no serial port is opened (added for Arduinos based on ATMEGA32U4)
  
  SPI.begin();                  // Init SPI bus

  delay(1000);

  /* Initializing Inputs and Outputs */
  pinMode(GreenLed, OUTPUT);
  digitalWrite(GreenLed, LOW);

  pinMode(SLIDER1_PIN, INPUT);
  pinMode(SLIDER2_PIN, INPUT);  

  delay(1000);

  for (uint8_t reader = 0; reader < NR_OF_READERS; reader++) {
    mfrc522[reader].PCD_Init(ssPins[reader], RST_PIN);
//    mfrc522[reader].PCD_SetAntennaGain(mfrc522[reader].RxGain_max);
    delay(1000);
    Serial.print(F("Reader "));
    Serial.print(reader);
    Serial.print(F(": "));
    mfrc522[reader].PCD_DumpVersionToSerial();
  }

  for (uint8_t reader = 0; reader < 3; reader++) {
    pixels[reader].begin();
    resetPixels(reader);
  }
  
  delay(1000);
}

/*
   Main loop.
*/

void loop() {

  readFromUno();

  readSliders();

  for (uint8_t reader = 0; reader < NR_OF_READERS; reader++) {

    rfid_tag_present_prev[reader] = rfid_tag_present[reader];

    _rfid_error_counter[reader] += 1;
    if(_rfid_error_counter[reader] > 5){
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
              setPixelsValue(2, x);
              triggerKeyboard(2, x);
            }
          }
        }
      }
      if(!foundTag) {
        denyTag(2);
      }
    }
    
    // falling edge
    if (!rfid_tag_present[reader] && rfid_tag_present_prev[reader]){
      resetPixels(2);
      triggerGone(2);
    }
    
    delay(200);

  }
}

void readFromUno() {
  static byte ndx = 0;
  char endMarker = '\n';
  char rc;
  const byte numChars = 32;
  char receivedChars[numChars]; // an array to store the received data
  boolean newData = false;
 // if (Serial.available() > 0) {
  while (Serial.available() > 0 && newData == false) {
    rc = Serial.read();
    if (rc != endMarker) {
      receivedChars[ndx] = rc;
      ndx++;
      if (ndx >= numChars) {
        ndx = numChars - 1;
      }
    }
    else {
      receivedChars[ndx] = '\0'; // terminate the string
      ndx = 0;
      newData = true;
    }
  }
  if(newData) {
    String command = String(receivedChars);
    command.replace("\r", "");
    command.replace("\n", "");
    if(command.startsWith("R")) {
      command.remove(0, 1);
      int reader = 1-command.substring(0,1).toInt();
      if(command.endsWith("NO")) {
        resetPixels(reader);
      }
      else if(command.endsWith("??")) {
        setPixelsValueDenied(reader);
      } 
      else {
        int id = command.substring(3,4).toInt();
        setPixelsValue(reader, id); 
      }
    }
  }
}

void recvWithEndMarker() {
 
}

void readSliders() {
  int slider1 = analogRead(SLIDER1_PIN);
  if(slider1 != slider1Val) {
    slider1Val = slider1;
    triggerKeyboardSlider1Value(slider1Val);
  }
  int slider2 = analogRead(SLIDER2_PIN);
  if(slider2 != slider2Val) {
    slider2Val = slider2;
    triggerKeyboardSlider2Value(slider2Val);
  }
}

void triggerKeyboard(int reader, int tag) {
  String out = "R";
    out = out + reader + "_T" + tag;
  Serial.println(out);
}

void triggerGone(int reader) {
  String out = "R";
    out = out + reader + "_NO";
  Serial.println(out);
}

void triggerKeyboardSlider2Value(int sliderValue) {
  String out = "S2";
    out = out + "_" + sliderValue;
  Serial.println(out);
}

void triggerKeyboardSlider1Value(int sliderValue) {
  String out = "S1";
  out = out + "_" + sliderValue;
  Serial.println(out);
}


size_t tagsNum(int reader) {
  if(reader == 0) return sizeof(tagarray_SS1) / sizeof(tagarray_SS1[0]);
}

byte* getTag(int reader, int tag) {
  if(reader == 0) return tagarray_SS1[tag];
}

void denyTag(int reader) {
  String out = "R";
  out = out + reader + "_??";
  Serial.println(out);
  setPixelsValueDenied(reader);
}

void setPixelsValueDenied(int reader) {
  int red[] = {255, 0, 0};
  setPixelsValue(reader, red);
}

void setPixelsValue(int reader, int value) {

  if(value < 0) return;
  
  if(reader == 0) {
    value1 = value;
    rotatePixelsValue(reader, value, col1);
  }
  if(reader == 1) {
    value2 = value;
    rotatePixelsValue(reader, value, col2);
  }
  if(reader == 2) {
    value3 = value;
    rotatePixelsValue(reader, value, col3);
  }
}

void setPixelsValue(int reader, int color[]) {

  for(int i=0; i<NUMPIXELS; i++) {
    pixels[reader].setPixelColor(i, pixels[reader].Color(color[0],color[1],color[2]));
  }
  pixels[reader].show();

}

void rotatePixelsValue(int reader, int value, int col[]) {
  for(int i=0; i<NUMPIXELS; i++) {
    pixels[reader].setPixelColor(i, pixels[reader].Color(col[0], col[1], col[2]));
    pixels[reader].show();
    delay(20);
  }
}

void resetPixels(int reader) {  
  if(reader == 0) {
    resetPixels(reader, col1);
  }
  if(reader == 1) {
    resetPixels(reader, col2);
  }
  if(reader == 2) {
    resetPixels(reader, col3);
  }
}


void resetPixels(int reader, int col[]) {
  for(int i=0; i<NUMPIXELS; i++) {
    if(i % 4 == 0) {
      pixels[reader].setPixelColor(i, pixels[reader].Color(col[0], col[1], col[2]));
    } else {
      pixels[reader].setPixelColor(i, pixels[reader].Color(0,0,0));
    }
    pixels[reader].show();
    delay(20);
  }
}


void setPixelsValue(int reader, int value, int col[]) {
  for(int i=0; i<NUMPIXELS; i++) {
    pixels[reader].setPixelColor(i, pixels[reader].Color(col[0], col[1], col[2]));
  }
}
