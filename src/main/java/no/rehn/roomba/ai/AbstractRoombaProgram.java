package no.rehn.roomba.ai;

import no.rehn.roomba.ui.RoombaBean;

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
	public void onExit(RoombaBean roomba) {
	}

	/**
	 * Override in subclasses
	 */
	public void onStart(RoombaBean roomba) {
	}

	/**
	 * Override in subclasses
	 */
	public void onTick(RoombaBean roomba, long currentTime) {
	}
}
