package no.rehn.roomba.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO doc
public class Roomba5xxDevice implements RoombaDevice {
	final Logger logger = LoggerFactory.getLogger(getClass());

	final RoombaConnection connection;

	final RoombaConnectionListenerImpl connectionListener;
	
	volatile long sentByteCounter;
	volatile long receivedByteCounter;

	public Roomba5xxDevice(RoombaConnection connection) {
		this.connection = connection;
		this.connectionListener = new RoombaConnectionListenerImpl(connection);
	}
	
	//TODO document
	public void wakeUp() {
		connection.close();
		connection.open();
	}

	/**
	 * <pre>
	 * LEDs                                               Opcode: 139                           Data Bytes: 3
	 * 	 This command controls the LEDs common to all models of Roomba 500. The Clean/Power LED is
	 * 	 specified by two data bytes: one for the color and the other for the intensity.
	 * 	 •    Serial sequence: [139] [LED Bits] [Clean/Power Color] [Clean/Power Intensity]
	 * 	 •    Available in modes: Safe or Full
	 * 	 •    Changes mode to: No Change
	 * 	 •    LED Bits (0 – 255)
	 * 	 Home and Spot use green LEDs: 0 = off, 1 = on
	 * 	 Check Robot uses an orange LED.
	 * 	 Debris uses a blue LED.
	 * 	 Clean/Power uses a bicolor (red/green) LED. The intensity and color of this LED can be controlled with
	 * 	 8-bit resolution.
	 * 	 LED Bits (0-255)
	 * 	 TODO copy table
	 * 	 Clean/Power LED Color (0 – 255)
	 * 	 0 = green, 255 = red. Intermediate values are intermediate colors (orange, yellow, etc).
	 * 	 Clean/Power LED Intensity (0 – 255)
	 * 	 0 = off, 255 = full intensity. Intermediate values are intermediate intensities.
	 * </pre>
	 */
	// TODO document and validate input
	public void setLeds(boolean spot, boolean clean, boolean dirt, boolean max,
			int powerIntensity, int powerColor) {
		int led = (spot ? 0x08 : 0) | (clean ? 0x04 : 0) | (max ? 0x02 : 0)
				| (dirt ? 0x01 : 0);
		send((byte) 139, (byte) led, (byte) powerColor, (byte) powerIntensity);
	}

	/**
	 * <pre>
	 * Song                                                 Opcode: 140                       Data Bytes: 2N+2,
	 * 	 where N is the number of notes in the song
	 * 	 This command lets you specify up to four songs to the OI that you can play at a later time. Each song is
	 * 	 associated with a song number. The Play command uses the song number to identify your song selection.
	 * 	 Each song can contain up to sixteen notes. Each note is associated with a note number that uses MIDI
	 * 	 note definitions and a duration that is specified in fractions of a second. The number of data bytes varies,
	 * 	 depending on the length of the song specified. A one note song is specified by four data bytes. For each
	 * 	 additional note within a song, add two data bytes.
	 * 	 •   Serial sequence: [140] [Song Number] [Song Length] [Note Number 1] [Note Duration 1] [Note
	 * 	 Number 2] [Note Duration 2], etc.
	 * 	 •   Available in modes: Passive, Safe, or Full
	 * 	 •   Changes mode to: No Change
	 * 	 •   Song Number (0 – 4)
	 * 	 The song number associated with the specific song. If you send a second Song command, using the
	 * 	 same song number, the old song is overwritten.
	 * 	 •   Song Length (1 – 16)
	 * 	 The length of the song, according to the number of musical notes within the song.
	 * 	 •   Song data bytes 3, 5, 7, etc.: Note Number (31 – 127)
	 * 	 The pitch of the musical note Roomba will play, according to the MIDI note numbering scheme. The
	 * 	 lowest musical note that Roomba will play is Note #31. Roomba considers all musical notes outside
	 * 	 the range of 31 – 127 as rest notes, and will make no sound during the duration of those notes.
	 * 	 •   Song data bytes 4, 6, 8, etc.: Note Duration (0 – 255)
	 * 	 The duration of a musical note, in increments of 1/64th of a second. Example: a half-second long
	 * 	 musical note has a duration value of 32.
	 * </pre>
	 */
	// TODO document and validate input
	public void saveSong(int songNumber, List<Note> notes) {
		int[] song = parseNotes(notes);
		int length = song.length;
		int songlength = length / 2;
		byte[] cmd = new byte[length + 3];
		cmd[0] = (byte) 140;
		cmd[1] = (byte) songNumber;
		cmd[2] = (byte) songlength;
		for (int i = 0; i < length; i++) {
			cmd[3 + i] = (byte) song[i];
		}
		send(cmd);
	}

