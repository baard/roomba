package no.rehn.roomba.ai.examples;

import no.rehn.roomba.Roomba;
import no.rehn.roomba.Roomba.Mode;
import no.rehn.roomba.ai.AbstractRoombaProgram;

public class PowerLedEffects extends AbstractRoombaProgram {
	static final double MILLI_RADIANS = Math.PI * 2 / 1000;

	public void onStart(Roomba roomba) {
		// must enable safe-mode before setting parameters
		roomba.setMode(Mode.SAFE);
		// enable the blue LED for show-off
		roomba.setDirt(true);
	}
	
	public void onExit(Roomba roomba) {
		// reset to defaults
		roomba.setPowerColor(Roomba.POWER_GREEN);
		roomba.setPowerIntensity(Roomba.POWER_MAX);
		roomba.setDirt(false);
	}
	
	public void onTick(Roomba roomba, long currentTime) {
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
