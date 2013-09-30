typedef unsigned char byte;
typedef unsigned int msg;
typedef struct { byte board[100]; byte timeouts[100]; } State;
#define idx(x,y) (10*(y%10) + x%10)

#define MASK(n) ((1<<n)-1)
#define PACK(num,size,offset) ((num&(MASK(size)))<<offset)
#define UNPACK(packed,size,offset) ((packed&(MASK(size)<<offset))>>offset)

// Message Packing and Unpacking
enum act { facenorth,  facesouth, faceeast, facewest, walk, shoot }; // 3 bits
// A board index is 0-100 which is 7 bits
#define MSG(act,where,when) (PACK(act,2,0) | PACK(where,7,3) | PACK(when,21,10))
#define ACT(msg)   UNPACK(msg,3,0)
#define WHERE(msg) UNPACK(msg,7,3)
#define WHEN(msg)  UNPACK(msg,21,10)

// Tile Packing and Unpacking
enum ty { empty, player, rocket, solid }; // 2 bits
enum dir { north, south, east, west }; // 2 bits
#define TILE(ty,dir,arg) (PACK(ty,2,0) | PACK(dir,2,2) | PACK(arg,4,4))
#define TY(tile)  UNPACK(tile,2,0)
#define DIR(tile) UNPACK(tile,2,2)
#define ARG(tile) UNPACK(tile,4,4)
#define EXPOSION (TILE(rocket,0,0))

State apply(State,msg);
