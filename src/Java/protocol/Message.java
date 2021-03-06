package protocol;
import protocol.exceptions.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Stock et Parse les informations d'un message
 */
public class Message {
	
	private boolean multi;
	private byte[] data;
	
	private TypeMessage type;
	
	private String id;
	private String ip;
	private String ip_diff;
	private String ip_succ;
	
	private Integer port;
	private String portString;
	
	private Integer port_diff;
	private String port_diffString;
	
	private Integer port_succ;
	private String port_succString;
	
	private long idm;
	private byte [] idmLITTLE_ENDIAN_8;
	
	private String id_app;
	private byte[] data_app;
	
	private final static Integer sizeIp = Ringo.byteSizeIP;
	private final static Integer sizePort = Ringo.byteSizePort;
	private final static Integer sizeTypeMSG = Ringo.byteSizeTypeMSG;
	
	private final static int FLAG_IP_DIFF = 1;
	private final static int FLAG_IP_NORMAL = 2;
	private final static int FLAG_IP_SUCC = 3;

	
	private final static ByteOrder byteORDER_IDM=ByteOrder.LITTLE_ENDIAN;
	
	/**
	 * Create a new Message and Parse it from data
	 * @param data le contenu du message a parser
	 * @throws UnknownTypeMesssage if the type Message is unknow
	 * @throws ParseException if the data do no correcpond to the Type Message
	 * @throws IpException 
	 */
	public static Message parseMessage(byte [] data) throws ParseException, UnknownTypeMesssage {
		return new Message(data);
	}
	
	private Message(byte[] data) throws ParseException, UnknownTypeMesssage {
		super();
		this.data = data;
		try {
			this.parse();
		} catch (IndexOutOfBoundsException e) {
			throw new ParseException();
		}
		this.convertALL();
	}
	
	/**
	 * Constructeur pour les Appels statique , pattern FACTORIE
	 * @param data
	 * @param type
	 */
	private Message(byte[] data, TypeMessage type) {
		super();
		this.setMulti(false);
		this.data = data;
		this.type = type;
	}
	/**
	 * Convertir les chiffres dans la representation attendu par RINGO
	 * @param msg
	 * @throws ParseException 
	 * @throws IpException 
	 */
	private void convertALL() throws ParseException{
		if(this.ip!=null){
			this.ip=convertIP(this.ip);
		}
		if(this.port!=null){
			this.portString=convertPort(this.port);
		}
		if(this.port_diff!=null){
			this.port_diffString=convertPort(this.port_diff);
		}
		if(this.port_succ!=null){
			this.port_succString=convertPort(this.port_succ);
		}
		if(this.ip_diff!=null){
			this.ip_diff=convertIP(this.ip_diff);
		}
		if(this.ip_succ!=null){
			this.ip_succ=convertIP(this.ip_succ);
		}
		if(this.idm!=-1){
			this.idmLITTLE_ENDIAN_8=Message.longToByteArray(this.idm,8,byteORDER_IDM);
		}
	}
	
