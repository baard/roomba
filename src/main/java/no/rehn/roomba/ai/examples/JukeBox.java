package no.rehn.roomba.ai.examples;

import java.util.ArrayList;
import java.util.List;

import no.rehn.roomba.Roomba;
import no.rehn.roomba.Roomba.Mode;
import no.rehn.roomba.ai.AbstractRoombaProgram;
import no.rehn.roomba.tunes.RTTTLParser;
import no.rehn.roomba.tunes.Song;

/**
 * A Jukebox
 * <p>
 * Plays listed songs in random order, and schedules new
 * songs when previous song has been played.
 * 
 * @author Baard H. Rehn Johansen
 */
public class JukeBox extends AbstractRoombaProgram {
	final static String[] RTTTL_SONGS = {
		"Muppet show theme:d=4,o=5,b=250:c6,c6,a,b,8a,b,g,p,c6,c6,a,8b,8a,8p,g.,p,e,e,g,f,8e,f,8c6,8c,8d,e,8e,8e,8p,8e,g,2p,c6,c6,a,b,8a,b,g,p,c6,c6,a,8b,a,g.,p,e,e,g,f,8e,f,8c6,8c,8d,e,8e,d,8d,c",
		"Axel Foley:d=4,o=5,b=160:f#,8a.,8f#,16f#,8a#,8f#,8e,f#,8c.6,8f#,16f#,8d6,8c#6,8a,8f#,8c#6,8f#6,16f#,8e,16e,8c#,8g#,f#.",
		"Take on me:d=4,o=4,b=160:8f#5,8f#5,8f#5,8d5,8p,8b,8p,8e5,8p,8e5,8p,8e5,8g#5,8g#5,8a5,8b5,8a5,8a5,8a5,8e5,8p,8d5,8p,8f#5,8p,8f#5,8p,8f#5,8e5,8e5,8f#5,8e5,8f#5,8f#5,8f#5,8d5,8p,8b,8p,8e5,8p,8e5,8p,8e5,8g#5,8g#5,8a5,8b5,8a5,8a5,8a5,8e5,8p,8d5,8p,8f#5,8p,8f#5,8p,8f#5,8e5,8e5",
		"Bullet me:d=4,o=5,b=112:b.6,g.6,16f#6,16g6,16f#6,8d.6,8e6,p,16e6,16f#6,16g6,8f#.6,8g6,8a6,b.6,g.6,16f#6,16g6,16f#6,8d.6,8e6,p,16c6,16b,16a,16b",
		"Indiana Jones:d=4,o=5,b=250:e,8p,8f,8g,8p,1c6,8p.,d,8p,8e,1f,p.,g,8p,8a,8b,8p,1f6,p,a,8p,8b,2c6,2d6,2e6,e,8p,8f,8g,8p,1c6,p,d6,8p,8e6,1f.6,g,8p,8g,e.6,8p,d6,8p,8g,e.6,8p,d6,8p,8g,f.6,8p,e6,8p,8d6,2c6",
		"KnightRider:d=4,o=5,b=125:16e,16p,16f,16e,16e,16p,16e,16e,16f,16e,16e,16e,16d#,16e,16e,16e,16e,16p,16f,16e,16e,16p,16f,16e,16f,16e,16e,16e,16d#,16e,16e,16e,16d,16p,16e,16d,16d,16p,16e,16d,16e,16d,16d,16d,16c,16d,16d,16d,16d,16p,16e,16d,16d,16p,16e,16d,16e,16d,16d,16d,16c,16d,16d,16d"
	};

	// 2 sec between each song
	final static long PAUSE = 2000;

	final List<Song> songs = new ArrayList<Song>();
	
	public JukeBox() {
		RTTTLParser parser = new RTTTLParser();
		for (String song : RTTTL_SONGS) {
			songs.add(parser.parse(song));
		}
	}
	
	public void onExit(Roomba roomba) {
		// stops the currently playing song
		roomba.setSong(null);
	}

	public void onStart(Roomba roomba) {
		// must enable safe-mode before setting parameters
		roomba.setMode(Mode.SAFE);
		nextStart = 0;
	}
	

	long nextStart;
	public void onTick(Roomba roomba, long currentTime) {
		if (currentTime > nextStart) {
			// enqueue next song
			int randomSong = (int) (Math.random() * songs.size());
			Song song = songs.get(randomSong);
			logger.info("Playing random song #{}: {}", randomSong, song.getName());
			roomba.setSong(song);
			long songLength = song.getLengthInMillis();
			nextStart = currentTime + songLength + PAUSE;
			logger.info("Next song due in {}ms", nextStart - currentTime);
		}
	}
	
	public String toString() {
		return "Plays some tunes";
	}
}
