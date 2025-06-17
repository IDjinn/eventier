package idjinn.tools.eventier;

import java.util.List;

public interface IEventHandler {
    void registerEvents(List<Class<?>> classes);

    void unregisterEvents(List<Class<?>> classes);

    <TListener> boolean subscribe(TListener listener);

    <TListener> boolean unsubscribe(TListener listener);

    <TEvent extends Event> TEvent onEvent(TEvent event);
}
