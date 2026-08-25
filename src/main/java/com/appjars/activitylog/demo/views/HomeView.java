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

import com.appjars.activitylog.demo.views.tour.DemoTours.DemoTour;
import com.appjars.activitylog.demo.views.tour.TourNavigation;
import com.appjars.activitylog.flow.view.ActivityLogView;
import com.appjars.activitylog.flow.view.ExtractorsListView;
import com.appjars.activitylog.flow.view.LogViewerListView;
import com.appjars.activitylog.flow.view.RemoversListView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Public landing page of the demo: presents the appjar features, the license model and offers
 * guided tours of the views.
 */
@SuppressWarnings("serial")
@AnonymousAllowed
@Route(value = "", layout = MainLayout.class)
public class HomeView extends VerticalLayout implements HasDynamicTitle {

  private static final String KEY_PREFIX = "appjars.activitylog.demo.home.";

  private static final String APPJARS_SITE_URL = "https://www.appjars.com";
  private static final String GITHUB_ORG_URL = "https://github.com/AppJars";
  private static final String DOCS_URL = "https://docs.appjars.com/activity-log/overview/";

  private final TourNavigation tourNavigation;

  public HomeView(TourNavigation tourNavigation) {
    this.tourNavigation = tourNavigation;
    addClassName("home-view");
    add(createHero(), createFeaturesSection(), createTryItSection(), createLicenseSection(),
        createLinksSection());
    setAlignItems(Alignment.STRETCH);
  }

  private Component createHero() {
    Image logo = new Image("icons/icon-appjars-full.png", t("hero.title"));
    logo.addClassName("home-logo");
    H1 title = new H1(t("hero.title"));
    Paragraph tagline = new Paragraph(t("hero.tagline"));
    tagline.addClassName("home-tagline");

    Div hero = new Div(logo, title, tagline);
    hero.setId("home-hero");
    hero.addClassName("home-hero");
    return hero;
  }

  private Component createFeaturesSection() {
    Div cards = new Div(
        featureCard(VaadinIcon.RECORDS, "features.auditlog"),
        featureCard(VaadinIcon.SEARCH, "features.filters"),
        featureCard(VaadinIcon.BOLT, "features.live"),
        featureCard(VaadinIcon.DOWNLOAD, "features.extractors"),
        featureCard(VaadinIcon.TRASH, "features.removers"),
        featureCard(VaadinIcon.LIST, "features.logviews"),
        featureCard(VaadinIcon.COG, "features.configurable"));
    cards.addClassName("home-features");

    return section("home-features", t("features.title"), cards);
  }

  private Card featureCard(VaadinIcon icon, String key) {
    Card card = new Card();
    card.addClassName("home-feature-card");
    Icon prefix = icon.create();
    prefix.addClassName("home-feature-icon");
    card.setHeaderPrefix(prefix);
    card.setTitle(t(key + ".title"));
    card.add(new Paragraph(t(key + ".desc")));
    return card;
  }

  private Component createTryItSection() {
    Paragraph intro = new Paragraph(t("tryit.intro"));
    Paragraph preloaded = new Paragraph(t("tryit.preloaded"));

    Div actions = new Div(
        viewButton("tryit.activitylog", ActivityLogView.class),
        viewButton("tryit.extractors", ExtractorsListView.class),
        viewButton("tryit.removers", RemoversListView.class),
        viewButton("tryit.logviews", LogViewerListView.class),
        createTourMenu());
    actions.addClassName("home-actions");

    return section("home-tryit", t("tryit.title"), intro, preloaded, actions);
  }

  private Button viewButton(String key, Class<? extends Component> view) {
    Button button = new Button(t(key), e -> getUI().ifPresent(ui -> ui.navigate(view)));
    button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    return button;
  }

  private Component createTourMenu() {
    MenuBar menu = new MenuBar();
    menu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY);
    SubMenu tours = menu
        .addItem(new Div(VaadinIcon.MAP_MARKER.create(), new Span(t("tour.button")))).getSubMenu();

    tours.addItem(t("tour.activitylog"), e -> startViewTour(DemoTour.ACTIVITY_LOG));
    tours.addItem(t("tour.extractors"), e -> startViewTour(DemoTour.EXTRACTORS));
    tours.addItem(t("tour.removers"), e -> startViewTour(DemoTour.REMOVERS));
    tours.addItem(t("tour.logviews"), e -> startViewTour(DemoTour.LOG_VIEWS));
    tours.addItem(t("tour.savedlogview"), e -> startViewTour(DemoTour.SAVED_LOG_VIEW));
    tours.addSeparator();
    tours.addItem(t("tour.extractoreditor"), e -> startViewTour(DemoTour.EXTRACTOR_EDITOR));
    tours.addItem(t("tour.removereditor"), e -> startViewTour(DemoTour.REMOVER_EDITOR));
    tours.addItem(t("tour.logvieweditor"), e -> startViewTour(DemoTour.LOG_VIEW_EDITOR));
    return menu;
  }

  private void startViewTour(DemoTour tour) {
    getUI().ifPresent(ui -> tourNavigation.navigateTo(tour, ui));
  }

  private Component createLicenseSection() {
    Paragraph desc = new Paragraph(t("license.desc"));
    Anchor link = new Anchor(APPJARS_SITE_URL, t("license.link"));
    link.setTarget("_blank");
    return section("home-license", t("license.title"), desc, new Paragraph(link));
  }

  private Component createLinksSection() {
    Anchor github = new Anchor(GITHUB_ORG_URL, t("links.github"));
    github.setTarget("_blank");
    Anchor docs = new Anchor(DOCS_URL, t("links.readme"));
    docs.setTarget("_blank");
    Div links = new Div(github, docs);
    links.addClassName("home-links");
    return section("home-links", t("links.title"), links);
  }

  private Div section(String id, String title, Component... content) {
    Div section = new Div();
    section.setId(id);
    section.addClassName("home-section");
    section.add(new H3(title));
    section.add(content);
    return section;
  }

  private String t(String key) {
    return getTranslation(KEY_PREFIX + key);
  }

  @Override
  public String getPageTitle() {
    return t("title");
  }
}
