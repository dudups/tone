package com.ezone.ezproject.modules.event;

import com.ezone.ezproject.common.transactional.AfterCommit;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;

@Setter(onMethod_ = {@Autowired, @Lazy})
@NoArgsConstructor
@Slf4j
@Service
public class EventDispatcher {
    private List<IEventListener> listeners;

    public void dispatch(IEvent event) {
        if (event == null) {
            return;
        }
        for (IEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }

    @AfterCommit
    @Async
    public void asyncDispatch(Supplier<IEvent> supplier) {
        dispatch(supplier.get());
    }

    @AfterCommit
    @Async
    public void asyncDispatch(IEvent event) {
        if (event == null) {
            return;
        }
        dispatch(event);
    }
}