	/**
	 * Retourne This.data entre N et SIZE-N
	 * 
	 * @param n debut 
	 * @param size taille demander
	 * @return le string de this.data de N jusqu'a Size-N
	 */
	private String getDataFrom_N(int n, int size) {
		try{
			String tmp = new String(this.data, n, size);
			return tmp;
		}catch(StringIndexOutOfBoundsException e){
			return "";
		}
	}
	
	
	private byte[] getDataFrom_N_byte(int n, int size) {
		try{
			byte [] tmp =new byte [size];
			for(int i=0; i<size ; i++){
				tmp[i]=this.data[n+i];
			}
			return tmp;
		}catch(StringIndexOutOfBoundsException e){
			return null;
		}
	}
	
	
	/**
	 * Parcer le contenu d'un nouveau message
	 * 
	 * @throws UnknownTypeMesssage
	 * @throws IndexOutOfBoundsException
	 * @throws ParseException
	 */
	private void parse() throws IndexOutOfBoundsException,UnknownTypeMesssage, ParseException{
		int curseur=0;
		int sizeIp_SPACE_PORT=sizeIp+1+sizePort;
		String strParsed=getDataFrom_N(curseur,sizeTypeMSG);
		
		curseur+=sizeTypeMSG;
		//System.out.println("type reconnu : "+strParsed);
		
		
		try{
			this.type=TypeMessage.valueOf(strParsed);	
		}catch(IllegalArgumentException e){
			throw new UnknownTypeMesssage();
		}
		
		if(type==TypeMessage.DOWN){
			//parseTestEnd(curseur);
			return;
		}
		if(type==TypeMessage.ACKC || type==TypeMessage.ACKD || type==TypeMessage.NOTC){
			if(type==TypeMessage.ACKD){
				parseTestSpace(curseur);
				curseur++;
				strParsed=getDataFrom_N(curseur, sizePort);
				parseTestPort(strParsed);
				this.port=Integer.parseInt(strParsed);
				curseur+=sizePort;
			}
			strParsed=getDataFrom_N(curseur,1);
			parseBackslash_N(strParsed);
			curseur++;
			//parseTestEnd(curseur+1);
			return;
		}
		
		parseTestSpace(curseur);
		curseur++;
		
		if(type==TypeMessage.NEWC || type==TypeMessage.WELC || type==TypeMessage.DUPL){
			
			parse_IP_SPACE_Port(curseur,FLAG_IP_NORMAL);
			
			curseur+=sizeIp_SPACE_PORT;
			if(type==TypeMessage.NEWC){
				strParsed=getDataFrom_N(curseur,1);
				parseBackslash_N(strParsed);
				curseur++;
				//parseTestEnd(curseur);
				return;
			}
			parseTestSpace(curseur);
			curseur++;
			parse_IP_SPACE_Port(curseur,FLAG_IP_DIFF);
			curseur+=sizeIp_SPACE_PORT;
			strParsed=getDataFrom_N(curseur,1);
			parseBackslash_N(strParsed);
			curseur++;
			//parseTestEnd(curseur);
			return;
		}
		
		byte[] valIdm=getDataFrom_N_byte(curseur,Ringo.byteSizeIdm);
		this.idm=byteArrayToLong(valIdm,Ringo.byteSizeIdm,byteORDER_IDM);
		
		curseur+=Ringo.byteSizeIdm;
		if(type==TypeMessage.WHOS || type==TypeMessage.EYBG){
			//parseTestEnd(curseur);
			return;
		}
		parseTestSpace(curseur);
		curseur++;
		
		
		if(type==TypeMessage.TEST){
			parse_IP_SPACE_Port(curseur,FLAG_IP_DIFF);
			curseur+=sizeIp_SPACE_PORT;
			//parseTestEnd(curseur);
			return;
		}
		
		if(type==TypeMessage.MEMB){
			strParsed=getDataFrom_N(curseur,Ringo.byteSizeId);
			this.id=strParsed;
			curseur+=Ringo.byteSizeId;
			parseTestSpace(curseur);
			curseur++;
			parse_IP_SPACE_Port(curseur,FLAG_IP_NORMAL);
			curseur+=sizeIp_SPACE_PORT;
			return;
		}
		
		if(type==TypeMessage.APPL){
			strParsed=getDataFrom_N(curseur,Ringo.byteSizeIdApp);
			this.id_app=strParsed;
			curseur+=Ringo.byteSizeIdApp;
			parseTestSpace(curseur);
			curseur++;
			this.data_app= Arrays.copyOfRange(this.data, curseur, data.length);
			return;
		}
		
		parse_IP_SPACE_Port(curseur,FLAG_IP_NORMAL);
		curseur+=sizeIp_SPACE_PORT;
		parseTestSpace(curseur);
		curseur++;
		
		if(type==TypeMessage.GBYE){
			parse_IP_SPACE_Port(curseur,FLAG_IP_SUCC);
			curseur+=sizeIp_SPACE_PORT;
			//parseTestEnd(curseur);
			return;
		}	
	}
	/**
	 * Permet de parser une adrese IP puis un espace puis un port
	 * @param start position de debut
	 * @param FLAG_IP = IP_NORMAL || IP_DIFF || IP_SUCC
	 * @throws ParseException
	 */
	public void parse_IP_SPACE_Port(int start,int FLAG_IP) throws ParseException{
		
		String strParsed;
		int curseur= start+Ringo.byteSizeIP;
		
		strParsed=getDataFrom_N(start,Ringo.byteSizeIP);
		parseTestIp(strParsed);
		if(FLAG_IP==FLAG_IP_DIFF){
			this.ip_diff=strParsed;
		}else if(FLAG_IP==FLAG_IP_NORMAL){
			this.ip=strParsed;
		}
		else{
			this.ip_succ=strParsed;
		}
		parseTestSpace(curseur);
		strParsed=getDataFrom_N(curseur+1,Ringo.byteSizePort);
		parseTestPort(strParsed);
		
		int valPort=Integer.parseInt(strParsed);
		if(FLAG_IP==FLAG_IP_DIFF){
			this.port_diff=valPort;
		}else if(FLAG_IP==FLAG_IP_NORMAL){
			this.port=valPort;
		}
		else{
			this.port_succ=valPort;
		}
	}
	
