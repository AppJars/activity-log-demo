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
package com.appjars.activitylog.demo.views.tour;

import com.appjars.activitylog.flow.util.TestIds;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.SerializableFunction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.vaadin.addons.antlerflow.tour.EngineType;
import org.vaadin.addons.antlerflow.tour.Tour;
import org.vaadin.addons.antlerflow.tour.TourButton;
import org.vaadin.addons.antlerflow.tour.TourButtonType;
import org.vaadin.addons.antlerflow.tour.TourStep;

/**
 * Factory of the guided tours offered by the demo. Steps are anchored either to the ids the appjar
 * views already set on their components or to the {@code data-testid} attributes they expose through
 * {@code TestIds}, resolved client-side to the first <em>visible</em> match so hidden duplicates
 * (the mobile filter dialog holds a copy of every filter field) cannot steal a step.
 *
 * <p>The tour engine is always {@link EngineType#DRIVER}: Driver.js is MIT licensed, while
 * Shepherd.js is not free for commercial use and this demo is public.
 */
public final class DemoTours {

  /** Session attribute used to start a tour after navigating to its view. */
  public static final String PENDING_TOUR_ATTRIBUTE = DemoTours.class.getName() + ".pendingTour";

  static final String KEY_PREFIX = "appjars.activitylog.demo.tour.";

  /** Attribute the resolver below sets on the first visible match of each step's selector. */
  private static final String TARGET_ATTR = "data-antler-target";

  // The list views hide their grid and show an empty-state hint instead while they have no rows, so
  // the steps that describe a list offer both and let the resolver pick whichever is showing.
  private static final String EMPTY_ROLES_HINT = "#no-roles-found-hint";
  private static final String EMPTY_RULES_HINT = "#no-rules-found-hint";

  /**
   * Resolves every step's selector to its first <em>visible</em> match and tags it with
   * {@value #TARGET_ATTR}, keeping the tags in sync as the view re-renders. A selector that matches
   * nothing gets no tag, so Driver centers that step, the fallback used by the license step when a
   * full license hides the restrictions bar, and by the row-actions steps while a list is still
   * empty. $0 is a JSON map of {stepId: cssSelector}.
   */
  private static final String RESOLVE_TARGETS_JS =
      """
      const MAP = JSON.parse($0);
      const ATTR = '%s';
      const resolve = () => {
        Object.keys(MAP).forEach(id => {
          let pick = null;
          for (const el of document.querySelectorAll(MAP[id])) {
            const r = el.getBoundingClientRect();
            if (r.width > 4 && r.height > 4) { pick = el; break; }
          }
          document.querySelectorAll("[" + ATTR + "='" + id + "']")
              .forEach(el => { if (el !== pick) { el.removeAttribute(ATTR); } });
          if (pick && pick.getAttribute(ATTR) !== id) { pick.setAttribute(ATTR, id); }
        });
      };
      if (window.__antlerResolver) { window.__antlerResolver.stop(); }
      let scheduled = false;
      const schedule = () => { if (scheduled) { return; } scheduled = true;
        requestAnimationFrame(() => { scheduled = false; resolve(); }); };
      resolve();
      const obs = new MutationObserver(schedule);
      obs.observe(document.body, {childList: true, subtree: true, attributes: true,
          attributeFilter: ['hidden', 'style', 'class']});
      window.__antlerResolver = { stop() { obs.disconnect();
        document.querySelectorAll('[' + ATTR + ']').forEach(el => el.removeAttribute(ATTR));
        window.__antlerResolver = null; } };
      """
          .formatted(TARGET_ATTR);

  /**
   * Undoes three things that would break the tour while it runs: Driver forces
   * {@code overflow:hidden} on the highlighted element's parent (which clips its siblings, e.g. the
   * filter fields next to the highlighted button), Vaadin's modal overlays set
   * {@code pointer-events:none} on the body (which would make the tour's own Next button
   * unclickable while such an overlay is showing), and Driver leaves the highlighted element itself
   * interactive, the one hole in its overlay. A tour narrates a view, so nothing it points at is
   * meant to be operated: clicking a highlighted "new" button or grid row would navigate away and
   * leave the tour of the previous view running over the new one. Blocking pointer events is enough
   * because the row-actions steps deploy their menu with a programmatic {@code click()}, which
   * {@code pointer-events} does not affect.
   */
  private static final String TOUR_CSS_JS =
      """
      if (!document.getElementById('demo-tour-css')) {
        const style = document.createElement('style');
        style.id = 'demo-tour-css';
        style.textContent =
            'body :not(body):has(> .driver-active-element) { overflow: visible !important; }'
            + '.driver-popover { pointer-events: auto !important; }'
            + '.driver-active-element, .driver-active-element * '
            + '{ pointer-events: none !important; }';
        document.head.appendChild(style);
      }
      """;

