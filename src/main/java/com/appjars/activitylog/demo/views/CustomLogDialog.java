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

import com.appjars.activitylog.demo.services.SimulatedAppLoggers;
import com.appjars.activitylog.demo.services.DemoLogTrigger;
import com.appjars.activitylog.model.AuditLevel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableConsumer;
import java.util.Arrays;

/**
 * Lets the visitor write a log by hand (logger, detail and level) and emit it through the
 * application's logging framework. Whether it ends up in the activity log is up to the active
 * extractors
 */
@SuppressWarnings("serial")
public class CustomLogDialog extends Dialog {

  private static final String KEY_PREFIX = "appjars.activitylog.demo.customlog.";

  private final TextField loggerField = new TextField(t("logger"));
  private final TextArea detailField = new TextArea(t("detail"));
  private final Select<AuditLevel> levelSelect = new Select<>();

  /**
   * @param onSent notified with the logger the log was emitted through, once it is sent
   */
  public CustomLogDialog(SerializableConsumer<String> onSent) {
    setHeaderTitle(t("title"));
    setId("custom-log-dialog");

    // Wide enough for a fully qualified logger name, and it caps the dialog so the hint wraps
    setWidth("28rem");

    loggerField.setValue(SimulatedAppLoggers.ORDER_SERVICE);
    loggerField.setId("custom-log-logger");

    detailField.setValue("Order ORD-10440 cancelled at the customer's request");
    detailField.setId("custom-log-detail");

    levelSelect.setLabel(t("level"));
    levelSelect.setItems(Arrays.asList(AuditLevel.values()));
    levelSelect.setValue(AuditLevel.INFO);
    levelSelect.setId("custom-log-level");

    Span hint = new Span(t("hint"));
    hint.getStyle().set("font-size", "var(--lumo-font-size-s)")
        .set("color", "var(--lumo-secondary-text-color)");

    // One field per row: the dialog's own content area lays its children out in a row
    VerticalLayout content = new VerticalLayout(loggerField, detailField, levelSelect, hint);
    content.setPadding(false);
    content.setSpacing(false);
    content.getThemeList().add("spacing-s");
    content.setWidthFull();
    loggerField.setWidthFull();
    detailField.setWidthFull();
    levelSelect.setWidthFull();
    add(content);

    Button send = new Button(t("send"), e -> {
      onSent.accept(DemoLogTrigger.trigger(loggerField.getValue(), detailField.getValue(),
          levelSelect.getValue()));
      close();
    });
    send.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    send.setId("custom-log-send");
    send.setEnabled(isValid());
    loggerField.setValueChangeMode(ValueChangeMode.EAGER);
    detailField.setValueChangeMode(ValueChangeMode.EAGER);
    loggerField.addValueChangeListener(e -> send.setEnabled(isValid()));
    detailField.addValueChangeListener(e -> send.setEnabled(isValid()));

    getFooter().add(new Button(t("cancel"), e -> close()), send);
  }

  private boolean isValid() {
    return !loggerField.getValue().isBlank() && !detailField.getValue().isBlank();
  }

  private String t(String key) {
    return getTranslation(KEY_PREFIX + key);
  }
}
