import idjinn.tools.api.EventListener;
import idjinn.tools.api.EventListenerPriority;
import idjinn.tools.core.EventHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EventHandlerTests {

    @Test
    public void test_emit_event() {
        final var event = new HelloWorldEvent();
        final var handler = new EventHandler();

        final var result = handler.onEvent(event);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(event, result);
    }

    @Test
    public void test_subscribe_event() {
        final var event = new HelloWorldEvent();
        final var handler = new EventHandler();

        final var listener = new ListenerWrapper();
        handler.subscribe(listener);

        final var result = handler.onEvent(event);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.flag);
    }

    @Test
    void test_unsubscribe_event() {
        final var event = new HelloWorldEvent();
        final var handler = new EventHandler();
        final var listener = new ListenerWrapper();
        handler.subscribe(listener);

        final var result = handler.onEvent(event);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.flag);

        final var success = handler.unsubscribe(listener);
        Assertions.assertTrue(success);
        Assertions.assertEquals(0, handler.getListeners().size());
    }

    public static class ListenerWrapper {
        @EventListener(priority = EventListenerPriority.High, listenCancelled = false)
        public void onHelloWorldEvent(final HelloWorldEvent event) {
            Assertions.assertNotNull(event);
            event.flag = true;
        }
    }
}
