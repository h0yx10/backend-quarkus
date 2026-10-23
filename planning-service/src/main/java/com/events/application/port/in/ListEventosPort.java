package com.events.application.port.in;

import com.events.domain.entity.Evento;
import java.util.List;

public interface ListEventosPort {
    List<Evento> execute();
}
