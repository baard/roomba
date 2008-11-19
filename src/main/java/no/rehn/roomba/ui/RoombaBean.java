package no.rehn.roomba.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.SwingUtilities;


import no.rehn.roomba.tunes.Song;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO doc
public class RoombaBean {
	final Logger logger = LoggerFactory.getLogger(getClass());
	public enum Mode {
		FULL, SAFE, PASSIVE, POWER_OFF
	}
	
	public static final int POWER_RED = 255;
	public static final int POWER_GREEN = 0;
	public static final int POWER_MAX = 255;
	public static final int POWER_MIN = 0;


	private boolean spot = false;

	private boolean clean = false;

	private boolean max = false;

	private boolean dirt = false;

	private int powerColor;

	private int powerIntensity;

	private Mode mode;
	
	private boolean start;
	
	boolean updateSensors;
	
	public boolean isUpdateSensors() {
		return updateSensors;
	}
	
	public void setUpdateSensors(boolean updateSensors) {
		pcs.firePropertyChange("updateSensors", this.updateSensors, this.updateSensors= updateSensors);
	}

	public void setPowerColor(int powerColor) {
		checkUnsignedByte("power-color", powerColor);
		pcs.firePropertyChange("powerColor", this.powerColor, this.powerColor= powerColor);
	}

	public void start() {
		pcs.firePropertyChange("start", this.start, this.start = true);
		pcs.firePropertyChange("mode", this.mode, this.mode = Mode.PASSIVE);
	}

	public void wakeup() {
		pcs.firePropertyChange("wakeup", false, true);
		start();
	}

	public void power() {
		pcs.firePropertyChange("power", false, true);
		pcs.firePropertyChange("mode", this.mode, this.mode = Mode.POWER_OFF);
	}

	public boolean isStart() {
		return start;
	}

	public Mode getMode() {
		return mode;
	}
	
	public void setMode(Mode mode) {
		if (mode == Mode.SAFE || mode == Mode.FULL) {
			pcs.firePropertyChange("mode", this.mode, this.mode = mode);
		}
		else {
			throw new IllegalArgumentException("Illegal to set mode " + mode);
		}
	}

	public void setPowerIntensity(int powerIntensity) {
		checkUnsignedByte("power-intensity", powerIntensity);
		pcs.firePropertyChange("powerIntensity", this.powerIntensity, this.powerIntensity = powerIntensity);
	}
	
	public int getPowerIntensity() {
		return powerIntensity;
	}
	
	private void checkUnsignedByte(String property, int byteParameter) {
		if (byteParameter < 0 || byteParameter > 255) {
			throw new IllegalArgumentException(
					property + " must be between 0 and 255");
		}
	}

	public void setDirt(boolean enabled) {
		pcs.firePropertyChange("dirt", this.dirt, this.dirt = enabled);
	}
	
	public boolean isDirt() {
		return dirt;
	}
	
	public void setClean(boolean clean) {
		pcs.firePropertyChange("clean", this.clean, this.clean = clean);
	}
	
	public boolean isClean() {
		return clean;
	}
	
	public void setMax(boolean max) {
		pcs.firePropertyChange("max", this.max, this.max = max);
	}
	
	public boolean isMax() {
		return max;
	}
	
	public void setSpot(boolean spot) {
		pcs.firePropertyChange("spot", this.spot, this.spot = spot);
	}
	
	public boolean isSpot() {
		return spot;
	}

	private boolean wheelDropped;
	
	public boolean isWheelDropped() {
		return wheelDropped;
	}
	
	void setWheelDropped(boolean wheelDropped) {
		pcs.firePropertyChange("wheelDropped", this.wheelDropped, this.wheelDropped = wheelDropped);
	}
	
	private boolean bumpSensor;
	
	public boolean isBumpSensor() {
		return bumpSensor;
	}
	
	void setBumpSensor(boolean bumpSensor) {
		pcs.firePropertyChange("bumpSensor", this.bumpSensor, this.bumpSensor = bumpSensor);
	}
	
