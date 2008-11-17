package no.rehn.roomba.ai;

import no.rehn.roomba.Roomba;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base-class for Roomba programs
 * <p>
 * Provides logger and default implementation of lifecycle-methods.
 * 
 * @author Baard H. Rehn Johansen
 */
public abstract class AbstractRoombaProgram implements RoombaProgram {
	protected Logger logger = LoggerFactory.getLogger(getClass());
	
	/**
	 * Override in subclasses
	 */
	public void onExit(Roomba roomba) {
	}

	/**
	 * Override in subclasses
	 */
	public void onStart(Roomba roomba) {
	}

	/**
	 * Override in subclasses
	 */
	public void onTick(Roomba roomba, long currentTime) {
	}
}
