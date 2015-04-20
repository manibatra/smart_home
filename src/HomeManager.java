import Demo._sensorsDisp;
import Ice.Current;
import Ice.ObjectAdapter;
import IceStorm.AlreadySubscribed;
import IceStorm.BadQoS;
import IceStorm.TopicExists;

class HomeManager extends Ice.Application{

	class SensorsI extends _sensorsDisp {

		@Override
		public void sayTime(String temp, Current __current) {

			System.out.println("The current temp is "+temp);

		}

		@Override
		public void sayOther(String loc, int energy, Current __current) {

			System.out.println("The current location is "+loc);
			System.out.println("The current energy consumption is "+energy);
			
		}



	}

	public static void main(String args[]){

		HomeManager hm = new HomeManager();
		int status = hm.main("Subscriber", args, "config.sub");
		System.exit(status);

	}

	@Override
	public int run(String[] args) {


		IceStorm.TopicManagerPrx topicManager = IceStorm.TopicManagerPrxHelper.checkedCast(
				communicator().propertyToProxy("TopicManager.Proxy"));

		if(topicManager == null)
		{
			System.err.println("invalid proxy");
			return 1;
		}

		//topic for temperature
		IceStorm.TopicPrx topic = null;

		try {
			topic = topicManager.retrieve("Temperature");

		}
		catch (IceStorm.NoSuchTopic ex) {

			try {
				topic = topicManager.create("Temperature");
			} catch (TopicExists e) {
				System.err.println(appName() + ": temporary failure, try again.");
				return 1;
			}

		}
		
		//topic for location and energy
				IceStorm.TopicPrx other_topic = null;

				try {
					other_topic = topicManager.retrieve("Other");

				}
				catch (IceStorm.NoSuchTopic ex) {

					try {
						other_topic = topicManager.create("Other");
					} catch (TopicExists e) {
						System.err.println(appName() + ": temporary failure, try again.");
						return 1;
					}

				}

		Ice.ObjectAdapter adapter = communicator().createObjectAdapter("Clock.Subscriber");

		SensorsI sensor = new SensorsI();

		Ice.ObjectPrx proxy = adapter.addWithUUID(sensor).ice_oneway();

		adapter.activate();




		java.util.Map<String, String> qos = null;
		try {
			topic.subscribeAndGetPublisher(qos, proxy);
			other_topic.subscribeAndGetPublisher(qos, proxy);
		} catch (AlreadySubscribed e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadQoS e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		shutdownOnInterrupt();

		communicator().waitForShutdown();
		topic.unsubscribe(proxy);
		other_topic.unsubscribe(proxy);
		// TODO Auto-generated method stub
		return 0;
	}




}