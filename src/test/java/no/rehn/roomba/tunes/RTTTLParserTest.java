package no.rehn.roomba.tunes;

import junit.framework.TestCase;
import no.rehn.roomba.tunes.RTTTLParser;

public class RTTTLParserTest extends TestCase {
	RTTTLParser parser = new RTTTLParser();

	public void testSimpleTone() {
		RTTTLNote note = parser.parseNote("b");
		assertEquals("b", note.getNote());
		assertDefaults(note);
	}
	
	public void testPause() {
		RTTTLNote note = parser.parseNote("p");
		assertEquals("p", note.getNote());
		assertDefaults(note);
	}
	
	public void testTripletPlacement() {
		RTTTLNote note = parser.parseNote("e.6");
		assertEquals("e", note.getNote());
		assertTrue(note.isSpecialDuration());
		assertEquals(6, (int) note.getScale());
	}

	public void testIllegalNote() {
		try {
			parser.parseNote("ey6");
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	public void testToneWithDuration() {
		RTTTLNote note = parser.parseNote("10b");
		assertEquals("b", note.getNote());
		assertFalse(note.isSpecialDuration());
		assertEquals(10, (int) note.getDuration());
		assertNull(note.getScale());
	}

	public void testToneWithScale() {
		RTTTLNote note = parser.parseNote("b2");
		assertEquals("b", note.getNote());
		assertFalse(note.isSpecialDuration());
		assertNull(note.getDuration());
		assertEquals(2, (int) note.getScale());
	}

	public void testTripletTone() {
		RTTTLNote note = parser.parseNote("b.");
		assertEquals("b", note.getNote());
		assertTrue(note.isSpecialDuration());
		assertNull(note.getDuration());
		assertNull(note.getScale());
	}

	public void testCompleteTone() {
		RTTTLNote note = parser.parseNote("10b#2.");
		assertEquals("b#", note.getNote());
		assertTrue(note.isSpecialDuration());
		assertEquals(10, (int) note.getDuration());
		assertEquals(2, (int) note.getScale());
	}

	public void testToneWithHashSymbol() {
		RTTTLNote note = parser.parseNote("b#");
		assertEquals("b#", note.getNote());
		assertDefaults(note);
	}
	
	void assertDefaults(RTTTLNote note) {
		assertFalse(note.isSpecialDuration());
		assertNull(note.getDuration());
		assertNull(note.getScale());
	}
}