  /**
   * In Vaadin 25 every overlay opens through the native Popover API, so it paints in the browser
   * top layer, above all z-indexed content. The Driver popover is ordinary DOM, so the steps that
   * open a menu would be hidden behind it: promote the popover to the top layer as well, and
   * re-assert it whenever another overlay opens afterwards (top-layer paint order follows the last
   * {@code showPopover()} call).
   */
  private static final String PROMOTE_TOP_LAYER_JS =
      """
      if (window.__demoTourTopLayer) { window.__demoTourTopLayer.stop(); }
      const promote = () => document.querySelectorAll('.driver-popover').forEach(el => {
        if (el.getAttribute('popover') !== 'manual') { el.setAttribute('popover', 'manual'); }
        el.style.margin = '0';
        try { if (!el.matches(':popover-open')) { el.showPopover(); } } catch (e) {}
      });
      const reassert = () => { const el = document.querySelector('.driver-popover');
        if (el && el.matches(':popover-open')) {
          try { el.hidePopover(); el.showPopover(); } catch (e) {}
        } };
      const onToggle = (e) => { const t = e.target;
        if (e.newState === 'open' && t && t.classList
            && !t.classList.contains('driver-popover')) { reassert(); } };
      document.addEventListener('toggle', onToggle, true);
      const obs = new MutationObserver(promote);
      obs.observe(document.body, {childList: true, subtree: true});
      promote();
      window.__demoTourTopLayer = { stop() { obs.disconnect();
        document.removeEventListener('toggle', onToggle, true);
        document.querySelectorAll('.driver-popover[popover]').forEach(el => {
          try { el.hidePopover(); } catch (e) {} el.removeAttribute('popover'); });
        window.__demoTourTopLayer = null; } };
      """;

  /**
   * antler-tour has no per-step hook, so the steps that describe a row's actions menu deploy it
   * from the client instead: Driver reuses a single {@code .driver-popover} and updates it in place,
   * so the active step is read from the hidden marker each step embeds in its content. $0 is a JSON
   * map of {stepId: selector of the element to click when that step is shown}; nothing is clicked
   * when the selector matches nothing, which is the case while a list is still empty.
   */
  private static final String MENU_HOOK_JS =
      """
      const MAP = JSON.parse($0);
      if (window.__demoTourMenus) { window.__demoTourMenus.stop(); }
      const closeOverlays = () => document
          .querySelectorAll('vaadin-menu-bar-overlay, vaadin-popover-overlay')
          .forEach(o => { o.opened = false; });
      // The actions menu lives in a grid cell, so its click would bubble up to the grid, which
      // selects the row and toggles its details. The menu bar handles the click in its own shadow
      // root (on the container that slots the buttons), i.e. before the event reaches the menu bar
      // element itself, so stopping it there opens the menu and leaves the row alone.
      const clickIsolated = (el) => {
        const menuBar = el.closest('vaadin-menu-bar');
        const stop = (e) => e.stopPropagation();
        if (menuBar) { menuBar.addEventListener('click', stop); }
        try { el.click(); } finally {
          if (menuBar) { menuBar.removeEventListener('click', stop); }
        }
      };
      const clickFirstVisible = (selector) => {
        for (const el of document.querySelectorAll(selector)) {
          const r = el.getBoundingClientRect();
          if (r.width > 4 && r.height > 4) { clickIsolated(el); return; }
        }
      };
      let current = null;
      const sync = () => {
        const marker = document.querySelector('.driver-popover [data-tour-step]');
        const id = marker ? marker.getAttribute('data-tour-step') : null;
        if (id === current) { return; }
        current = id;
        closeOverlays();
        const selector = id ? MAP[id] : null;
        if (selector) { setTimeout(() => clickFirstVisible(selector), 150); }
      };
      const obs = new MutationObserver(sync);
      obs.observe(document.body, {childList: true, subtree: true});
      window.__demoTourMenus = { stop() { obs.disconnect(); closeOverlays();
        window.__demoTourMenus = null; } };
      sync();
      """;

  /** Stops every client-side helper installed for the tour. */
  private static final String STOP_JS =
      """
      ['__antlerResolver', '__demoTourTopLayer', '__demoTourMenus']
          .forEach(k => { if (window[k]) { window[k].stop(); } });
      const css = document.getElementById('demo-tour-css');
      if (css) { css.remove(); }
      """;

