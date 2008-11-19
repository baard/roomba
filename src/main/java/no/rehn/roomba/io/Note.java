package no.rehn.roomba.io;

//TODO doc
public class Note {
	final int note; // midi note number

	final int duration; // in 1/64 of a second

	public Note(int note, int duration) {
		this.note = note;
		this.duration = duration;
	}

	public int getNote() {
		return note;
	}

	public int getDuration() {
		return duration;
	}
}
