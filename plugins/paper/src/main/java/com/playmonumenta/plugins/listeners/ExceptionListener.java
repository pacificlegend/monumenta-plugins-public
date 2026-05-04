package com.playmonumenta.plugins.listeners;

import com.destroystokyo.paper.event.server.ServerExceptionEvent;
import com.destroystokyo.paper.exception.ServerException;
import com.destroystokyo.paper.exception.ServerSchedulerException;
import com.playmonumenta.plugins.utils.MMLog;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

public class ExceptionListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void serverExceptionEvent(ServerExceptionEvent event) {
		ServerException exception = event.getException();

		// Paper already logs these with full cause chain
		if (exception instanceof ServerSchedulerException schedException) {
			BukkitTask task = schedException.getTask();
			String msg = "Caught exception in " + (task.isSync() ? "sync" : "async") +
				" task from " + task.getOwner().getName() +
				" in class " + task.getClass().getName() +
				" - killing it";
			// Log these at warning level instead of error - paper already logs the exception itself at severe level
			// This warning log is only to indicate the task was killed
			MMLog.warning(msg, exception);
			if (!task.isCancelled()) {
				task.cancel();
			}
		} else {
			// Not taking any action here, log with debug level to avoid duplication
			MMLog.debug("Caught server exception", exception);
		}
	}

}
