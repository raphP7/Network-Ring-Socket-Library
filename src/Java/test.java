public class test {
	public static void main(String[] args) {
		
		
		Message a =new Message("XXXX 12345678".getBytes());
		a.setId(0);
		System.out.println(new String());
		/*
		try {
			System.out.println("arg0 : "+args[0]); //4242
			System.out.println("arg1 : "+args[1]); //5555

			Serv premier = new Serv(Integer.parseInt(args[0]),Integer.parseInt(args[1]));
			premier.verboseMode=true;
			
					Thread.sleep(4000);
					//System.out.println(premier.lire());	
					
					premier.send("mamama");
					
					premier.test(true);
					
					premier.close();
			
		} catch (InterruptedException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DOWNmessageException e) {
			System.out.println("DOWN recu dans main");
		} catch (SizeMessageException e) {
			e.printStackTrace();
		}
		
		*/
		System.out.println("fin main");
	}
}