  public enum DemoTour {
    ACTIVITY_LOG, EXTRACTORS, REMOVERS, LOG_VIEWS, SAVED_LOG_VIEW, EXTRACTOR_EDITOR, REMOVER_EDITOR,
    LOG_VIEW_EDITOR
  }

  /**
   * A step of a tour. {@code selector} is the real element selector, which the resolver tags on its
   * first visible match; {@code attach} tells whether the step anchors to (and highlights) that
   * match or renders centered.
   */
  private record StepDef(String key, String selector, String position, boolean attach) {

    String id() {
      return key.replace('.', '-');
    }

    /** Marker selector the step attaches to, or {@code null} when it renders centered. */
    String attachTo() {
      return attach && selector != null ? resolved(key) : null;
    }
  }

  private DemoTours() {}

  public static Tour create(DemoTour tour, SerializableFunction<String, String> translator) {
    List<StepDef> defs = steps(tour);
    List<TourStep> steps = new ArrayList<>();
    for (int i = 0; i < defs.size(); i++) {
      steps.add(step(defs.get(i), i == 0, i == defs.size() - 1, translator));
    }
    return Tour.builder().engineType(EngineType.DRIVER).steps(steps).showCancelButton(true)
        .allowClose(true).build();
  }

  /**
   * Creates the tour, attaches it to {@code host} and starts it, detaching it again once it is
   * completed or canceled.
   */
  public static void start(DemoTour tour, Component host,
      SerializableFunction<String, String> translator) {
    Tour t = create(tour, translator);
    host.getElement().appendChild(t.getElement());
    host.getElement().executeJs(TOUR_CSS_JS);
    host.getElement().executeJs(RESOLVE_TARGETS_JS, toJson(targets(tour)));
    t.addTourCompletedListener(e -> stop(t, host));
    t.addTourCanceledListener(e -> stop(t, host));
    t.start();
    // Every tour ends up over an overlay: the row actions menu, or the mobile filters dialog.
    host.getElement().executeJs(PROMOTE_TOP_LAYER_JS);
    host.getElement().executeJs(MENU_HOOK_JS, toJson(menuActions(tour)));
  }

  private static void stop(Tour tour, Component host) {
    host.getElement().executeJs(STOP_JS);
    tour.getElement().removeFromParent();
  }

  private static List<StepDef> steps(DemoTour tour) {
    return switch (tour) {
      case ACTIVITY_LOG -> activityLogSteps();
      case EXTRACTORS -> extractorsSteps();
      case REMOVERS -> removersSteps();
      case LOG_VIEWS -> logViewsSteps();
      case SAVED_LOG_VIEW -> savedLogViewSteps();
      case EXTRACTOR_EDITOR -> extractorEditorSteps();
      case REMOVER_EDITOR -> removerEditorSteps();
      case LOG_VIEW_EDITOR -> logViewEditorSteps();
    };
  }

  /** Every tour opens with a centered step, so the view is laid out before anything is anchored. */
  private static List<StepDef> activityLogSteps() {
    return List.of(
        centered("activitylog.intro"),
        anchored("activitylog.filters", testId(TestIds.LOG_FILTERS), "bottom"),
        anchored("activitylog.filter", "#filter-button", "bottom"),
        anchored("activitylog.grid", testId(TestIds.LOG_GRID), "top"),
        anchored("activitylog.live", "#live-toggle", "bottom"),
        anchored("activitylog.trigger", "#trigger-log-button", "right"),
        anchored("activitylog.license", testId(TestIds.RESTRICTIONS_BAR), "bottom"));
  }

  private static List<StepDef> extractorsSteps() {
    return List.of(
        centered("extractors.intro"),
        over("extractors.grid", "#extractors-grid, " + EMPTY_ROLES_HINT),
        anchored("extractors.filters", testId(TestIds.LIST_FILTERS), "bottom"),
        anchored("extractors.new", "#new-extractor-button", "bottom"),
        deployingMenu("extractors.actions", testId(TestIds.EXTRACTOR_ACTIONS_MENU)),
        anchored("extractors.license", testId(TestIds.RESTRICTIONS_BAR), "bottom"));
  }

