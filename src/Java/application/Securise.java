package application;

import java.io.IOException;
import java.net.BindException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import protocol.*;
import protocol.exceptions.*;
import application.core.*;

public class Securise extends Appl implements ReceveSend {

	String idSecurise;
	
	KeyPairGenerator kpg ;
	Cipher cipher;

	KeyPair keyPair;
	
	HashMap<String,PublicKey > listeCorrespondant = new HashMap<String,PublicKey>();
	
	public final static int byteSizeMess = 3;

	/**
	 * Application 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	public Securise(String ip,Integer udpPort, Integer tcpPort,Integer multiPort, boolean verbose) throws BindException, IOException, ParseException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		super(ip,"SECURISE", udpPort, tcpPort, multiPort,verbose);
		init();
		super.initThread(new MyRunnableReceve(this), new MyRunnableSend(this));
		
	}
	
	/**
	 * Service
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	public Securise(RingoSocket ringosocket) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException{
		super("SECURISE",ringosocket);
		init();
		super.initThread(new MyRunnableReceve(this), new MyRunnableSend(this));
		
	}

	public void init() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException{
		this.kpg=KeyPairGenerator.getInstance("RSA");
		this.cipher = Cipher.getInstance("RSA");
		this.kpg.initialize(2048);
		this.keyPair = kpg.generateKeyPair();
		this.idSecurise=testName();
		
	}
	
	private PublicKey reconstruct_public_key( byte[] pub_key) {
	    PublicKey public_key = null;

	    try {
	        KeyFactory kf = KeyFactory.getInstance("RSA");
	        EncodedKeySpec pub_key_spec = new X509EncodedKeySpec(pub_key);
	        public_key = kf.generatePublic(pub_key_spec);
	    } catch(NoSuchAlgorithmException e) {
	        System.out.println("Could not reconstruct the public key, the given algorithm oculd not be found.");
	    } catch(InvalidKeySpecException e) {
	        System.out.println("Could not reconstruct the public key");
	    }

	    return public_key;
	}
	
	
	public void doReceve(Message msg) throws RingoSocketCloseException {
		byte[] msgInByte =msg.getData_app();
		
		String debut =new String(msgInByte,0,8);
		
		
		if(debut.equals("PUB__KEY")){
			RecevePUBLICKEY(msgInByte);
		}
		
		else if(debut.equals(this.idSecurise)){
			int taille = Integer.parseInt(new String(msgInByte, 9, byteSizeMess));	
			byte []encrypted=Arrays.copyOfRange(msgInByte, 13,13+taille);
			byte[] decrypted = null;
			try {
				this.cipher.init(Cipher.DECRYPT_MODE, this.keyPair.getPrivate());
				decrypted = this.cipher.doFinal(encrypted);
			} catch (IllegalBlockSizeException | BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String message = new String(decrypted);
			if(super.modeService){
				synchronized (listOutput) {
					listOutput.addLast(msgInByte);
					listOutput.notify();
				}
			}else{
				printModeApplication(style + "\n" + LocalDateTime.now() + " -> " + "RECEVE : from "+debut+" : " + message + "\n" + style);
			}
			
		}
		
	
		super.ringoSocket.send(msg);// renvoi sur l'anneau du message
	}
	
	
	private void RecevePUBLICKEY(byte [] data){
		String name=new String(data,9,8);
		
		int tailleKey = Integer.parseInt(new String(data,18,3));
		printModeApplication(style + "\n" + LocalDateTime.now() + " -> " + "TAILLE DE CLEF RECU "+tailleKey+" DE "+name + style);
		
		byte [] pubkey = Arrays.copyOfRange(data, 22, 22+tailleKey);
		
		PublicKey key =reconstruct_public_key(pubkey);
		
		this.listeCorrespondant.put(name,key);
		
	}

	public void doSend(String input) throws NumberOfBytesException, RingoSocketCloseException, InterruptedException, ParseException, IOException {
	
		if(input.equals("SENDPUBLIC")){
			this.sendPublicKey();
			return;
		}
		
		if(input.length()>240){
			return;
		}
		byte[] encrypted = null;
		
		System.out.println("ENTER THE NAME OF RECEIVER");
		
		String nameRece=testName();
		
		PublicKey keyRece = this.listeCorrespondant.get(nameRece);
		if(keyRece==null){
			System.out.println("RECEIVER NOT IN DATA BASE , ASK HIM TO SEND IS PUBKEY");
			return;
		}
		
		try {
			this.cipher.init(Cipher.ENCRYPT_MODE,keyRece);
			encrypted=this.cipher.doFinal(input.getBytes());
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		byte[] entete =(nameRece+" " +Message.intToStringRepresentation(encrypted.length, 3)+" ").getBytes();
		
		byte[]contenu =new byte[300];
		Message.remplirData(contenu, entete,encrypted);
		
		Message msg=Message.APPL(ringoSocket.getUniqueIdm(), "SECURISE", contenu);
		
		printModeApplication(style + "\n" + LocalDateTime.now() + " -> " + "SEND : to "+nameRece+" : " + input + "\n" + style);
		
		super.ringoSocket.send(msg);
		
	}
	
	
	public void sendPublicKey() throws ParseException, RingoSocketCloseException, InterruptedException, IOException{
		
		
		byte[] publicKey=this.keyPair.getPublic().getEncoded();
		
		byte [] entete =("PUB__KEY "+this.idSecurise+" "+publicKey.length+" ").getBytes();
		
		
		byte[]contenu =new byte[450];
		Message.remplirData(contenu, entete,publicKey);
		
		Message msg=Message.APPL(ringoSocket.getUniqueIdm(), "SECURISE", contenu);
		
		printModeApplication(style + "\n" + LocalDateTime.now() + " -> " + "SEND MY PUBLIC KEY" + "\n" + style);
		
		super.ringoSocket.send(msg);
	}
	
	public  String testName(){
		System.out.println("NOW ENTER A NAME OF max 8 char : ");
		Scanner scanner= new Scanner(System.in);
		String name=scanner.nextLine();
		if(name.length()>8){
			name=name.substring(0,8);
		}
		else if(name.length()<8){
			StringBuilder builder = new StringBuilder(name);
			for (int i = 0; i < 8-name.length(); i++) {
			    builder.append("#");
			}
			name=builder.toString();
		}
		System.out.println("YOU SELECTED \""+name+"\"");
		System.out.println(Appl.style);
		return name;
	}

	public static void main(String[] args) {
		boolean verbose = Appl.testArgs(args);
		
		try {
			String ip = Appl.selectIp();
			
			System.out.println("## for sending your public key   type : SENDPUBLIC          ##");
			System.out.println(Appl.style);
			new Securise(ip,Integer.parseInt(args[0]), Integer.parseInt(args[1]),Integer.parseInt(args[2]) , verbose);
		} catch (BindException | ParseException e) {
			System.out.println("The ports are already in use or are bigger than 4digit");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}