	int[] parseNotes(List<Note> notes) {
		int[] ints = new int[notes.size() * 2];
		int index = 0;
		for (Note note : notes) {
			ints[index] = note.getNote();
			ints[index + 1] = note.getDuration();
			index += 2;
		}
		return ints;
	}

	/**
	 * <pre>
	 * Play                                            Opcode: 141                           Data Bytes: 1
	 * 	 This command lets you select a song to play from the songs added to Roomba using the Song command.
	 * 	 You must add one or more songs to Roomba using the Song command in order for the Play command to
	 * 	 work.
	 * 	 •   Serial sequence: [141] [Song Number]
	 * 	 •   Available in modes: Safe or Full
	 * 	 •   Changes mode to: No Change
	 * 	 •   Song Number (0 – 4)
	 * 	 The number of the song Roomba is to play.
	 * </pre>
	 */
	// TODO document and validate input
	public void playSong(int songNumber) {
		send((byte) 141, (byte) songNumber);
	}

	/**
	 * <pre>
	 * Power                                          Opcode: 133                           Data Bytes: 0
	 * 	 This command powers down Roomba. The OI can be in Passive, Safe, or Full mode to accept this
	 * 	 command.
	 * 	 •   Serial sequence: [133]
	 * 	 •   Available in modes: Passive, Safe, or Full
	 * 	 •   Changes mode to: Passive
	 * </pre>
	 */
	// TODO document
	public void powerDown() {
		send((byte) 133);
	}

	/**
	 * <pre>
	 * Start                                          Opcode: 128                             Data Bytes: 0
	 * 	 This command starts the OI. You must always send the Start command before sending any other
	 * 	 commands to the OI.
	 * 	 •   Serial sequence: [128].
	 * 	 •   Available in modes: Passive, Safe, or Full
	 * 	 •   Changes mode to: Passive. Roomba beeps once to acknowledge it is starting from “off” mode.
	 * 	
	 * </pre>
	 */
	// TODO document
	public void startUp() {
		send((byte) 128);
	}

	/**
	 * <pre>
	 * Drive PWM                                         Opcode: 146                              Data Bytes: 4
	 * 	 This command lets you control the raw forward and backward motion of Roomba’s drive wheels
	 * 	 independently. It takes four data bytes, which are interpreted as two 16-bit signed values using two’s
	 * 	 complement. The first two bytes specify the PWM of the right wheel, with the high byte sent first. The
	 * 	 next two bytes specify the PWM of the left wheel, in the same format. A positive PWM makes that wheel
	 * 	 drive forward, while a negative PWM makes it drive backward.
	 * 	 •   Serial sequence: [146] [Right PWM high byte] [Right PWM low byte] [Left PWM high byte] [Left PWM
	 * 	 low byte]
	 * 	 •   Available in modes: Safe or Full
	 * 	 •   Changes mode to: No Change
	 * 	 •   Right wheel PWM (-255 – 255)
	 * 	 •   Left wheel PWM (-255 – 255)
	 * </pre>
	 */
	// TODO document and validate input
	public void setDriveWheelPwm(int rightWheel, int leftWheel) {
		send((byte) 146, (byte) (rightWheel >>> 8), (byte) (rightWheel & 0xFF),
				(byte) (leftWheel >>> 8), (byte) (leftWheel & 0xFF));
	}

	/**
	 * <pre>
	 * PWM Motors                                          Opcode: 144                                Data Bytes: 3
	 * 	 This command lets you control the speed of Roomba’s main brush, side brush, and vacuum
	 * 	 independently. With each data byte, you specify the duty cycle for the low side driver (max 128). For
	 * 	 example, if you want to control a motor with 25% of battery voltage, choose a duty cycle of 128 * 25%
	 * 	 = 32. The main brush and side brush can be run in either direction. The vacuum only runs forward.
	 * 	 Positive speeds turn the motor in its default (cleaning) direction. Default direction for the side brush is
	 * 	 counterclockwise. Default direction for the main brush/flapper is inward.
	 * 	 Serial sequence: [144] [Main Brush PWM] [Side Brush PWM] [Vacuum PWM]
	 * 	 •   Available in modes: Safe or Full
	 * 	 •   Changes mode to: No Change
	 * 	 •   Main Brush and Side Brush duty cycle (-127 – 127)
	 * 	 •   Vacuum duty cycle (0 – 127)
	 * </pre>
	 */
	// TODO document and validate input
	public void setBrushAndVacuumPwm(int mainBrush, int sideBrush, int vacuum) {
		send((byte) 144, (byte) mainBrush, (byte) sideBrush, (byte) vacuum);
	}

