module Demo{
interface sensors{

	void sayTemp(int temp);
	void sayTempRanged(int temp);
	void sayLoc(string loc, string home);
	void sayEnergy(int energy);
	
	
};

interface shutdownsensor{
	void shutdown();
};
};