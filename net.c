#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>

#include "net.h"
#include "dbg.h"

#define SERVER_SOCKET   2000
#define MAX_PACKET_SIZE 512

bool server;//whether this instance should act as server

Client **clients = malloc(sizeof(Client) * 16);
Server *server = malloc(sizeof(Server));

void 
net_init( char **argv )
{

  int c = SDLNet_Init();
  check( c < 0, "Couldn't initialize SDLNet: %s\n", SDLNet_GetError() );

  if (server){

    /* unsure if this is needed
    c = SDLNet_ResolveHost( &(server->addr), NULL, atoi(argv[2]));
    check( c == -1, "SDLNet_ResolveHost(%s %d): %s\n", argv[2], atoi(argv[3]), SDLNet_GetError());
    */

    UDPsocket sd = SDLNet_UDP_Open(SERVER_SOCKET);
    check( !sd, "SDLNet_UPD_Open: %s\n", SDLNet_GetError() );
   
    server->socket = sd;
  } else {

    c = SDLNet_ResolveHost( &(server->addr), argv[2], atoi(argv[3]));
    check( c == -1, "SDLNet_ResolveHost(%s %d): %s\n", argv[2], atoi(argv[3]), SDLNet_GetError());

    UDPsocket sd = SDLNet_UDP_Open(0);
    check( !sd, "SDLNet_UDP_Open: %s\n", SDLNet_GetError() );

    server->socket = sd
  }

error:
  quit(1);
}

//used instead of exit() to ensure cleanup
static void
quit(int rc)
{
  if(clients)
    free(clients);
  if(server)
    free(server);

  SDLNet_Quit();
  exit(rc);
}

//should be integrated into actual main
//expected argv; ./netgame server {socket}
//               ./netgame client {host IP} {socket}
int 
main (int argc, char **argv)
{

  if strcmp(argv[1], "server") {
    server = 1;
    check (argc < 3, "Not enough arguments for server to be established.");
  } else {
    server = 0;
    check( argc < 4, "Not enough arguments for client to connect.");
  }

  net_init( argv );
 
  UDPpacket pack = SDLNet_AllocPacket(MAX_PACKET_SIZE);
  check( !pack, "SDLNet_AllocPacket: %s\n", SDLNet_GetError() );

  //game loop
  while (1){
    if (server){
      if (SDLNet_UDP_Recv(server->socket, pack)){
        //packet is in pack

      }
      //send received packet to all clients
    } else {

      //put data in pack->data

      pack->address.host = server->addr.host;
      pack->address.port = server->addr.port;
      p->len = strlen((char*)p->data) + 1;
      SDLNet_UDP_Send(server->socket, -1, pack);

    }
  }
 
  SDLNet_FreePacket(pack);
  SDLNet_Quit();
  return 0;

error:
  quit(1);
  return 1;
}