	/**
	 * <pre>
	 * Digit LEDs ASCII                                   Opcode: 164                             Data Bytes: 4
	 * 	 This command controls the four 7 segment displays on the Roomba 560 and 570 using ASCII character
	 * 	 codes. Because a 7 segment display is not sufficient to display alphabetic characters properly, all
	 * 	 characters are an approximation, and not all ASCII codes are implemented.
	 * 	 •   Serial sequence: [164] [Digit 3 ASCII] [Digit 2 ASCII] [Digit 1 ASCII] [Digit 0 ASCII]
	 * 	 •   Available in modes: Safe or Full
	 * 	 •   Changes mode to: No Change
	 * 	 •   Digit N ASCII (32 – 126)
	 * 	 •   All use red LEDs. Digits are ordered from left to right on the robot 3,2,1,0.
	 * 	 Example:
	 * 	 To write ABCD to the display, send the serial byte sequence: [164] [65] [66] [67] [68]
	 * </pre>
	 */
	// TODO document and validate input
	public void setDigitLeds(char digit1, char digit2, char digit3, char digit4) {
		send((byte) 164, (byte) digit1, (byte) digit2, (byte) digit3,
				(byte) digit4);
	}
	
	/**
	 * <pre>
	 * Safe                                               Opcode: 131                             Data Bytes: 0
	 * 	 This command puts the OI into Safe mode, enabling user control of Roomba. It turns off all LEDs. The OI
	 * 	 can be in Passive, Safe, or Full mode to accept this command. If a safety condition occurs (see above)
	 * 	 Roomba reverts automatically to Passive mode.
	 * 	 •   Serial sequence: [131]
	 * 	 •   Available in modes: Passive, Safe, or Full
	 * 	 •   Changes mode to: Safe
	 * 	 Note: The effect and usage of the Control command (130) are identical to the Safe command.
	 * 	
	 * </pre>
	 */
	// TODO document
	public void enterSafeMode() {
		send((byte) 131);
	}
	
	/**
	 * <pre>
	 * Full                                                Opcode: 132                            Data Bytes: 0
	 * 	 This command gives you complete control over Roomba by putting the OI into Full mode, and turning off
	 * 	 the cliff, wheel-drop and internal charger safety features. That is, in Full mode, Roomba executes any
	 * 	 command that you send it, even if the internal charger is plugged in, or command triggers a cliff or wheel
	 * 	 drop condition.
	 * 	 •   Serial sequence: [132]
	 * 	 •   Available in modes: Passive, Safe, or Full
	 * 	 •   Changes mode to: Full
	 * 	 Note: Use the Start command (128) to change the mode to Passive.
	 * 	
	 * </pre>
	 */
	// TODO document
	public void enterFullMode() {
		send((byte) 132);
	}
	
	/**
	 * <pre>
	 * Sensors                                            Opcode: 142                            Data Bytes: 1
	 * 	 This command requests the OI to send a packet of sensor data bytes. There are 58 different sensor data
	 * 	 packets. Each provides a value of a specific sensor or group of sensors.
	 * 	 For more information on sensor packets, refer to the next section, “Roomba Open Interface Sensors
	 * 	 Packets”.
	 * 	 •   Serial sequence: [142] [Packet ID]
	 * 	 •   Available in modes: Passive, Safe, or Full
	 * 	 •   Changes mode to: No Change
	 * 	 •   Packet ID: Identifies which of the 58 sensor data packets should be sent back by the OI. A value of
	 * 	 100 indicates a packet with all of the sensor data. Values of 0 through 6 and 101 through 107
	 * 	 indicate specific subgroups of the sensor data.
	 * 	
	 * </pre>
	 */
	//TODO document and validate input
	public void updateSensors(int packetGroup, RoombaStatusListener callback) {
		connectionListener.requestedPacketGroup = (byte) packetGroup;
		connectionListener.statusListener = callback;
		send((byte) 142, (byte) packetGroup);
	}

	void send(byte... bytes) {
		sentByteCounter += bytes.length;
		connection.send(bytes);
	}
	
	public long getReceivedBytes() {
		return receivedByteCounter;
	}
	
	public long getSentBytes() {
		return sentByteCounter;
	}
	
	class RoombaConnectionListenerImpl implements RoombaConnectionListener {
		volatile RoombaStatusListener statusListener;
		volatile byte requestedPacketGroup;
		
		RoombaConnectionListenerImpl(RoombaConnection connection) {
			connection.setListener(this);
		}