	/**
	 * Pour parse
	 * test si le message est fini
	 * @param end
	 * @throws ParseException souleve une erreur si message pas fini
	 */
	private void parseTestEnd(int end) throws ParseException{	
		if(this.data.length!=end){
				throw new ParseException();
			}
	}
	/**
	 * Pour parse
	 * test si le caractere start est un caractere d'espace
	 * @param start
	 * @throws ParseException souleve une erreur si ce n'est pas un espace
	 */
	private void parseTestSpace(int start) throws ParseException{
		if(! (new String(this.data,start,1).equals(" "))){
			throw new ParseException();
		}
	}
	private void parseBackslash_N(String strParsed) throws ParseException{
		if(!strParsed.equals("\n")){
			throw new ParseException();
		}
	}
	
	/**
	 * Pour parse
	 * test si le parametre est un numero de port conventionel
	 * @param portTest
	 * @throws ParseException
	 */
	public static void parseTestPort(String portTest)throws ParseException{
		if(portTest.length()!=4){
			throw new ParseException();
		}
		try{
			int tmp=Integer.parseInt(portTest.substring(0,4));
			if(tmp<0 || tmp>9999){
				throw new ParseException();
			}
		}catch(NumberFormatException e){
			throw new ParseException();
		}
		
		
	}
	
	/**
	 * pour parse
	 * test si le parametre est un numero d'adresse Ip conventionnel
	 * @param ipTest
	 * @throws ParseException
	 */
	private void parseTestIp(String ipTest) throws ParseException{
		if(ipTest.length()!=15){
			throw new ParseException();
		}
		int tmp;
		for(int i=0;i<15;i++){
			if(i==3 || i==7 || i==11){
				if(ipTest.charAt(i)!='.'){
					throw new ParseException();
				}
			}
			else{
				try{
					tmp=Integer.parseInt(ipTest.substring(i, i+3));
					 if(tmp<0 || tmp>255){
						 throw new ParseException();
					 }
					 i=i+2;
				}catch(NumberFormatException e){
					throw new ParseException();
				}
			}
		}
	}
	
