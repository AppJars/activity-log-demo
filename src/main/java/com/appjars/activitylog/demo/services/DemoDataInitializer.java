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

import com.appjars.activitylog.business.service.ActivityLogService;
import com.appjars.activitylog.business.service.ExtractorService;
import com.appjars.activitylog.business.service.LogViewerService;
import com.appjars.activitylog.business.service.RemoverService;
import com.appjars.activitylog.model.ActivityLogDto;
import com.appjars.activitylog.model.AuditLevel;
import com.appjars.activitylog.model.ExtractorDto;
import com.appjars.activitylog.model.LogViewerDto;
import com.appjars.activitylog.model.LoggerFilterDto;
import com.appjars.activitylog.model.LoggerFilterType;
import com.appjars.activitylog.model.RemoverDto;
import com.appjars.activitylog.model.TimestampFilterDto;
import com.appjars.activitylog.util.TimestampFilterType;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Service;

/**
 * Preloads the demo with the configuration and the history a real installation would already have,
 * so an evaluator lands on a populated application: an active extractor capturing the
 * {@link SimulatedAppLoggers}, a retention rule ready to be switched on, a saved view of the errors
 * and a week of activity from the fictional order-management application.
 *
 * <p>Everything is hardcoded and idempotent: each item is created only when it is missing, so
 * restarting the demo neither duplicates nor resets what the evaluator did. Deleting the {@code
 * ./data} directory restores the whole set.
 *
 */
@Service
public class DemoDataInitializer implements SmartInitializingSingleton {

  /** Public so the editor tours can open the very entities preloaded here. */
  public static final String EXTRACTOR_NAME = "Application activity";

  public static final String REMOVER_NAME = "Prune activity older than 1 hour";
  public static final String LOG_VIEW_ROUTE = "al/errors";

  static final String LOG_VIEW_TITLE = "Application errors";

  /** Regex matching any detail: {@code EVERYTHING} is not honoured by the extraction engine. */
  private static final String ANY_DETAIL = ".*";

  private static final Set<AuditLevel> ALL_LEVELS = EnumSet.allOf(AuditLevel.class);

  /** Two of the preloaded entries' sessions, so the session filter has something to find. */
  private static final String SESSION_JSMITH = "8f14e45f-ea2b-4c1e-9d3a-71b0c5a4d201";
  private static final String SESSION_MGARCIA = "3f6a1b2c-77de-4b58-8a0f-2c9e6b41f8d3";

  private static final Logger logger = LoggerFactory.getLogger(DemoDataInitializer.class);

  private final ActivityLogService logService;
  private final ExtractorService extractorService;
  private final RemoverService removerService;
  private final LogViewerService logViewerService;

  public DemoDataInitializer(ActivityLogService logService, ExtractorService extractorService,
      RemoverService removerService, LogViewerService logViewerService) {
    this.logService = logService;
    this.extractorService = extractorService;
    this.removerService = removerService;
    this.logViewerService = logViewerService;
  }

  @Override
  public void afterSingletonsInstantiated() {
    seedExtractor();
    seedRemover();
    seedLogView();
    seedLogs();
  }

  /**
   * The extractor that makes the demo work: captures every level of the
   * {@link SimulatedAppLoggers}, at any time, storing stack traces and session ids. Framework logs
   * stay out because their loggers do not match the prefix.
   */
  private void seedExtractor() {
    if (extractorService.findByName(EXTRACTOR_NAME).isPresent()) {
      return;
    }
    extractorService.save(ExtractorDto.builder().name(EXTRACTOR_NAME).active(true)
        .saveStacktrace(true).saveSessionId(true).levels(EnumSet.copyOf(ALL_LEVELS))
        .loggerFilters(appLoggerFilter()).detailFilters(anyDetailFilter())
        .timestampFilters(allTimeFilter()).build());
    logger.info("Preloaded extractor '{}'", EXTRACTOR_NAME);
  }

  /**
   * Retention rule with the extractor's filters, left <em>inactive</em> on purpose: active, the
   * pruner would drop every preloaded entry (they are all older than an hour) seconds after startup.
   * Switching it on is one of the things the demo invites you to do.
   */
  private void seedRemover() {
    if (removerService.findByName(REMOVER_NAME).isPresent()) {
      return;
    }
    removerService.save(RemoverDto.builder().name(REMOVER_NAME).active(false)
        .expirationValue(1).expirationUnit(ChronoUnit.HOURS).levels(EnumSet.copyOf(ALL_LEVELS))
        .loggerFilters(appLoggerFilter()).detailFilters(anyDetailFilter())
        .timestampFilters(allTimeFilter()).build());
    logger.info("Preloaded remover '{}' (inactive)", REMOVER_NAME);
  }

