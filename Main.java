import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;

/* ====================================================================
 * LOGIC/STATE CODE
 * ====================================================================*/

class State {
		public static final byte empty=0, player=1, rocket=2, solid=3;
		public static final byte left=0, up=1, right=2, down=3, walk=4, shoot=5;
		public static final byte explosion = pack(rocket,0,0);
		public byte s[];
		
		public State(byte[] state){ s=state; }
		
		public static byte dir (byte b) { return (byte)(b&0x03); }
		public static byte ty (byte b) { return (byte)((b>>2)&0x03); }
		public static byte arg (byte b) { return (byte)(b>>4); } 
		public static byte pack (int ty, int dir, int arg) { return (byte) ((dir&0x03) | (ty<<2) | (arg<<4)); }
		public static int idx (int x, int y) { return 10*(y%10) + x%10; }
		public static boolean opposite (int dir1, int dir2) { return (dir1+2==dir2 || dir2+2==dir1); }
		public static int offset(int i, int dir) { return offset(i%10, i/10, dir); }
		public static int offset(int x, int y, int dir) {
		if (dir==left) return idx(x-1+10,y);
		if (dir==right) return idx(x+1,y);
		if (dir==down) return idx(x,y+1);
		if (dir==up) return idx(x,y-1+10); 
		return idx(x,y); }

		void apply (byte act, byte loc) {
		byte b = s[loc];
		if (player != ty(b)) return;
		int nu = offset(loc, dir(b));
		int t = ty(s[nu]);
		if (walk == act) {
		if (solid == t || player == t) return;
		s[loc] = pack(empty,0,0);
		if (rocket == t) s[nu] = explosion;
		else s[nu] = b; }
		else if (shoot == act) {
		if (rocket == t || player == t) s[nu] = explosion;
		if (empty == t) s[nu] = pack(rocket,dir(b),8); }
		else // act is a direction, so we change the player facing.
		s[loc] = pack(player,act,arg(b)); }

		void clearExplosions () {
		for (int i=0; i<100; i++)
		if (rocket == ty(s[i]) && 0==arg(s[i])) s[i] = pack(empty,0,0); }

		void moveRockets() {
		// Figuire out the new locations for all rockets.
		byte[] R = new byte[100];
		for (int i=0; i<100; i++) {
		if (rocket != ty(s[i])) continue;
		int l = offset(i,dir(s[i]));
		if (0 == R[l]) R[l] = pack(rocket,dir(s[i]),arg(s[i])-1);
		else R[l] = explosion;
		if (rocket == ty(s[l]) && opposite(dir(s[i]), dir(s[l])))
		R[l] = R[i] = explosion; }
		// Copy in the non-rocket objects from the old state array.
		for (int i=0; i<100; i++) {
		if (solid == ty(s[i])) R[i] = s[i];
		else if (player == ty(s[i])) {
		if (rocket == ty(R[i])) R[i]  = explosion;
		else R[i]=s[i]; }}
		s=R; }

		public State update(byte[] actions, byte[] locations) {
		State n = new State(s);
		n.clearExplosions();
		n.moveRockets();
		for (int l=0; l<100; l++)
		for (int i=0; i<actions.length; i++) {
		if (locations[i] != l) continue;
		n.apply(actions[i], locations[i]); }
		return n; }

		public static State getTestState(){
		State s=new State(new byte[100]);
		s.s[0]=pack(player,up,0);
		s.s[2]=pack(player,down,1);
		s.s[15]=pack(rocket,up,10);
		s.s[45]=pack(rocket,down,10);
		s.s[77]=pack(solid,0,0);
		s.s[89]=pack(rocket,left,10);
		s.s[38]=pack(rocket,right,10);
		return s; }}

/* ====================================================================
 * MAIN CODE
 * ====================================================================*/
		
public class Main{
		static GUI gui;
		static Network net;
		public static State stateToDraw;
		public static boolean running=true;
		public static final Object stateUpdateLock=new Object();
		static long gameStartAt=0;
		static long lastFrameAt=0;
		static int frameCount=0;
		public static final long MILLISPERFRAME=500;
		public static Action nextAction=null;
		
		public static int playercount=0;
		public static HashMap<SocketAddress,Integer> playernumbers=new HashMap<>();
		public static ArrayList<PriorityQueue<Action>> hostActions=new ArrayList<>();
		public static ArrayList<Integer> maxTimestampsSent=new ArrayList<Integer>();
		
		public LinkedList<Action> actionsMemory=new LinkedList<Action>();
		
		public static boolean isServer=false;
		