		public void dataAvailable(InputStream in) throws IOException {
			//TODO handle multiple sensors-packet sizes
			byte[] inputBuffer = new byte[26];
			for (int index = 0; index < inputBuffer.length; index++) {
				inputBuffer[index] = (byte) in.read();
				logger.trace("Received: {}", inputBuffer[index]);
				receivedByteCounter++;
			}
			logger.debug("Dispatching event to statuslistener");
			//TODO should dispatch event in own thread, to be able to handle new data quickly?
			statusListener.onStatusUpdate(new RoombaStatusEvent(Roomba5xxDevice.this, new RoombaStatusImpl(inputBuffer)));
		}
	}

	class RoombaStatusImpl implements RoombaStatus {
		final byte[] data;
		
		RoombaStatusImpl(final byte[] data) {
			this.data = data;
		}
		
	// which sensor packet, argument for sensors(int)
//	static final int SENSORS_ALL = 0;
//	static final int SENSORS_PHYSICAL = 1;
//	static final int SENSORS_INTERNAL = 2;
//	static final int SENSORS_POWER = 3;

//	static final int REMOTE_NONE = 0xff;
//	static final int REMOTE_POWER = 0x8a;
//	static final int REMOTE_PAUSE = 0x89;
//	static final int REMOTE_CLEAN = 0x88;
//	static final int REMOTE_MAX = 0x85;
//	static final int REMOTE_SPOT = 0x84;
//	static final int REMOTE_SPINLEFT = 0x83;
//	static final int REMOTE_FORWARD = 0x82;
//	static final int REMOTE_SPINRIGHT = 0x81;

	// offsets into sensor_bytes data
	static final int BUMPSWHEELDROPS = 0;
	static final int WALL = 1;
	static final int CLIFFLEFT = 2;
	static final int CLIFFFRONTLEFT = 3;
	static final int CLIFFFRONTRIGHT = 4;
	static final int CLIFFRIGHT = 5;
//	static final int VIRTUALWALL = 6;
	static final int MOTOROVERCURRENTS = 7;
//	static final int DIRTLEFT = 8;
//	static final int DIRTRIGHT = 9;
//	static final int REMOTEOPCODE = 10;
//	static final int BUTTONS = 11;
//	static final int DISTANCE_HI = 12;
//	static final int DISTANCE_LO = 13;
//	static final int ANGLE_HI = 14;
//	static final int ANGLE_LO = 15;
//	static final int CHARGINGSTATE = 16;
	static final int VOLTAGE_HI = 17;
	static final int VOLTAGE_LO = 18;
	static final int CURRENT_HI = 19;
	static final int CURRENT_LO = 20;
	static final int TEMPERATURE = 21;
	static final int CHARGE_HI = 22;
	static final int CHARGE_LO = 23;
	static final int CAPACITY_HI = 24;
	static final int CAPACITY_LO = 25;

		/**
		 * <pre>
		 * Voltage                                          Packet ID: 22          Data Bytes: 2, unsigned
		 * 		 This code indicates the voltage of Roomba’s battery in millivolts (mV).
		 * 		 Range: 0 – 65535 mV
		 * </pre>
		 */
		public int getVoltage() {
			return toUnsignedShort(data[VOLTAGE_HI], data[VOLTAGE_LO]);
		}
		
		/**
		 * <pre>
		 * Bumps and Wheel Drops                         Packet ID: 7               Data Bytes: 1, unsigned
		 * 		 The state of the bumper (0 = no bump, 1 = bump) and wheel drop sensors (0 = wheel raised, 1 = wheel
		 * 		 dropped) are sent as individual bits.
		 * 		 Range: 0 – 15
		 * 		 TODO table
		 * </pre>
		 */
		public byte getBumpAndWheelDrops() {
			return data[BUMPSWHEELDROPS];
		}

		public boolean isWheelDropped() {
			return isSet(getBumpAndWheelDrops(), 0x1C);
		}

		public boolean isBumped() {
			return isSet(getBumpAndWheelDrops(), 0x03);
		}

		/**
		 * <pre>
		 * Wall                                                Packet ID: 8                 Data Bytes: 1, unsigned
		 * The state of the wall sensor is sent as a 1 bit value (0 = no wall, 1 = wall seen).
		 * Range: 0 – 1
		 * </pre>
		 */
		public boolean isWallDetected() {
			return data[WALL] != 0;
		}

		/**
		 * <pre>
		 * Temperature                                    Packet ID: 24 Data Bytes: 1, signed
		 * 		 The temperature of Roomba’s battery in degrees Celsius.
		 * 		 Range: -128 – 127
		 * </pre>
		 */
		public int getBatteryTemperature() {
			return data[TEMPERATURE];
		}