  /** Saved view of the errors only: the level is preconfigured, so its users cannot widen it. */
  private void seedLogView() {
    if (logViewerService.findByRoute(LOG_VIEW_ROUTE).isPresent()) {
      return;
    }
    logViewerService.save(LogViewerDto.builder().title(LOG_VIEW_TITLE).route(LOG_VIEW_ROUTE)
        .levels(EnumSet.of(AuditLevel.ERROR)).timestampColumnVisible(true).levelColumnVisible(true)
        .loggerColumnVisible(true).detailColumnVisible(true).loggerFilterEnabled(true)
        .detailFilterEnabled(true).timestampFilterEnabled(true).sessionFilterEnabled(true)
        .levelFilterEnabled(false).liveEnabled(true).build());
    logger.info("Preloaded log view '{}' on route '{}'", LOG_VIEW_TITLE, LOG_VIEW_ROUTE);
  }

  private Set<LoggerFilterDto> appLoggerFilter() {
    return newSet(LoggerFilterDto.builder().type(LoggerFilterType.STARTS_WITH)
        .pattern(SimulatedAppLoggers.LOGGER_PREFIX).build());
  }

  private Set<LoggerFilterDto> anyDetailFilter() {
    return newSet(LoggerFilterDto.builder().type(LoggerFilterType.REGEX).pattern(ANY_DETAIL).build());
  }


  private Set<TimestampFilterDto> allTimeFilter() {
    return newSet(TimestampFilterDto.builder().filterType(TimestampFilterType.DAYS_OF_WEEK)
        .daysOfWeek(EnumSet.allOf(DayOfWeek.class)).sinceDate(Optional.empty())
        .untilDate(Optional.empty()).fromTime(Optional.empty()).toTime(Optional.empty()).build());
  }

  private static <T> Set<T> newSet(T item) {
    Set<T> set = new LinkedHashSet<>();
    set.add(item);
    return set;
  }

  /**
   * A week in the life of the fictional application: orders confirmed and paid, a payment that fails
   * and leaves an order pending, stock running out, a locked account and a database pool that
   * collapses and recovers. Saved through the service, so the entries land whatever the extractors
   * say.
   */
  private void seedLogs() {
    if (!logService.findAll().isEmpty()) {
      return;
    }
    Instant now = Instant.now();

    log(now, hours(6, 21), AuditLevel.INFO, SimulatedAppLoggers.AUTH_SERVICE,
        "User jsmith signed in from 190.12.44.7", null, SESSION_JSMITH);
    log(now, hours(6, 20), AuditLevel.INFO, SimulatedAppLoggers.CART_SERVICE,
        "Cart CART-4471 created for customer CUST-3391", null, SESSION_JSMITH);
    log(now, hours(6, 19), AuditLevel.INFO, SimulatedAppLoggers.ORDER_SERVICE,
        "Order ORD-10388 confirmed for customer CUST-3391, total 149.90 EUR", null, SESSION_JSMITH);
    log(now, hours(6, 19), AuditLevel.INFO, SimulatedAppLoggers.PAYMENT_GATEWAY,
        "Payment PAY-88213 captured for order ORD-10388 (VISA ****4242)", null, SESSION_JSMITH);
    log(now, hours(6, 18), AuditLevel.INFO, SimulatedAppLoggers.SHIPPING_SERVICE,
        "Shipment SHP-5521 dispatched for order ORD-10388 via DHL", null, null);

    log(now, hours(5, 15), AuditLevel.WARN, SimulatedAppLoggers.INVENTORY_SERVICE,
        "Stock for SKU-99120 fell to 3 units, below the reorder threshold of 10", null, null);
    log(now, hours(5, 14), AuditLevel.INFO, SimulatedAppLoggers.ORDER_SERVICE,
        "Order ORD-10390 confirmed for customer CUST-2210, total 89.50 EUR", null, null);
    log(now, hours(5, 14), AuditLevel.ERROR, SimulatedAppLoggers.PAYMENT_GATEWAY,
        "Payment capture failed for order ORD-10390 after 3 attempts", PAYMENT_STACKTRACE, null);
    log(now, hours(5, 13), AuditLevel.INFO, SimulatedAppLoggers.ORDER_SERVICE,
        "Order ORD-10390 moved to PENDING_PAYMENT", null, null);

    log(now, hours(4, 18), AuditLevel.WARN, SimulatedAppLoggers.AUTH_SERVICE,
        "3 failed sign-in attempts for user mgarcia; account locked for 15 minutes", null, null);
    log(now, hours(4, 11), AuditLevel.INFO, SimulatedAppLoggers.ORDER_SERVICE,
        "Order ORD-10402 confirmed for customer CUST-1187, total 312.00 EUR", null, null);
    log(now, hours(4, 11), AuditLevel.DEBUG, SimulatedAppLoggers.ORDER_REPOSITORY,
        "Order search executed in 41 ms (page 0, size 25, filter status=CONFIRMED)", null, null);

    log(now, hours(3, 16), AuditLevel.INFO, SimulatedAppLoggers.INVENTORY_SERVICE,
        "Purchase order PO-771 received: 120 units of SKU-99120 added to stock", null, null);
    log(now, hours(3, 9), AuditLevel.ERROR, SimulatedAppLoggers.INVENTORY_SERVICE,
        "Stock reservation for order ORD-10405 failed: SKU-88710 is out of stock",
        INVENTORY_STACKTRACE, null);

    log(now, hours(2, 22), AuditLevel.FATAL, SimulatedAppLoggers.INVENTORY_SERVICE,
        "Inventory database pool exhausted, order intake suspended", POOL_STACKTRACE, null);
    log(now, hours(2, 21), AuditLevel.INFO, SimulatedAppLoggers.INVENTORY_SERVICE,
        "Inventory database pool recovered, order intake resumed", null, null);
    log(now, hours(2, 12), AuditLevel.INFO, SimulatedAppLoggers.AUTH_SERVICE,
        "User mgarcia signed in from 190.12.44.9", null, SESSION_MGARCIA);
    log(now, hours(2, 12), AuditLevel.INFO, SimulatedAppLoggers.ORDER_SERVICE,
        "Order ORD-10411 confirmed for customer CUST-5502, total 47.20 EUR", null, SESSION_MGARCIA);

    log(now, hours(1, 19), AuditLevel.WARN, SimulatedAppLoggers.PAYMENT_GATEWAY,
        "Payment authorization for ORD-10419 retried after gateway timeout (attempt 2 of 3)", null,
        null);
    log(now, hours(1, 18), AuditLevel.INFO, SimulatedAppLoggers.PAYMENT_GATEWAY,
        "Payment PAY-88240 captured for order ORD-10419 (MASTERCARD ****1881)", null, null);
    log(now, hours(1, 10), AuditLevel.TRACE, SimulatedAppLoggers.CART_SERVICE,
        "Cart CART-4498 recalculated: 3 items, subtotal 149.90 EUR", null, null);

    log(now, hours(0, 5), AuditLevel.ERROR, SimulatedAppLoggers.ORDER_SERVICE,
        "Order ORD-10427 could not be confirmed: shipping address validation failed",
        ADDRESS_STACKTRACE, SESSION_MGARCIA);
    log(now, hours(0, 3), AuditLevel.INFO, SimulatedAppLoggers.SHIPPING_SERVICE,
        "Shipment SHP-5540 delivered for order ORD-10411", null, null);
    log(now, hours(0, 2), AuditLevel.WARN, SimulatedAppLoggers.INVENTORY_SERVICE,
        "Stock for SKU-44012 fell to 5 units, below the reorder threshold of 10", null, null);

    logger.info("Preloaded {} activity log entries", logService.findAll().size());
  }

