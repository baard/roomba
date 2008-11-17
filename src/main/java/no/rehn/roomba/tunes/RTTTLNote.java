package no.rehn.roomba.tunes;

class RTTTLNote {
	final String note;
	final Integer duration;
	final Integer scale;
	final boolean specialDuration;
	
	RTTTLNote(String note, Integer duration, Integer scale, boolean specialDuration) {
		this.note = note;
		this.duration = duration;
		this.scale = scale;
		this.specialDuration = specialDuration;
	}
	
	public boolean isSpecialDuration() {
		return specialDuration;
	}
	
	public Integer getDuration() {
		return duration;
	}
	
	public int getDuration(int defaultDuration) {
		return duration != null ? duration : defaultDuration;
	}
	
	public String getNote() {
		return note;
	}
	
	public Integer getScale() {
		return scale;
	}
	
	public int getScale(int defaultScale) {
		return scale != null ? scale : defaultScale;
	}
}
