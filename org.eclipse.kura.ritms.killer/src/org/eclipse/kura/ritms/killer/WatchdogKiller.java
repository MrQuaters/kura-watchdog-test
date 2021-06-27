package org.eclipse.kura.ritms.killer;

import java.lang.Thread.State;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.watchdog.CriticalComponent;
import org.eclipse.kura.watchdog.WatchdogService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatchdogKiller implements ConfigurableComponent, CriticalComponent {
	
	private int pollInterval = 60000; // mills
	private AtomicBoolean pollEnabled = new AtomicBoolean(true);
	private boolean stopThread = false;
	private Thread workerThread;
	private boolean watchdogRegistred = false;
	
	 private static final Logger slog = LoggerFactory.getLogger(WatchdogKiller.class);
	
	private WatchdogService m_watchdogService;
	
	public void setWatchdogService(WatchdogService watchdogService) {
        this.m_watchdogService = watchdogService;
    }

    public void unsetWatchdogService(WatchdogService watchdogService) {
        this.m_watchdogService = null;
    }
	
	@Override
	public String getCriticalComponentName() {
		return "WatchdogKiller";
	}

	@Override
	public int getCriticalComponentTimeout() {
		// TODO Auto-generated method stub
		return pollInterval * 2;
	}
	
	private void poll() {
		while (!stopThread) {
			if(m_watchdogService != null) {
				if (!watchdogRegistred) {
					slog.info("Registering watchdog");
					m_watchdogService.registerCriticalComponent(this);
					watchdogRegistred = true;
				}
				
				if (watchdogRegistred && pollEnabled.get()) {
					m_watchdogService.checkin(this);
				}
			}
			try {
				Thread.sleep(pollInterval);
			} catch (Exception e) {
				e.printStackTrace();
	        }
		}
	}
	
	protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
		setPollStatus(properties);
		stopThread = false;
		workerThread = new Thread(new Runnable() {

			@Override
			public void run() {
				Thread.currentThread().setName(getClass().getSimpleName());
				poll();
			}
			
		});
		workerThread.start();
		
    }

    protected void deactivate(ComponentContext componentContext) {
	   if (this.m_watchdogService != null) {
           this.m_watchdogService.unregisterCriticalComponent(this);
       }
	   stopThread = true;
	   while (workerThread.getState() != State.TERMINATED) {
		   try {
               Thread.sleep(100);
           } catch (Exception e) {
               e.printStackTrace();
           }
	   }
    }
    
    private void setPollStatus(Map<String, Object> properties) {
    	if (((String) properties.get("pollwatchdog")).equals("deactivate")){
			slog.info("Polling watchdog disabled");
			pollEnabled.set(false);
    	} else {
    		slog.info("Polling watchdog enabled");
    		pollEnabled.set(true);
    	}
    }
    
    public void updated(Map<String, Object> properties) {
    	setPollStatus(properties);
    }

	
}
