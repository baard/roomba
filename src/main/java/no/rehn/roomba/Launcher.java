package no.rehn.roomba;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import groovy.lang.GroovyShell;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import no.rehn.roomba.ai.RoombaProgram;
import no.rehn.roomba.ai.examples.Dancer;
import no.rehn.roomba.ai.examples.JukeBox;
import no.rehn.roomba.ai.examples.PowerLedEffects;
import no.rehn.roomba.io.RoombaConnectionRxTxImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher {
	final static long DEFAULT_UPDATE_SPEED = 50;
	Logger logger = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) throws InterruptedException {
		new Launcher().run(args);
	}
	
	CommandHandler commandListener;
	Roomba model;
	
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
		

		model = new Roomba();
		commandListener = new CommandHandler(connection,
				model);
		// must connect to send start-command
		connection.setListener(commandListener);

		setConnected(true);

		setNativeLookAndFeel();

		final GroovyShell shell = new GroovyShell();
		shell.setVariable("model", model);
		shell.setVariable("executor", this);
		shell.setVariable("handler", commandListener);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				shell.evaluate(getClass().getResourceAsStream(
				"/no/rehn/roomba/RoombaView.groovy"));
			}
		});

		model.start();
		commandListener.flushCommands();
		RoombaProgram previous = null;
		while (!shutdown) {
			if (program != previous) {
				// program-switch
				if (previous != null) {
					logger.info("Stopping program '{}'", previous);
					previous.onExit(model);
					commandListener.flushCommands();
				}
				if (program != null) {
					logger.info("Starting program '{}'", program);
					program.onStart(model);
					commandListener.flushCommands();
				}
				previous = program;
			}
			if (program != null) {
				program.onTick(model, System.currentTimeMillis());
			}
			commandListener.flushCommands();
			Thread.sleep(updateSpeed);
		}
		if (program != null) {
			program.onExit(model);
		}
		commandListener.flushCommands();
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
		if (this.connected != connect) {
			if (connect) {
				model.addPropertyChangeListener(commandListener);
			}
			else {
				model.removePropertyChangeListener(commandListener);
			}
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