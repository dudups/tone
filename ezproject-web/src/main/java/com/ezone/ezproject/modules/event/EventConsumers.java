package com.ezone.ezproject.modules.event;

import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@NoArgsConstructor
public class EventConsumers {
    private Map<Class<? extends IEvent>, Consumer<? extends IEvent>> consumers = new HashMap<>();

    public <E extends IEvent> void register(Class<E> clazz, Consumer<E> consumer) {
        consumers.put(clazz, consumer);
    }

    public void consumer(IEvent event) {
        if (event == null) {
            return;
        }
        Consumer consumer = consumers.get(event.getClass());
        if (consumer == null) {
            return;
        }
        consumer.accept(event);
    }
}
