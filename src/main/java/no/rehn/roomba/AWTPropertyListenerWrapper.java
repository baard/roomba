package no.rehn.roomba;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.SwingUtilities;

public class AWTPropertyListenerWrapper implements PropertyChangeListener {
	final PropertyChangeListener delegate;
	
	public AWTPropertyListenerWrapper(PropertyChangeListener delegate) {
		this.delegate = delegate;
	}

	public void propertyChange(final PropertyChangeEvent evt) {
		if (SwingUtilities.isEventDispatchThread()) {
			delegate.propertyChange(evt);
		}
		else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					delegate.propertyChange(evt);
				}
			});
		}
	}
}