	/**
	 * Afficher un message
	 */
	public String toString(){
		
		String str =this.type.toString();
		
		if(type==TypeMessage.DOWN){
			return str;
		}
		if(type==TypeMessage.ACKC || type==TypeMessage.ACKD || type==TypeMessage.NOTC){
			if(type==TypeMessage.ACKD){
				str+=" "+this.portString;
			}
			return str+"\\n";
		}
		
		if(type==TypeMessage.NEWC || type==TypeMessage.WELC ||type==TypeMessage.DUPL){
			str+=" "+this.ip+" "+this.portString;
			if(type==TypeMessage.NEWC){
				return str+"\\n";
			}
			return str+" "+this.ip_diff+" "+this.port_diffString+"\\n";
			
		}	
		try {
			str=str+" "+Message.byteArrayToLong(this.idmLITTLE_ENDIAN_8, Ringo.byteSizeIdm,byteORDER_IDM);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
		
		if(type==TypeMessage.WHOS || type==TypeMessage.EYBG){
			return str;
		}
		
		if(type==TypeMessage.MEMB){
			return str+" "+this.id+" "+this.ip+" "+this.portString;
		}
		
		if(type==TypeMessage.TEST){
			return str+" "+this.ip_diff +" "+this.port_diffString;
		}
		if(type==TypeMessage.APPL){
			return str+" "+this.id_app+" "+new String(this.data_app);
		}
		str=str+" "+this.ip+" "+this.portString+" ";
		if(type==TypeMessage.GBYE){
			return str+this.ip_succ+" "+this.port_succString;
		}
		//TODO POURT TESTS
		else{
			return new String(this.data);
		}
	}
	
	public String toStringSHORT(int sizeMax){
		String tmp=this.toString();
		if(tmp.length()>sizeMax){
			return tmp.substring(0,sizeMax)+"...";
		}
		return tmp;
	}
	
	static Message WELC(String ip, int listenPortUDP, String ip_diff ,int port_diff) throws ParseException {
		
		byte[] WELC = new byte[4+1+sizeIp+1+sizePort+1+sizeIp+1+sizePort+1];
		Message tmp=new Message(WELC,TypeMessage.WELC);
		
		tmp.ip=ip;
		tmp.port=listenPortUDP;
		tmp.ip_diff=ip_diff;
		tmp.port_diff=port_diff;
		tmp.convertALL();;
		remplirData(WELC,"WELC ".getBytes(),tmp.ip.getBytes(),(" "+tmp.portString+" ").getBytes(),
				tmp.ip_diff.getBytes(),(" "+tmp.port_diffString+"\n").getBytes());
		return tmp;

	}
	
	
	static Message NEWC(String ip ,int portUDP1)throws ParseException {
		byte[] NEWC = new byte[4+1+sizeIp+1+sizePort+1];
		Message tmp=new Message(NEWC,TypeMessage.NEWC);
		tmp.ip=ip;
		tmp.port=portUDP1;
		tmp.convertALL();;
		remplirData(NEWC,"NEWC ".getBytes(),tmp.ip.getBytes(),(" "+tmp.portString+"\n").getBytes());
		return tmp;
	}
	
	static Message MEMB(long idm,String id,String ip ,int portUDP1)throws ParseException {
		byte[] MEMB = new byte[4+1+Ringo.byteSizeIdm+1+Ringo.byteSizeId+1+sizeIp+1+sizePort];
		Message tmp=new Message(MEMB,TypeMessage.MEMB);
		tmp.idm=idm;
		tmp.id=id;
		tmp.ip=ip;
		tmp.port=portUDP1;
		tmp.convertALL();;
		remplirData(MEMB,"MEMB ".getBytes(),tmp.idmLITTLE_ENDIAN_8,(" "+tmp.id).getBytes(),(" "+tmp.ip+" ").getBytes(),tmp.portString.getBytes());
		return tmp;
	}
	
	static Message GBYE(long idm, String ip, int listenPortUDP, String ip_succ, int port_succ)throws ParseException {
		byte[] GBYE = new byte[4+1+Ringo.byteSizeIdm+1+sizeIp+1+sizePort+1+sizeIp+1+sizePort];
		Message tmp=new Message(GBYE,TypeMessage.GBYE);
		tmp.idm=idm;
		tmp.ip=ip;
		tmp.port=listenPortUDP;
		tmp.ip_succ=ip_succ;
		tmp.port_succ=port_succ;
		
		tmp.convertALL();;
		remplirData(GBYE,"GBYE ".getBytes(),tmp.idmLITTLE_ENDIAN_8,
				(" "+tmp.ip+" "+tmp.portString+" "+tmp.ip_succ+" "+tmp.port_succString).getBytes());
		return tmp;
	}
	
	static Message DUPL(String ip, int listenPortUDP, String ip_diff ,int port_diff) throws ParseException{
		byte[] DUPL = new byte[4+1+sizeIp+1+sizePort+1+sizeIp+1+sizePort+1];
		
		Message tmp=new Message(DUPL,TypeMessage.DUPL);
		tmp.ip=ip;
		tmp.port=listenPortUDP;
		tmp.ip_diff=ip_diff;
		tmp.port_diff=port_diff;
		tmp.convertALL();;
		remplirData(DUPL,"DUPL ".getBytes(),
				(tmp.ip+" "+tmp.portString+" "+tmp.ip_diff+" "+tmp.port_diffString+"\n").getBytes());
		return tmp;
	}
	static Message EYBG(long idm)throws ParseException {
		byte[] EYBG = new byte[4+1+Ringo.byteSizeIdm];
		Message tmp=new Message(EYBG,TypeMessage.EYBG);
		tmp.idm=idm;
		tmp.convertALL();;
		remplirData(EYBG,"EYBG ".getBytes(),tmp.idmLITTLE_ENDIAN_8);
		return tmp;
	}
	static Message WHOS(long idm) throws ParseException{
		byte[] WHOS = new byte[4+1+Ringo.byteSizeIdm];//[5+8]=13

		Message tmp=new Message(WHOS,TypeMessage.WHOS);
		tmp.idm=idm;
		tmp.convertALL();;
		remplirData(WHOS,"WHOS ".getBytes(),tmp.idmLITTLE_ENDIAN_8);
		return tmp;
	}
	
	public static Message APPL(long idm , String id_app, byte[] data_app)throws ParseException {
		byte[] APPL = new byte[4+1+Ringo.byteSizeIdm+1+8+1+data_app.length];
		Message tmp=new Message(APPL,TypeMessage.APPL);
		tmp.idm=idm;
		tmp.id_app=id_app;
		tmp.data_app=data_app;
		tmp.convertALL();;
		remplirData(APPL,"APPL ".getBytes(),tmp.idmLITTLE_ENDIAN_8,(" "+tmp.id_app+" ").getBytes(),tmp.data_app);
		return tmp;
	}
	
	static Message TEST(long idm, String ip_diff ,int port_diff)throws ParseException {
		byte[] TEST = new byte[4+1+Ringo.byteSizeIdm+1+sizeIp+1+sizePort];
		
		Message tmp=new Message(TEST,TypeMessage.TEST);
		tmp.idm=idm;
		tmp.ip_diff=ip_diff;
		tmp.port_diff=port_diff;
		tmp.convertALL();;
		remplirData(TEST,"TEST ".getBytes(),tmp.idmLITTLE_ENDIAN_8,
				(" "+tmp.ip_diff+" ").getBytes(),tmp.port_diffString.getBytes());
		return tmp;
	}
	
	static Message ACKD(int port)throws ParseException {
		byte[] ACKD = new byte[4+1+sizePort+1];
		Message tmp = new Message(ACKD, TypeMessage.ACKD);
		tmp.port=port;
		tmp.convertALL();
		remplirData(ACKD,"ACKD ".getBytes(),(tmp.portString+"\n").getBytes());
		return tmp;
	}

	static Message ACKC() {
		byte[] ACKC = new String("ACKC\n").getBytes();
		Message tmp = new Message(ACKC, TypeMessage.ACKC);
		return tmp;
	}
	
	static Message DOWN() {
		byte[] DOWN = new String("DOWN").getBytes();
		Message tmp = new Message(DOWN, TypeMessage.DOWN);
		tmp.multi = true;
		return tmp;
	}

	static Message NOTC() {
		byte[] NOTC = new String("NOTC\n").getBytes();
		Message tmp = new Message(NOTC, TypeMessage.NOTC);
		return tmp;
	}
	
	/**
	 * Rempli data avec les args
	 * @param args
	 */
	public static void remplirData(byte [] data ,byte[]... args) {
		int i = 0;
		for (byte[] arg1 : args) {
			for (byte arg2 : arg1) {
				data[i] = arg2;
				i++;
			}
		}
	}
	
	/**
	 * 
	 * Cree un String de la valeur 562 sur 6 -> 000562
	 * 
	 * @param value
	 * @param numberOfBytes
	 * @return 
	 * @throws Exception
	 */
	public static String intToStringRepresentation(int value,int numberOfBytes) throws NumberOfBytesException{
		if(value<0){
			throw new NumberOfBytesException();
		}
		int numberOfZERO = numberOfBytes - (Long.toString(value)).length();
		if(numberOfZERO<0){
			throw new NumberOfBytesException();
		}
		String tmp="";
		for(int i=0;i<numberOfZERO;i++){
			tmp=tmp+"0";
		}
		tmp=tmp+value;
		return tmp;
	}
	
	public static byte[] longToByteArray(Long val,int numberOfByte,ByteOrder ENDIAN){
		if(val<0){
		}
		//long values = Long.parseUnsignedLong("18446744073709551615");
		return ByteBuffer.allocate(numberOfByte).order(ENDIAN).putLong(val).array();
	}
	
	public static Long byteArrayToLong(byte[] bytes ,int numberOfByte,ByteOrder ENDIAN){
		ByteBuffer buffer = ByteBuffer.allocate(numberOfByte).order(ENDIAN);
		buffer.put(bytes);
		buffer.flip();//need flip 
		return buffer.getLong();
	}
	
	/**
	 * Convertir un port 45 -> 0045
	 * @param port
	 * @return
	 * @throws Exception
	 */
	private String convertPort(int port) throws ParseException{
		
		int size=(""+port).length();
		if(size>4 || port<0){
			throw new ParseException();
		}
		int diff=4-size;
		String result=(""+port);
		for(int i=0;i<diff;i++){
			result="0"+result;
		}
		return result;
	}
	
	/**
	 * Convertir une ip 192.0.0.1 -> 192.000.000.001
	 * @param ip
	 * @return 
	 * @throws Exception
	 */
	public static String convertIP(String ip) throws ParseException{
		
		if(ip.equals("localhost")){
			return "127.000.000.001";
		}
		String[]tmp=ip.split("\\.");
		
		if(tmp.length!=4){
			throw new ParseException();
		}
		//to put the 000
		for(int i=0; i<4;i++){
			if(tmp[i].length()==1){
				tmp[i]="00"+tmp[i];
			}
			else if(tmp[i].length()==2){
				tmp[i]="0"+tmp[i];
			}
		}
		String tmp2=tmp[0]+"."+tmp[1]+"."+tmp[2]+"."+tmp[3];
		
		return tmp2;
	}
	
	public boolean isMulti() {
		return multi;
	}
	public void setMulti(boolean multi) {
		this.multi = multi;
	}
	public byte[] getData() {
		return data;
	}
	public TypeMessage getType() {
		return type;
	}
	public String getIp_diff() {
		return ip_diff;
	}
	public Integer getPort_diff() {
		return port_diff;
	}
	public long getIdm() {
		return idm;
	}
	public String getId_app() {
		return id_app;
	}
	public byte[] getData_app() {
		return data_app;
	}
	public String getIp_succ() {
		return ip_succ;
	}
	public Integer getPort_succ() {
		return port_succ;
	}
	public String getIp(){
		return this.ip;
	}
	public Integer getPort(){
		return this.port;
	}

	public String getId() {
		return id;
	}
	
	
}