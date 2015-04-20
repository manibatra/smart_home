import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import Demo._shutdownsensorDisp;
import Demo.sensorsPrx;
import Demo.sensorsPrxHelper;
import Ice.Current;
import IceStorm.AlreadySubscribed;
import IceStorm.BadQoS;

class Sensor extends Ice.Application{

	static String[] value;
	static int[] time;

	static String[] value2;
	static int[] time2;
	static String home;

	static int prev_temp = 0;


	static int global_time = 0;
	static int N;
	static int M;

	IceStorm.TopicPrx topic;
	IceStorm.TopicPrx ranged_topic;

	int shutdown = 0;
	int shutdownSub = 0;

	class ShutdownI extends _shutdownsensorDisp {

		public void shutdown(Current __current) {
			// TODO Auto-generated method stub
			topic.destroy();
			if(ranged_topic!=null)
				ranged_topic.destroy();
			shutdown = 1;
			System.out.println("this gets called");
			communicator().shutdown();

		}


	}


	public static void main(String args[]) throws IOException{
		String type = args[0];
		String filename = args[1];

		System.out.println(type);


		LineNumberReader  lnr = new LineNumberReader(new FileReader(new File(filename)));
		lnr.skip(Long.MAX_VALUE);
		N = lnr.getLineNumber();
		// Finally, the LineNumberReader object should be closed to prevent resource leak
		lnr.close();



		BufferedReader in = new BufferedReader(new FileReader(filename));
		value = new String[N];
		time = new int[N];
		for(int i = 0; i < N; i++){
			String read = in.readLine();
			String temp[] = read.split(",");
			value[i] = temp[0];
			time[i] = Integer.parseInt(temp[1]);
			if(i > 0){
				time[i] +=time[i-1];
			}

		}

		if(type.equalsIgnoreCase("location") && args.length >= 3){

			filename = args[2];

			lnr = new LineNumberReader(new FileReader(new File(filename)));
			lnr.skip(Long.MAX_VALUE);
			M = lnr.getLineNumber();
			// Finally, the LineNumberReader object should be closed to prevent resource leak
			lnr.close();

			in.close();
			in = new BufferedReader(new FileReader(filename));
			value2 = new String[M];
			time2 = new int[M];
			for(int i = 0; i < N; i++){
				String read = in.readLine();
				String temp[] = read.split(",");
				value2[i] = temp[0];
				time2[i] = Integer.parseInt(temp[1]);
				if(i > 0){
					time2[i] +=time2[i-1];
				}

			}







		}

		in.close();



		Sensor s = new Sensor();
		int status = s.main("Publisher", args, "config.pub");
		System.exit(status);

	}

	@Override
	public int run(String[] args) {
		String type = args[0];

		//code to act as a publisher
		IceStorm.TopicManagerPrx topicManager = IceStorm.TopicManagerPrxHelper.checkedCast(
				communicator().propertyToProxy("TopicManager.Proxy"));

		if(topicManager == null)
		{
			System.err.println("invalid proxy");
			return 1;
		}

		//topic for publishing temperature
		topic = null;
		while(topic == null){

			try{

				topic = topicManager.retrieve(type);

			} catch (IceStorm.NoSuchTopic ex){

				try{

					topic = topicManager.create(type);

				} catch (IceStorm.TopicExists e){

					System.err.println(appName() + ": temporary failure, try again.");
					return 1;

				}

			}

		}

		Ice.ObjectPrx pub = topic.getPublisher().ice_twoway();

		sensorsPrx sensor = sensorsPrxHelper.uncheckedCast(pub);

		ranged_topic = null;

		sensorsPrx range_sensor = null;

		if(type.equalsIgnoreCase("temperature")){




			while(ranged_topic == null){

				try{

					ranged_topic = topicManager.retrieve(type+"Ranged");

				} catch (IceStorm.NoSuchTopic ex){

					try{

						ranged_topic = topicManager.create(type+"Ranged");

					} catch (IceStorm.TopicExists e){

						System.err.println(appName() + ": temporary failure, try again.");
						return 1;

					}

				}

			}

			Ice.ObjectPrx ranged_pub = ranged_topic.getPublisher().ice_twoway();

			range_sensor = sensorsPrxHelper.uncheckedCast(ranged_pub);
		}
		//code for making a subscriber to listen for shutdown
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

		Ice.ObjectAdapter adapter = communicator().createObjectAdapter("Clock.Subscriber");

		ShutdownI shutdownSensor = new ShutdownI();

		Ice.ObjectPrx proxy = adapter.addWithUUID(shutdownSensor).ice_oneway();

		adapter.activate();

		java.util.Map<String, String> qos = null;








		try{

			while(true && shutdown == 0){

				if(shutdownSub == 0){
					try {
						shutdown_topic.subscribeAndGetPublisher(qos, proxy);
					} catch (AlreadySubscribed e) {
						// TODO Auto-generated catch block
						shutdownSub = 1;

					} catch (BadQoS e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				global_time++;

				if(type.equalsIgnoreCase("location")){

					String loc = getValue();
					String loc2 = "";
					if(args.length >= 3)
						loc2 = getValue2();
					String final_loc = "";
					//System.out.println(loc+"  "+loc2);
					if(loc.equalsIgnoreCase("home") && loc2.equalsIgnoreCase("home")){

						final_loc = "home";
						home = args[1].split("\\.")[0] +" and "+ args[2].split("\\.")[0];

					} else if(loc.equalsIgnoreCase("home")){

						final_loc = "home";
						home = args[1].split("\\.")[0];

					} else if(loc2.equalsIgnoreCase("home")){

						final_loc = "home";
						home = args[2].split("\\.")[0];

					} else {

						final_loc = "away";
						home = "";

					}

					sensor.sayLoc(final_loc, home);

				}	

				if(type.equalsIgnoreCase("energy")){


					int energy = Integer.parseInt(getValue());
					sensor.sayEnergy(energy);

				}

				if(type.equalsIgnoreCase("temperature")){

					int temp = Integer.parseInt(getValue());
					if((temp<15 || temp>28) && prev_temp!=temp){

						range_sensor.sayTempRanged(temp);

					}

					prev_temp = temp;


					sensor.sayTemp(temp);


				}




				try
				{
					Thread.currentThread().sleep(1000);
				}
				catch(java.lang.InterruptedException e)
				{
				}

			}


		}

		catch(Ice.CommunicatorDestroyedException ex){

			System.out.println("So this happened");

		}
		
		communicator().waitForShutdown();
		try{
		shutdown_topic.unsubscribe(proxy);
		shutdown_topic.destroy();
		}
		catch(Ice.ObjectNotExistException e){
			
			//Hmm has been shutdown
		}
		
		




		// TODO Auto-generated method stub
		return 0;
	}


	private String getValue2() {

		int local_time;
		if(global_time <= time2[N-1])
			local_time = global_time;
		else
			local_time= global_time % time2[N-1];

		if(local_time == 0)
			local_time = time2[N-1];

		int index = 0;

		for(int i = 0; i < N; i++){

			if(local_time <= time2[i]){
				index = i;
				break;
			}	

		}


		return value2[index];

	}

	private String getValue() {


		int local_time;
		if(global_time <= time[N-1])
			local_time = global_time;
		else
			local_time= global_time % time[N-1];

		if(local_time == 0)
			local_time = time[N-1];

		int index = 0;

		for(int i = 0; i < N; i++){

			if(local_time <= time[i]){
				index = i;
				break;
			}	

		}


		return value[index];
	}






}