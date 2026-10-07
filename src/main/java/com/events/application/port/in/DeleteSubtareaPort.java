package com.events.application.port.in;

import java.util.UUID;

public interface DeleteSubtareaPort {
    void execute(UUID subtareaId);
}
