package com.mt.access.port.adapter.messaging;

import com.mt.common.domain.CommonDomainRegistry;
import com.mt.common.domain.model.constant.AppInfo;
import com.mt.common.domain.model.domain_event.DomainEvent;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class ListenerHelper {
    public static <T extends DomainEvent> void listen(T event, Consumer<T> consumer) {
        listen(event, consumer, 1);
    }

    public static <T extends DomainEvent> void listen(T event, Consumer<T> consumer,
                                                      Integer concurrent) {
        IntStream.range(0, concurrent).forEach((ignore) -> {
            Class<? extends DomainEvent> aClass = event.getClass();
            CommonDomainRegistry.getEventStreamService()
                .of(AppInfo.MT_ACCESS_APP_ID, event.getInternal(), event.getTopic(), (Class<T>) aClass, consumer);
        });
    }
}
