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
package com.appjars.activitylog.demo.views;

import com.appjars.activitylog.demo.services.DemoLogTrigger;
import com.appjars.activitylog.demo.views.tour.DemoTours;
import com.appjars.activitylog.demo.views.tour.DemoTours.DemoTour;
import com.appjars.activitylog.demo.views.tour.TourNavigation;
import com.appjars.activitylog.flow.view.ActivityLogView;
import com.appjars.activitylog.flow.view.ExtractorsListView;
import com.appjars.activitylog.flow.view.LogViewerListView;
import com.appjars.activitylog.flow.view.RemoversListView;
import com.appjars.activitylog.model.AuditLevel;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility.AlignItems;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.Flex;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import java.util.Optional;

@SuppressWarnings("serial")
@AnonymousAllowed
public class MainLayout extends AppLayout implements AfterNavigationObserver {

  private static final String KEY_PREFIX = "appjars.activitylog.demo.";

  private final TourNavigation tourNavigation;

  private H2 viewTitle;

  /** "This page" tour entry, only enabled while a view that has a tour is showing. */
  private MenuItem thisPageTourItem;

  public MainLayout(TourNavigation tourNavigation) {
    this.tourNavigation = tourNavigation;
    setPrimarySection(Section.DRAWER);
    addDrawerContent();
    addHeaderContent();
  }

  private void addHeaderContent() {
    DrawerToggle toggle = new DrawerToggle();
    toggle.getElement().setAttribute("aria-label", "Menu toggle");

    viewTitle = new H2();
    viewTitle.addClassNames(FontSize.LARGE, Margin.NONE);
    viewTitle.addClassName(Flex.GROW);

    addToNavbar(true, toggle, viewTitle, createTourMenu());
  }

