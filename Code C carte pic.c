//-----------------------------------------------------
//------------- Programme DrawBot ---------------
//-----------------------------------------------------
//-- Projet: DrawBot                                 --
//-- Date:   07/04/2020                              --
//-- Progr:                                          --
//-- Auteurs: Nutte Matthias & Corduant Valentin     --
//-----------------------------------------------------
//-- Programme de gestion PWM Pololu, contrôlé par   --
//--     un smartphone Androïd via bluetooth,        --
//-- représentation graphique sur différent support  --
//----------------------------------------------------- 

#include <16F887.h> // choix de la carte Pic
#device ADC=8
#include <string.h>
#include <stdlib.h>
#include <math.h>

#FUSES NOWDT, HS, NOPUT, NOPROTECT, NOBROWNOUT, NOLVP, NOCPD, NOWRT, NODEBUG // Fusibles permettant la bonne réalisation du projet
#use delay(clock=20M) // ??????????????????????????????
#use rs232(baud=115200,parity=N,xmit=PIN_C6,rcv=PIN_C7,bits=8,stream=bluetooth) // port utilisé pour la connexion bluetooth

//-------------------------E/S------------------------ 
// define permet une simplifiaction dans l'appel des pin et permet de facilement changer certaines valeurs sans modifier le code
#define pwm_out1_on output_high(PIN_C1);      // moteur 1 sens direct
#define pwm_out1_off output_low(PIN_C1);
#define pwm_out2_on output_high(PIN_D1);      // Moteur 1 sens inverse
#define pwm_out2_off output_low(PIN_D1);
#define pwm_out3_on output_high(PIN_C2);      // moteur 2 sens direct
#define pwm_out3_off output_low(PIN_C2);
#define pwm_out4_on output_high(PIN_D2);      // Moteur 2 sens inverse
#define pwm_out4_off output_low(PIN_D2);

#define pwm1 100                         // Nombre d'itération dans l'interruption (50 µsec x 100 = 5 msec = 200 Hz)
#define cycle_t0 0xFF36                     // réglage du timer pour 50 µsec exrimé en Hexadecimale
#define led pin_c5

#define BUFFER_SIZE 8                        // Buffer maxi de lecture RS232
#define bkbhit (next_in!=next_out)               // pointeur de remplissage


//----Déclaration des variables-----

byte pt,ptc,dc1,dc2,dc3,dc4;   //compteurs moteurs

//----Déclaration des fonctions-----

void pwm_isr();
void init_motors();
void initialisation();
void motors(signed byte speedd,signed byte speedg);
//-------------------x variable pour le bluetooth x------------------

byte buffer[BUFFER_SIZE];
byte next_in = 0;
byte next_out = 0;
byte cmd=0;
byte newvar=1;

//-----------------xinterruption pour remplir le bufferx-------------
#int_rda                                         // interruption serie
void serial_isr() {

   byte t;
   buffer[next_in]=getc();                     // remplissage du buffer
   t=next_in;
   next_in=(next_in+1) % BUFFER_SIZE;
   if(next_in==next_out){                  // Buffer full !!   
     next_in=t;     
   }      
   newvar=1;                           // signale l'arrivée d'une nouvelle donnée                      
}

//-------------------------Lectureblue------------------------
byte Lectureblue() {                              // lecture du buffer serie

   while(!bkbhit) ;                           
   cmd=buffer[next_out];                        
   next_out=(next_out+1) % BUFFER_SIZE;            // mise a jour du pointeur
   return(cmd);
}

//-------------------------xxx------------------------
#int_timer1 
void pwm_isr() {

    set_timer1(cycle_t0);                      // adjust timer0 pour 50 µsec
   if (pt == dc1) {                     // compteur = pointeur PWM Moteur
      pwm_out1_off;
   }
   if (pt == dc2) {
      pwm_out2_off;
   }
   if (pt == dc3) {
      pwm_out3_off;
   }
   if (pt == dc4) {
      pwm_out4_off;
   }
   if (pt == pwm1) {
     output_bit(PIN_C1,(dc1!=0));
     output_bit(PIN_D1,(dc2!=0));
     output_bit(PIN_C2,(dc3!=0));
     output_bit(PIN_D2,(dc4!=0));

      pt=255;                           // remise à zero du pointeur (instruc. suiv. = pt++)
      ptc++;                        
   }                             
   pt++;
}

//-------------------------xxx------------------------
void init_motors() {                     // init ports motors + all motors off

   set_tris_c(0xF9);                     // pin C1 & C2 output, other input
   set_tris_d(0xF9);                     // pin D1 & D2 output, other input
   dc1=dc2=dc3=dc4=0;
   pwm_out1_off;                        // All Motors Off
   pwm_out2_off;
   pwm_out3_off;
   pwm_out4_off;
}

void initialisation() {

   setup_adc_ports(sAN0|sAN1);
   setup_adc(ADC_CLOCK_INTERNAL);            // Config ADC
   setup_timer_1(T1_INTERNAL|T1_DIV_BY_1);    // set the timer 1 overflow at 13.1ms (resolution= 0.2µs)
   enable_interrupts(int_timer1);            // interrupt for timer 1 ON
   enable_interrupts(int_rda);
   enable_interrupts(global);               // enable interrupt
   set_timer1(cycle_t0);                  // set timer 1 to 50 µsec
   pt=0;
   ptc=210;
}


void motors(signed byte speedd,signed byte speedg) {

   if (speedg >= 0){               
       dc1=0;                           // direction moteur gauche = avant
       dc2=(speedg * 2);                  // reglage de la vitesse
   }
   else {
      dc2=0;                           // direction moteur gauche = arriere
      dc1= (~speedg+1)*2;                  // reglage de la vitesse absolue (complement a 2 pour enlever le signe moins)
   }
   if (speedd >= 0){
       dc3=0;                           // direction moteur droit = avant
       dc4=(speedd * 2);                  // reglage de la vitesse
   }
   else {
      dc4=0;                           // direction moteur droit = arriere
      dc3= (~speedd+1)*2;                  // reglage de la vitesse absolue (complement a 2 pour enlever le signe moins)
   }
}

unsigned int straight(byte size) {
   return 1000*size; //according to size : coef proportionnelle a la dimension du dessin
}

void rotate(byte angl)  {
   // compute motors speed according to angle
   motors( -8  , 8*2.35 ); 
   delay_ms(angl*1.74); // according to angle
   motors(0,0);                //stopper
}

//-------------------------xxx------------------------
void main() {
   printf("first");
   //delay_ms(1000);
   initialisation();                           // initialisation et configuration
   init_motors(); 
   while (true) {
		char c='#';
		
		if (input(pin_c5)) {                     // si bluetooth activé 
		printf("received 1\n");
     
		while(Lectureblue()!='#') ; 		//35 en Numerique Nr = decimal
		printf("%c",c);
		byte angl = Lectureblue();
		printf("%u",angl);
		rotate(angl);
		byte size = Lectureblue();  
		printf("%u",size);
		
		rotate(angl);
		motors( 8 , 8*2.35 );
		delay_ms(straight(size));
		motors( 0 , 0 );
	   }
	}
}