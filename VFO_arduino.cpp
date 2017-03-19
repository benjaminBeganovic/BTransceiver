#include <MorseCode.h>
#include <AltSoftSerial.h>

MorseCode morse(13);
AltSoftSerial altSerial;
char posString = 4; //endChar - message termination
String snt = "T";
String message = "";
String message2 = "";
char tmp_char = 'p';
char tmp_char2 = 'p';

void setup() {
  snt += posString;
  Serial.begin(9600);
  altSerial.begin(9600);
}

void loop() {

  morse.sender();

  if (morse.messageSentFlag())
    altSerial.print(snt);

  if (altSerial.available() && tmp_char != posString)
  {
    tmp_char = altSerial.read();
    if (tmp_char != posString)
      message += tmp_char;
  }
  if (tmp_char == posString && morse.messageSent())
  {
    if (message.substring(0, 1) == "P")
    {
      morse.sendMessage(message.substring(1, message.length()));
    }
    else if (message == "W")
    {
      String pom = morse.getCurrentSpeedWpm();
      altSerial.print("W"+pom+posString);
    }
    else if (message.substring(0, 1) == "W")
    {
      morse.setSpeedWpm(message.substring(1, message.length()));
      String pom = morse.getCurrentSpeedWpm();
      altSerial.print("W"+pom+posString);
    }
    tmp_char = 'p';
    message = "";
  }

  if (Serial.available())
  {
    tmp_char2 = Serial.read();
    message2 += tmp_char2;
  }
  if (tmp_char2 == '*')
  {
    message2.setCharAt(message2.length() - 1, posString);
    altSerial.print(message2);
    message2 = "";
    tmp_char2 = 'p';
  }
}

