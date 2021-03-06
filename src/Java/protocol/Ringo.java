package protocol;
import protocol.exceptions.*;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;

/**
 * Cette interface defini les actions realisable par une applicationa avec cette implementation reseaux
 * 
 */
public interface Ringo extends Closeable{

	public final static int maxSizeMsg = 512;
	public final static int byteSizeType = 4;
	public final static int byteSizeId = 8;
	public final static int byteSizeIdm = 8;
	public final static int byteSizeIdApp = 8;
	public final static int byteSizeIP = 15;
	public final static int byteSizePort = 4;
	public final static int byteSizeTypeMSG = 4;
	public final static int byteSizeSpace = 1;
	
	public final static int maximumWaitTimeMessage=5000;
	
	/**
	 * Tester si l'entiter RINGO est fermer
	 * @return
	 */
	public boolean isClose();
	
	
	/**
	 * 	Demande un test de l'anneau
	 * @param sendDownIfBreak true -> si l'anneau est cassee alors averti sur multi diffusion | else -> pas d'alert
	 * @return true -> anneau pas casse
	 * @throws InterruptedException
	 * @throws RingoSocketCloseException
	 * @throws ParseException 
	 * @throws IOException 
	 */
	public boolean test(boolean sendDownIfBreak) throws InterruptedException, RingoSocketCloseException, ParseException, IOException;
	
	/**
	 * Demande l'envoi de WHOS
	 * @return
	 * @throws RingoSocketCloseException
	 * @throws InterruptedException
	 * @throws ParseException
	 * @throws IOException 
	 */
	public HashMap<InetSocketAddress,String> whos() throws RingoSocketCloseException, InterruptedException, ParseException, IOException;
	
	/**
	 * demande la deconnection de l'entiter ,elle boucle sur elle meme
	 * @throws InterruptedException
	 * @throws RingoSocketCloseException
	 * @throws ParseException 
	 * @throws IOException 
	 * @throws ImpossibleDisconnectDupl 
	 */
	public void disconnect() throws InterruptedException, RingoSocketCloseException, ParseException, IOException, ImpossibleDisconnectDupl;
	
	
	/**
	 * Permet de quitter un anneau
	 * @throws IOException 
	 * @throws  
	 */
	public void close() throws IOException;
	
	/**
	 * pour s'inserer dans un anneau
	 * @param adresse
	 * @param idTCP
	 * @param modeDUPL
	 * @throws ParseException
	 * @throws RingoSocketCloseException
	 * @throws ProtocolException
	 * @throws InterruptedException
	 * @throws AlreadyConnectException
	 * @throws ImpossibleDUPLConnection
	 * @throws IOException
	 * @throws UnknownTypeMesssage
	 */
	public void connect(String adresse, int idTCP,boolean modeDUPL)
			throws ParseException, RingoSocketCloseException, ProtocolException, 
			InterruptedException, AlreadyConnectException, ImpossibleDUPLConnection, IOException, UnknownTypeMesssage;

	
	public void connect(RingoSocket ringo,boolean modeDUPL) 
			throws ParseException, RingoSocketCloseException, ProtocolException, 
			InterruptedException, AlreadyConnectException, ImpossibleDUPLConnection, IOException, UnknownTypeMesssage;
	
	/**
	 * Pour demander l'envoi d'un message
	 * @param message
	 * @throws SizeException
	 */
	public void send(Message msg) throws RingoSocketCloseException, SizeMessageException;

	/**
	 * Demande un message en attente de lecture par l'apply
	 * attente passive
	 * @return Contenu du message
	 * @throws InterruptedException 
	 */
	public Message receive() throws RingoSocketCloseException, InterruptedException;
	
	/**
	 * Envoyer un down sur multidiffusion
	 * @throws RingoSocketCloseException
	 */
	public void down() throws RingoSocketCloseException;
	
	/**
	 * Get an unique IDM message of the RINGO entiter
	 * @return
	 * @throws RingoSocketCloseException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public long getUniqueIdm() throws RingoSocketCloseException, InterruptedException, IOException;
}
