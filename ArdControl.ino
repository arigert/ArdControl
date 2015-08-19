unsigned long timer;
String pinName[] = {"D2","D3","D4","D5","D6","D7","D8","D9","D10","D11","D12","D13","A0","A1","A2","A3","A4","A5"};
int pinNum[] = {2,3,4,5,6,7,8,9,10,11,12,13,0,1,2,3,4,5};
int pinModeNum[] = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
int numPins = 18;

// controls which pin we're reading/sending
int currPin = 0;

void setup() {
  // Do not change this baud rate unless your BT module is different!
  Serial.begin (115200);
  
  // make it possible to use pullup resistors
  pinMode(7, INPUT_PULLUP);

  // start timer
  timer = millis();
}

void loop() {
  if (millis() - timer > 5) {
    // read any input
    if (Serial.available()) {
      String data = Serial.readStringUntil('\n');
      String indData[3];
      
      if (arraySplit(data, indData, 3) == 3) {
        
        // get each component (name, mode, value)
        int pinIndex = arrayIndexOf(pinName, numPins, indData[0]);
        
        if (pinIndex >= 0) {
          int mode = indData[1].toInt();
          setMode(pinIndex, mode);
          if (mode == 2) digitalWrite(pinNum[pinIndex], indData[2].toInt());
          else if (mode == 4) analogWrite(pinNum[pinIndex], indData[2].toInt());
        }
      }
    }
    Serial.flush();

    // send value of pin
    Serial.print(pinName[currPin]);
    Serial.print(" ");
    if (pinModeNum[currPin] == 1 || pinModeNum[currPin] == 5) Serial.println(digitalRead(pinNum[currPin]));
    else if (pinModeNum[currPin] == 3) Serial.println(analogRead(pinNum[currPin]));
    else Serial.println("0");
    
    currPin = (++currPin % numPins);
    
    timer = millis();
  }
}
  
void setMode(int pinIndex, int mode) {
  if (mode < 0 || mode > 5) return;
  
  // modes: 0 = off, 1 = digital input, 2 = digital output, 3 = analog input, 4 = analog output, 5 = input w/pullup
  if (pinModeNum[pinIndex] == mode) return;
  
  // turn off
  if (pinModeNum[pinIndex] == 2) digitalWrite(pinNum[pinIndex], LOW);
  if (pinModeNum[pinIndex] == 4) analogWrite(pinNum[pinIndex], 0);
  
  // store new pin mode
  pinModeNum[pinIndex] = mode;
  
  // set new pin mode
  if (mode == 1 || mode == 3) pinMode(pinNum[pinIndex], INPUT);
  else if (mode == 2 || mode == 4) pinMode(pinNum[pinIndex], OUTPUT);
  else if (mode == 5) pinMode(pinIndex, INPUT_PULLUP);
}

int arrayIndexOf(String array[], int numItems, String searchFor) {
  searchFor.trim();
  
  for (int i=0; i<numItems; i++) {
    if (array[i].equals(searchFor)) return i;
  }
  return -1;
}

int arraySplit(String contents, String* array, int arrLen) {
  contents.trim();
  for (int i=0;i<arrLen;i++) {
    array[i] = "";
  }
  
  int endPos = 0;
  int i = 0;
  while (endPos >= 0 && i < arrLen) {
    endPos = contents.indexOf(" ");
    if (endPos < 0) {
      array[i] = contents.substring(0, contents.length());
    }
    else {
      array[i] = contents.substring(0, endPos);
      contents = contents.substring(endPos+1, contents.length());
    }
    i++;
  }
  
  return i;
}
    
