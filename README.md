# Activity Log AppJars: Demo

This is a runnable demo of the **Activity Log** AppJar: a drop-in module that turns the events that
matter in your application into a searchable, filterable audit trail, with its own Vaadin views,
no logging layer of your own to write.

## Running the demo

You need **Java 21** and **Maven 3.8+**. Nothing else: no database to install, no Docker.

Run `mvn` (the default goal is `spring-boot:run`) and open <http://localhost:8080>. No login is
needed. The application starts on a landing page that presents what the AppJar does, links to each
of its views and offers a **guided tour** of every one of them; the **Tour** menu in the top bar
starts a tour from anywhere in the app. Data is kept in a local H2 database under `./data`.

## What you can try

| View | What it does |
| --- | --- |
| **Logs** | The full activity log: filter by logger, detail, level, session and date-time range, sort and resize columns, inspect an entry in full (detail, session, stack trace), and turn **Live** on to see new entries pushed in as they are persisted. |
| **Extractors** | Rules that decide which events of your logging framework become activity logs: by level, by logger or detail pattern, within the days and times you choose, optionally storing the stack trace and the session id. |
| **Removers** | Retention rules: what the scheduled pruner is allowed to drop, and when. |
| **Log Views** | Saved, preconfigured versions of the log view (fixed filters, chosen columns, its own route) so each team gets the view it needs. |

The **Tour** menu also walks a saved view in use, and the three editors (extractor, remover and log
view) where the rules are actually configured. Each editor tour opens the preloaded entity, so it
explains a filled-in form rather than an empty one.

## What comes preloaded

The demo arrives configured as a real installation would be, so there is something to look at from
the first screen:

| Item | Preloaded as |
| --- | --- |
| **Activity** | A week of events from a fictional order-management application, from orders confirmed and paid to a payment that fails and leaves an order pending, stock running out, a locked account and a database pool that collapses and recovers, with stack traces on the errors and session ids on two of the visits. |
| **Extractor** | *Application activity*, **active**: captures every level of the `com.appjars.activitylog.demo.app` loggers, at any time, storing stack traces and session ids. The demo's own framework logs (Spring, Hibernate, Vaadin) fall outside that prefix and are not stored. |
| **Remover** | *Prune activity older than 1 hour*, **inactive** on purpose: every preloaded entry is older than an hour, so activating it makes the pruner drop them within seconds. That is the point: activate it and watch it work. |
| **Log view** | *Application errors*, published on `al/errors`: the `ERROR` level is preconfigured, so its users can filter by logger, detail, session and date-time, but never widen the level. |

The drawer's **Trigger log** menu continues the story: each level records one more event of the order
narrative, and **Custom log…** lets you write the logger, detail and level yourself. Entries are
emitted through the application's logging framework, not saved directly, so you can watch the
extractor capture them and the **Live** toggle show them right away, and watch nothing being stored
when the logger falls outside the extractor's filters.

Everything above is created only when missing, so restarting the demo neither duplicates nor resets
what you did. Delete the `./data` directory to get the whole set back.

## Free vs. full license

The demo runs in **free mode**: every feature is fully functional, limited to **100 logs per day**.
Once that limit is reached the log turns read-only and live updates are disabled. A full license
removes the limit: nothing else changes. Licenses are available at <https://www.appjars.com>.

## Learn more

* Documentation: <https://docs.appjars.com/activity-log/overview/>
* AppJars on GitHub: <https://github.com/AppJars>

## Configurable properties

Every property the AppJar reads is listed here, and all of them are written out in
`src/main/resources/application.properties` so they can be changed in place.

### View routes

Each view is published on the route configured here. A saved log view is published on the route
carried by its own configuration, not on one of these.

| Property | Default | View |
| --- | --- | --- |
| `com.appjars.activitylog.url.activitylog` | `al/activitylog` | The full activity log |
| `com.appjars.activitylog.url.extractors` | `al/extractors` | Extractor list |
| `com.appjars.activitylog.url.extractors-create` | `al/extractors/create` | Extractor form, creating |
| `com.appjars.activitylog.url.extractors-edit` | `al/extractors/edit` | Extractor form, editing |
| `com.appjars.activitylog.url.removers` | `al/removers` | Remover list |
| `com.appjars.activitylog.url.removers-create` | `al/removers/create` | Remover form, creating |
| `com.appjars.activitylog.url.removers-edit` | `al/removers/edit` | Remover form, editing |
| `com.appjars.activitylog.url.logviewer` | `al/logviewer` | Saved log view list |
| `com.appjars.activitylog.url.logviewer-create` | `al/logviewer/create` | Saved log view form, creating |
| `com.appjars.activitylog.url.logviewer-edit` | `al/logviewer/edit` | Saved log view form, editing |

### Retention

| Property | Default | What it does |
| --- | --- | --- |
| `com.appjars.activitylog.logspruner.removalEnabled` | `true` | Whether the scheduled pruner may delete the logs the active removers cover. With it `false`, removers can be configured but nothing is ever deleted. |
| `com.appjars.activitylog.logspruner.pruningFrequency` | `5` | Seconds between pruning rounds. |
| `com.appjars.activitylog.logspruner.maxLogsPerRound` | `500` | Most logs a single round may delete. |

### Capture

| Property | Default | What it does |
| --- | --- | --- |
| `com.appjars.activitylog.appendersconnector.queueCapacity` | `1000` | Capacity of the queue between the appender and the thread that persists. A log arriving at a full queue is discarded rather than blocking the caller. |
| `com.appjars.activitylog.appender.autoRegister` | `true` | Register the appender automatically when `log4j2.xml` does not declare it. This demo declares it, so nothing is registered automatically. |
| `com.appjars.activitylog.appender.strict` | `true` | Fail startup when the logging setup cannot be captured. With it `false` the application starts anyway and the failure is reported as a warning. |

### Date and time formats

| Property | Default | What it does |
| --- | --- | --- |
| `com.appjars.activitylog.dateformat` | `dd-MM-yy` | Pattern the views use for a date. |
| `com.appjars.activitylog.timeformat` | `HH:mm` | Pattern the views use for a time of day. |
| `com.appjars.activitylog.datetimeformat` | `dd-MM-yyyy HH:mm:ss` | Pattern the views use for a timestamp. |
