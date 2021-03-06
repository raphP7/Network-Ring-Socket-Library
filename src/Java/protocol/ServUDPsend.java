package protocol;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import protocol.ServMULTI.MultiChanel;
import protocol.exceptions.RingoSocketCloseException;

class ServUDPsend implements Runnable{
	private RingoSocket ringoSocket;
	private Message msg;
	private DatagramPacket paquet1;
	private DatagramPacket paquet2;
	private DatagramPacket paquetMulti;
	private byte[] dataTosend ;
	
	public void run() {
		boolean erreur = false;
		while (!erreur) {
			try {
				ringoSocket.testClose();
				sendMessage();
			} catch (InterruptedException | IOException | RingoSocketCloseException e) {
				erreur = true;
				ringoSocket.boolClose.set(true);;
			}
		}
		ringoSocket.sockSender.close();
		ringoSocket.printVerbose("END");
	}
	
	public ServUDPsend(RingoSocket ringoSocket) {
		this.ringoSocket = ringoSocket;
	}

	private void sendMulti(String ip_diff,int port_diff) throws IOException{
		InetSocketAddress ia=new InetSocketAddress(ip_diff,port_diff);
		
		paquetMulti = new DatagramPacket(dataTosend, dataTosend.length,ia);
		
		ringoSocket.printVerbose("Message Envoyer DIFF : "+ msg.toString());
		ringoSocket.sockSender.send(paquetMulti);
	}
	
	private void sendMessage() throws IOException, InterruptedException {

		synchronized (ringoSocket.listToSend) {
			while (ringoSocket.listToSend.isEmpty()) {
				ringoSocket.listToSend.notifyAll(); // pour le wait de closeServ
				ringoSocket.listToSend.wait();
			}
			this.msg = ringoSocket.listToSend.pop();
		}
		ringoSocket.UDP_MULTI_ipPort_Acces.acquire();
		this.dataTosend = msg.getData();
		if (msg.isMulti()) {
			for(MultiChanel mc : ringoSocket.servMulti.listMultiChannel){
				sendMulti(mc.entityinfo.ip_diff,mc.entityinfo.port_diff);
			}
			ringoSocket.UDP_MULTI_ipPort_Acces.release();
			return;
		} else {
			
			
			try{
				ringoSocket.printVerbose("Message Envoyer : "+ msg.toStringSHORT(90));
				
				if(msg.getType()==TypeMessage.WHOS){
					if(ringoSocket.ValTest!=null){
						if(ringoSocket.members!=null){
							ringoSocket.members.clear();
						}
						else{
							ringoSocket.members=new ConcurrentHashMap<InetSocketAddress, String>();
						}
						
					}
				}
				
				this.paquet1 = new DatagramPacket(dataTosend, dataTosend.length,InetAddress.getByName(ringoSocket.principal.ipUdp),ringoSocket.principal.portUdp);
				ringoSocket.sockSender.send(paquet1);
				
				if (ringoSocket.isDUPL.get()){
					this.paquet2 = new DatagramPacket(dataTosend, dataTosend.length,InetAddress.getByName(ringoSocket.secondaire.ipUdp),ringoSocket.secondaire.portUdp);
					ringoSocket.sockSender.send(paquet2);
				}
				
			}catch(IOException e){
				ringoSocket.UDP_MULTI_ipPort_Acces.release();
				throw e;
			}
			ringoSocket.UDP_MULTI_ipPort_Acces.release();
		}
		
		// Pour debloquer l'attente de changement de port
		if (this.msg.getType() == TypeMessage.EYBG) {
			ringoSocket.EYBG_Acces.release();
		}
	}
}