package no.rehn.roomba.ui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.SwingUtilities;

import no.rehn.roomba.io.RoombaDevice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO doc
public class DeviceBandwidthMonitor implements Runnable {
	final Logger logger = LoggerFactory.getLogger(getClass());
	final RoombaDevice device;
	final int MILLIS_IN_SECOND = 1000;

	long sent;
	long received;
	
	Counter sentCounter = new Counter();
	Counter receivedCounter = new Counter();
	Counter firedCounter = new Counter();

	public DeviceBandwidthMonitor(RoombaDevice device) {
		this.device = device;
	}

	public void run() {
		logger.info("Starting bandwidth monitor");
		firedCounter.update(System.currentTimeMillis());
		while (true) {
			long duration = firedCounter.update(System.currentTimeMillis());
			if (duration <= 0) {
				// avoid divide-by-zero
				continue;
			}
			received = receivedCounter.update(device.getReceivedBytes()) * MILLIS_IN_SECOND / duration;
			sent = sentCounter.update(device.getSentBytes()) * MILLIS_IN_SECOND / duration;

			pcs.firePropertyChange("received", null, received);
			pcs.firePropertyChange("sent", null, sent);

			try {
				// update every sec
				Thread.sleep(MILLIS_IN_SECOND);
			} catch (InterruptedException e) {
				// we exit
				return;
			}
		}
	}
	
	class Counter {
		long lastValue;
		long update(long newValue) {
			long diff = newValue - lastValue;
			lastValue = newValue;
			return diff;
		}
	}
	
	public long getSent() {
		return sent;
	}
	
	public long getReceived() {
		return received;
	}

	PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		if (SwingUtilities.isEventDispatchThread()) {
			// we wrap this listener to work in the dispatcht-thread
			pcs.addPropertyChangeListener(new AWTPropertyListenerWrapper(listener));
		}
		else {
			pcs.addPropertyChangeListener(listener);
		}
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}
}
