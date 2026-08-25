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
package com.appjars.activitylog.demo;

import com.appjars.AppJarsAutoConfiguration;
import com.appjars.activitylog.ActivityLogAutoConfiguration;
import com.appjars.activitylog.demo.views.MainLayout;
import com.appjars.activitylog.flow.util.RouteConfigurer;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.spring.annotation.EnableVaadin;
import com.vaadin.flow.theme.lumo.Lumo;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SuppressWarnings("serial")
@SpringBootApplication
@ComponentScan(
    basePackageClasses = {ActivityLogAutoConfiguration.class, AppJarsAutoConfiguration.class})
@EnableJpaRepositories(basePackageClasses = ActivityLogAutoConfiguration.class)
@EnableVaadin(value = {"com.appjars.activitylog.flow", "com.appjars.activitylog.demo"})
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(Lumo.UTILITY_STYLESHEET)
@StyleSheet("styles.css")
@CssImport(value = "./styles/vaadin-date-time-picker.css", themeFor = "vaadin-date-time-picker")
@Push
@PWA(
    name = "Activity Log Demo",
    shortName = "Activity Log Demo",
    offlineResources = {"icons/icon.png"})
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {

  @Autowired RouteConfigurer routeConfigurer;

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @PostConstruct
  public void configure() {
    routeConfigurer.setViewsRouterLayout(MainLayout.class);
  }

  /**
   * Declares the browser tab icon. {@code @PWA} covers the installed application's icons but not
   * the favicon, which has to be added here.
   */
  @Override
  public void configurePage(AppShellSettings settings) {
    settings.addFavIcon("icon", "icons/icon.png", "180x180");
  }
}
