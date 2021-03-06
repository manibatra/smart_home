import hManager._hmDisp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import smartui.uiPrx;
import smartui.uiPrxHelper;
import mediamanager.mediaPrx;
import mediamanager.mediaPrxHelper;
import Demo._sensorsDisp;
import Demo.sensorsPrx;
import Demo.sensorsPrxHelper;
import Demo.shutdownsensorPrx;
import Demo.shutdownsensorPrxHelper;
import Ice.Current;
import IceStorm.AlreadySubscribed;
import IceStorm.BadQoS;
import IceStorm.TopicExists;

class HomeManager extends Ice.Application{

	IceStorm.TopicPrx temp_topic;
	Ice.ObjectPrx proxy;
	IceStorm.TopicManagerPrx topicManager;
	String prev_loc = "";
	int counter = 0;
	int adjusting = 0;
	static BufferedWriter bw;
	String home_users = "";
	mediaPrx media; //proxy to interact with emm as a client
	uiPrx ui; //proxy to interact with UI as a client
	int log_lines = 0;
	shutdownsensorPrx shutdownSensor; //to shutdown the sensors

	class SensorsI extends _sensorsDisp {

		@Override
		public void sayTemp(int temp, Current __current) {

			if(adjusting == 0 && temp != 22){

				//System.out.println("The current temp is "+temp);

				adjusting = 1;
				log_lines+=4;
				try {
					FileWriter fw = new FileWriter("temp.log", true);
					bw = new BufferedWriter(fw);
					bw.write("Air-conditioning adjusted.\n"
							+ "Temperature: at "+temp+" degrees\n"
							+ "At Home:"+home_users+"\n\n");
					bw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} 
		}

		@Override
		public void sayLoc(String loc, String home, Current __current) {

			//System.out.println("The current location is "+loc);

			if(adjusting == 1 && counter!=5){

				counter++;

			}else{

				adjusting = 0;
				counter = 0;
			}

			home_users = home;

			String topic_name = null;

			if(!prev_loc.equalsIgnoreCase(loc)){
				//System.out.println("in here");
				if(!prev_loc.equalsIgnoreCase(""))		
					temp_topic.unsubscribe(proxy);

				prev_loc = loc;

				if(loc.equalsIgnoreCase("home")){

					topic_name = "temperature";



				} else if(loc.equalsIgnoreCase("away")){

					topic_name = "temperatureRanged";


				}

				//topic for temperature
				temp_topic = null;

				try {
					temp_topic = topicManager.retrieve(topic_name);

				}
				catch (IceStorm.NoSuchTopic ex) {

					try {
						temp_topic = topicManager.create(topic_name);
					} catch (TopicExists e) {
						System.err.println(appName() + ": temporary failure, try again.");

					}

				}

				java.util.Map<String, String> qos = new java.util.HashMap<String, String>();
				qos.put("reliability", "ordered");
				try {
					temp_topic.subscribeAndGetPublisher(qos, proxy);
				} catch (AlreadySubscribed e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (BadQoS e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


			}

		}

		@Override
		public void sayTempRanged(int temp, Current __current) {
			// TODO Auto-generated method stub
			//System.out.println("The temperature is out of range at "+temp+"   "+adjusting);

			if(adjusting == 0 && temp != 22){

				adjusting = 1;
				log_lines+=4;
				try {
					FileWriter fw = new FileWriter("temp.log", true);
					bw = new BufferedWriter(fw);
					bw.write("Air-conditioning adjusted.\n"
							+ "Temperature: at "+temp+" degrees\n"
							+ "At Home:"+home_users+"\n\n");
					bw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} 

		}

		@Override
		public void sayEnergy(int energy, Current __current) {

			double energyKW = (double)energy/1000;
			if(energyKW > 4.0){

				if(ui == null){	
					//code for HomeManager as a client to UI(for energy warning)
					try{
						Ice.ObjectPrx uiClient = communicator().stringToProxy("ui_hm:tcp -h localhost -p 6666");
						ui = uiPrxHelper.uncheckedCast(uiClient);
					} 
					catch(Ice.ConnectionRefusedException e){

						//Server has not been started

					}
					
				} 

				try{
					ui.sayWarning(energyKW);
				}
				catch(Ice.ConnectionRefusedException e){

					//Server has not been started

				}
			}


		}



	}

	class HmI extends _hmDisp {

		@Override
		public String[] getLog(Current __current) {
			String[] result = null;
			if(log_lines == 0){
				result = new String[1];
				result[0] = "Log of temperature adjustment is empty";

			} else {

				result = new String[log_lines];
				try {
					BufferedReader in = new BufferedReader(new FileReader("temp.log"));
					for(int i = 0; i < log_lines; i++){

						result[i] = in.readLine();

					}
					in.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}




			}
			return result;
		}

		@Override
		public String[] getMedia(Current __current) {
			String result[] = null;
			String fileNames[] = media.getFiles();
			if(fileNames.length == 0){

				result = new String[1];
				result[0] = "No media files were found";

			} else {

				result = new String[fileNames.length];
				for(int i = 0; i < fileNames.length; i++){

					result[i] = fileNames[i]+", "+media.getTitle(fileNames[i])+", "+media.getDisc(fileNames[i]);

				}


			}

			return result;
		}

		@Override
		public String[] getTracks(String disc, Current __current) {
			// TODO Auto-generated method stub
			String result[] = null;
			String tracks[] = media.getTracks(disc);
			if(tracks.length == 0){

				result = new String[1];
				result[0] = "The disc "+disc+" was not found in the media collection.";

			} else {

				result = tracks;

			}
			return result;
		}

		@Override
		public void shutDown(Current __current) {
			
			media.shutDown();
			shutdownSensor.shutdown();
			communicator().shutdown();
			
		}


	}
	public static void main(String args[]){


		HomeManager hm = new HomeManager();
		int status = hm.main("Subscriber", args, "config.sub");
		try {
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(status);

	}

	@Override
	public int run(String[] args) {


		topicManager = IceStorm.TopicManagerPrxHelper.checkedCast(
				communicator().propertyToProxy("TopicManager.Proxy"));

		if(topicManager == null)
		{
			System.err.println("invalid proxy");
			return 1;
		}


		//topic for location
		IceStorm.TopicPrx loc_topic = null;

		try {
			loc_topic = topicManager.retrieve("location");

		}
		catch (IceStorm.NoSuchTopic ex) {

			try {
				loc_topic = topicManager.create("location");
			} catch (TopicExists e) {
				System.err.println(appName() + ": temporary failure, try again.");
				return 1;
			}

		}

		//topic for energy
		IceStorm.TopicPrx energy_topic = null;

		try {
			energy_topic = topicManager.retrieve("energy");

		}
		catch (IceStorm.NoSuchTopic ex) {

			try {
				energy_topic = topicManager.create("energy");
			} catch (TopicExists e) {
				System.err.println(appName() + ": temporary failure, try again.");
				return 1;
			}

		}

		Ice.ObjectAdapter adapter = communicator().createObjectAdapter("Clock.Subscriber");

		SensorsI sensor = new SensorsI();

		proxy = adapter.addWithUUID(sensor).ice_twoway();

		adapter.activate();

		//code for HomeManager as client to EMM
		Ice.ObjectPrx emmClient = communicator().stringToProxy("emm_hm:tcp -h localhost -p 7777");
		media = mediaPrxHelper.uncheckedCast(emmClient);

		//code for HomeManager as a server to SmartHomeUi
		Ice.ObjectAdapter uiServer = communicator().createObjectAdapterWithEndpoints("hm_ui", "tcp -h localhost -p 8888");
		uiServer.add(new HmI(), communicator().stringToIdentity("hm_ui"));
		uiServer.activate();

		//code for HomeManager as publisher
		IceStorm.TopicPrx shutdown_topic = null;
		while(shutdown_topic == null){

			try{

				shutdown_topic = topicManager.retrieve("shutdown");

			} catch (IceStorm.NoSuchTopic ex){

				try{

					shutdown_topic = topicManager.create("shutdown");

				} catch (IceStorm.TopicExists e){

					System.err.println(appName() + ": temporary failure, try again.");
					return 1;

				}

			}

		}

		Ice.ObjectPrx pub = shutdown_topic.getPublisher().ice_oneway();

		shutdownSensor = shutdownsensorPrxHelper.uncheckedCast(pub);




		java.util.Map<String, String> qos = new java.util.HashMap<String, String>();
		qos.put("reliability", "ordered");
		try {
			//	temp_topic.subscribeAndGetPublisher(qos, proxy);
			loc_topic.subscribeAndGetPublisher(qos, proxy);
			energy_topic.subscribeAndGetPublisher(qos, proxy);
		} catch (AlreadySubscribed e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadQoS e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		shutdownOnInterrupt();

		communicator().waitForShutdown();
		try{
		temp_topic.unsubscribe(proxy);
		loc_topic.unsubscribe(proxy);
		energy_topic.unsubscribe(proxy);
		shutdown_topic.destroy();
		}
		catch(Ice.ObjectNotExistException e){
			
			//Already destroyed
			
		}
		
		System.out.println("This got executed");
		// TODO Auto-generated method stub
		return 0;
	}




}