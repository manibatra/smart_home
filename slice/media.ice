module mediamanager{

	sequence<string> FileNames;
	sequence<string> TrackNames;
	
interface media{


	string getTitle(string fileName);
	string getDisc(string fileName);
	TrackNames getTracks(string discTitle);
	FileNames getFiles();
	void shutDown();
};
};