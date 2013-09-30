srcdir = src
outdir = bin

CFLAGS = -std=c99 -Wall -g -I/usr/local/include/SDL -I./tools
LIBS = -L/usr/local/lib -Wl,-rpath,/usr/local/lib -lSDL
TARGETS = $(outdir)/netgame

%.o: %.c
	cc -o $@ $^ $(CFLAGS)

netgame: $(srcdir)/main.c
	cc -o $(outdir)/$@ $? $(CFLAGS) $(LIBS)

clean:
	rm -f $(TARGETS) *.o
