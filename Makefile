srcdir = src
outdir = bin

CFLAGS = -Wall -g -I/usr/local/include/SDL -I./tools
LIBS = -L/usr/local/lib -Wl,-rpath,/usr/local/lib -lSDL
TARGETS = $(outdir)/netgame

netgame: $(srcdir)/main.c
	cc -o $(outdir)/$@ $? $(CFLAGS) $(LIBS)

clean:
	rm -f $(TARGETS)
