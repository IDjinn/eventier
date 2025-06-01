package idjinn.tools.api;

import java.util.List;

public interface IEventHandler {
    boolean registerEvents(List<Class<?>> classes);

    boolean unregisterEvents(List<Class<?>> classes);

    <TListener> boolean subscribe(TListener listener);

    <TListener> boolean unsubscribe(TListener listener);

    <TEvent extends Event> TEvent onEvent(TEvent event);
}