  private void log(Instant now, Duration age, AuditLevel level, String loggerName, String detail,
      String stacktrace, String sessionId) {
    logService.save(ActivityLogDto.builder().timestamp(now.minus(age)).level(level)
        .logger(loggerName).detail(detail).stacktrace(stacktrace).sessionId(sessionId).build());
  }

  private static Duration hours(int days, int hours) {
    return Duration.ofDays(days).plusHours(hours);
  }

  private static final String PAYMENT_STACKTRACE = """
      com.appjars.activitylog.demo.app.PaymentDeclinedException: Gateway responded 502 for authorization PAY-88221
      \tat com.appjars.activitylog.demo.app.PaymentGateway.capture(PaymentGateway.java:118)
      \tat com.appjars.activitylog.demo.app.OrderService.confirm(OrderService.java:74)
      \tat com.appjars.activitylog.demo.app.OrderController.postOrder(OrderController.java:52)
      Caused by: java.net.SocketTimeoutException: Read timed out
      \tat java.base/sun.nio.ch.NioSocketImpl.timedRead(NioSocketImpl.java:288)
      \tat java.base/sun.nio.ch.NioSocketImpl.implRead(NioSocketImpl.java:314)
      \t... 24 more
      """;

  private static final String INVENTORY_STACKTRACE = """
      com.appjars.activitylog.demo.app.OutOfStockException: No units of SKU-88710 available to reserve
      \tat com.appjars.activitylog.demo.app.InventoryService.reserve(InventoryService.java:96)
      \tat com.appjars.activitylog.demo.app.OrderService.confirm(OrderService.java:68)
      \tat com.appjars.activitylog.demo.app.OrderController.postOrder(OrderController.java:52)
      """;

  private static final String POOL_STACKTRACE = """
      java.sql.SQLTransientConnectionException: HikariPool-1 - Connection is not available, request timed out after 30000ms
      \tat com.zaxxer.hikari.pool.HikariPool.createTimeoutException(HikariPool.java:696)
      \tat com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:181)
      \tat com.appjars.activitylog.demo.app.InventoryRepository.findBySku(InventoryRepository.java:41)
      \tat com.appjars.activitylog.demo.app.InventoryService.reserve(InventoryService.java:88)
      """;

  private static final String ADDRESS_STACKTRACE = """
      com.appjars.activitylog.demo.app.AddressValidationException: Postal code 9999X is not valid for country ES
      \tat com.appjars.activitylog.demo.app.ShippingService.validate(ShippingService.java:57)
      \tat com.appjars.activitylog.demo.app.OrderService.confirm(OrderService.java:61)
      \tat com.appjars.activitylog.demo.app.OrderController.postOrder(OrderController.java:52)
      """;
}
