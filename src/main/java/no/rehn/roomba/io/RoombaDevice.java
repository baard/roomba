package no.rehn.roomba.io;

import java.util.List;

public interface RoombaDevice {
	void setLeds(boolean spot, boolean clean, boolean dirt, boolean max,
			int powerIntensity, int powerColor);
	void saveSong(int songNumber, List<Note> notes);
	void playSong(int songNumber);
	void powerDown();
	void startUp();
	void setDriveWheelPwm(int rightWheel, int leftWheel);
	void setBrushAndVacuumPwm(int mainBrush, int sideBrush, int vacuum);
	void setDigitLeds(char digit1, char digit2, char digit3, char digit4);
	void enterSafeMode();
	void enterFullMode();
	void updateSensors(int packetGroup, RoombaStatusListener callback);

	// connection-wake-up
	void wakeUp();
	
	// for monitoring
	long getSentBytes();
	long getReceivedBytes();
}