import hManager.hmPrx;
import hManager.hmPrxHelper;

import java.io.IOException;
import java.util.Scanner;

import Ice.Current;
import smartui._uiDisp;

class SmartHomeUI extends Ice.Application{

	class UiI extends _uiDisp {

		@Override
		public void sayWarning(double energy, Current __current) {
			// TODO Auto-generated method stub
			System.out.println("Energy Usage Warning : Current electricity consumption is "+energy+"kW\n");
			System.out.println("Please consider the environment before switching on any electrical appliance.\n");
			
		}
		
		
	}
	
	
	public static void main(String args[]){

		SmartHomeUI ui = new SmartHomeUI();
		int status = ui.main("SmartHomeUI", args);
		System.exit(status);
		
	}

	@Override
	public int run(String[] args) {
		int exit = 0;
		
		//code for UI as client to HomeManager
		Ice.ObjectPrx hmClient = communicator().stringToProxy("hm_ui:tcp -h localhost -p 8888");
		hmPrx hm = hmPrxHelper.uncheckedCast(hmClient);
		
		//code for UI as server to HomeMangaer
		Ice.ObjectAdapter adapter = communicator().createObjectAdapterWithEndpoints("ui_hm", "tcp -h localhost -p 6666");
		adapter.add(new UiI(), communicator().stringToIdentity("ui_hm"));
		adapter.activate();

		while(true){
			System.out.println("Welcome to the Smart Home Monitoring System\n"
					+ "Please select an option:\n"
					+ "1. View log – temperature adjustment\n"
					+ "2. View media files\n"
					+ "3. View disc tracks\n"
					+ "E. Exit");
			
			int in = 0;
			try {
				in = System.in.read();
				if(in !=10)
				System.in.read();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
						
			switch (in) {
			case 49:
				String[] log = hm.getLog();
				for(int i = 0;  i < log.length; i++){
					System.out.println(log[i]);
				}
				System.out.println();
				
				break;
				
			case 50: 
				String[] media = hm.getMedia();
				for(int i = 0; i < media.length; i++){
					System.out.println(media[i]);
				}
				System.out.println();

				break;
				
			case 51: 
				System.out.println("Please enter the disc title :");
				Scanner scan=new Scanner(System.in);
				String disc = scan.nextLine();
				String tracks[] = hm.getTracks(disc);
				for(int i = 0; i < tracks.length; i++){
					System.out.println(tracks[i]);
				}
				System.out.println();
				break;
			
			case 69: 
				System.out.println("About to exit");
				hm.shutDown();
				communicator().shutdown();
				exit = 1;
				break;
			case 10:
				break;
			default:
				System.out.println("Invalid Command");
				System.out.println();
				break;
			}
			
			if(exit == 1)
				break;

		}
		
		communicator().waitForShutdown();

		
		
		return 0;
	}





}