import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import Demo.sensorsPrx;
import Demo.sensorsPrxHelper;

class Sensor extends Ice.Application{

	static int[] temp_value;
	static int[] temp_time;
	static String[] loc_value;
	static int[] loc_time;
	static int[] energy_value;
	static int[] energy_time;

	
	static int global_time = 0;
	static int N;
	
	
	public static void main(String args[]) throws IOException{
		LineNumberReader  lnr = new LineNumberReader(new FileReader(new File("temperature.txt")));
		lnr.skip(Long.MAX_VALUE);
		N = lnr.getLineNumber();
		// Finally, the LineNumberReader object should be closed to prevent resource leak
		lnr.close();
		
		BufferedReader in = new BufferedReader(new FileReader("temperature.txt"));
		temp_value = new int[N];
		temp_time = new int[N];
		for(int i = 0; i < N; i++){
			String read = in.readLine();
			String temp[] = read.split(",");
			temp_value[i] = Integer.parseInt(temp[0]);
			temp_time[i] = Integer.parseInt(temp[1]);
			if(i > 0){
				temp_time[i] += temp_time[i-1];
			}
			
		}
		
		in.close();
		
		in = new BufferedReader(new FileReader("location1.txt"));
		loc_value = new String[N];
		loc_time = new int[N];
		for(int i = 0; i < N; i++){
			String read = in.readLine();
			String temp[] = read.split(",");
			loc_value[i] = temp[0];
			loc_time[i] = Integer.parseInt(temp[1]);
			if(i > 0){
				loc_time[i] += loc_time[i-1];
			}
			
		}
		
		in.close();
		
		in = new BufferedReader(new FileReader("energy.txt"));
		energy_value = new int[N];
		energy_time = new int[N];
		for(int i = 0; i < N; i++){
			String read = in.readLine();
			String temp[] = read.split(",");
			energy_value[i] = Integer.parseInt(temp[0]);
			energy_time[i] = Integer.parseInt(temp[1]);
			if(i > 0){
				energy_time[i] += energy_time[i-1];
			}
			
		}
		
		in.close();
		
		
		Sensor s = new Sensor();
		int status = s.main("Publisher", args, "config.pub");
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
		
		//topic for publishing temperature
		IceStorm.TopicPrx temp_topic = null;
		while(temp_topic == null){
			
			try{
				
				temp_topic = topicManager.retrieve("Temperature");
				
			} catch (IceStorm.NoSuchTopic ex){
				
					try{
						
						temp_topic = topicManager.create("Temperature");
						
					} catch (IceStorm.TopicExists e){
						
						 System.err.println(appName() + ": temporary failure, try again.");
			             return 1;
						
				}
				
			}
			
		}
		
		Ice.ObjectPrx temp_pub = temp_topic.getPublisher().ice_oneway();
		
		sensorsPrx temp_sensor = sensorsPrxHelper.uncheckedCast(temp_pub);
		
		//topic for publishing everything else i.e. location and energy
		IceStorm.TopicPrx other_topic = null;
		while(other_topic == null){
			
			try{
				
				other_topic = topicManager.retrieve("Other");
				
			} catch (IceStorm.NoSuchTopic ex){
				
					try{
						
						other_topic = topicManager.create("Other");
						
					} catch (IceStorm.TopicExists e){
						
						 System.err.println(appName() + ": temporary failure, try again.");
			             return 1;
						
				}
				
			}
			
		}
		
		Ice.ObjectPrx other_pub = other_topic.getPublisher().ice_oneway();
		
		sensorsPrx other_sensor = sensorsPrxHelper.uncheckedCast(other_pub);
		
		try{
			
			while(true){
				
				global_time++;
				int time = getTime();
				String loc = getLoc();
				int energy = getEnergy();
				temp_sensor.sayTime(Integer.toString(time));
				other_sensor.sayOther(loc, energy);
				
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
			
			//Ignore
			
		}
		
		
		
		// TODO Auto-generated method stub
		return 0;
	}

	private int getEnergy() {
		
		int local_time;
		if(global_time <= energy_time[N-1])
			local_time = global_time;
		else
			local_time= global_time % energy_time[N-1];
		
		if(local_time == 0)
			local_time = energy_time[N-1];
		
		int index = 0;

		for(int i = 0; i < N; i++){
			
			if(local_time <= energy_time[i]){
				index = i;
				break;
			}	
			
		}
		
		
		return energy_value[index];
		
	}

	private String getLoc() {
		
		
		int local_time;
		if(global_time <= loc_time[N-1])
			local_time = global_time;
		else
			local_time= global_time % loc_time[N-1];
		
		if(local_time == 0)
			local_time = loc_time[N-1];
		
		int index = 0;

		for(int i = 0; i < N; i++){
			
			if(local_time <= loc_time[i]){
				index = i;
				break;
			}	
			
		}
		
		
		return loc_value[index];
	}

	private int getTime() {
		
		int local_time;
		if(global_time <= temp_time[N-1])
			local_time = global_time;
		else
			local_time= global_time % temp_time[N-1];
		
		if(local_time == 0)
			local_time = temp_time[N-1];
		
		int index = 0;

		for(int i = 0; i < N; i++){
			
			if(local_time <= temp_time[i]){
				index = i;
				break;
			}	
			
		}
		
		
		return temp_value[index];
	}
	
	
	
	
}