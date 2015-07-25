#include <SoftwareSerial.h>
#define bluetoothTx  2
#define bluetoothRx  3

typedef struct lampada{
  unsigned int pino, ligado=0;
  char id;
};

int blueReceived;
char comando;
SoftwareSerial bluetooth(bluetoothTx, bluetoothRx);

// alocando memoria para controle de 6 lampadas
lampada* lampadas = (lampada*)malloc(sizeof(lampada) * 6); 

void setup(){
  //Serial do PC
  Serial.begin(9600);
  //Serial Bluetooth
  bluetooth.begin(115200);

  for(int i=0; i<6; i++) {
    lampadas[i].ligado = 0;
    lampadas[i].pino = i+4; // eu começo a ligar a partir do pino 4
    lampadas[i].id = (char)97+i; // id alfabetica dos pinos: a=quartoSolteiro; b=quartoCasal; c=sala; d=cozinha; e=garagem; f=banheiro
    pinMode(lampadas[i].pino, OUTPUT);
  }
  
  delay(100);
}

void loop(){ 
  //lê do bluetooth e aplica os comandos
  if(bluetooth.available())  {
    blueReceived = bluetooth.read();
    if(blueReceived < 255){
      comando = (char)blueReceived;
      Serial.print(comando);

      if(comando == 'z'){
        retornaTodosStatus();
      } else {
        for(int i=0; i<6; i++) {
          if(lampadas[i].id == comando){
            lampadas[i].ligado = !lampadas[i].ligado;
            digitalWrite(lampadas[i].pino, lampadas[i].ligado);
            bluetooth.print((char)lampadas[i].id);
            bluetooth.println(lampadas[i].ligado);
            delay(500);
            break;
          }
        }
      }      
    }
  }
}

void retornaTodosStatus(){
  for(int i=0; i<6; i++) {
      bluetooth.print((char)lampadas[i].id);

      if(i == 5){
        bluetooth.println(lampadas[i].ligado);
      } else {
        bluetooth.print(lampadas[i].ligado);
        bluetooth.print(',');
      }
    }
    delay(500);
  }
 