  /** Tour menu of the navigation bar. */
  private MenuBar createTourMenu() {
    MenuBar menu = new MenuBar();
    menu.addClassName("tour-menu");
    menu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY);
    SubMenu tours =
        menu.addItem(new Div(VaadinIcon.MAP_MARKER.create(), new Span(tourLabel("button"))))
            .getSubMenu();
    thisPageTourItem = tours.addItem(tourLabel("home"), e -> startCurrentTour());
    tours.addSeparator();
    tours.addItem(tourLabel("activitylog"), e -> startTour(DemoTour.ACTIVITY_LOG));
    tours.addItem(tourLabel("extractors"), e -> startTour(DemoTour.EXTRACTORS));
    tours.addItem(tourLabel("removers"), e -> startTour(DemoTour.REMOVERS));
    tours.addItem(tourLabel("logviews"), e -> startTour(DemoTour.LOG_VIEWS));
    tours.addItem(tourLabel("savedlogview"), e -> startTour(DemoTour.SAVED_LOG_VIEW));
    tours.addSeparator();
    tours.addItem(tourLabel("extractoreditor"), e -> startTour(DemoTour.EXTRACTOR_EDITOR));
    tours.addItem(tourLabel("removereditor"), e -> startTour(DemoTour.REMOVER_EDITOR));
    tours.addItem(tourLabel("logvieweditor"), e -> startTour(DemoTour.LOG_VIEW_EDITOR));
    return menu;
  }

  /** Reuses the tour labels of the landing page, so both menus stay in sync. */
  private String tourLabel(String key) {
    return getTranslation(KEY_PREFIX + "home.tour." + key);
  }

  /**
   * Starts a tour: if its view is already showing it runs right away, on whatever the visitor has
   * open, rather than navigating away from it. Otherwise {@link TourNavigation} takes it to its
   * view, where {@link #startPendingTour()} picks it up.
   */
  private void startTour(DemoTour tour) {
    if (currentTour().filter(tour::equals).isPresent()) {
      DemoTours.start(tour, this, this::getTranslation);
      return;
    }
    getUI().ifPresent(ui -> tourNavigation.navigateTo(tour, ui));
  }

  private void startCurrentTour() {
    currentTour().ifPresent(tour -> DemoTours.start(tour, this, this::getTranslation));
  }

  /** The tour of the view currently being shown, if it has one. */
  private Optional<DemoTour> currentTour() {
    return getContent() == null ? Optional.empty() : tourNavigation.tourOf(getContent().getClass());
  }

  private void addDrawerContent() {
    VerticalLayout drawerLayout = new VerticalLayout();
    drawerLayout.addClassNames(Margin.NONE, Padding.NONE, AlignItems.STRETCH, Gap.XSMALL);
    drawerLayout.setSizeFull();

    Image logo = new Image("/icons/icon.png", null);
    logo.setHeight("5vh");
    logo.setWidth("5vh");

    H3 title = new H3(getTranslation(KEY_PREFIX + "layout.drawertitle"));
    title.addClassName(Flex.GROW);

    Header header = new Header(logo, title);
    header.addClassNames(Display.FLEX, Gap.XSMALL, AlignItems.CENTER, Margin.MEDIUM);

    Scroller scroller = new Scroller(createNavigation());

    Footer footer = new Footer(createTriggerLogButton());
    footer.getStyle().set("padding", "var(--lumo-space-s)");

    drawerLayout.add(header, scroller);
    drawerLayout.expand(scroller);

    addToDrawer(drawerLayout, footer);
  }

  /**
   * Generates activity on demand: one entry per level, as the simulated application would produce
   * it, plus a log the visitor writes by hand. Belongs to the demo, not to the appjar: it is what
   * lets an extractor be seen capturing (or filtering out) a log, and the Live toggle showing it.
   */
  private Component createTriggerLogButton() {
    Button trigger = new Button(getTranslation(KEY_PREFIX + "layout.triggerlog.title"));
    trigger.setId("trigger-log-button");
    trigger.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    trigger.setWidthFull();

    ContextMenu levels = new ContextMenu(trigger);
    levels.setOpenOnClick(true);
    for (AuditLevel level : AuditLevel.values()) {
      levels.addItem(level.name(), e -> logSent(DemoLogTrigger.trigger(level), level));
    }
    levels.addSeparator();
    levels.addItem(getTranslation(KEY_PREFIX + "layout.triggerlog.custom"),
        e -> new CustomLogDialog(loggerName -> Notification
            .show(getTranslation(KEY_PREFIX + "layout.triggerlog.sent", loggerName))).open());

    return trigger;
  }

  private void logSent(String loggerName, AuditLevel level) {
    Notification
        .show(getTranslation(KEY_PREFIX + "layout.triggerlog.sentlevel", level.name(), loggerName));
  }

  private SideNav createNavigation() {
    SideNav nav = new SideNav();

    SideNavItem homeItem =
        new SideNavItem(getTranslation(KEY_PREFIX + "menuitem.home"), HomeView.class);
    homeItem.setPrefixComponent(VaadinIcon.HOME.create());

    SideNavItem activityLogItem =
        new SideNavItem(getTranslation(KEY_PREFIX + "menuitem.activityLogItem"));
    activityLogItem.setPrefixComponent(VaadinIcon.FILE_SEARCH.create());
    activityLogItem.setExpanded(true);

    activityLogItem.addItem(
        new SideNavItem(getTranslation(KEY_PREFIX + "menuitem.logs"), ActivityLogView.class),
        new SideNavItem(getTranslation(KEY_PREFIX + "menuitem.extractors"),
            ExtractorsListView.class),
        new SideNavItem(getTranslation(KEY_PREFIX + "menuitem.removers"), RemoversListView.class),
        new SideNavItem(getTranslation(KEY_PREFIX + "menuitem.logviewerview"),
            LogViewerListView.class));

    nav.addItem(homeItem, activityLogItem);

    return nav;
  }

  @Override
  public void afterNavigation(AfterNavigationEvent event) {
    updateTitle();
    thisPageTourItem.setEnabled(currentTour().isPresent());
    startPendingTour();
  }

  /**
   * Runs the tour stashed by the menu that navigated here. The stash is consumed either way: a
   * navigation that ends somewhere else, an editor forwarding back to its list because the entity
   * it needed is gone, means that tour is no longer going to happen, and leaving it behind would
   * fire it later on an unrelated navigation.
   */
  private void startPendingTour() {
    VaadinSession session = VaadinSession.getCurrent();
    if (session.getAttribute(DemoTours.PENDING_TOUR_ATTRIBUTE) instanceof DemoTour pending) {
      session.setAttribute(DemoTours.PENDING_TOUR_ATTRIBUTE, null);
      if (currentTour().filter(pending::equals).isPresent()) {
        DemoTours.start(pending, this, this::getTranslation);
      }
    }
  }

  /** Reflects the active view's dynamic title in the navbar, falling back to the app name. */
  private void updateTitle() {
    if (getContent() instanceof HasDynamicTitle hasTitle) {
      viewTitle.setText(hasTitle.getPageTitle());
    } else {
      viewTitle.setText(getTranslation(KEY_PREFIX + "layout.drawertitle"));
    }
  }
}
