/*-
 * #%L
 * Activity Log AppJars - Demo
 * %%
 * Copyright (C) 2023 - 2026 AppJars
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.appjars.activitylog.demo.services;

import com.appjars.activitylog.model.AuditLevel;
import java.net.SocketTimeoutException;
import java.sql.SQLTransientConnectionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

/**
 * Generates activity on demand, as the application behind {@link SimulatedAppLoggers} would: one log
 * per level, continuing the order narrative the preloaded entries start, with an incrementing order
 * reference so no two clicks produce the same detail.
 *
 * <p>Logs are emitted through Log4j2 (never saved directly), so they travel the real path:
 * appender, extractor filtering and persistence. A log whose logger does not match an active
 * extractor is simply not stored, which is what makes the extractor filters visible.
 */
public final class DemoLogTrigger {

  /** Continues where the preloaded entries left off (they reach ORD-10427). */
  private static final AtomicInteger ORDER_SEQUENCE = new AtomicInteger(10427);

  private DemoLogTrigger() {}

  /**
   * Emits one log of the given level and returns the logger it was emitted through.
   *
   * @param level level of the log to emit, not null
   * @return name of the logger used
   */
  public static String trigger(AuditLevel level) {
    int order = ORDER_SEQUENCE.incrementAndGet();
    return switch (level) {
      case TRACE -> log(SimulatedAppLoggers.CART_SERVICE, Level.TRACE,
          "Cart CART-%d recalculated: 3 items, subtotal 149.90 EUR".formatted(order), null);
      case DEBUG -> log(SimulatedAppLoggers.ORDER_REPOSITORY, Level.DEBUG,
          "Order ORD-%d loaded from the database in 12 ms".formatted(order), null);
      case INFO -> log(SimulatedAppLoggers.ORDER_SERVICE, Level.INFO,
          "Order ORD-%d confirmed for customer CUST-3391, total 149.90 EUR".formatted(order), null);
      case WARN -> log(SimulatedAppLoggers.PAYMENT_GATEWAY, Level.WARN,
          "Payment authorization for ORD-%d retried after gateway timeout (attempt 2 of 3)"
              .formatted(order),
          null);
      case ERROR -> log(SimulatedAppLoggers.PAYMENT_GATEWAY, Level.ERROR,
          "Payment capture failed for order ORD-%d".formatted(order),
          new IllegalStateException("Gateway responded 502 for order ORD-%d".formatted(order),
              new SocketTimeoutException("Read timed out")));
      case FATAL -> log(SimulatedAppLoggers.INVENTORY_SERVICE, Level.FATAL,
          "Inventory database pool exhausted, order intake suspended while confirming ORD-%d"
              .formatted(order),
          new SQLTransientConnectionException(
              "HikariPool-1 - Connection is not available, request timed out after 30000ms"));
    };
  }

  /**
   * Emits a log the user wrote by hand.
   *
   * @param logger name of the logger to emit through, not null
   * @param detail message of the log, not null
   * @param level level of the log, not null
   * @return name of the logger used
   */
  public static String trigger(String logger, String detail, AuditLevel level) {
    return log(logger, toLog4jLevel(level), detail, null);
  }

  private static String log(String logger, Level level, String detail, Throwable thrown) {
    LogManager.getLogger(logger).log(level, detail, thrown);
    return logger;
  }

  private static Level toLog4jLevel(AuditLevel level) {
    return switch (level) {
      case TRACE -> Level.TRACE;
      case DEBUG -> Level.DEBUG;
      case INFO -> Level.INFO;
      case WARN -> Level.WARN;
      case ERROR -> Level.ERROR;
      case FATAL -> Level.FATAL;
    };
  }
}
