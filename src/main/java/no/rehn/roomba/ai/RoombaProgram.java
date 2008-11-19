package no.rehn.roomba.ai;

import no.rehn.roomba.ui.RoombaBean;

/**
 * A Roomba Program
 * <p>
 * Make your robot sing, dance or vacuum you way!
 *  
 * @author Baard H. Rehn Johansen
 */
public interface RoombaProgram {

	/**
	 * This method is called when the program is started.
	 * No ticks will occur before this method returns.
	 * 
	 * @param roomba The robot to operate
	 */
	void onStart(RoombaBean roomba);

	/**
	 * This method is called when the program is stopped.
	 * No ticks will occur after this method is called.
	 * 
	 * @param roomba The robot to operate
	 */
	void onExit(RoombaBean roomba);
	
	/**
	 * This method is called every n-th millisecond,
	 * depending on the system that runs the program.
	 * 
	 * @param roomba The robot to operate
	 * @param currentTime The current time for this tick in millis
	 */
	void onTick(RoombaBean roomba, long currentTime);
	
	/**
	 * Provide a description of the program. To be used
	 * as display when running the program.
	 * 
	 * @return a description of the program
	 */
	String toString();
}
