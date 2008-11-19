package no.rehn.roomba.io;

public interface RoombaStatus {
	// packets
	int getVoltage();
	int getBatteryTemperature();
	int getCurrent();
	int getBatteryCharge();
	int getBatteryCapacity();
	byte getBumpAndWheelDrops();
	byte getOvercurrents();
	boolean isWallDetected();
	boolean isCliffLeft();
	boolean isCliffFrontLeft();
	boolean isCliffRight();
	boolean isCliffFrontRight();
	
	//TODO map rest

	// handy utilities (utilises above data)
	boolean isWheelDropped();
	boolean isBumped();
	boolean isMainBrushOvercurrent();
	boolean isSideBrushOvercurrent();
	boolean isLeftWheelOvercurrent();
	boolean isRightWheelOvercurrent();
}
