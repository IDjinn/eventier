# Eventier

A library to make event handling easier in your java project. It does use reflection to capture methods anotated with `EventListener`, when subscribed they are called by priority.
They are scopped based by default, meaning you can restrict/propaate events as your infrascructure grows.

### Exemple
```java
public class CommandManager {
  private final EventHandler eventHandler = new EventHandler();

  public CommandManager() {
    this.eventHandler.subscribe(this);
  }
  
  @EventListener(priority = EventListenerPriority.High, listenCancelled = false)
  public void onCommandEvent(final CommandEvent event) {
    // ... do some work
    event.cancelled(true);
  }
}

public class Player {
  @EventListener(priority = EventListenerPriority.High, listenCancelled = false)
  public void onTalk(final TalkEvent event) {
    if (event.message().startsWith(">")) {
      commandManager.getEventHandler().emit(new CommandEvent(event.message());
      event.cancelled(true);
    }
  }
}

```
