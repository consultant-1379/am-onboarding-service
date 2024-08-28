/*
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 */
package com.ericsson.amonboardingservice.logging;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class InMemoryAppender extends ListAppender<ILoggingEvent> {
    public void reset() {
        this.list.clear();
    }

    public boolean contains(String query, Level level) {
        return this.list.stream()
                .anyMatch(event -> event.toString().contains(query)
                        && event.getLevel().equals(level));
    }

    public int countEventsWithLevel(Level level) {
        return (int) this.list.stream()
                .filter(event -> level.equals(event.getLevel()))
                .count();
    }

    public int countEventsForLogger(String loggerName) {
        return (int) this.list.stream()
                .filter(event -> event.getLoggerName().contains(loggerName))
                .count();
    }

    public List<ILoggingEvent> search(String string) {
        return this.list.stream()
                .filter(event -> event.toString().contains(string))
                .collect(Collectors.toList());
    }

    public Map<String, String> searchMdcMap(String string, Level level) {
        final Optional<ILoggingEvent> iLoggingEvent = this.search(string, level)
                .stream()
                .min(Comparator.comparingLong(ILoggingEvent::getTimeStamp));

        return iLoggingEvent.map(ILoggingEvent::getMDCPropertyMap).orElse(Collections.emptyMap());
    }


    public Map<String, String> searchMdcMap(String string) {
        final Optional<ILoggingEvent> iLoggingEvent = this.search(string)
                .stream()
                .min(Comparator.comparingLong(ILoggingEvent::getTimeStamp));

        return iLoggingEvent.map(ILoggingEvent::getMDCPropertyMap).orElse(Collections.emptyMap());
    }

    public List<ILoggingEvent> search(String string, Level level) {
        return this.list.stream()
                .filter(event -> event.toString().contains(string)
                        && event.getLevel().equals(level))
                .collect(Collectors.toList());
    }

    public int getSize() {
        return this.list.size();
    }

    public List<ILoggingEvent> getLoggedEvents() {
        return Collections.unmodifiableList(this.list);
    }

    public ILoggingEvent getlastLoggingEvent() {
        return Collections.max(getLoggedEvents(), Comparator.comparing(ILoggingEvent::getTimeStamp));
    }

    public List<Map<String, String>> getListOfMdcMaps() {
        return this.list.stream()
                .map(ILoggingEvent::getMDCPropertyMap)
                .collect(Collectors.toList());
    }
}
