package com.events.application.port.in;

import java.util.UUID;

public interface DeleteEventoPort {
    void execute(UUID id);
}
