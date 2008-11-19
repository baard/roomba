package no.rehn.roomba.ui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import groovy.lang.GroovyShell;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import no.rehn.roomba.ai.RoombaProgram;
import no.rehn.roomba.ai.examples.Dancer;
import no.rehn.roomba.ai.examples.JukeBox;
import no.rehn.roomba.ai.examples.PowerLedEffects;
import no.rehn.roomba.io.Roomba5xxDevice;
import no.rehn.roomba.io.RoombaConnectionRxTxImpl;
import no.rehn.roomba.io.RoombaDevice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher {
	final static long DEFAULT_UPDATE_SPEED = 50;
	Logger logger = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) throws InterruptedException {
		new Launcher().run(args);
	}
	
	ModelDeviceMapper commandHandler;
	RoombaBean model;
	RoombaDevice device;
	
	RoombaProgram[] programs = {
			new PowerLedEffects(),
			new JukeBox(),
			new Dancer()
	};

	void run(String[] args) throws InterruptedException {
		Runtime.getRuntime().addShutdownHook(new Thread(shutdownHook));
		RoombaConnectionRxTxImpl connection = new RoombaConnectionRxTxImpl();
		//TODO set serial-parameters based on arguments
		connection.open();
		

		model = new RoombaBean();
		device = new Roomba5xxDevice(connection);
		commandHandler = new ModelDeviceMapper(model, device);
		
		DeviceBandwidthMonitor bandwidthMonitor = new DeviceBandwidthMonitor(device);
		Thread eventFirer = new Thread(bandwidthMonitor, "bandwidth-monitor");
		eventFirer.setDaemon(true);
		eventFirer.start();

		setConnected(true);

		setNativeLookAndFeel();

		final GroovyShell shell = new GroovyShell();
		shell.setVariable("model", model);
		shell.setVariable("executor", this);
		shell.setVariable("bandwidthMonitor", bandwidthMonitor);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				shell.evaluate(getClass().getResourceAsStream(
				"/no/rehn/roomba/ui/RoombaView.groovy"));
			}
		});

		model.start();
		commandHandler.flushCommands();
		RoombaProgram previous = null;
		while (!shutdown) {
			if (program != previous) {
				// program-switch
				if (previous != null) {
					logger.info("Stopping program '{}'", previous);
					previous.onExit(model);
					commandHandler.flushCommands();
				}
				if (program != null) {
					logger.info("Starting program '{}'", program);
					program.onStart(model);
					commandHandler.flushCommands();
				}
				previous = program;
			}
			if (program != null) {
				program.onTick(model, System.currentTimeMillis());
			}
			commandHandler.flushCommands();
			Thread.sleep(updateSpeed);
		}
		if (program != null) {
			program.onExit(model);
		}
		commandHandler.flushCommands();
		logger.info("exiting");
		synchronized (shutdownHook) {
			shutdownHook.notify();
		}
	}
	
	private long updateSpeed = DEFAULT_UPDATE_SPEED;

	public void setUpdateSpeed(long updateSpeed) {
		this.updateSpeed = updateSpeed;
	}
	
	public long getUpdateSpeed() {
		return updateSpeed;
	}
	
	private boolean connected;
	
	public boolean isConnected() {
		return connected;
	}
	
	public void setConnected(boolean connect) {
		if (!connect) {
			model.removePropertyChangeListener(commandHandler);
		}
		else {
			model.addPropertyChangeListener(commandHandler);
		}
		this.connected = connect;
	}
	
	private void setNativeLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			logger.warn("Couldn't set look-and-feel", e);
		}
	}

	volatile boolean shutdown = false;

	Runnable shutdownHook = new Runnable() {
		public void run() {
			shutdown = true;
			synchronized (this) {
				// give app 1 sec to shut down
				try {
					wait(1000);
				} catch (InterruptedException e) {
					// we shut down
					return;
				}
			}
		}
	};
	
	volatile RoombaProgram program;

	public void setProgram(RoombaProgram program) {
		pcs.firePropertyChange("program", this.program, this.program = program);
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

	public RoombaProgram getProgram() {
		return program;
	}
	
	public RoombaProgram[] getPrograms() {
		return programs;
	}
}