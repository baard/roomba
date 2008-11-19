package no.rehn.roomba.ai.examples;

import no.rehn.roomba.ai.AbstractRoombaProgram;
import no.rehn.roomba.ui.RoombaBean;
import no.rehn.roomba.ui.RoombaBean.Mode;

public class PowerLedEffects extends AbstractRoombaProgram {
	static final double MILLI_RADIANS = Math.PI * 2 / 1000;

	public void onStart(RoombaBean roomba) {
		// must enable safe-mode before setting parameters
		roomba.setMode(Mode.SAFE);
		// enable the blue LED for show-off
		roomba.setDirt(true);
	}
	
	public void onExit(RoombaBean roomba) {
		// reset to defaults
		roomba.setPowerColor(RoombaBean.POWER_GREEN);
		roomba.setPowerIntensity(RoombaBean.POWER_MAX);
		roomba.setDirt(false);
	}
	
	public void onTick(RoombaBean roomba, long currentTime) {
		// sine-curve for color with 2 sec interval
		double color = Math.sin((double) currentTime / 2000 * 2 * Math.PI);
		// sine-curve for intensity with 5 sec interval
		double power = Math.sin((double) currentTime / 5000 * 2 * Math.PI);
		
		// scale value between 0 and 255
		color = (color + 1) * 127;
		power = (power + 1) * 127;
		roomba.setPowerColor((int) color);
		roomba.setPowerIntensity((int) power);
	}

	public String toString() {
		return "Cycles power-LEDs";
	}
}
