package no.rehn.roomba.io;

import java.util.EventListener;

public interface RoombaStatusListener extends EventListener {
	void onStatusUpdate(RoombaStatusEvent event);
}
