#include "state.h"
#include <stdio.h>

State apply(State s, msg m) { return s; }
int main (int argc, char **argv) {
	byte a[4] = {TILE(3,3,15), TILE(0,0,0), TILE(1,2,3), TILE(3,0,1)};
	for (int i=0; i<4; i++) {
		printf("%d %d %d\n", TY(a[i]), DIR(a[i]), ACT(a[i])); }}