		/**
		 * <pre>
		 * Battery Capacity                               Packet ID: 26               Data Bytes: 2, unsigned
		 * 		 The estimated charge capacity of Roomba’s battery in milliamp-hours (mAh).
		 * 		 Range: 0 – 65535 mAh
		 * </pre>
		 */
		public int getBatteryCapacity() {
			return toUnsignedShort(data[CAPACITY_HI], data[CAPACITY_LO]);
		}

		/**
		 * <pre>
		 * Battery Charge                                   Packet ID: 25                Data Bytes: 2, unsigned
		 * 		 The current charge of Roomba’s battery in milliamp-hours (mAh). The charge value decreases as the
		 * 		 battery is depleted during running and increases when the battery is charged.
		 * 		 Range: 0 – 65535 mAh
		 * 		
		 * </pre>
		 */
		public int getBatteryCharge() {
			return toUnsignedShort(data[CHARGE_HI], data[CHARGE_LO]);
		}

		/**
		 * <pre>
		 * Current                                            Packet ID: 23                    Data Bytes: 2, signed
		 * 		 The current in milliamps (mA) flowing into or out of Roomba’s battery. Negative currents indicate that the
		 * 		 current is flowing out of the battery, as during normal running. Positive currents indicate that the current
		 * 		 is flowing into the battery, as during charging.
		 * 		 Range: -32768 – 32767 mA
		 * </pre>
		 */
		public int getCurrent() {
			return toShort(data[CURRENT_HI], data[CURRENT_LO]);
		}

		/**
		 * <pre>
		 * Wheel Overcurrents                                Packet ID: 14                 Data Bytes: 1, unsigned
		 * 		 The state of the four wheel overcurrent sensors are sent as individual bits (0 = no overcurrent, 1 =
		 * 		 overcurrent). There is no overcurrent sensor for the vacuum on Roomba 500.
		 * 		 Range: 0 – 31
		 * 		 TODO table
		 * </pre>
		 */
		public byte getOvercurrents() {
			return data[MOTOROVERCURRENTS];
		}

		public boolean isLeftWheelOvercurrent() {
			return isSet(getOvercurrents(), 0x10);
		}

		public boolean isMainBrushOvercurrent() {
			return isSet(getOvercurrents(), 0x04);
		}

		public boolean isRightWheelOvercurrent() {
			return isSet(getOvercurrents(), 0x08);
		}

		public boolean isSideBrushOvercurrent() {
			return isSet(getOvercurrents(), 0x01);
		}

		/**
		 * <pre>
		 * Cliff Left                                           Packet ID: 9                 Data Bytes: 1, unsigned
		 * 		 The state of the cliff sensor on the left side of Roomba is sent as a 1 bit value (0 = no cliff, 1 = cliff).
		 * 		 Range: 0 – 1
		 * </pre>
		 */
		public boolean isCliffLeft() {
			return data[CLIFFLEFT] != 0;
		}

		/**
		 * <pre>
		 * Cliff Front Left                                    Packet ID: 10                 Data Bytes: 1, unsigned
		 * 		 The state of the cliff sensor on the front left of Roomba is sent as a 1 bit value (0 = no cliff, 1 = cliff).
		 * 		 Range: 0 – 1
		 * </pre>
		 */
		public boolean isCliffFrontLeft() {
			return data[CLIFFFRONTLEFT] != 0;
		}

		/**
		 * <pre>
		 * Cliff Front Right                                   Packet ID: 11                 Data Bytes: 1, unsigned
		 * 		 The state of the cliff sensor on the front right of Roomba is sent as a 1 bit value (0 = no cliff, 1 = cliff)
		 * 		 Range: 0 – 1
		 * </pre>
		 */
		public boolean isCliffFrontRight() {
			return data[CLIFFFRONTRIGHT] != 0;
		}

		/**
		 * <pre>
		 * Cliff Right                                         Packet ID: 12                 Data Bytes: 1, unsigned
		 * 		 The state of the cliff sensor on the right side of Roomba is sent as a 1 bit value (0 = no cliff, 1 = cliff)
		 * 		 Range: 0 – 1
		 * 		
		 * </pre>
		 */
		public boolean isCliffRight() {
			return data[CLIFFRIGHT] != 0;
		}

		
		// utils
		
		int toUnsignedShort(byte hi, byte lo) {
			return (int) (hi & 0xff) << 8 | lo & 0xff;
		}

		short toShort(byte hi, byte lo) {
			return (short) ((hi << 8) | (lo & 0xff));
		}
		
		boolean isSet(byte data, int bitMask) {
			return (data & bitMask) != 0;
		}
	}
}
