package no.rehn.roomba.tunes;

import java.util.List;


public class Song {
	List<Note> notes;
	String name;
	
	public Song(String name, List<Note> notes) {
		this.notes = notes;
		this.name = name;
	}

	public List<Note> getNotes() {
		return notes;
	}
	
	public String getName() {
		return name;
	}

	public long getLengthInMillis() {
		return getLength(notes);
	}
	
	public static long getLength(List<Note> notes) {
		int lengthSum = 0;
		for (int i = 0; i < notes.size(); i++) {
			lengthSum += notes.get(i).getDuration();
		}
		long wait = lengthSum * 1000 / 64;
		return wait;
	}
}
