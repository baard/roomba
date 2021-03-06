package no.rehn.roomba.ai.examples;

import no.rehn.roomba.ai.AbstractRoombaProgram;
import no.rehn.roomba.ui.RoombaBean;
import no.rehn.roomba.ui.RoombaBean.Mode;

/**
 * Robot dance
 * <p>
 * Makes your Roomba dance.
 * 
 * @author Baard H. Rehn Johansen
 */
public class Dancer extends AbstractRoombaProgram {
	static final double MILLI_RADIANS = Math.PI * 2 / 1000;
	@Override
	public void onStart(RoombaBean roomba) {
		// must enable safe-mode before setting parameters
		roomba.setMode(Mode.SAFE);
	}
	
	@Override
	public void onExit(RoombaBean roomba) {
		// need to stop the dancing robot
		roomba.stop();
	}
	
	public void onTick(RoombaBean roomba, long currentTime) {
		// forward/backward in 2 sec sin-curve
		double velocity = Math.sin(MILLI_RADIANS * currentTime / 2);
		// scale to -127..127
		velocity = 127 * velocity;
		// left/right in 4 sec sin-curve
		double wheelDiff = Math.sin(MILLI_RADIANS * currentTime / 4);
		// scale to -63..63
		wheelDiff = 63 * wheelDiff;

		roomba.setVelocity((int) velocity);
		roomba.setWheelDiff((int) wheelDiff);
	}
	
	public String toString() {
		return "Robot dance";
	}
}
