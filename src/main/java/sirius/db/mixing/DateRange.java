/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.db.mixing;

import sirius.db.mixing.query.Query;
import sirius.db.mixing.query.constraints.Constraint;
import sirius.kernel.nls.NLS;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Represents a date range which will collect and count all matching entities in a query to provide
 * an appropriate facet filter.
 */
public class DateRange {

    private final String key;
    private final String name;
    private final LocalDateTime from;
    private final LocalDateTime until;
    private boolean useLocalDate;

    /**
     * Creates a new DateRange with the given unique key, translated (shown) name and two dates specifying the
     * range
     *
     * @param key   the unique name of the ranged used as filter value
     * @param name  the translated name shown to the user
     * @param from  the lower limit (including) of the range
     * @param until the upper limit (including) of the range
     */
    public DateRange(String key, String name, @Nullable LocalDateTime from, @Nullable LocalDateTime until) {
        this.key = key;
        this.name = name;
        this.from = from;
        this.until = until;
    }

    /**
     * Creates a date range filtering on the last five minutes.
     *
     * @return a date range for the given interval
     */
    public static DateRange lastFiveMinutes() {
        return new DateRange("5m", NLS.get("DateRange.5m"), LocalDateTime.now().minusMinutes(5), LocalDateTime.now());
    }

    /**
     * Creates a date range filtering on the last 15 minutes.
     *
     * @return a date range for the given interval
     */
    public static DateRange lastFiveteenMinutes() {
        return new DateRange("15m",
                             NLS.get("DateRange.15m"),
                             LocalDateTime.now().minusMinutes(15),
                             LocalDateTime.now());
    }

    /**
     * Creates a date range filtering on the last hour.
     *
     * @return a date range for the given interval
     */
    public static DateRange lastHour() {
        return new DateRange("1h", NLS.get("DateRange.1h"), LocalDateTime.now().minusHours(1), LocalDateTime.now());
    }

    /**
     * Creates a date range filtering on the last two hours.
     *
     * @return a date range for the given interval
     */
    public static DateRange lastTwoHours() {
        return new DateRange("2h", NLS.get("DateRange.2h"), LocalDateTime.now().minusHours(2), LocalDateTime.now());
    }

    /**
     * Creates a date range filtering on "today".
     *
     * @return a date range for the given interval
     */
    public static DateRange today() {
        return new DateRange("today", NLS.get("DateRange.today"), LocalDate.now().atStartOfDay(), LocalDateTime.now());
    }

    /**
     * Creates a date range filtering on "yesterday".
     *
     * @return a date range for the given interval
     */
    public static DateRange yesterday() {
        return new DateRange("yesterday",
                             NLS.get("DateRange.yesterday"),
                             LocalDate.now().minusDays(1).atStartOfDay(),
                             LocalDate.now().minusDays(1).atTime(23, 59));
    }

    /**
     * Creates a date range filtering on this week.
     *
     * @return a date range for the given interval
     */
    public static DateRange thisWeek() {
        return new DateRange("thisWeek",
                             NLS.get("DateRange.thisWeek"),
                             LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1).atStartOfDay(),
                             LocalDateTime.now());
    }

    /**
     * Creates a date range filtering on the last week.
     *
     * @return a date range for the given interval
     */
    public static DateRange lastWeek() {
        return new DateRange("lastWeek",
                             NLS.get("DateRange.lastWeek"),
                             LocalDate.now()
                                      .minusWeeks(1)
                                      .with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
                                      .atStartOfDay(),
                             LocalDate.now()
                                      .minusWeeks(1)
                                      .with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 7)
                                      .atTime(23, 59));
    }

    /**
     * Creates a date range filtering on the current month.
     *
     * @return a date range for the given interval
     */
    public static DateRange thisMonth() {
        return new DateRange("thisMonth",
                             NLS.get("DateRange.thisMonth"),
                             LocalDate.now().withDayOfMonth(1).atStartOfDay(),
                             LocalDateTime.now());
    }

    /**
     * Creates a date range filtering on the last month.
     *
     * @return a date range for the given interval
     */
    public static DateRange lastMonth() {
        return new DateRange("lastMonth",
                             NLS.get("DateRange.lastMonth"),
                             LocalDate.now().minusMonths(1).withDayOfMonth(1).atStartOfDay(),
                             LocalDate.now().withDayOfMonth(1).minusDays(1).atTime(23, 59));
    }

    /**
     * Creates a date range filtering on the current year.
     *
     * @return a date range for the given interval
     */
    public static DateRange thisYear() {
        return new DateRange("thisYear",
                             NLS.get("DateRange.thisYear"),
                             LocalDate.now().withDayOfYear(1).atStartOfDay(),
                             LocalDateTime.now());
    }

    /**
     * Creates a date range filtering on the last year.
     *
     * @return a date range for the given interval
     */
    public static DateRange lastYear() {
        return new DateRange("lastYear",
                             NLS.get("DateRange.lastYear"),
                             LocalDate.now().minusYears(1).withDayOfYear(1).atStartOfDay(),
                             LocalDate.now().withDayOfYear(1).minusDays(1).atStartOfDay());
    }

    /**
     * Creates a date range filtering on everything before this year.
     *
     * @return a date range for the given interval
     */
    public static DateRange beforeThisYear() {
        return new DateRange("beforeThisYear",
                             NLS.get("DateRange.beforeThisYear"),
                             null,
                             LocalDate.now().withDayOfYear(1).atStartOfDay());
    }

    /**
     * Creates a date range filtering on everything before the last year.
     *
     * @return a date range for the given interval
     */
    public static DateRange beforeLastYear() {
        return new DateRange("beforeLastYear",
                             NLS.get("DateRange.beforeLastYear"),
                             null,
                             LocalDate.now().minusYears(1).withDayOfYear(1).atStartOfDay());
    }

    /**
     * Can be used if the {@link DateRange} should be used on a database field of type {@link LocalDate} and not {@link
     * LocalDateTime}.
     *
     * @return the DateRange object itself for fluent method calls
     */
    public DateRange useLocalDate() {
        useLocalDate = true;
        return this;
    }

    public String getKey() {
        return key;
    }

    public LocalDateTime getFrom() {
        return from;
    }

    public LocalDateTime getUntil() {
        return until;
    }

    /**
     * Applies this date range to the given query in the given field.
     *
     * @param <C>   the constraint type to generate
     * @param field the field to filter on
     * @param qry   the query to expand
     */
    public <C extends Constraint> void applyTo(String field, Query<?, ?, C> qry) {
        List<C> constraints = new ArrayList<>(2);
        if (from != null) {
            constraints.add(qry.filters().gte(Mapping.named(field), useLocalDate ? from.toLocalDate() : from));
        }
        if (until != null) {
            constraints.add(qry.filters().lte(Mapping.named(field), useLocalDate ? until.toLocalDate() : until));
        }
        qry.where(qry.filters().and(constraints));
    }

    @Override
    public String toString() {
        return name;
    }
}
