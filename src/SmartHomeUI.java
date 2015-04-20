import java.io.IOException;

class SmartHomeUI extends Ice.Application{

	public static void main(String args[]){

		int exit = 0;

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
				System.in.read();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
						
			switch (in) {
			case 49:
				System.out.println("view the log");
				
				break;
				
			case 50: 
				System.out.println("view the media files");

				break;
				
			case 51: 
				System.out.println("view disc tracks");
				break;
			
			case 69: 
				System.out.println("About to exit");
				exit = 1;
				break;
			default:
				System.out.println("Invalid Command");
				break;
			}
			
			if(exit == 1)
				break;

		}
		
	}

	@Override
	public int run(String[] args) {
		// TODO Auto-generated method stub
		return 0;
	}





}