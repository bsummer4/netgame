#include "state.h"
#include <stdio.h>
#include <stdlib.h>
#include <err.h>

static int offsetXY(int x,int y,Dir dir) {
	switch (dir) {
		case West: return IDX(x-1+10,y);
		case East: return IDX(x+1,y);
		case South: return IDX(x,y+1);
		case North: return IDX(x,y-1+10); }}

static int offset(int idx, int dir) { return offsetXY(idx%10, idx/10, dir); }
static void turn(State *s, int loc, Dir d) {
			s->board[loc] = TILE(Player,d,ARG(s->board[loc])); }

void place(State *s, int i, byte b) { s->board[i]=b; s->timeouts[i]=50; }
static void walk(State *s, int old, int new) {
	int player = s->board[old];
	int target = s->board[new];
	int ty = TY(target);
	if (0 != s->timeouts[old]) { warnx("not ready to move"); return; }
	if (Solid == ty || Player == ty) return;
	place(s,old,TILE(Empty,0,0));
	place(s,new,((Rocket==ty) ? EXPLOSION : player)); }

static void shoot(State *s, int old, int new) {
	if (0 != s->timeouts[old]) return;
	switch (TY(s->board[old])) {
	case Rocket: case Player: place(s,new,EXPLOSION); break;
	case Empty: place(s,new,TILE(Rocket,DIR(s->board[old]),8)); break;
	case Solid: break; }}

void apply(State *s, msg m) {
	byte loc = WHERE(m);
	byte tile = s->board[loc];
	if (Player != TY(tile)) return;
	int newloc = offset(loc, DIR(tile));
	switch (ACT(m)) {
		case Walk: walk(s,loc,newloc); return;
		case Shoot: shoot(s,loc,newloc); return;
		default: turn(s,loc,(enum dir) ACT(m)); }}

void dump(char *s) { fputs(s,stdout); }
char *showrocket[] = {"↑","↓","→","←"};
char *showplayer[] = {"^","v",">","<"};
void showtile(int t) {
	switch(TY(t)) {
	case Rocket: dump(showrocket[DIR(t)]); break;
	case Empty: dump("·"); break;
	case Player: dump(showplayer[DIR(t)]); break;
	case Solid: dump("█"); break;
	default: break; }}

void line(int ontop) {
	dump(ontop?"┌":"└");
	for (int j=0; j<10; j++) dump("─");
	dump(ontop?"┐":"┘"); dump("\n"); }

void show(State *s) {
	line(1);
	for (int i=0; i<10; i++) {
		for (int j=0; j<10; j++) {
			if (j==0) dump("│");
			showtile(s->board[IDX(j,i)]); }
		dump("│\n"); }
	line(0); }

State s;
int main() {
	s.board[IDX(1,1)] = TILE(Player,North,3);
	s.board[IDX(1,0)] = TILE(Rocket,North,3);
	s.board[IDX(1,9)] = TILE(Rocket,North,3);
	s.board[IDX(3,3)] = TILE(Player,West,3);
	for (int i=0; i<10; i++)
		s.board[IDX(5,i)] = TILE(Solid,0,0);
	system("clear");
	show(&s);
	system("read");
	apply(&s,MSG(Walk,IDX(3,3),0));
	s.timeouts[IDX(2,3)] = 0;
	system("clear");
	show(&s);
	system("read");
	apply(&s,MSG(Walk,IDX(2,3),0));
	s.timeouts[IDX(1,3)] = 0;
	system("clear");
	show(&s);
	system("read");
	apply(&s,MSG(Facesouth,IDX(1,3),0));
	system("clear");
	show(&s);
	system("read");
	apply(&s,MSG(Walk,IDX(1,3),0));
	system("clear");
	show(&s); }
