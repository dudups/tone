package com.ezone.ezproject.modules.event;

import java.util.function.Consumer;

public class AbstractEventListener implements IEventListener {
    private final EventConsumers consumers = new EventConsumers();

    /**
     * 注册事件处理，注意不要为同一class重复注册多个consumer，会覆盖。不同的业务单独建立EventListener
     * @param clazz
     * @param consumer
     * @param <E>
     */
    protected <E extends IEvent> void registerEventConsumer(Class<E> clazz, Consumer<E> consumer) {
        consumers.register(clazz, consumer);
    }

    @Override
    public void onEvent(IEvent event) {
        consumers.consumer(event);
    }
}
