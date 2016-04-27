package protocol;
import protocol.exceptions.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class RingoSocket implements Ringo {
	
	String idApp;
	
	/********************************************************************
	 * NETWORK
	 */
	
	String ip;
	Integer portTcp;
	String ip_diff;
	Integer port_diff;
	Integer portUDP1;
	String ipPortUDP1;
	Integer portUDP2;
	String ipPortUDP2;
	
	Integer listenPortUDP;
	MulticastSocket sockMultiRECEP;
	DatagramSocket sockRecever;
	DatagramSocket sockSender;
	ServerSocket sockServerTCP;

	/********************************************************************
	 * THREADS
	 */
	ServMULTI servMulti;
	private Thread ThRecev;
	private Thread ThMULTIrecev;
	private Thread ThServTCP;
	private Thread ThSend;
	
	/********************************************************************
	 * Verroux , set , boolean
	 */
	
	Semaphore tcpAcces;
	Semaphore EYBG_Acces;
	Semaphore UDP_ipPort_Acces;
	Semaphore idmAcces;
	Object EYBGisArrive=new Object();//mutex
	boolean EYBGisArriveBool;
	Object TESTisComeBack=new Object();//mutex
	boolean TESTisComeBackBool;
	long ValTEST;
	Boolean isDUPL;
	
	Set<Long> IdAlreadyReceveUDP1;// hashSet contenant les id deja croise
	LinkedList<Message> listForApply; // liste des message recu qui sont pour cette ID
	LinkedList<Message> listToSend;// liste des message a envoyer

	
	private byte [] idmStart;
	private Long idmActuel;
	private boolean boolClose;
	private boolean boolDisconnect;
	private boolean verboseMode;
	/*********************************************************************/


	/**
	 * Ferme tout les Thread de la RingoSocket
	 * @param modeDOWN if true envoi un DOWN en multi avant de fermer
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	void closeRingoSocket(boolean modeDOWN) throws IOException {

		this.boolClose = true;
		this.sockRecever.close();
		this.sockServerTCP.close();
		
		this.ThRecev.interrupt();
		this.ThServTCP.interrupt();

		if (modeDOWN) {
			this.sockSender.close();
			this.ThSend.interrupt();
		} else {
			synchronized (listToSend) {
				while (!listToSend.isEmpty()) {
					listToSend.notify();
					try {
						listToSend.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}	
				}
				this.sockSender.close();
				this.ThSend.interrupt();
			}
		}
		synchronized (listForApply) {
			this.listForApply.notify();
		}
		
		this.sockMultiRECEP.close();
		this.ThMULTIrecev.interrupt();
		
	}

	public void disconnect() throws InterruptedException, DOWNmessageException{
		testClose();
		Message msg = Message.GBYE(getUniqueIdm(), this.ip, this.listenPortUDP, this.ipPortUDP1, this.portUDP1);

		send(msg);
		
		synchronized (this.EYBGisArrive) {
			this.EYBGisArriveBool = false;
			printVerbose("WAITING for EYBG message");
			this.EYBGisArrive.wait(maximumWaitTimeMessage); // attend que EYBG soit arriver a l'entite
			printVerbose("EYBG comeback ? :"+EYBGisArriveBool);
			boolDisconnect=true;
			
			UDP_ipPort_Acces.acquire();
			this.ipPortUDP1=this.ip;
			this.portUDP1=this.listenPortUDP;
			UDP_ipPort_Acces.release();
			
			printVerbose("DISCONNECT DONE");
			return;
		}
	}
	
	public void close() throws IOException {
		if(isClose()){
			return;
		}
		if(boolDisconnect){
			//already disconnect of an other ringoSocket
			closeRingoSocket(false);
		}
		else{
			//not yet disconnect
			try {
				disconnect();
			} catch (InterruptedException | DOWNmessageException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			closeRingoSocket(false);
		}
	}

	/**
	 * Tester si la ringoSocket est fermer
	 * @throws DOWNmessageException si la ringoSocket est fermer
	 */
	void testClose() throws DOWNmessageException {
		if (boolClose) {
			throw new DOWNmessageException();
		}
	}

	public boolean isClose(){
		try{
			testClose();
		}catch(DOWNmessageException e){
			return true;
		}
		return false;
	}

	public boolean test(boolean sendDownIfBreak) throws InterruptedException, DOWNmessageException {
		testClose();
		
		long idm=getUniqueIdm();
		Message test = Message.TEST(idm, this.ip_diff, this.port_diff);
		
		send(test);
		
		synchronized (this.TESTisComeBack) {
			this.ValTEST = idm;
			this.TESTisComeBackBool= false;
			printVerbose("WAITING for TEST message");
			this.TESTisComeBack.wait(maximumWaitTimeMessage); // attend que EYBG soit arriver a l'entite
			if (!TESTisComeBackBool) {
				printVerbose("message TEST is NOT comeback");
				if (sendDownIfBreak) {
					down();
				}
				return false;
			}
			printVerbose("message TEST is comeback");
			return true;
		}
	}

	public void down() throws DOWNmessageException{
		send(Message.DOWN());
	}
	
	public void connectTo(String adresse, int idTCP,boolean modeDUPL)
			throws AlreadyAllUdpPortSet, ParseMessageException, DOWNmessageException, ProtocolException, InterruptedException, AlreadyConnectException, ImpossibleDUPLConnection, IOException ,UnknownTypeMesssage {
		testClose();
		
		if(!modeDUPL){
			if(!boolDisconnect){
				throw new AlreadyConnectException();
			}
		}else{
			if(this.isDUPL){
				throw new AlreadyAllUdpPortSet();
			}
		}
		tcpAcces.acquire();
		
		Socket socket;
		try {
			socket = new Socket(adresse, idTCP);
		} catch (UnknownHostException e) {
			tcpAcces.release();
			throw e;
		} catch (IOException e) {
			tcpAcces.release();
			throw e;
		}
		printVerbose("Conecter en TCP a :" + adresse + " sur port : " + idTCP);

		BufferedOutputStream buffOut = new BufferedOutputStream(socket.getOutputStream());
		BufferedInputStream buffIn = new BufferedInputStream(socket.getInputStream());
		byte[] tmp = new byte[Ringo.maxSizeMsg];
		Message msg1 = null;
		int sizeReturn;
		try {

			sizeReturn = buffIn.read(tmp);
			/*
			 * //TODO if (sizeReturn != 47) { socket.close(); throw new
			 * ProtocolException(); } tmp = Arrays.copyOfRange(tmp, 0, 46);
			 */
			
			try {
				msg1 = Message.parseMessage(tmp);
			} catch (ParseMessageException | UnknownTypeMesssage e1) {
				e1.printStackTrace();// TODO a retirer apres tests
				throw new ProtocolException();
			}
			if (msg1.getType() == TypeMessage.NOTC) {
				throw new ImpossibleDUPLConnection();
			}
			if (msg1.getType() != TypeMessage.WELC) {
				throw new ProtocolException();
			}

			printVerbose("TCP : message RECEVE : " + msg1.toString());
			Message msg2;
			if (modeDUPL) {
				msg2 = Message.DUPL(this.ip, this.listenPortUDP, this.ip_diff, this.port_diff);
			} else {
				msg2 = Message.NEWC(this.ip, this.listenPortUDP);
			}
			buffOut.write(msg2.getData());
			buffOut.flush();

			printVerbose("TCP : message SEND   : " + msg2.toString());

			/*
			 * //TODO if (sizeReturn != 6) { socket.close(); throw new
			 * ProtocolException(); } tmp = Arrays.copyOfRange(tmp, 0, 5);
			 */

		} catch (Exception e) {
			socket.close();
			buffIn.close();
			socket.close();
			this.tcpAcces.release();
			throw e;
		}
		
		this.UDP_ipPort_Acces.acquire();
		
		
		Message msg3 = null;
		try {
			sizeReturn = buffIn.read(tmp);
			msg3 = Message.parseMessage(tmp);
		} catch (UnknownTypeMesssage | ParseMessageException | IOException e) {
			this.tcpAcces.release();
			this.UDP_ipPort_Acces.release();
			buffOut.close();
			buffIn.close();
			socket.close();
			throw e;
		}
		
		printVerbose("TCP : message RECEVE : " + msg3.toString());
		
		boolean erreur=false;
		if(modeDUPL){
			if (msg3.getType() != TypeMessage.ACKD) {
				erreur=true;
			}
		}
		else{
			if (msg3.getType() != TypeMessage.ACKC) {
				erreur=true;
			}
		}
		if(erreur){
			
			buffOut.close();
			buffIn.close();
			socket.close();
			this.UDP_ipPort_Acces.release();
			this.tcpAcces.release();
			
			throw new ProtocolException();
		}

		if(modeDUPL){
			this.ipPortUDP2=msg1.getIp();
			this.portUDP2 = msg1.getPort();
			this.isDUPL=true;
		}
		else{
			this.ipPortUDP1 = msg1.getIp();
			this.portUDP1 = msg1.getPort();
			this.ip_diff = msg1.getIp_diff();
			this.port_diff = msg1.getPort_diff();
			this.servMulti.updateMulti();
			this.boolDisconnect=false;
		}
		
		buffOut.close();
		buffIn.close();
		socket.close();
		this.UDP_ipPort_Acces.release();
		this.tcpAcces.release();
		
		
	
	}

	
	public void send(Message msg) throws DOWNmessageException {
		testClose();
		if(msg==null){
			return;
		}
		IdAlreadyReceveUDP1.add(msg.getIdm());
		synchronized (listToSend) {
			this.listToSend.add(msg);
			this.listToSend.notify();
		}
	}

	public Message receive() throws DOWNmessageException, InterruptedException {
		testClose();
		synchronized (listForApply) {
			while (listForApply.isEmpty()) {
				testClose();// en cas de down durant l'attente
				listForApply.wait();
			}
			return listForApply.pop();
		}
	}

	
	public RingoSocket(String idApp, Integer listenUDPport, Integer portTcp,boolean modeService) throws IOException, IpException{
		this("localhost",idApp,listenUDPport,portTcp,modeService);
	}
	
	
	/**
	 * Creer une entite RINGO
	 * @param idApp le nom de l'application
	 * @param listenUDPport port d'ecoute UDP
	 * @param portTcp port TCP 
	 * @param modeService  
	 * @throws IOException
	 * @throws IpException 
	 */
	public RingoSocket(String ip,String idApp, Integer listenUDPport, Integer portTcp,boolean modeService) throws IOException, IpException
			 {
		if(idApp==null){
			this.idApp ="";
		}else{
			this.idApp = idApp;
		}
		
		/********************************************************************
		 * NETWORK
		 */
		
		this.ip = Message.convertIP(ip);		
		this.ip_diff =Message.convertIP("225.1.2.4");
		this.port_diff = 9999;
		this.portTcp = portTcp;
		this.listenPortUDP = listenUDPport;
		
		this.ipPortUDP1 = this.ip;
		this.ipPortUDP2 = null;
		this.portUDP1 = this.listenPortUDP;
		this.portUDP2 = null;
		
		this.sockServerTCP = new ServerSocket(portTcp);
		this.sockSender = new DatagramSocket();
		this.sockRecever = new DatagramSocket(listenUDPport);

		
		/********************************************************************
		 * boolean , semaphores , set , listes
		 */
		this.listToSend = new LinkedList<Message>();
		this.listForApply = new LinkedList<Message>();
		this.IdAlreadyReceveUDP1 = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
		this.EYBGisArriveBool= false;
		this.isDUPL=false;
		this.boolClose = false;
		this.boolDisconnect =true;
		this.tcpAcces=new Semaphore(1);
		this.idmAcces=new Semaphore(1);
		this.UDP_ipPort_Acces = new Semaphore(1);
		this.EYBG_Acces= new Semaphore(0);
		this.idmActuel=0L;
		
		this.build_IDM_array();
		
		
		
		/********************************************************************
		 * THREADS
		 */
		this.servMulti =new ServMULTI(this);
		this.ThRecev = new Thread(new ServUDPlisten(this).runServUDPlisten);
		this.ThSend = new Thread(new ServUDPsend(this).runServUDPsend);
		this.ThServTCP = new Thread(new ServTCP(this).runServTcp);
		this.ThMULTIrecev = new Thread(servMulti.runServMULTI);

		this.ThRecev.setName("Receve UDP");
		this.ThSend.setName("Send UDP  ");
		this.ThServTCP.setName("Server TCP");
		this.ThMULTIrecev.setName("MULTI-DIFF");

		if(modeService){
			this.ThRecev.setDaemon(true);
			this.ThSend.setDaemon(true);
			this.ThServTCP.setDaemon(true);
			this.ThMULTIrecev.setDaemon(true);
		}
		
		this.ThRecev.start();
		this.ThSend.start();
		this.ThServTCP.start();
		this.ThMULTIrecev.start();

		
	}
	
	public void setVerbose(boolean verbose){
		this.verboseMode=verbose;
	}
	
	/**
	 * Build the constant start of the IDM array
	 * 
	 * @throws UnknownHostException
	 */
	private void build_IDM_array() throws UnknownHostException{

		InetAddress ip = InetAddress.getByName(this.ip);
		byte[] ipBytes = ip.getAddress();
		
		byte[] portBytes = new byte[2];
		portBytes[0] = (byte)(this.portTcp & 0xFF);
		portBytes[1] = (byte)((this.portTcp >> 8) & 0xFF);
		
		this.idmStart= new byte[Ringo.byteSizeIdm];
		
		int cmp=0;
		for(int j=0;j<ipBytes.length;j++){
			this.idmStart[cmp]=ipBytes[j];
			cmp++;
		}
		for(int j=0; j<portBytes.length;j++){
			this.idmStart[cmp]=portBytes[j];
			cmp++;
		}
	}

	/**
	 * Affiche l'argument si en mode VERBOSE
	 * @param toPrint text a afficher
	 */
	void printVerbose(String toPrint) {
		if (verboseMode) {
			System.out.println(threadToString() + toPrint);
		}
	}
	/**
	 * Recuprer le nom du Thread actuel
	 * @return le String du nom du Thread actuel
	 */
	private String threadToString() {
		return "THREAD: " + Thread.currentThread().getName() + " | ";
	}
	
	public long getUniqueIdm() throws DOWNmessageException, InterruptedException{
		testClose();
		
		byte [] end_of_IDM= new byte[2];
		idmAcces.acquire();
		
		this.idmActuel=this.idmActuel%65000;//~limite de 2^255;
		end_of_IDM[0] = (byte)(this.idmActuel & 0xFF);
		end_of_IDM[1] = (byte)((this.idmActuel >> 8) & 0xFF);
		idmActuel++;
		idmAcces.release();
		byte[] val=Arrays.copyOf(this.idmStart, Ringo.byteSizeIdm);
		
		val[6]=end_of_IDM[0];
		val[7]=end_of_IDM[1];
		
		/*
		System.out.print("IDM :");
		for (byte b : val) {
		    System.out.print(b & 0xFF);
		    System.out.print(" ");
		}
		*/
		
		return Message.byteArrayToLong(val,Ringo.byteSizeIdm, ByteOrder.nativeOrder());
	}

	
}