		public static void main(String[] args){
		if(args.length!=2 && args.length !=1) failUsage();
		net=new Network();
		if(args.length==1) isServer=true;
		if(args.length==1) net.init();
		else if(args.length==2 && args[0].equals("client")) net.init();
		else failUsage();
		State s = new State(new byte[100]);
		gui=new GUI();
		gui.init();
		//debug:
		//Action.testActions();
		//end debug

		start();
		
		while(running){ //this is the game loop
		//net.recv();
		if(!isServer) updateClient();
		else updateServer();
		draw();
		if(isServer){
		try{
		Thread.sleep(MILLISPERFRAME/10);
		}catch(Exception e){}
		}
		}
		System.exit(0);
		}
    
    public static void failUsage(){
        System.out.println("Usage: \"java Main host\"");
        System.out.println("Usage: \"java Main client 192.168.0.3\" where 192.168.0.3 is the IPA of the host.");
        System.exit(0);
    }
    
    public static void start(){
		stateToDraw=State.getTestState();
		lastFrameAt=System.currentTimeMillis();
		gameStartAt=lastFrameAt;
    }

		public static void addPlayer(SocketAddress s){
		playernumbers.put(s,playercount++);
		hostActions.add(new PriorityQueue<Action>());
		maxTimestampsSent.add(-1);
		}

		private static int getMinFrame(){
		int minFrame=Integer.MAX_VALUE;
		for(Integer ts : maxTimestampsSent){
		minFrame=Math.min(minFrame,ts);
		}
		return minFrame;
		}

		public static void updateServer(){
		if(Main.playercount==0) return;
		int minFrame=getMinFrame();
		if(minFrame<frameCount) return;
		while(minFrame>=frameCount){
		LinkedList<Action> actions=new LinkedList<>();
		for(PriorityQueue<Action> pq: hostActions){
		if(pq.peek().timestamp==frameCount) actions.add(pq.poll());
		}
		Action[] actsArray=actions.toArray(new Action[0]);
		Arrays.sort(actsArray);
		byte[] acts=new byte[actsArray.length];
		byte[] locs=new byte[actsArray.length];
		for(int i=0;i<acts.length;i++){
		acts[i]=actsArray[i].act;
		locs[i]=actsArray[i].loc;
		}
		stateToDraw=stateToDraw.update(acts,locs);
		frameCount++;
		}
		//Network.transmitState();
		}

		public static void updateClient(){
		long time=System.currentTimeMillis();
		if(lastFrameAt==0){
		//game hasn't started yet.
		try{
		Thread.sleep(1000);
		}catch(Exception e){ }
		}else{
		if(time-lastFrameAt>=MILLISPERFRAME){
		while(time-lastFrameAt>=MILLISPERFRAME){
		synchronized(stateUpdateLock){
		if(nextAction==null){
		stateToDraw=stateToDraw.update(new byte[0],new byte[0]);
		}else{
		stateToDraw=stateToDraw.update(new byte[]{nextAction.act},new byte[]{nextAction.loc});
		actionsMemory.add(nextAction);
		}
		lastFrameAt+=MILLISPERFRAME;
		System.out.println("Frame "+(frameCount++)+" at "+(System.currentTimeMillis()-gameStartAt));
		//Network.transmitAction(nextAction);
		nextAction=null;
		}
		}
		}else{
		try{
		Thread.sleep(MILLISPERFRAME-(time-lastFrameAt));
		}catch(Exception e){ }
		}
		}
		}
		
		
		public static void handleStateBroadcast(State nu, int time){
		while(time<frameCount){
		Action nextAct=actionsMemory.peek();
		while(nextAct!=null && nextAct.timestamp<time){
		actionsMemory.poll();
		nextAct=actionsMemory.peek();
		}
		byte[] act=new byte[0];
		byte[] loc=new byte[0];
		if(nextAct!=null && nextAct.timestamp==time){
		act=new byte[]{nextAct.act}
		loc=new byte[]{nextAct.loc}
		actionsMemory.poll();
		}
		nu=nu.update(act,loc);
		time++;
		}
		stateToDraw=nu;
		}
		
		public static void handleAction(Action a, int time, SocketAddress src){
		
		}

		public static void draw(){
		gui.draw(stateToDraw);
		}
}

class Action implements Comparable<Action>{
    byte loc;
    byte act;
    int timestamp;
    int player;
    
    public Action(byte a, byte l, int t, int p){
        player=p;
        act=a;
        loc=l;
        timestamp=t;
    }
    
    public int compareTo(Action a){
        return timestamp-a.timestamp;
    }
    
