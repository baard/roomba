package no.rehn.roomba

import groovy.swing.SwingBuilder
import net.miginfocom.swing.MigLayout 
import javax.swing.WindowConstants as WC
import groovy.beans.Bindable
import no.rehn.roomba.Roomba.Mode
import javax.swing.JSlider
import javax.swing.JTabbedPane
import javax.swing.DefaultComboBoxModel
import no.rehn.roomba.tunes.RTTTLParser

def rtttlParser = new RTTTLParser()

def frame = SwingBuilder.build {
	frame(title:'Roomba Control', size:[800,600],
        visible: true, defaultCloseOperation:WC.EXIT_ON_CLOSE) {
        panel(layout:new MigLayout('fill')) {
		    panel(constraints: 'aligny top, width 300', layout:new MigLayout()) {
		    	label('Run program:', constraints: 'wrap')
		    	scrollPane(constraints: 'span 2, grow, wrap') {
		    		programList = list(model: new DefaultComboBoxModel(executor.programs))
		    	}
		    	label('Current program:')
		    	textField(
		    		text: bind {executor.program}, 
		    		enabled: false,
		    		constraints: 'grow, wrap' 
		    	)
		    	button(
		    		text: 'Start program',
		    		actionPerformed: {
		    			executor.program = programList.selectedValue
		    		}
		    	)
		    	button(
		    		text: 'Stop program',
		    		constraints: 'wrap',
		    		enabled: bind {executor.program != null},
		    		actionPerformed: {
		    			executor.program = null
		    		}
		    	)
		    	connected = checkBox(text:'Connected', 
    		    	selected: bind {executor.connected},
    		    	actionPerformed: { executor.connected = connected.selected },
    		    	constraints: 'wrap'
    		    )
    		    label(text: "Update speed (ms)")
    		    updateSpeed = formattedTextField(
    		    	columns: 10, 
    		    	value: executor.DEFAULT_UPDATE_SPEED,
    		    	constraints:'wrap, grow'
    		    )
   			    bind(source: updateSpeed, sourceProperty:'value', target:executor , targetProperty:'updateSpeed')
				label(text: "Sending (bit/s):")
				progressBar(
					string: bind {handler.sent}, 
					stringPainted: true, 
					value: bind {handler.sent},
					maximum: 56000,
					constraints:'wrap,grow'
				)
				label(text: "Receiving (bit/s):")
				progressBar(
					string: bind {handler.received}, 
					stringPainted: true, 
					value: bind {handler.received},
					maximum: 56000,
					constraints:'wrap,grow'
				)
			    label(constraints: 'span, align center', icon: imageIcon(resource:'roomba_small.png'))
		    }

		    tabbedPane(constraints: 'grow') {
    		    panel(title: 'Modes', layout:new MigLayout()) {
    		    	label(text: "Set operation mode")
    				button(
    					text: "Safe", 
    					actionPerformed: { model.mode = Mode.SAFE } 
    		    	)
    				button(
    					text: "Full", 
    					actionPerformed: { model.mode = Mode.FULL }, 
    					constraints:'wrap, grow'
    				)
    				label(text: "Current mode")
    				textField(
    					text: bind {model.mode}, 
    					enabled: false,
    					constraints:'wrap,grow,span 2'
    				)
    				button(
    					text: "Wake up", 
    					actionPerformed: { model.wakeup() }, 
    					constraints:'grow'
    				)
    				button(
    					text: "Power down", 
    					actionPerformed: { model.power() }, 
    					constraints:'wrap, grow, span 2'
    				)
    		    }
        		panel(title: 'Tones', layout:new MigLayout('fill')) {
    				label(text: "Currently playing:")
    				textField(
    					text: bind {model.songName},
    					enabled: false,
    					constraints: 'growx, wrap'
    				)
            		label('Song (ringtone format):', constraints: 'wrap')
            		scrollPane(constraints: 'grow, span 2, wrap') {
            			song = textArea(lineWrap: true)
            		}
            		button(
            			'Play song',
            			actionPerformed: {
        					model.song = rtttlParser.parse(song.text)
            			},
            		)
            	}
    		    panel(title: 'Wheels', layout:new MigLayout()) {
    		    	label('Left wheel:')
    		    	leftWheelSpeed = slider(
    		    		value: bind {model.leftWheelSpeed}, minimum: -255, maximum: 255,
    		    		stateChanged: {model.leftWheelSpeed = leftWheelSpeed.value },
    		    		constraints: 'wrap'
    		    	)
    		    	label('Right wheel:')
    		    	rightWheelSpeed = slider(
    		    		value: bind {model.rightWheelSpeed}, minimum: -255, maximum: 255, 
    		    		stateChanged: {model.rightWheelSpeed = rightWheelSpeed.value },
    		    		constraints: 'wrap'
    		    	)
    		    	label('Velocity: ')
    		    	velocity = slider(
    		    		value: bind {model.velocity}, minimum: -255, maximum: 255, 
    		    		stateChanged: {model.velocity = velocity.value },
    		    		majorTickSpacing: 255,
    		    		paintTicks: true,
    		    	)
    		    	button(
    		    		text: "Zero", 
    		    		actionPerformed: { model.velocity = 0 },
    		    		constraints: 'wrap'
    		    	)
    		        label('Wheel diff: ')
    		    	wheelDiff = slider(
    		        	value: bind {model.wheelDiff}, minimum: -511, maximum: 511, 
    		        	stateChanged: {model.wheelDiff = wheelDiff.value },
    		    		majorTickSpacing: 511,
    		    		paintTicks: true,
    		        )
    		        button(
    		        	text: "Zero", 
    		        	actionPerformed: { model.wheelDiff = 0 },
    		    		constraints: 'wrap'
    		        )
    		        button(
    		        	text: "Stop both wheels", 
    		        	actionPerformed: { model.stop() }
    		    	)
    		    }
    		    panel(title: 'Vacuum', layout:new MigLayout()) {
    		        label('Main-brush: ')
        		    mainBrushPwm = slider(
        		    	value: bind {model.mainBrushPwm}, minimum: -127, maximum: 127,
        		    	stateChanged: {model.mainBrushPwm = mainBrushPwm.value },
        		    	constraints: 'grow'
        		 	)
        		 	button(
        		 		text: "Stop",
        		 		actionPerformed: { model.mainBrushPwm = 0 },
        		 		constraints:'wrap'
        		 	)
    		        label('Side-brush: ')
    				sideBrushPwm = slider(
        		    	value: bind {model.sideBrushPwm}, minimum: -127, maximum: 127, 
        		    	stateChanged: {model.sideBrushPwm = sideBrushPwm.value },
        		    	constraints:'grow'
        		    )
        		 	button(
        		 		text: "Stop",
        		 		actionPerformed: { model.sideBrushPwm = 0 },
        		 		constraints:'wrap'
        		 	)
    		        label('Vacuum pwm: ')
    				vacuumPwm = slider(
        		    	value: bind {model.vacuumPwm}, minimum: 0, maximum: 127, 
        		    	stateChanged: {model.vacuumPwm = vacuumPwm.value },
        		    	constraints:'grow'
        		    )
        		 	button(
        		 		text: "Stop",
        		 		actionPerformed: { model.vacuumPwm = 0 },
        		 		constraints:'wrap',
        		 	)
    		    }
    		    panel(title: "LEDs", layout:new MigLayout()) {
        		    dirt = checkBox(text:'Dirt', 
        		    	selected: bind {model.dirt},
        		    	actionPerformed: { model.dirt = dirt.selected },
        		    	constraints:'wrap, grow'
        		    )
        		    spot = checkBox(text:'Spot', 
        		    	selected: bind {model.spot},
        		    	actionPerformed: { model.spot = spot.selected },
        		    	constraints:'wrap, grow'
        		    )
        		    clean = checkBox(text:'Clean', 
        		    	selected: bind {model.clean},
        		    	actionPerformed: { model.clean = clean.selected },
        		    	constraints:'wrap, grow'
        		    )
    	    		max = checkBox(text:'Max', 
        		    	selected: bind {model.max},
        		    	actionPerformed: { model.max = max.selected },
        		    	constraints:'wrap, grow'
        		    )
    		        label 'Power LED color: '
        		    color = slider(
        		    	value: bind {model.powerColor}, maximum: 255,
        		    	stateChanged: {model.powerColor = color.value },
        		    	constraints:'wrap, grow'
        		 	)
    		        label 'Power LED intensity: '
    				intensity = slider(
        		    	value: bind {model.powerIntensity}, maximum: 255, 
        		    	stateChanged: {model.powerIntensity = intensity.value },
        		    	constraints:'wrap, grow'
        		    )
        		    label(text: "Text (4 digits):")
        		    ledText = textField(columns: 4, constraints:'wrap, grow',
        		    	actionPerformed: { model.text = ledText.text },
        		    	text: bind { model.text }
        		    )
    		    }
    		    panel(title: 'Sensors', layout:new MigLayout()) {
    	    		updateSensors = checkBox(
    	    			text:'Update sensors', 
        		    	selected: bind {model.updateSensors},
        		    	actionPerformed: { model.updateSensors = updateSensors.selected },
        		    	constraints:'wrap, grow'
        		    )
        		    separator(constraints: 'wrap')
    	    		checkBox(
    	    			text:'Wheels dropped', 
    	    			selected: bind {model.wheelDropped},
    	    			enabled: false, constraints:'wrap, grow'
    	    		)
    	    		checkBox(
    	    			text:'Bump sensor engaged', 
    	    			selected: bind {model.bumpSensor},
    	    			enabled: false, constraints:'wrap, grow'
    	    		)
    	    		checkBox(text:'Wall detected', selected: bind {model.wallSensor},
    	    			enabled: false, constraints:'wrap, grow'
    	    		)
    	    		label(text: "Voltage (mV):")
    	    		textField(
    	    			text: bind {model.voltage}, 
    	    			columns: 8, 
    	    			enabled: false, 
    	    			constraints:'wrap, grow'
    	    		)
    	    		label(text: "Battery temp (deg C):")
    	    		textField(
    	    			text: bind {model.batteryTemperature}, 
    	    			columns: 3, 
    	    			enabled: false, 
    	    			constraints:'wrap, grow'
    	    		)
    	    		label(text: "Current (mA):")
    	    		textField(
    	    			text: bind {model.current}, 
    	    			columns: 8, 
    	    			enabled: false, 
    	    			constraints:'wrap, grow'
    	    		)
    	    		checkBox(
    	    			text:'Overcurrent left wheel motor', 
    	    			selected: bind {model.overcurrentWheelLeft}, 
    	    			enabled: false, 
    	    			constraints: 'grow, wrap'
    	    		)
    	    		checkBox(
    	    			text:'Overcurrent right wheel motor', 
    	    			selected: bind {model.overcurrentWheelRight}, 
    	    			enabled: false, 
    	    			constraints: 'grow, wrap'
    	    		)
    	    		checkBox(
    	    			text:'Overcurrent main brush motor', 
    	    			selected: bind {model.overcurrentMainBrush}, 
    	    			enabled: false, 
    	    			constraints: 'grow, wrap'
    	    		)
    	    		checkBox(
    	    			text:'Overcurrent side brush motor', 
    	    			selected: bind {model.overcurrentSideBrush}, 
    	    			enabled: false, 
    	    			constraints: 'grow, wrap'
    	    		)
    	    		label(text: "Charge (mAh):")
    	    		textField(
    	    			text: bind {model.charge}, 
    	    			columns: 8, 
    	    			enabled: false, 
    	    			constraints:'wrap, grow'
    	    		)
    	    		label(text: "Capacity (mAh):")
    	    		textField(
    	    			text: bind {model.capacity}, 
    	    			columns: 8, 
    	    			enabled: false, 
    	    			constraints:'wrap, grow'
    	    		)
    				label(text: "Battery charge left:")
    				progressBar(
    					string: bind {
    	    				if (model.capacity > 0) {
    	    					(int) (100 * model.charge / model.capacity) + "%"
    	    				}
    	    				else {
    	    					"?"
    	    				}
    	    			}, 
    					stringPainted: true, 
    					value: bind {model.charge},
    					maximum: bind {model.capacity},
    					constraints:'wrap,grow'
    				)
    		    }
        	}
		}
	}
}