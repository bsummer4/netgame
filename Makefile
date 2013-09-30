srcdir = .
outdir = .

CFLAGS = -Wall -g -I/usr/local/include/SDL -I./tools
LIBS = -L/usr/local/lib -Wl,-rpath,/usr/local/lib -lSDL_Net
TARGETS = $(outdir)/netgame

netgame: $(srcdir)/net.c
	cc -o $(outdir)/$@ $? $(CFLAGS) $(LIBS)

clean:
	rm -f $(TARGETS)