    public boolean equals(Action a){
        return timestamp==a.timestamp;
    }
		public static void testActions(){
		PriorityQueue<Action> a=new PriorityQueue<Action>();
		for(int i=0;i<10;i++){
		a.add(new Action((byte)0,(byte)0,(int)(Math.random()*1000),0));
		}
		while(a.peek()!=null){
		System.out.println(a.poll().timestamp+", ");
		}
		}
}


/* ====================================================================
 * GUI CODE
 * ====================================================================*/

class GUI implements KeyListener{
    Frame frame;
    GameCanvas canvas;
    int whoami=0;
    
    public void init(){
        frame=new Frame("Project");
        canvas=new GameCanvas();
        frame.add(canvas);
        canvas.setPreferredSize(new Dimension(400,400));
        canvas.addKeyListener(this);
        frame.pack();
        frame.setVisible(true);
        canvas.createBufferStrategy(3);
    }
    
    public void draw(State state){
        canvas.setState(state);
        frame.repaint();
        Graphics2D g=(Graphics2D)canvas.getBufferStrategy().getDrawGraphics();
        canvas.draw(g);
        canvas.getBufferStrategy().show();
    }
    
    public void keyPressed(KeyEvent e){
        if(e.getKeyCode()==KeyEvent.VK_ESCAPE)
            Main.running=false;
        else if(e.getKeyCode()==KeyEvent.VK_W)
            registerMoveAction(1);
        else if(e.getKeyCode()==KeyEvent.VK_A)
            registerMoveAction(0);
        else if(e.getKeyCode()==KeyEvent.VK_S)
            registerMoveAction(3);
        else if(e.getKeyCode()==KeyEvent.VK_D)
            registerMoveAction(2);
        else if(e.getKeyCode()==KeyEvent.VK_SPACE)
            registerShotAction();
        else
            System.out.println("Don't understand this key.");
		}
    public void keyTyped(KeyEvent e){}
    public void keyReleased(KeyEvent e){ }

    public void registerShotAction(){
        synchronized(Main.stateUpdateLock){
		        State s=Main.stateToDraw;
		        byte loc=-1;
		        for(int i=0;i<100;i++){
		            if(s.ty(s.s[i])==s.player && s.arg(s.s[i])==whoami){
		                loc=(byte)i;
		                break;
		            }
		        }
		if(loc==-1) return; //we have no character - probably died to a rocket.
		commitAction(State.shoot,loc);
		}
		}

    public void registerMoveAction(int dir){
        synchronized(Main.stateUpdateLock){
		        State s=Main.stateToDraw;
		        byte loc=-1;
		        for(int i=0;i<100;i++){
		            if(s.ty(s.s[i])==s.player && s.arg(s.s[i])==whoami){
		                loc=(byte)i;
		                break;
		            }
		        }
		        if(loc==-1) return; //we have no character - probably died to a rocket.
		        if(s.dir(s.s[loc])==dir){
		commitAction(State.walk,loc);
		        }else{
		commitAction((byte)dir,loc);
		        }
        }
    }

    public void commitAction(byte act, byte loc){
        Main.nextAction=new Action(act,loc,Main.frameCount,whoami);
    }
    
		static class GameCanvas extends Canvas{
		public int whoami=0;

		
        State state;
        public void setState(State s){ state=s; }
        public void draw(Graphics2D g2){
            for(int i=0;i<100;i++){
                int y=(i/10)*40;
                int x=(i%10)*40;
                byte cell=state.s[i];
                byte type=State.ty(cell);
                Color c=null;
                if(type==State.empty) c=new Color(255,255,255);
                else if(type==State.solid) c=new Color(0,0,0);
                else if(type==State.rocket && State.arg(cell)==0) c=new Color(176,128,128);
                else if(type==State.rocket) c=new Color(255,0,0);
                else if(type==State.player && State.arg(cell)==whoami) c=new Color(64,255,64);
                else if(type==State.player) c=new Color(0,128,0);
                else c=new Color(112,66,0);
                g2.setPaint(c);
                g2.fillRect(x,y,40,40);
                if(type!=State.solid && type!=State.empty && !(type==State.rocket && State.arg(cell)==0)){
                    g2.setPaint(new Color(255,255,255));
                    if((cell&0x3)==0) g2.fillRect(x+4,y+16,8,8);
                    if((cell&0x3)==1) g2.fillRect(x+16,y+4,8,8);
                    if((cell&0x3)==2) g2.fillRect(x+28,y+16,8,8);
                    if((cell&0x3)==3) g2.fillRect(x+16,y+28,8,8);
                }
            }
            g2.setPaint(new Color(0,0,0));
            for(int i=0;i<9;i++){
                g2.drawLine(40+i*40,0,40+i*40,400);
                g2.drawLine(0,40+i*40,400,40+i*40);
            }
        }
    }
}

