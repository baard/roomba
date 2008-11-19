package no.rehn.roomba.io;

import java.io.IOException;
import java.io.InputStream;

//TODO doc
public interface RoombaConnectionListener {
	void dataAvailable(InputStream in) throws IOException;
}
