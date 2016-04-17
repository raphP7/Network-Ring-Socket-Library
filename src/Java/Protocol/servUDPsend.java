package Protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

class servUDPsend {
	private RingoSocket ringoSocket;
	Runnable runServUDPsend;
	
	public servUDPsend(RingoSocket ringoSocket) {
		this.ringoSocket = ringoSocket;
		this.runServUDPsend= new Runnable() {
			public void run() {
				boolean erreur = false;
				while (!erreur) {
					try {
						sendMessage();
					} catch (InterruptedException | IOException e) {
						erreur = true;
					}
				}
				ringoSocket.printVerbose("END thread SEND");
			}
		};
	}
	

	private void sendMessage() throws IOException, InterruptedException {

		Message msg;
		synchronized (ringoSocket.listToSend) {
			while (ringoSocket.listToSend.isEmpty()) {
				ringoSocket.listToSend.notifyAll(); // pour le wait de closeServ
				ringoSocket.listToSend.wait();
			}
			msg = ringoSocket.listToSend.pop();
		}

		byte[] dataTosend = msg.getData();

		if (msg.isMulti()) {
			DatagramPacket paquetMulti = new DatagramPacket(dataTosend, dataTosend.length,
					InetAddress.getByName(ringoSocket.ip_diff.toString()),ringoSocket.port_diff);
			ringoSocket.sockSender.send(paquetMulti);
		} else {

			if (ringoSocket.portUDP1 != null) {

				String ipTemp;
				synchronized(ringoSocket.ipPortUDP1){
					ipTemp=new String(ringoSocket.ipPortUDP1);
				}
				
				
				DatagramPacket paquet1 = new DatagramPacket(dataTosend, dataTosend.length,
						InetAddress.getByName(ipTemp), ringoSocket.portUDP1);

				ringoSocket.sockSender.send(paquet1);

			}
			if (ringoSocket.portUDP2 != null) {
				
				String ipTemp;
				synchronized(ringoSocket.ipPortUDP2){
					ipTemp=new String(ringoSocket.ipPortUDP2);
				}
				DatagramPacket paquet2 = new DatagramPacket(dataTosend, dataTosend.length,
						InetAddress.getByName(ipTemp), ringoSocket.portUDP2);

				ringoSocket.sockSender.send(paquet2);
			}
		}
		
		//Pour debloquer l'attente de changement de port
		if(msg.getType()==TypeMessage.EYBG){
			synchronized(ringoSocket.ipPortUDP1){
				ringoSocket.ipPortUDP1.notifyAll();
				ringoSocket.ipPortUDP1.wait();//attent le changement de port
			}
		}
		
		ringoSocket.printVerbose("Message Envoyer : " + msg.toString());

	}

	
	
}