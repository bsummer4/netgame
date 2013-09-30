#ifndef _net_h_
#define _net_h_

#include "SDL_net.h"

//used by the server 
typedef struct Client {

  unsigned char clientid;
  unsigned char playerid;

  UDPsocket sourcesocket;
} Client;

typedef struct Server {

  UDPsocket socket;
  IPaddress addr;
} Server;

typedef struct Packet {

  //
} Packet;