  private static List<StepDef> removersSteps() {
    return List.of(
        centered("removers.intro"),
        over("removers.grid", "#removers-grid, " + EMPTY_ROLES_HINT),
        anchored("removers.filters", testId(TestIds.LIST_FILTERS), "bottom"),
        anchored("removers.new", "#new-remover-button", "bottom"),
        deployingMenu("removers.actions", testId(TestIds.REMOVER_ACTIONS_MENU)),
        anchored("removers.license", testId(TestIds.RESTRICTIONS_BAR), "bottom"));
  }

  private static List<StepDef> logViewsSteps() {
    return List.of(
        centered("logviews.intro"),
        anchored("logviews.grid", "#log-viewer-list-view vaadin-grid, " + EMPTY_RULES_HINT, "top"),
        anchored("logviews.filters", testId(TestIds.LIST_FILTERS), "bottom"),
        anchored("logviews.new", "#new-rule-button", "bottom"),
        deployingMenu("logviews.actions", testId(TestIds.LOG_VIEW_ACTIONS_MENU)),
        anchored("logviews.license", testId(TestIds.RESTRICTIONS_BAR), "bottom"));
  }

  /**
   * The editors are reached through {@link TourNavigation}, which hands them the preloaded entity,
   * so every step describes a filled-in form. Each step anchors to the layout wrapping a whole
   * section (title, controls and grid) rather than to its first control.
   */
  private static List<StepDef> extractorEditorSteps() {
    return List.of(
        centered("extractoreditor.intro"),
        anchored("extractoreditor.name", "#name-field", "bottom"),
        anchored("extractoreditor.timefilters", testId(TestIds.EDITOR_TIME_FILTERS), "bottom"),
        anchored("extractoreditor.levels", testId(TestIds.EDITOR_LOG_LEVELS), "top"),
        anchored("extractoreditor.stacktrace", testId(TestIds.EDITOR_STACKTRACE), "top"),
        anchored("extractoreditor.session", testId(TestIds.EDITOR_SESSION), "top"),
        over("extractoreditor.loggerfilters", testId(TestIds.EDITOR_LOGGER_FILTERS)),
        over("extractoreditor.detailfilters", testId(TestIds.EDITOR_DETAIL_FILTERS)),
        anchored("extractoreditor.calendar", testId(TestIds.EDITOR_CALENDAR), "left"),
        anchored("extractoreditor.actions", testId(TestIds.EDITOR_ACTIONS), "bottom"));
  }

  private static List<StepDef> removerEditorSteps() {
    return List.of(
        centered("removereditor.intro"),
        anchored("removereditor.name", "#name-field", "bottom"),
        anchored("removereditor.expiration", testId(TestIds.REMOVER_EXPIRATION), "bottom"),
        anchored("removereditor.timefilters", testId(TestIds.EDITOR_TIME_FILTERS), "bottom"),
        anchored("removereditor.levels", testId(TestIds.EDITOR_LOG_LEVELS), "top"),
        over("removereditor.loggerfilters", testId(TestIds.EDITOR_LOGGER_FILTERS)),
        over("removereditor.detailfilters", testId(TestIds.EDITOR_DETAIL_FILTERS)),
        anchored("removereditor.calendar", testId(TestIds.EDITOR_CALENDAR), "left"),
        anchored("removereditor.actions", testId(TestIds.EDITOR_ACTIONS), "bottom"));
  }

  /**
   * The log view editor is a flat form, so here a "section" is a field: every one of them decides
   * something different about the view that gets published.
   */
  /**
   * The saved view itself, the one the editor produces. Runs on the route the preloaded view is
   * published on, so the steps describe a view that is already configured and populated.
   */
  private static List<StepDef> savedLogViewSteps() {
    return List.of(
        centered("savedlogview.intro"),
        anchored("savedlogview.filters", testId(TestIds.LOG_FILTERS), "bottom"),
        anchored("savedlogview.grid", testId(TestIds.LOG_GRID), "top"),
        anchored("savedlogview.live", "#live-toggle", "bottom"),
        anchored("savedlogview.license", testId(TestIds.RESTRICTIONS_BAR), "bottom"));
  }

  private static List<StepDef> logViewEditorSteps() {
    return List.of(
        centered("logvieweditor.intro"),
        anchored("logvieweditor.title", "#title-textfield", "bottom"),
        anchored("logvieweditor.logger", "#logger-textfield", "bottom"),
        anchored("logvieweditor.levels", "#levels-combobox", "bottom"),
        anchored("logvieweditor.detail", "#detail-filter-textfield", "bottom"),
        anchored("logvieweditor.dates", testId(TestIds.LOG_VIEW_PERIOD), "bottom"),
        anchored("logvieweditor.route", "#route-textfield", "bottom"),
        anchored("logvieweditor.columns", "#columns-visible-group", "top"),
        anchored("logvieweditor.filters", "#filters-enabled-group", "top"),
        anchored("logvieweditor.live", "#live-enabled-checkbox", "top"),
        anchored("logvieweditor.actions", testId(TestIds.EDITOR_ACTIONS), "top"));
  }

