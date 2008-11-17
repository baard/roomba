package no.rehn.roomba.tunes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// see http://merwin.bespin.org/t4a/specs/nokia_rtttl.txt
public class RTTTLParser {
	Logger logger = LoggerFactory.getLogger(getClass());

	static final HashMap<String, Integer> noteToMidi;
	static {
		noteToMidi = new HashMap<String, Integer>();
		noteToMidi.put("p", -1);
		noteToMidi.put("c", 0);
		noteToMidi.put("c#", 1);
		noteToMidi.put("d", 2);
		noteToMidi.put("d#", 3);
		noteToMidi.put("e", 4);
		noteToMidi.put("f", 5);
		noteToMidi.put("f#", 6);
		noteToMidi.put("g", 7);
		noteToMidi.put("g#", 8);
		noteToMidi.put("a", 9);
		noteToMidi.put("a#", 10);
		noteToMidi.put("b", 11);
		// norwegian alias h == b
		noteToMidi.put("h", 11);
	}

	// global defaults
	static final int DEFAULT_BPM = 63;

	static final int DEFAULT_SCALE = 6;

	static final int DEFAULT_DURATION = 4;

	public Song parse(String inputSong) {
		//remove some common errors
		String song = inputSong.trim();
		String parts[] = song.split(":");
		String songName = parts[0];
		String defaults[] = parts[1].toLowerCase().split("[,=]");
		String rawNotes[] = parts[2].toLowerCase().split(",");

		List<Note> notes = new ArrayList<Note>();

		int bpm = DEFAULT_BPM;
		int defaultScale = DEFAULT_SCALE;
		int defaultDuration = DEFAULT_DURATION;

		for (int i = 0; i < defaults.length; i++) {
			if (defaults[i].equals("b")) {
				bpm = Integer.parseInt(defaults[i + 1]);
			} else if (defaults[i].equals("o")) {
				defaultScale = Integer.parseInt(defaults[i + 1]);
			} else if (defaults[i].equals("d")) {
				defaultDuration = Integer.parseInt(defaults[i + 1]);
			}
		}

		logger.info("Parsed song '{}', bpm={}, default-scale={}, default-duration={}",
				new Object[] { songName, bpm, defaultScale, defaultDuration });

		for (String rawNote : rawNotes) {
			RTTTLNote parsedNote = parseNote(rawNote);
			Integer toneIndex = noteToMidi.get(parsedNote.getNote());
			if (toneIndex == null) {
				throw new IllegalArgumentException("Unknown note: "
						+ parsedNote.getNote());
			}
			int scale = parsedNote.getScale(defaultScale);
			int tone = 0; // default pause
			if (toneIndex != -1) {
				tone = toneIndex + 12 * scale;
			}
			int duration = parsedNote.getDuration(defaultDuration);
			duration = to64ths(bpm, duration, parsedNote.isSpecialDuration());
			if (lastDurationRounding < -0.5 || lastDurationRounding > 0.5) {
				logger.info("Correcting after excessive rounding: "+ lastDurationRounding);
				if (lastDurationRounding < 0) {
					duration += Math.round(lastDurationRounding);
				}
				else {
					duration -= Math.round(lastDurationRounding);
				}
				// keep non-adjusted rounding
				lastDurationRounding = lastDurationRounding - Math.round(lastDurationRounding);
				logger.info("Rounding after correction: "+ lastDurationRounding);
			}
			
			logger.info("Parsed {} as note {} with duration {}, note [duration={}, note={}, scale={}, specialDuration={}]", new Object[] {
					rawNote, tone, duration, parsedNote.getDuration(), parsedNote.getNote(), parsedNote.getScale(), parsedNote.isSpecialDuration()});
			notes.add(new Note(tone, duration));
		}
		return new Song(songName, notes);
	}
	
	double lastDurationRounding = 0;
	
	int to64ths(int bpm, int duration, boolean dotted) {
		double durationIn64ths = (60.0*4*64)/(duration*bpm);
		if (dotted){
			durationIn64ths = (60*4*64)/((duration*(1 + 0.5))*bpm);
		}
		double durationRounding = (((int) durationIn64ths) - durationIn64ths);
		logger.info("Rounding: " + durationRounding);
		lastDurationRounding += durationRounding;
		return (int) durationIn64ths;
	}

	final static Pattern toneMatcher = Pattern
			.compile("^(\\d+)*([a-gA-Gp]#?)(\\.)?(\\d)*(\\.)?$");

	RTTTLNote parseNote(String ringtone) {
		Matcher m = toneMatcher.matcher(ringtone);
		if (!m.find()) {
			throw new IllegalArgumentException("Cannot parse RTTTL note: "
					+ ringtone);
		}
		String note;
		Integer duration = null;
		Integer octave = null;
		boolean triplet = false;
		if (m.group(1) != null) {
			duration = Integer.parseInt(m.group(1));
		}
		note = m.group(2);
		if (m.group(3) != null && m.group(3).equals(".")) {
			triplet = true;
		}
		if (m.group(4) != null) {
			octave = Integer.parseInt(m.group(4));
		}
		if (m.group(5) != null && m.group(5).equals(".")) {
			triplet = true;
		}
		return new RTTTLNote(note, duration, octave, triplet);
	}
}
