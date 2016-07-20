/* Copyright 2016 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.collector.cron;

import org.torproject.collector.conf.Configuration;
import org.torproject.collector.conf.ConfigurationException;
import org.torproject.collector.conf.Key;
import org.torproject.collector.cron.CollecTorMain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler that starts the modules configured in collector.properties.
 */
public class Scheduler {

  public static final String ACTIVATED = "Activated";
  public static final String PERIODMIN = "PeriodMinutes";
  public static final String OFFSETMIN = "OffsetMinutes";

  private final Logger log = LoggerFactory.getLogger(Scheduler.class);

  private final ScheduledExecutorService scheduler =
      Executors.newScheduledThreadPool(1);

  private static Scheduler instance = new Scheduler();

  private Scheduler(){}

  public static Scheduler getInstance() {
    return instance;
  }

  /**
   * Schedule all classes given according to the parameters in the
   * the configuration.
   */
  public void scheduleModuleRuns(Map<Key,
      Class<? extends CollecTorMain>> collecTorMains, Configuration conf) {
    for ( Map.Entry<Key, Class<? extends CollecTorMain>> ctmEntry
        : collecTorMains.entrySet() ) {
      try {
        if ( conf.getBool(ctmEntry.getKey()) ) {
          String prefix = ctmEntry.getKey().name().replace(ACTIVATED, "");
          CollecTorMain ctm = ctmEntry.getValue()
              .getConstructor(Configuration.class).newInstance(conf);
          scheduleExecutions(ctm,
              conf.getInt(Key.valueOf(prefix + OFFSETMIN)),
              conf.getInt(Key.valueOf(prefix + PERIODMIN)));
        }
      } catch (ConfigurationException | IllegalAccessException
          | InstantiationException | InvocationTargetException
          | NoSuchMethodException | RuntimeException ex) {
        log.error("Cannot schedule " + ctmEntry.getValue().getName()
            + ". Reason: " + ex.getMessage(), ex);
        shutdownScheduler();
        throw new RuntimeException("Halted scheduling.", ex);
      }
    }
  }

  private void scheduleExecutions(CollecTorMain ctm, int offset, int period) {
    this.log.info("Periodic updater started for " + ctm.getClass().getName()
        +  "; offset=" + offset + ", period=" + period + ".");
    int currentMinute = Calendar.getInstance().get(Calendar.MINUTE);
    int initialDelay = (60 - currentMinute + offset) % 60;

    /* Run after initialDelay delay and then every period min. */
    this.log.info("Periodic updater will start every " + period + "th min "
        + "at minute " + ((currentMinute + initialDelay) % 60) + ".");
    this.scheduler.scheduleAtFixedRate(ctm, initialDelay, 60,
        TimeUnit.MINUTES);
  }

  /**
   * Try to shutdown smoothly, i.e., wait for running tasks to terminate.
   */
  public void shutdownScheduler() {
    try {
      scheduler.shutdown();
      scheduler.awaitTermination(20L, java.util.concurrent.TimeUnit.MINUTES);
      log.info("Shutdown of all scheduled tasks completed successfully.");
    } catch ( InterruptedException ie ) {
      List<Runnable> notTerminated = scheduler.shutdownNow();
      log.error("Regular shutdown failed for: " + notTerminated);
      if ( !notTerminated.isEmpty() ) {
        log.error("Forced shutdown failed for: " + notTerminated);
      }
    }
  }
}
