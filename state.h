typedef unsigned char byte;
typedef unsigned int msg;
typedef struct { byte board[100]; byte timeouts[100]; } State;
#define IDX(x,y) (10*((y)%10) + (x)%10)

#define MASK(n) ((1<<(n))-1)
#define PACK(NUM,SIZE,OFFSET) (((NUM)&(MASK(SIZE)))<<(OFFSET))
#define UNPACK(PACKED,SIZE,OFFSET) (((PACKED)&(MASK(SIZE)<<(OFFSET)))>>(OFFSET))

// Message Packing and Unpacking
enum act { Facenorth,  Facesouth, Faceeast, Facewest, Walk, Shoot }; // 3 bits
typedef enum act Act;
// A board index is 0-100 which is 7 bits
#define MSG(act,where,when) (PACK(act,3,0) | PACK(where,7,3) | PACK(when,21,10))
#define ACT(MSG)   UNPACK((MSG),3,0)
#define WHERE(MSG) UNPACK((MSG),7,3)
#define WHEN(MSG)  UNPACK((MSG),21,10)

// Tile Packing and Unpacking
typedef enum ty { Empty, Player, Rocket, Solid } Ty; // 2 bits
typedef enum dir { North, South, East, West } Dir; // 2 bits
#define TILE(TY,DIR,ARG) (PACK((TY),2,0) | PACK((DIR),2,2) | PACK((ARG),4,4))
#define TY(TILE)  (UNPACK((TILE),2,0))
#define DIR(TILE) (UNPACK((TILE),2,2))
#define ARG(TILE) (UNPACK((TILE),4,4))
#define EXPLOSION (TILE(Rocket,0,0))

void apply(State*,msg);
