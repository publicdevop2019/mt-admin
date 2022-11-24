package com.mt.access.application.endpoint.command;

import java.io.Serializable;
import lombok.Data;

@Data
public class EndpointCreateCommand implements Serializable {
    private static final long serialVersionUID = 1;
    private String name;
    private String description;
    private boolean secured;
    private boolean isWebsocket;
    private boolean csrfEnabled;
    private boolean shared;
    private String corsProfileId;
    private String cacheProfileId;
    private String resourceId;
    private String path;
    private boolean external;
    private int maxInvokePerSecond;
    private int maxInvokePerMinute;
    private String method;
}
