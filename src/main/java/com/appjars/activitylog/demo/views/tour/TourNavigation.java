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

import com.appjars.activitylog.business.service.ExtractorService;
import com.appjars.activitylog.business.service.LogViewerService;
import com.appjars.activitylog.business.service.RemoverService;
import com.appjars.activitylog.demo.services.DemoDataInitializer;
import com.appjars.activitylog.demo.views.tour.DemoTours.DemoTour;
import com.appjars.activitylog.flow.view.ActivityLogView;
import com.appjars.activitylog.flow.view.ExtractorView;
import com.appjars.activitylog.flow.view.ExtractorsListView;
import com.appjars.activitylog.flow.view.LogViewerCrudView;
import com.appjars.activitylog.flow.view.LogViewerListView;
import com.appjars.activitylog.flow.view.LogViewerView;
import com.appjars.activitylog.flow.view.RemoverView;
import com.appjars.activitylog.flow.view.RemoversListView;
import com.appjars.activitylog.model.ExtractorDto;
import com.appjars.activitylog.model.LogViewerDto;
import com.appjars.activitylog.model.RemoverDto;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Takes a tour to the view it runs on. Both tour menus, the landing page's and the navbar's, go
 * through here, so the mapping between a tour and its view lives in one place.
 *
 * <p>The three editors cannot be reached by URL: their {@code beforeEnter} reads the entity being
 * edited from the UI-scoped data and forwards back to the list view when it finds none, so the
 * appjar's own list views set it right before navigating. This service does the same, handing each
 * editor tour the entity {@link DemoDataInitializer} preloaded: a filled-in form tells the story
 * far better than an empty one, and it is the only way the delete action is even shown. Should the
 * evaluator have deleted it, the tour falls back to a blank form.
 */
@Service
public class TourNavigation {

  private final ExtractorService extractorService;
  private final RemoverService removerService;
  private final LogViewerService logViewerService;

  public TourNavigation(ExtractorService extractorService, RemoverService removerService,
      LogViewerService logViewerService) {
    this.extractorService = extractorService;
    this.removerService = removerService;
    this.logViewerService = logViewerService;
  }

  /** The view each tour runs on. */
  public Class<? extends Component> viewOf(DemoTour tour) {
    return switch (tour) {
      case ACTIVITY_LOG -> ActivityLogView.class;
      case EXTRACTORS -> ExtractorsListView.class;
      case REMOVERS -> RemoversListView.class;
      case LOG_VIEWS -> LogViewerListView.class;
      case SAVED_LOG_VIEW -> LogViewerView.class;
      case EXTRACTOR_EDITOR -> ExtractorView.class;
      case REMOVER_EDITOR -> RemoverView.class;
      case LOG_VIEW_EDITOR -> LogViewerCrudView.class;
    };
  }

  /**
   * The tour of a view, if it has one.
   *
   * <p>Saved views all use {@link LogViewerView}, regardless of their configured route. The demo
   * tour describes only the preloaded {@code al/errors} view, so it cannot be inferred from that
   * shared component class. {@link #navigateTo(DemoTour, UI)} always takes that tour to its route.
   */
  public Optional<DemoTour> tourOf(Class<?> viewClass) {
    return Arrays.stream(DemoTour.values()).filter(tour -> tour != DemoTour.SAVED_LOG_VIEW)
        .filter(tour -> viewOf(tour).equals(viewClass)).findFirst();
  }

  /**
   * Stashes the tour and navigates to its view, where {@code MainLayout} picks it up once the view
   * is showing.
   */
  public void navigateTo(DemoTour tour, UI ui) {
    VaadinSession.getCurrent().setAttribute(DemoTours.PENDING_TOUR_ATTRIBUTE, tour);
    prepareEntity(tour, ui);
    if (tour == DemoTour.SAVED_LOG_VIEW) {
      // A saved view is registered on the route its own configuration carries, and an evaluator can
      // publish more of them: navigating by class would be ambiguous as soon as there is a second
      // one. If the preloaded view was deleted the route no longer resolves, the appjar forwards
      // home, and the pending tour is dropped there rather than running over the wrong view.
      ui.navigate(DemoDataInitializer.LOG_VIEW_ROUTE);
      return;
    }
    ui.navigate(viewOf(tour));
  }

  /** Puts the entity an editor tour is about where the editor expects to find it. */
  private void prepareEntity(DemoTour tour, UI ui) {
    switch (tour) {
      case EXTRACTOR_EDITOR -> ComponentUtil.setData(ui, ExtractorDto.class,
          extractorService.findByName(DemoDataInitializer.EXTRACTOR_NAME)
              .orElseGet(ExtractorDto::new));
      case REMOVER_EDITOR -> ComponentUtil.setData(ui, RemoverDto.class,
          removerService.findByName(DemoDataInitializer.REMOVER_NAME).orElseGet(RemoverDto::new));
      case LOG_VIEW_EDITOR -> ComponentUtil.setData(ui, LogViewerDto.class,
          logViewerService.findByRoute(DemoDataInitializer.LOG_VIEW_ROUTE)
              .orElseGet(LogViewerDto::new));
      default -> {
        // The list views need nothing prepared.
      }
    }
  }
}
