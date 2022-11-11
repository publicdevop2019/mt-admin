package com.mt.common.domain.model.domain_event.event;

import com.mt.common.domain.model.domain_event.AnyDomainId;
import com.mt.common.domain.model.domain_event.DomainEvent;
import com.mt.common.domain.model.domain_event.StoredEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class UnrountableMsgReceivedEvent extends DomainEvent {
    public static final String UNROUTABLE_MSG_EVENT = "unroutable_msg_event";
    public static final String name = "UNROUTABLE_MSG_EVENT";
    private String sourceTopic;
    private long sourceEventId;
    public UnrountableMsgReceivedEvent(StoredEvent event) {
        super();
        setTopic(UNROUTABLE_MSG_EVENT);
        setName(name);
        this.sourceTopic = event.getTopic();
        this.sourceEventId = event.getId();
        this.setDomainId(new AnyDomainId(event.getDomainId()));

    }

}