package no.rehn.roomba.io;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO doc
// txrx-lib
public class RoombaConnectionRxTxImpl implements RoombaConnection {
	final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String DEFAULT_PORT_NAME = "/dev/ttyS0";

	public static final int DEFAULT_BAUD = 115200;

	public static final int DEFAULT_DATA_BITS = 8;

	public static final int DEFAULT_PARITY = SerialPort.PARITY_NONE;

	public static final int DEFAULT_STOP_BITS = SerialPort.STOPBITS_1;

	private int baud = DEFAULT_BAUD;

	private int dataBits = DEFAULT_DATA_BITS;

	private int parity = DEFAULT_PARITY;

	private int stopBits = DEFAULT_STOP_BITS;

	private String portName = DEFAULT_PORT_NAME;

	private InputStream inputStream;

	private OutputStream outputStream;

	private SerialPort port;

	public void open() {
		try {
			openPort();
			configurePort();
			sendWakeup();
		} catch (Exception e) {
			logger.error("Failed to connect to " + portName, e);
			// better error-handling
			throw new RuntimeException(e);
		}
	}
	
	public void flush() {
		try {
			outputStream.flush();
		} catch (IOException e) {
			// handle better
			throw new RuntimeException(e);
		}
	}

	private void sendWakeup() throws InterruptedException {
		logger.info("Pulling down RTS");
		port.setRTS(false);
        Thread.sleep(100);
        logger.info("Raising RTS");
        port.setRTS(true);
	}

	private void openPort() throws NoSuchPortException, PortInUseException,
			UnsupportedCommOperationException, IOException,
			TooManyListenersException {
		String portOwnerName = getClass().getName();
		CommPortIdentifier portIdentifier = CommPortIdentifier
				.getPortIdentifier(portName);
		CommPort commPort = portIdentifier.open(portOwnerName, 2000);
		if (commPort instanceof SerialPort) {
			this.port = (SerialPort) commPort;
		} else {
			throw new IllegalStateException("Port " + portName
					+ " is not a serial port");
		}
	}

	private void configurePort() throws UnsupportedCommOperationException,
			IOException, TooManyListenersException {
		port.setSerialPortParams(baud, dataBits, stopBits, parity);
		inputStream = port.getInputStream();
		outputStream = port.getOutputStream();
		port.addEventListener(portListener);
		port.notifyOnDataAvailable(true);
		logger.info("Connected to " + port);
	}

	public void close() {
		port.close();
	}

	public void send(byte... bytes) {
		try {
			outputStream.write(bytes);
			if (logger.isInfoEnabled()) {
				StringBuilder sent = new StringBuilder();
				appendByteArray(sent, bytes);
				logger.info("Sent: [" + sent + "]");
			}
		} catch (IOException e) {
			// better error-handling
			throw new RuntimeException(e);
		}
	}
	
	static void appendByteArray(StringBuilder sent, byte[] bytes) {
		boolean first = true;
		for (byte b : bytes) {
			if (first) {
				first = false;
			}
			else {
				sent.append(", ");
			}
			sent.append(b & 0xFF);
		}
	}

	private RoombaListener portListener = new RoombaListener();
	private RoombaConnectionListener listener;
	
	public void setListener(RoombaConnectionListener listener) {
		this.listener = listener;
	}
	
	private class RoombaListener implements SerialPortEventListener {
		public void serialEvent(SerialPortEvent event) {
			try {
				listener.dataAvailable(inputStream);
			} catch (IOException e) {
				// only log error, don't die
				logger.error("Couldn't receive data", e);
			}
		}
	}
}
