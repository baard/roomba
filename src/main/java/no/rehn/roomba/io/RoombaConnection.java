package no.rehn.roomba.io;



public interface RoombaConnection {
	void open();
	void close();
	void send(byte... bytes);
	void flush();
	//TODO change to add/remove
	void setListener(RoombaInputListener listener);
}
