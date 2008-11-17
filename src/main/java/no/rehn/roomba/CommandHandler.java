package no.rehn.roomba;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import javax.swing.SwingUtilities;

import no.rehn.roomba.Roomba.Mode;
import no.rehn.roomba.io.RoombaConnection;
import no.rehn.roomba.io.RoombaInputListener;
import no.rehn.roomba.tunes.Note;
import no.rehn.roomba.tunes.Song;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandHandler implements PropertyChangeListener, RoombaInputListener {
	Logger logger = LoggerFactory.getLogger(getClass());

	private final RoombaConnection connection;

	private final Roomba model;
	
	volatile int lastSlot = 1;

	public CommandHandler(RoombaConnection connection, Roomba model) {
		this.connection = connection;
		this.model = model;

		logger.info("Starting bandwidth monitor");
		Thread eventFirer = new Thread(new PropertyEventFirer(), "bandwidth-monitor");
		eventFirer.setDaemon(true);
		eventFirer.start();
	}

	public synchronized void propertyChange(PropertyChangeEvent evt) {
		Roomba roomba = (Roomba) evt.getSource();
		String name = evt.getPropertyName();
		if ("powerIntensity".equals(name) || "powerColor".equals(name)
				|| "dirt".equals(name) || "max".equals(name)
				|| "clean".equals(name) || "spot".equals(name)) {
			enqueue(new LedCommand());
		}
		if ("power".equals(name)) {
			enqueue(new PowerCommand());
		}
		if ("wakeup".equals(name)) {
			wakeup();
			enqueue(new StartCommand());
		}
		if ("start".equals(name)) {
			enqueue(new StartCommand());
		}
		if ("mode".equals(name)) {
			// ignore passive mode, as we cannot change to this mode
			if (evt.getNewValue() != Mode.PASSIVE
					&& evt.getNewValue() != Mode.POWER_OFF) {
				enqueue(new ModeCommand());
			}
		}
		if ("updateSensors".equals(name)) {
			// always trigger new command on state-change (sort of reset)
			if (evt.getNewValue() == (Boolean) false) {
				updateSensors = false;
			} else {
				enqueue(new UpdateSensorsCommand());
				updateSensors = true;
				awaitingData = true;
			}
		}
		if ("rightWheelSpeed".equals(name) || "leftWheelSpeed".equals(name)) {
			WheelCommand command;
			synchronized(roomba.wheelLock) {
				command = new WheelCommand(roomba.getLeftWheelSpeed(), roomba.getRightWheelSpeed());
			}
			logger.info("Enqueued wheel-command left={}, right={}", command.leftSpeed, command.rightSpeed);
			enqueue(command);
		}

		if ("mainBrushPwm".equals(name) || "sideBrushPwm".equals(name)
				|| "vacuumPwm".equals(name)) {
			enqueue(new PwmCommand());
		}
		if ("text".equals(name)) {
			enqueue(new LedDigitsCommand());
		}
		if ("song".equals(name)) {
			// start at slot 0 (only 0 - 3 is valid slots, despise doc says otherwise)
			if (model.getSong() == null) {
				logger.info("Aborting song");
				enqueue(new StoreSongCommand(new ArrayList<Note>(), 0));
				// remove all scheduled songs
				Iterator<Command> it = pendingCommands.iterator();
				while (it.hasNext()) {
					Command command = it.next();
					if (command instanceof StoreSongCommand) {
						logger.info("Removing song-command {} from queue", command);
						it.remove();
					}
				}
			}
			else {
				int nextSlot = (lastSlot - 1) % 4;
				logger.info("Using {} as next slot", nextSlot);
				enqueue(new StoreSongCommand(model.getSong().getNotes(), nextSlot));
				nextSlot++;
			}
		}
	}

	private void wakeup() {
		connection.close();
		connection.open();
	}

	volatile boolean updateSensors;

	private void enqueue(Command command) {
		pendingCommands.remove(command);
		pendingCommands.add(command);
	}

	private PriorityQueue<Command> pendingCommands = new PriorityQueue<Command>();

	class StoreSongCommand extends Command {
		final List<Note> notes;
		int slot;
		StoreSongCommand(List<Note> notes, int slot) {
			this.notes = notes;
			this.slot = slot;
		}

		@Override
		byte[] toBytes() {
			List<Note> toPlay = notes;
			if (notes.size() > 16) {
				toPlay = toPlay.subList(0, 16);
				// we must split song and use multiple slots
				List<Note> nextPart = notes.subList(16, notes.size());
				long wait = Song.getLength(toPlay);
				int nextSlot = (slot % 4) + 1;
				logger.info("Enqueing for slot {} in {}ms", nextSlot, wait);
				enqueue(new StoreSongCommand(nextPart, nextSlot).setAwait(wait));
			}
			// wait 500 msec before playing song, 250 msec has caused roomba to not play the
			// song stored
			//enqueue(new PlayCommand(slot).setAwait(500));
			enqueue(new PlayCommand(slot).setAwait(250));
			return toSong(parseNotes(toPlay), slot);
		}

		int[] parseNotes(List<Note> notes) {
			int[] ints = new int[notes.size() * 2];
			int index = 0;
			for (Note note : notes) {
				ints[index] = note.getNote();
				ints[index + 1] = note.getDuration();
				index += 2;
			}
			return ints;
		}
		
		byte[] toSong(int[] song, int slot) {
			int length = song.length;
			int songlength = length/2;
			byte cmd[] = new byte[length+3]; 
	        cmd[0] = (byte) 140;
	        cmd[1] = (byte) slot;
	        cmd[2] = (byte) songlength;
	        for( int i=0; i < length; i++ ) {
	            cmd[3+i] = (byte)song[i];
	        }
	        return cmd;
		}
	}
	
	class StartCommand extends Command {
		@Override
		byte[] toBytes() {
			return new byte[] { (byte) 128 };
		}
	}

	class PlayCommand extends Command {
		final int slot;

		PlayCommand(int slot) {
			this.slot = slot;
		}

		byte[] toBytes() {
			return new byte[] { (byte) 141, (byte) (int) slot };
		}
	}

	class PowerCommand extends Command {
		@Override
		byte[] toBytes() {
			return new byte[] { (byte) 133 };
		}
	}

	class WheelCommand extends Command {
		int leftSpeed;
		int rightSpeed;
		WheelCommand(int leftSpeed, int rightSpeed) {
			this.leftSpeed = leftSpeed;
			this.rightSpeed = rightSpeed;
		}

		@Override
		byte[] toBytes() {
			byte[] cmd = { (byte) 146, (byte) (rightSpeed >>> 8),
					(byte) (rightSpeed & 0xFF), (byte) (leftSpeed >>> 8),
					(byte) (leftSpeed & 0xFF) };
			return cmd;
		}
		
		@Override
		public boolean equals(Object obj) {
			return sameClass(obj);
		}
	}

	class PwmCommand extends Command {
		@Override
		byte[] toBytes() {
			int mainBrush = model.getMainBrushPwm();
			int sideBrush = model.getSideBrushPwm();
			int vacuum = model.getVacuumPwm();
			byte[] cmd = { (byte) 144, (byte) mainBrush, (byte) sideBrush,
					(byte) vacuum };
			return cmd;
		}
		
		@Override
		public boolean equals(Object obj) {
			return sameClass(obj);
		}
	}

	class LedDigitsCommand extends Command {
		@Override
		byte[] toBytes() {
			String toDisplay = model.getText();
			while (toDisplay.length() < 4) {
				// pad to 4 digits
				toDisplay += " ";
			}
			byte[] cmd = new byte[] { (byte) 164, (byte) toDisplay.charAt(0),
					(byte) toDisplay.charAt(1), (byte) toDisplay.charAt(2),
					(byte) toDisplay.charAt(3), };
			return cmd;
		}
		
		@Override
		public boolean equals(Object obj) {
			return sameClass(obj);
		}
	}

	class ModeCommand extends Command {
		@Override
		byte[] toBytes() {
			byte mode;
			switch (model.getMode()) {
			case SAFE:
				mode = (byte) 131;
				break;
			case FULL:
				mode = (byte) 132;
				break;
			default:
				throw new IllegalStateException("Cannot set mode "
						+ model.getMode());
			}
			return new byte[] { mode };
		}
	}

	class LedCommand extends Command {
		@Override
		byte[] toBytes() {
			int led = (model.isSpot() ? 0x08 : 0)
					| (model.isClean() ? 0x04 : 0) | (model.isMax() ? 0x02 : 0)
					| (model.isDirt() ? 0x01 : 0);
			return new byte[] { (byte) 139, (byte) led,
					(byte) model.getPowerColor(),
					(byte) model.getPowerIntensity() };
		}
		
		@Override
		public boolean equals(Object obj) {
			return sameClass(obj);
		}
	}

	abstract class Command implements Comparable<Command> {
		long executeAfter;
		
		Command setAwait(long executeAfter) {
			this.executeAfter = System.currentTimeMillis() + executeAfter;
			return this;
		}
		
		boolean isDue() {
			return System.currentTimeMillis() > executeAfter;
		}
		
		abstract byte[] toBytes();
		
		public int compareTo(Command o) {
			return (int) (executeAfter - o.executeAfter);
		}
		
		boolean sameClass(Object obj) {
			if (obj == null) {
				return false;
			}
			return getClass() == obj.getClass();
		}

	}
	
	class UpdateSensorsCommand extends Command {
		@Override
		byte[] toBytes() {
			// 1 is all sensors
			return new byte[] { (byte) 142, 0 };
		}
	}

	volatile boolean awaitingData;

	synchronized void flushCommands() {
		if (updateSensors && !awaitingData) {
			pendingCommands.add(new UpdateSensorsCommand());
			awaitingData = true;
		}
		while (pendingCommands.peek() != null && pendingCommands.peek().isDue()) {
			Command command = pendingCommands.poll();
			logger.info("Sending " + command.getClass().getSimpleName());
			byte[] cmd = command.toBytes();
			connection.send(cmd);
			synchronized (counterLock) {
				sent += cmd.length;
			}
		}
		// flush after this tick
		connection.flush();
	}

	private volatile long sent = 0;

	// bitmasks for various thingems
	static final int WHEELDROP_MASK = 0x1C;

	static final int BUMP_MASK = 0x03;

	static final int BUMPRIGHT_MASK = 0x01;

	static final int BUMPLEFT_MASK = 0x02;

	static final int WHEELDROPRIGHT_MASK = 0x04;

	static final int WHEELDROPLEFT_MASK = 0x08;

	static final int WHEELDROPCENT_MASK = 0x10;

	static final int MOVERDRIVELEFT_MASK = 0x10;

	static final int MOVERDRIVERIGHT_MASK = 0x08;

	static final int MOVERMAINBRUSH_MASK = 0x04;

	static final int MOVERVACUUM_MASK = 0x02;

	static final int MOVERSIDEBRUSH_MASK = 0x01;

	static final int POWERBUTTON_MASK = 0x08;

	static final int SPOTBUTTON_MASK = 0x04;

	static final int CLEANBUTTON_MASK = 0x02;

	static final int MAXBUTTON_MASK = 0x01;

	// which sensor packet, argument for sensors(int)
	static final int SENSORS_ALL = 0;

	static final int SENSORS_PHYSICAL = 1;

	static final int SENSORS_INTERNAL = 2;

	static final int SENSORS_POWER = 3;

	static final int REMOTE_NONE = 0xff;

	static final int REMOTE_POWER = 0x8a;

	static final int REMOTE_PAUSE = 0x89;

	static final int REMOTE_CLEAN = 0x88;

	static final int REMOTE_MAX = 0x85;

	static final int REMOTE_SPOT = 0x84;

	static final int REMOTE_SPINLEFT = 0x83;

	static final int REMOTE_FORWARD = 0x82;

	static final int REMOTE_SPINRIGHT = 0x81;

	// offsets into sensor_bytes data
	static final int BUMPSWHEELDROPS = 0;

	static final int WALL = 1;

	static final int CLIFFLEFT = 2;

	static final int CLIFFFRONTLEFT = 3;

	static final int CLIFFFRONTRIGHT = 4;

	static final int CLIFFRIGHT = 5;

	static final int VIRTUALWALL = 6;

	static final int MOTOROVERCURRENTS = 7;

	static final int DIRTLEFT = 8;

	static final int DIRTRIGHT = 9;

	static final int REMOTEOPCODE = 10;

	static final int BUTTONS = 11;

	static final int DISTANCE_HI = 12;

	static final int DISTANCE_LO = 13;

	static final int ANGLE_HI = 14;

	static final int ANGLE_LO = 15;

	static final int CHARGINGSTATE = 16;

	static final int VOLTAGE_HI = 17;

	static final int VOLTAGE_LO = 18;

	static final int CURRENT_HI = 19;

	static final int CURRENT_LO = 20;

	static final int TEMPERATURE = 21;

	static final int CHARGE_HI = 22;

	static final int CHARGE_LO = 23;

	static final int CAPACITY_HI = 24;

	static final int CAPACITY_LO = 25;

	public void onData(byte[] data) {
		model.setBumpSensor(isSet(data[BUMPSWHEELDROPS], BUMP_MASK));
		model.setWheelDropped(isSet(data[BUMPSWHEELDROPS], WHEELDROP_MASK));
		model.setWallSensor(data[WALL] != 0);
		model.setVoltage(toUnsignedShort(data[VOLTAGE_HI], data[VOLTAGE_LO]));
		model.setBatteryTemperature(data[TEMPERATURE]);
		model.setCurrent(toShort(data[CURRENT_HI], data[CURRENT_LO]));
		model.setOvercurrentMainBrush(isSet(data[MOTOROVERCURRENTS], MOVERMAINBRUSH_MASK));
		model.setOvercurrentSideBrush(isSet(data[MOTOROVERCURRENTS], MOVERSIDEBRUSH_MASK));
		model.setOvercurrentWheelLeft(isSet(data[MOTOROVERCURRENTS], MOVERDRIVELEFT_MASK));
		model.setOvercurrentWheelRight(isSet(data[MOTOROVERCURRENTS], MOVERDRIVERIGHT_MASK));
		model.setCharge(toUnsignedShort(data[CHARGE_HI], data[CHARGE_LO]));
		model.setCapacity(toUnsignedShort(data[CAPACITY_HI], data[CAPACITY_LO]));

		//TODO handle rest of the sensors
		awaitingData = false;
		synchronized (counterLock) {
			received += data.length;
		}
	}

	private boolean isSet(byte data, int bitMask) {
		return (data & bitMask) != 0;
	}

	private volatile long received = 0;
	
	Object counterLock = new Object();

	PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		if (SwingUtilities.isEventDispatchThread()) {
			// we wrap this listener to work in the dispatcht-thread
			pcs.addPropertyChangeListener(new AWTPropertyListenerWrapper(listener));
		}
		else {
			pcs.addPropertyChangeListener(listener);
		}
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}

	volatile long actualReceived;
	volatile long actualSent;

	class PropertyEventFirer implements Runnable {
		long lastFired = System.currentTimeMillis();
		public void run() {
			while (true) {
				long now = System.currentTimeMillis();
				long duration = now - lastFired;
				lastFired = now;
				if (duration <= 0) {
					// ignore very fast updates
					continue;
				}
				// bit/s
				synchronized (counterLock ) {
					// gives bit/sec
					actualReceived = received * 8000 / duration; 
					actualSent = sent * 8000 / duration;
					received = 0;
					sent = 0;
				}
				logger.debug("Fire change, received {}, sent {}", actualReceived, actualSent);
				pcs.firePropertyChange("received", null, actualReceived);
				pcs.firePropertyChange("sent", null, actualSent);

				try {
					// update every sec
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// we exit
					return;
				}
			}
		}
	};

	static public final int toUnsignedShort(byte hi, byte lo) {
		return (int) (hi & 0xff) << 8 | lo & 0xff;
	}

	static public final short toShort(byte hi, byte lo) {
		return (short) ((hi << 8) | (lo & 0xff));
	}
	
	public long getSent() {
		return actualSent;
	}

	public long getReceived() {
		return actualReceived;
	}
}
