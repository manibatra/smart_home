import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;

import mediamanager.mediaPrx;
import mediamanager.mediaPrxHelper;
import Demo._sensorsDisp;
import Ice.Current;
import Ice.ObjectAdapter;
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
	mediaPrx media; //proxy to interact with emm

	class SensorsI extends _sensorsDisp {

		@Override
		public void sayTemp(int temp, Current __current) {

			if(adjusting == 0 && temp != 22){

				System.out.println("The current temp is "+temp);

				adjusting = 1;
				try {
					bw.write("Air-conditioning adjusted.\n"
							+ "Temperature: at "+temp+" degrees\n"
							+ "At Home:"+home_users+"\n\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} 
		}

		@Override
		public void sayLoc(String loc, String home, Current __current) {

			System.out.println("The current location is "+loc);
						
			if(adjusting == 1 && counter!=5){
				
				counter++;
				
			}else{
				
				adjusting = 0;
				counter = 0;
			}

			home_users = home;

			String topic_name = null;

			if(!prev_loc.equalsIgnoreCase(loc)){
				System.out.println("in here");
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
			System.out.println("The temperature is out of range at "+temp+"   "+adjusting);

			if(adjusting == 0 && temp != 22){

				adjusting = 1;
				try {
					bw.write("Air-conditioning adjusted.\n"
							+ "Temperature: at "+temp+" degrees\n"
							+ "At Home:"+home_users+"\n\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} 

		}

		@Override
		public void sayEnergy(int energy, Current __current) {
			// TODO Auto-generated method stub

		}



	}

	public static void main(String args[]){

		try {
			FileWriter fw = new FileWriter("temp.log");
			bw = new BufferedWriter(fw);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
		temp_topic.unsubscribe(proxy);
		loc_topic.unsubscribe(proxy);
		energy_topic.unsubscribe(proxy);
		// TODO Auto-generated method stub
		return 0;
	}




}