/*================================================================================
# Network Message Format
All parts of the message format are a byte unless otherwise specified.
Here are the possible messages:
 Sent by clients:
  HI =>> Try to connect to a the server.
  PLAY =>> Ask the server for a player to control.
  BYE =>> Disconnect from the server.
  DO timestamp(uint32) id action =>> Try to perform an action.
 Sent by servers:
  PLACE id pos =>> Gives the client a new player to control.
  STATE timestamp(uint32) board(byte[100]) =>> Announce a new state to the client.
  FULLBOARD =>> Refuse a new client because there isn't enough space on the map.
  FULLPLAYERS =>> Refuse a new client because all 16 ids are taken.
  END =>> Close the server session.
================================================================================*/
class Msg {
		public static final byte HI=0, PLAY=1, BYE=2, DO=3, PLACE=5, STATE=6, FULLBOARD=7, FULLPLAYERS=8, END=9;
		byte[] p(int i) { return new byte[i]; }
		public byte[] m (byte ty) {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream d = new DataOutputStream(b);
		d.writeByte(ty)
		return b.toByteArray(); }
		
		public byte[] m (byte ty, byte a, byte b) {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream d = new DataOutputStream(b);
		d.writeByte(ty);
		d.writeByte(a);
		d.writeByte(b);
		return b.toByteArray(); }
		
		public byte[] m (byte ty, uint32 i, byte b1, byte b2) {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream d = new DataOutputStream(b);
		d.writeByte(ty);
		d.writeUint32(i);
		d.writeByte(a);
		d.writeByte(b);
		return b.toByteArray(); }
		
		public byte[] m (byte ty, uint32 i, byte[] bs) {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream d = new DataOutputStream(b);
		d.writeByte(ty);
		d.writeUint32(i);
		for (int i=0; i<bs.length; bs++) d.writeByte(bs[i]);
		return b.toByteArray(); }
}
		
private static class Packet {
		public byte[] data;
		public SocketAddress source;
		public Packet(byte[] b, SocketAddress s){ data=b; source=s; }}

/*
Sends and recieves UDP messages.
Introduced artificial latency to message sends.
*/
class Network{
		public static final int PORT=45012;
		public static final int MAXPACKETSIZE=6000;
		public static final long ARTIFICIALLATENCY=0;

		public static Object networkLock = new Object();
		public static LinkedList<Packet> msgqueue = new LinkedList<>();
		public static DatagramSocket socket;

		public void init (){
		try {
		socket=new DatagramSocket(PORT);
		socket.setSoTimeout(1000);
		new Reciever().start();
		} catch(SocketException e){
		e.printStackTrace();
		}
		}

		public void send(Packet p){
		synchronized(networkLock){ new Sender(Arrays.copyOf(p.data,p.data.length),a).start(); }
		}

		public Message[] recv(){
		synchronized(networkLock){
		return msgqueue.toArray(new Message[0]);
		}
  }
		
		// Adds messages to `msgqueue' as they come off the network stack.
		private static class Reciever extends Thread{
		public void run(){
		DatagramPacket p = new DatagramPacket(new byte[MAXPACKETSIZE],MAXPACKETSIZE); 
		while(Main.running){
		try{
		Network.socket.receive(p);
		byte[] payload = Arrays.copyOf(p.getData(),p.getLength());
		SocketAddress sa = p.getSocketAddress();
		Network.msgqueue.add(new Message(payload,sa)); }
		catch(SocketTimeoutException e){ continue; }
		catch(Exception e){ e.printStackTrace(); break; }
		}
		}
		}

		// Handles packet sends and fake latency. Create one of these per message.
		private static class Sender extends Thread{
		byte[] data;
		SocketAddress dest;
		public Sender(byte[] data, SocketAddress destination){ this.data=data; dest=destination; }
		public void run(){
		if(Network.ARTIFICIALLATENCY!=0){
		try { sleep(Network.ARTIFICIALLATENCY); }
		catch(Exception e){ e.printStackTrace(); }
		}
		try{
		DatagramPacket p = new DatagramPacket(data,data.length,dest);
		synchronized(networkLock){ //don't send while another thread is sending.
		Network.socket.send(p);
		}
		}
		catch(Exception e){ e.printStackTrace(); }
		}
		}
}