	public int getPowerColor() {
		return powerColor;
	}
	
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);  

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		if (SwingUtilities.isEventDispatchThread()) {
			//TODO remove from model-package, is a gui-thingy
			pcs.addPropertyChangeListener(new AWTPropertyListenerWrapper(listener));
		}
		else {
			pcs.addPropertyChangeListener(listener);
		}
	}
	
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}
	
	private int leftWheelSpeed;
	
	public void setLeftWheelSpeed(int leftWheelSpeed) {
		if (leftWheelSpeed < -255 || leftWheelSpeed > 255) {
			throw new IllegalArgumentException(
					"wheel-speed must be between -255 and 255");
		}
		pcs.firePropertyChange("leftWheelSpeed", this.leftWheelSpeed, this.leftWheelSpeed = leftWheelSpeed);
		updateSpeed();
	}
	private int rightWheelSpeed;
	
	public void setRightWheelSpeed(int rightWheelSpeed) {
		if (rightWheelSpeed < -255 || rightWheelSpeed > 255) {
			throw new IllegalArgumentException(
					"wheel-speed must be between -255 and 255");
		}
		pcs.firePropertyChange("rightWheelSpeed", this.rightWheelSpeed, this.rightWheelSpeed = rightWheelSpeed);
		updateSpeed();
	}
	
	public int getLeftWheelSpeed() {
		return leftWheelSpeed;
	}
	
	public int getRightWheelSpeed() {
		return rightWheelSpeed;
	}
	
	private int sideBrushPwm;
	private int mainBrushPwm;
	private int vacuumPwm;

	public void setSideBrushPwm(int sideBrushPwm) {
		if (rightWheelSpeed < -127 || rightWheelSpeed > 127) {
			throw new IllegalArgumentException(
					"side-brush-pwm must be between -127 and 127");
		}
		pcs.firePropertyChange("sideBrushPwm", this.sideBrushPwm, this.sideBrushPwm = sideBrushPwm);
	}

	public void setMainBrushPwm(int mainBrushPwm) {
		if (mainBrushPwm < -127 || mainBrushPwm > 127) {
			throw new IllegalArgumentException(
					"main-brush-pwm must be between -127 and 127");
		}
		pcs.firePropertyChange("mainBrushPwm", this.mainBrushPwm, this.mainBrushPwm = mainBrushPwm);
	}
	
	public void setVacuumPwm(int vacuumPwm) {
		if (vacuumPwm < 0 || vacuumPwm > 127) {
			throw new IllegalArgumentException(
					"vacuum-pwm must be between 0 and 127");
		}
		pcs.firePropertyChange("vacuumPwm", this.vacuumPwm, this.vacuumPwm = vacuumPwm);
	}
	
	public int getSideBrushPwm() {
		return sideBrushPwm;
	}

	public int getMainBrushPwm() {
		return mainBrushPwm;
	}
	
	public int getVacuumPwm() {
		return vacuumPwm;
	}
	
	private String text;

	public void setText(String text) {
		pcs.firePropertyChange("text", this.text, this.text = text);
	}

	public String getText() {
		return text;
	}
	
	boolean wallSensor;

	void setWallSensor(boolean wallSensor) {
		pcs.firePropertyChange("wallSensor", this.wallSensor, this.wallSensor = wallSensor);
	}

	public boolean isWallSensor() {
		return wallSensor;
	}
	
	private int voltage;

	void setVoltage(int voltage) {
		pcs.firePropertyChange("voltage", this.voltage, this.voltage = voltage);
	}
	
	public int getVoltage() {
		return voltage;
	}
	
	private int batteryTemperature;
	
	void setBatteryTemperature(int batteryTemperature) {
		pcs.firePropertyChange("batteryTemperature", this.batteryTemperature, this.batteryTemperature = batteryTemperature);
	}
	
	public int getBatteryTemperature() {
		return batteryTemperature;
	}
	
	private int current;
	
	void setCurrent(int current) {
		pcs.firePropertyChange("current", this.current, this.current = current);
	}
	
	public int getCurrent() {
		return current;
	}
	
	private boolean overcurrentWheelRight;
	private boolean overcurrentWheelLeft;
	private boolean overcurrentMainBrush;
	private boolean overcurrentSideBrush;
	
	public void setOvercurrentWheelLeft(boolean overcurrentWheelLeft) {
		pcs.firePropertyChange("overcurrentWheelLeft", this.overcurrentWheelLeft, this.overcurrentWheelRight = overcurrentWheelLeft);
	}

	public void setOvercurrentWheelRight(boolean overcurrentWheelRight) {
		pcs.firePropertyChange("overcurrentWheelRight", this.overcurrentWheelRight, this.overcurrentWheelRight = overcurrentWheelRight);
	}

	public void setOvercurrentMainBrush(boolean overcurrentMainBrush) {
		pcs.firePropertyChange("overcurrentMainBrush", this.overcurrentMainBrush, this.overcurrentMainBrush = overcurrentMainBrush);
	}
	
	public void setOvercurrentSideBrush(boolean overcurrentSideBrush) {
		pcs.firePropertyChange("overcurrentSideBrush", this.overcurrentSideBrush, this.overcurrentSideBrush = overcurrentSideBrush);
	}

	public boolean isOvercurrentMainBrush() {
		return overcurrentMainBrush;
	}

	public boolean isOvercurrentSideBrush() {
		return overcurrentSideBrush;
	}
	
	public boolean isOvercurrentWheelLeft() {
		return overcurrentWheelLeft;
	}
	
	public boolean isOvercurrentWheelRight() {
		return overcurrentWheelRight;
	}
	
	private boolean statisSensor;
	
	public boolean isStatisSensor() {
		return statisSensor;
	}
	
	public void setStatisSensor(boolean statisSensor) {
		pcs.firePropertyChange("statisSensor", this.statisSensor, this.statisSensor = statisSensor);
	}
	
	private Song song;
	
	public Object wheelLock = new Object();
	public Song getSong() {
		return song;
	}
	
	public void setSong(Song song) {
		pcs.firePropertyChange("song", this.song, this.song = song);
		firePropertyChange("songName", getSongName());
	}
	
	public String getSongName() {
		return song != null ? song.getName() : null;
	}
	
	public int getWheelDiff() {
		return getLeftWheelSpeed() - getRightWheelSpeed();
	}

	public void setWheelDiff(int diff) {
		int delta = (diff - getWheelDiff()) / 2;
		synchronized (wheelLock) {
			setLeftWheelSpeed(getLeftWheelSpeed() + delta);
			setRightWheelSpeed(getRightWheelSpeed() - delta);
		}
		updateSpeed();
	}

	public int getVelocity() {
		return (getRightWheelSpeed() + getLeftWheelSpeed()) / 2;
	}

	public void setVelocity(int velocity) {
		int increase = velocity - getVelocity();
		synchronized (wheelLock) {
			setLeftWheelSpeed(getLeftWheelSpeed() + increase);
			setRightWheelSpeed(getRightWheelSpeed() + increase);
		}
		updateSpeed();
	}
	
	void updateSpeed() {
		// always trigger a change (for simplicity)
		firePropertyChange("velocity", getVelocity());
		firePropertyChange("wheelDiff", getWheelDiff());
	}
	
	public void stop() {
		synchronized (wheelLock) {
			setLeftWheelSpeed(0);
			setRightWheelSpeed(0);
		}
	}
	
	void firePropertyChange(String property, Object newValue) {
		pcs.firePropertyChange(new PropertyChangeEvent(this, property, null, newValue));
	}

	private int charge;
	
	public void setCharge(int charge) {
		pcs.firePropertyChange("charge", this.charge, this.charge = charge);
	}
	
	public int getCharge() {
		return charge;
	}

	private int capacity;
	
	public void setCapacity(int capacity) {
		pcs.firePropertyChange("capacity", this.capacity, this.capacity = capacity);
	}

	public int getCapacity() {
		return capacity;
	}
	
	private boolean cliffRight;
	private boolean cliffLeft;
	private boolean cliffLeftFront;
	private boolean cliffRightFront;

	public boolean isCliffLeft() {
		return cliffLeft;
	}

	public void setCliffLeft(boolean cliffLeft) {
		pcs.firePropertyChange("cliffLeft", this.cliffLeft, this.cliffLeft = cliffLeft);
	}

	public boolean isCliffLeftFront() {
		return cliffLeftFront;
	}

	public void setCliffLeftFront(boolean cliffLeftFront) {
		pcs.firePropertyChange("cliffLeftFront", this.cliffLeftFront, this.cliffLeftFront = cliffLeftFront);
	}

	public boolean isCliffRight() {
		return cliffRight;
	}

	public void setCliffRight(boolean cliffRight) {
		pcs.firePropertyChange("cliffRight", this.cliffRight, this.cliffRight = cliffRight);
	}

	public boolean isCliffRightFront() {
		return cliffRightFront;
	}

	public void setCliffRightFront(boolean cliffRightFront) {
		pcs.firePropertyChange("cliffRightFront", this.cliffRightFront, this.cliffRightFront = cliffRightFront);
	}

	
}
