#ifndef _net_h_
#define _net_h_

#include "SDL_net.h"

//used by the server 
struct Client {

  unsigned char clientid;
  unsigned char playerid;

  UDPsocket sourcesocket;
}

struct Server {

  UDPsocket socket;
  IPaddress addr;
}

struct Packet {

  //
}
