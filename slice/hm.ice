module hManager{

sequence<string> log;
sequence<string> media;
sequence<string> tracks;

interface hm{
	
	log getLog();
	media getMedia();
	tracks getTracks(string disc);
	void shutDown();
	
	
};
};