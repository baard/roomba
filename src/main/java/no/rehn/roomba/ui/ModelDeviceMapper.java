package no.rehn.roomba.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import no.rehn.roomba.io.Note;
import no.rehn.roomba.io.RoombaDevice;
import no.rehn.roomba.io.RoombaStatus;
import no.rehn.roomba.io.RoombaStatusEvent;
import no.rehn.roomba.io.RoombaStatusListener;
import no.rehn.roomba.tunes.Song;
import no.rehn.roomba.ui.RoombaBean.Mode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelDeviceMapper implements PropertyChangeListener {
	final Logger logger = LoggerFactory.getLogger(getClass());

	final RoombaBean model;
	
	int lastSongSlot = 1;
	
	final RoombaDevice device;
	
	final RoombaStatusListenerImpl statusListener = new RoombaStatusListenerImpl();

	final PriorityQueue<Command> pendingCommands = new PriorityQueue<Command>();

	volatile boolean awaitingData;

	volatile boolean updateSensors;

	public ModelDeviceMapper(RoombaBean model, RoombaDevice device) {
		this.model = model;
		this.device = device;
	}

	public void propertyChange(PropertyChangeEvent evt) {
		RoombaBean roomba = (RoombaBean) evt.getSource();
		String name = evt.getPropertyName();
		if ("powerIntensity".equals(name) || "powerColor".equals(name)
				|| "dirt".equals(name) || "max".equals(name)
				|| "clean".equals(name) || "spot".equals(name)) {
			enqueue(new LedCommand());
		}
		if ("power".equals(name)) {
			enqueue(new PowerDownCommand());
		}
		if ("wakeup".equals(name)) {
			enqueue(new WakeupCommand());
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
			}
		}
		if ("rightWheelSpeed".equals(name) || "leftWheelSpeed".equals(name)) {
			DriveWheelCommand command;
			synchronized(roomba.wheelLock) {
				command = new DriveWheelCommand(roomba.getLeftWheelSpeed(), roomba.getRightWheelSpeed());
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
				enqueue(new SaveSongCommand(new ArrayList<Note>(), 0));
				// remove all scheduled songs
				Iterator<Command> it = pendingCommands.iterator();
				while (it.hasNext()) {
					Command command = it.next();
					if (command instanceof SaveSongCommand) {
						logger.info("Removing song-command {} from queue", command);
						it.remove();
					}
				}
			}
			else {
				int nextSlot = (lastSongSlot - 1) % 4;
				logger.info("Using {} as next slot", nextSlot);
				enqueue(new SaveSongCommand(model.getSong().getNotes(), nextSlot));
				nextSlot++;
			}
		}
	}

	private void enqueue(Command command) {
		pendingCommands.remove(command);
		pendingCommands.add(command);
	}

	class SaveSongCommand extends Command {
		final List<Note> notes;
		final int slot;
		SaveSongCommand(List<Note> notes, int slot) {
			this.notes = notes;
			this.slot = slot;
		}
		
		@Override
		void execute() {
			List<Note> toPlay = notes;
			if (notes.size() > 16) {
				toPlay = toPlay.subList(0, 16);
				// we must split song and use multiple slots
				List<Note> nextPart = notes.subList(16, notes.size());
				long wait = Song.getLength(toPlay);
				int nextSlot = (slot % 4) + 1;
				logger.info("Enqueing for slot {} in {}ms", nextSlot, wait);
				enqueue(new SaveSongCommand(nextPart, nextSlot).setAwait(wait));
			}
			enqueue(new PlayCommand(slot).setAwait(250));
			device.saveSong(slot, toPlay);
		}
	}
	
	class StartCommand extends Command {
		@Override
		void execute() {
			device.startUp();
		}
	}

	class PlayCommand extends Command {
		final int slot;

		PlayCommand(int slot) {
			this.slot = slot;
		}
		
		@Override
		void execute() {
			device.playSong(slot);
		}
	}
	
	class WakeupCommand extends Command {
		@Override
		void execute() {
			device.wakeUp();
		}
	}

	class PowerDownCommand extends Command {
		@Override
		void execute() {
			device.powerDown();
		}
	}

	class DriveWheelCommand extends Command {
		final int leftSpeed;
		final int rightSpeed;
		DriveWheelCommand(int leftSpeed, int rightSpeed) {
			this.leftSpeed = leftSpeed;
			this.rightSpeed = rightSpeed;
		}
		
		@Override
		void execute() {
			device.setDriveWheelPwm(rightSpeed, leftSpeed);
		}
		
		@Override
		public boolean equals(Object obj) {
			return sameClass(obj);
		}
	}

	class PwmCommand extends Command {
		@Override
		void execute() {
			device.setBrushAndVacuumPwm(model.getMainBrushPwm(), model.getSideBrushPwm(), model.getVacuumPwm());
		}

		@Override
		public boolean equals(Object obj) {
			return sameClass(obj);
		}
	}

	class LedDigitsCommand extends Command {
		@Override
		void execute() {
			String toDisplay = model.getText();
			while (toDisplay.length() < 4) {
				// pad to 4 digits
				toDisplay += " ";
			}
			device.setDigitLeds(toDisplay.charAt(0), toDisplay.charAt(1), toDisplay.charAt(2), toDisplay.charAt(3));
		}
		
		@Override
		public boolean equals(Object obj) {
			return sameClass(obj);
		}
	}

	class ModeCommand extends Command {
		@Override
		void execute() {
			switch (model.getMode()) {
			case SAFE:
				device.enterSafeMode();
				break;
			case FULL:
				device.enterFullMode();
				break;
			default:
				throw new IllegalStateException("Cannot set mode "
						+ model.getMode());
			}
		}
	}

	class LedCommand extends Command {
		@Override
		void execute() {
			device.setLeds(model.isSpot(), model.isClean(), model.isDirt(),
					model.isMax(), model.getPowerIntensity(), model
							.getPowerColor());
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
		
		abstract void execute();

		public int compareTo(Command o) {
			return (int) (executeAfter - o.executeAfter);
		}

		// util
		boolean sameClass(Object obj) {
			if (obj == null) {
				return false;
			}
			return getClass() == obj.getClass();
		}
	}

	class UpdateSensorsCommand extends Command {
		@Override
		void execute() {
			device.updateSensors(0, statusListener);
		}
		
		@Override
		public boolean equals(Object obj) {
			return sameClass(obj);
		}
	}

	public void flushCommands() {
		if (updateSensors && !awaitingData) {
			pendingCommands.add(new UpdateSensorsCommand());
			awaitingData = true;
		}
		while (pendingCommands.peek() != null && pendingCommands.peek().isDue()) {
			Command command = pendingCommands.poll();
			logger.info("Sending " + command.getClass().getSimpleName());
			command.execute();
		}
	}

	class RoombaStatusListenerImpl implements RoombaStatusListener {
		public void onStatusUpdate(RoombaStatusEvent event) {
			RoombaStatus status = event.getStatus();
			model.setBumpSensor(status.isBumped());
			model.setWheelDropped(status.isWheelDropped());
			model.setWallSensor(status.isWallDetected());
			model.setBatteryTemperature(status.getBatteryTemperature());
			model.setCurrent(status.getCurrent());
			model.setOvercurrentMainBrush(status.isMainBrushOvercurrent());
			model.setOvercurrentSideBrush(status.isSideBrushOvercurrent());
			model.setOvercurrentWheelLeft(status.isLeftWheelOvercurrent());
			model.setOvercurrentWheelRight(status.isRightWheelOvercurrent());
			model.setCharge(status.getBatteryCharge());
			model.setCapacity(status.getBatteryCapacity());
			model.setVoltage(status.getVoltage());
			model.setCliffLeft(status.isCliffLeft());
			model.setCliffRight(status.isCliffRight());
			model.setCliffLeftFront(status.isCliffFrontLeft());
			model.setCliffRightFront(status.isCliffFrontRight());
			
			//TODO map rest
			awaitingData = false;
		}
	}
}
