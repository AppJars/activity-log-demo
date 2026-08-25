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

/**
 * Loggers of the fictional order-management application this demo simulates. Both the preloaded log
 * entries ({@link DemoDataInitializer}) and the ones generated on demand ({@link DemoLogTrigger})
 * come from these loggers, and the preloaded extractor captures exactly this prefix.
 */
public final class SimulatedAppLoggers {

  /** Prefix the preloaded extractor and remover filter on. */
  public static final String LOGGER_PREFIX = "com.appjars.activitylog.demo.app";

  public static final String AUTH_SERVICE = LOGGER_PREFIX + ".AuthService";
  public static final String CART_SERVICE = LOGGER_PREFIX + ".CartService";
  public static final String ORDER_SERVICE = LOGGER_PREFIX + ".OrderService";
  public static final String ORDER_REPOSITORY = LOGGER_PREFIX + ".OrderRepository";
  public static final String PAYMENT_GATEWAY = LOGGER_PREFIX + ".PaymentGateway";
  public static final String INVENTORY_SERVICE = LOGGER_PREFIX + ".InventoryService";
  public static final String SHIPPING_SERVICE = LOGGER_PREFIX + ".ShippingService";

  private SimulatedAppLoggers() {}
}