  /** Step rendered centered on the screen, highlighting nothing. */
  private static StepDef centered(String key) {
    return new StepDef(key, null, null, false);
  }

  /** Step anchored to, and highlighting, the first visible match of {@code selector}. */
  private static StepDef anchored(String key, String selector, String position) {
    return new StepDef(key, selector, position, true);
  }

  /**
   * Step highlighting the first visible match of {@code selector} with the popover centered over it.
   * Driver only places a popover beside an element while that side has room for it, and pins it to
   * the bottom of the viewport, arrowless and detached from what it describes, when none of the four
   * sides fit. A list grid takes the whole width and all the height the filter row leaves, so
   * whether {@code "top"} works at all depends on how many rows those filters wrap into: {@code
   * "over"} is the only side that always lands on the grid.
   */
  private static StepDef over(String key, String selector) {
    return new StepDef(key, selector, "over", true);
  }

  /**
   * Step that deploys a row's actions menu while it is shown. It stays centered on purpose: Driver
   * anchors the popover to the <em>closed</em> control, so whichever side is chosen, the menu
   * unfolds right over it. The selector is still resolved, so the hook clicks the element the step
   * talks about, and clicks nothing while the list is empty.
   */
  private static StepDef deployingMenu(String key, String selector) {
    return new StepDef(key, selector, null, false);
  }

  /** {stepId: selector} map consumed by {@link #RESOLVE_TARGETS_JS}. */
  private static Map<String, String> targets(DemoTour tour) {
    Map<String, String> targets = new LinkedHashMap<>();
    steps(tour).stream().filter(def -> def.selector() != null)
        .forEach(def -> targets.put(def.id(), def.selector()));
    return targets;
  }

  /**
   * {stepId: selector to click} map consumed by {@link #MENU_HOOK_JS}. The selector goes through the
   * resolved marker, so the menu that gets deployed is exactly the row the step points at.
   */
  private static Map<String, String> menuActions(DemoTour tour) {
    return switch (tour) {
      case EXTRACTORS -> menuAction("extractors.actions");
      case REMOVERS -> menuAction("removers.actions");
      case LOG_VIEWS -> menuAction("logviews.actions");
      // The activity log and the three editors have no row actions to deploy.
      default -> Map.of();
    };
  }

  private static Map<String, String> menuAction(String key) {
    return Map.of(stepId(key), resolved(key) + " vaadin-menu-bar-button");
  }

  private static TourStep step(StepDef def, boolean first, boolean last,
      SerializableFunction<String, String> t) {
    List<TourButton> buttons = new ArrayList<>();
    if (!first) {
      buttons.add(TourButton.builder().label(t.apply(KEY_PREFIX + "btn.back")).secondary(true)
          .type(TourButtonType.PREVIOUS).build());
    }
    buttons.add(TourButton.builder().label(t.apply(KEY_PREFIX + (last ? "btn.done" : "btn.next")))
        .type(TourButtonType.NEXT).build());
    // Driver renders the content as HTML, so the hidden marker survives and lets the client-side
    // hooks tell which step is currently shown.
    String content = t.apply(KEY_PREFIX + def.key() + ".desc")
        + "<span hidden data-tour-step='" + def.id() + "'></span>";
    return TourStep.builder().id(def.id()).attachTo(def.attachTo()).position(def.position())
        .title(t.apply(KEY_PREFIX + def.key() + ".title")).content(content).buttons(buttons).build();
  }

  private static String stepId(String key) {
    return key.replace('.', '-');
  }

  private static String resolved(String key) {
    return "[" + TARGET_ATTR + "='" + stepId(key) + "']";
  }

  private static String testId(String id) {
    return "[data-testid='" + id + "']";
  }

  /**
   * Minimal JSON object writer: every key and value here is a step id or a CSS selector, and the
   * result travels as an {@code executeJs} parameter, so no escaping beyond this is needed.
   */
  private static String toJson(Map<String, String> map) {
    StringBuilder json = new StringBuilder("{");
    map.forEach((key, value) -> {
      if (json.length() > 1) {
        json.append(',');
      }
      json.append('"').append(key).append("\":\"").append(value).append('"');
    });
    return json.append('}').toString();
  }
}
