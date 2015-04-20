import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import Ice.Current;
import mediamanager._mediaDisp;

class EMM extends Ice.Application{

	static String file[];
	static String title[];
	static String disc[];
	static String track[];
	static int N;

	class MediaI extends _mediaDisp{

		@Override
		public String getTitle(String fileName, Current __current) {

			for(int i = 0; i < N; i++){

				if(file[i].equalsIgnoreCase(fileName)){

					return title[i];

				}

			}

			return "No Such File";
		}

		@Override
		public String getDisc(String fileName, Current __current) {

			for(int i = 0; i < N; i++){

				if(file[i].equalsIgnoreCase(fileName)){

					return disc[i];

				}

			}

			return "No Such File";

		}

		@Override
		public String[] getTracks(String discTitle, Current __current) {

			java.util.Map<Integer, String> tracksMap = new java.util.HashMap<Integer, String>();
			
			for(int i = 0; i < N; i++){
				
				if(disc[i].equalsIgnoreCase(discTitle)){
					
					tracksMap.put(Integer.parseInt(track[i]), title[i]);
					
				}
				
			}
			
			SortedSet<Integer> keys = new TreeSet<Integer>(tracksMap.keySet());
			
			String[] result = new String[tracksMap.size()];
			
			int ptr = 0;
			
			for(int key : keys){
				
				String value = tracksMap.get(key);
				result[ptr++] = value;
				
			}

			return result;
		}

		@Override
		public String[] getFiles(Current __current) {
			// TODO Auto-generated method stub
			String result[] = Arrays.copyOf(file, file.length);
			Arrays.sort(result);
			return result;
		}




	}
	public static void main(String args[]) throws IOException{


		LineNumberReader  lnr = new LineNumberReader(new FileReader(new File(args[0])));
		lnr.skip(Long.MAX_VALUE);
		N = lnr.getLineNumber();
		N /= 4;
		// Finally, the LineNumberReader object should be closed to prevent resource leak
		lnr.close();

		try {
			BufferedReader in = new BufferedReader(new FileReader(args[0]));
			file = new String[N];
			title = new String[N];
			disc = new String[N];
			track = new String[N];

			for(int i = 0; i < N; i++){
				String file_name = in.readLine();
				String title_name = in.readLine();
				String disc_name = in.readLine();
				String track_no = in.readLine();
				in.readLine();
				file[i] = file_name.split(":")[1];
				title[i] = title_name.split(":")[1];
				disc[i] = disc_name.split(":")[1];
				track[i] = track_no.split(":")[1];	
			}

			in.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		EMM emm = new EMM();
		int status = emm.main("EMM", args);
		System.exit(status);
	}

	@Override
	public int run(String[] args) {
		// TODO Auto-generated method stub
		Ice.ObjectAdapter adapter = communicator().createObjectAdapterWithEndpoints("emm_hm", "tcp -h localhost -p 7777");
		adapter.add(new MediaI(), communicator().stringToIdentity("emm_hm"));
		adapter.activate();
		communicator().waitForShutdown();
		return 0;
	}




}