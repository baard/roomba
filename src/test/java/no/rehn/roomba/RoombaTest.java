package no.rehn.roomba;

import no.rehn.roomba.ui.RoombaBean;
import junit.framework.TestCase;

public class RoombaTest extends TestCase {
	RoombaBean roomba = new RoombaBean();
	public void testSetVelocity() {
		roomba.setVelocity(10);
		assertEquals(10, roomba.getVelocity());
		assertEquals(10, roomba.getLeftWheelSpeed());
		assertEquals(10, roomba.getRightWheelSpeed());
	}
	
	public void testWheelDiff() {
		roomba.setWheelDiff(10);
		assertEquals(-5, roomba.getRightWheelSpeed());
		assertEquals(5, roomba.getLeftWheelSpeed());
	}
	
	public void testWheelDiff2() {
		roomba.setLeftWheelSpeed(10);
		roomba.setRightWheelSpeed(20);
		roomba.setWheelDiff(0);
		assertEquals(roomba.getLeftWheelSpeed(), roomba.getRightWheelSpeed());
	}
}
