package no.rehn.roomba.io;

import java.util.EventObject;

//TODO doc
public class RoombaStatusEvent extends EventObject {
	private static final long serialVersionUID = 1L;

	final RoombaStatus status;
	
	public RoombaStatusEvent(RoombaDevice source, RoombaStatus status) {
		super(source);
		this.status = status;
	}

	public RoombaStatus getStatus() {
		return status;
	}
}
