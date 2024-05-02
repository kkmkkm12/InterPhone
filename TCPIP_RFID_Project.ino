#include <MFRC522v2.h>
#include <MFRC522DriverSPI.h>
#include <MFRC522DriverPinSimple.h>
#include <MFRC522Debug.h>

const uint8_t SERVO_PIN = 11U;
const uint8_t LIGHT_PIN = A1;
const uint8_t LIGHT_1 = 49U;
const uint8_t BUZZ_PIN = 48U;
enum RGBLED {
  RED = 8U,
  GREEN,  //자동 9번
  BLUE    //자동 10번
};

class MFRC522DriverPinSimple sda_pin(53);
class MFRC522DriverSPI driver {
  sda_pin
};
class MFRC522 mfrc522 {
  driver
};

bool card_touch = false;
bool transport = false;

bool led_auto = false;

bool light_auto_state = true;

uint8_t not_correct_count = 0;

void Led_Color(uint8_t r, uint8_t g, uint8_t b) {
  if (led_auto) {
    analogWrite(RED, r);
    analogWrite(GREEN, g);
    analogWrite(BLUE, b);
  }
}

void servo_open_close() {
  Led_Color(0, 100, 0);
  for (int i = 0; i <= 255; i += 2) {
    analogWrite(SERVO_PIN, i);
    delay(15UL);
  }
  delay(3000UL);
  for (int i = 255; i >= 0; i -= 2) {
    analogWrite(SERVO_PIN, i);
    delay(15UL);
  }
  Led_Color(0, 0, 0);
}

int light_1_auto() {
  uint16_t light_value{ analogRead(LIGHT_PIN) };
  if (light_auto_state && light_value >= 650) {
    digitalWrite(LIGHT_1, HIGH);
  } else
    digitalWrite(LIGHT_1, LOW);
  return light_value;
}

void siren() {
  if (not_correct_count < 5) {
    tone(BUZZ_PIN, 700);
    delay(100UL);
    digitalWrite(BUZZ_PIN, LOW);
    noTone(BUZZ_PIN);
    delay(100UL);
    tone(BUZZ_PIN, 700);
    delay(100UL);
    digitalWrite(BUZZ_PIN, LOW);
    noTone(BUZZ_PIN);
    delay(100UL);
  } else if (not_correct_count >= 5) {
    while (true) {
      const String in_siren_off = Serial.readStringUntil('\n');

      tone(BUZZ_PIN, 700);
      delay(400UL);
      digitalWrite(BUZZ_PIN, LOW);
      noTone(BUZZ_PIN);
      delay(50UL);

      if (in_siren_off.equals("Siren_Stop")) {
        not_correct_count = 0;
        break;
      }
    }
    Led_Color(0, 0, 0);
  }
}

void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200UL);
  pinMode(SERVO_PIN, OUTPUT);
  pinMode(LIGHT_PIN, INPUT);
  pinMode(LIGHT_1, OUTPUT);
  pinMode(BUZZ_PIN, OUTPUT);

  pinMode(RED, OUTPUT);
  pinMode(GREEN, OUTPUT);
  pinMode(BLUE, OUTPUT);

  mfrc522.PCD_Init();  //초기화
  MFRC522Debug::PCD_DumpVersionToSerial(mfrc522, Serial);
}

void loop() {
  // put your main code here, to run repeatedly:
  uint16_t light_value = light_1_auto();
  String sending_UID = "";
  if (Serial.available()) {
    const String in_comming_data = Serial.readStringUntil('\n');
    if (in_comming_data.equals("Open_the_door")) {
      
      //문을 열으라는 명령이 내려왔을 때 활동할 코드
      card_touch = false;
      transport = false;
      not_correct_count = 0;
      servo_open_close();
    } else if (in_comming_data.equals("Add_card")) {
      //카드 등록 되었을 때 활동할 코드
      card_touch = false;
      transport = false;
      Led_Color(0, 0, 100);
    } else if (in_comming_data.equals("Not Correct!")) {
      //카드가 일치하지 않을 때 활동할 코드
      not_correct_count++;

      String sending_data1 = "," + String(light_value) + "," + String(not_correct_count);
      Serial.println(sending_data1);

      card_touch = false;
      transport = false;
      Led_Color(100, 0, 0);
      siren();
    } else if (in_comming_data.equals("Auto_Led_On")) {  //자동으로 켜지고 꺼지는 명령어
      led_auto = true;
    } else if (in_comming_data.equals("Auto_Led_Off")) {  //자동으로 실행시키지 않는 명령어
      Led_Color(0, 0, 0);
      led_auto = false;
    } else if (in_comming_data.equals("Led_Off")) {  //3초, 몇초 후 led꺼지는 명령 할때 쓰는 명령어
      Led_Color(0, 0, 0);
    } else if (in_comming_data.equals("Auto_Light_On_Off")) {
      light_auto_state ^= true;
    }
  }

  if (!mfrc522.PICC_IsNewCardPresent()) {
    const String sending_data = sending_UID + "," + String(light_value) + "," + String(not_correct_count);
    Serial.println(sending_data);
    delay(200UL);
    return;
  }
  if (!mfrc522.PICC_ReadCardSerial()) return;


  if (!transport) {
    for (byte i = 0; i < mfrc522.uid.size; i++)
      sending_UID += String(mfrc522.uid.uidByte[i], HEX);

    const String sending_data = sending_UID + "," + String(light_value) + "," + String(not_correct_count);
    Serial.println(sending_data);
    transport = true;
  }
  delay(1000UL);
}