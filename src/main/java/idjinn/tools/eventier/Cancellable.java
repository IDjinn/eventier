package idjinn.tools.eventier;

public interface Cancellable {
    boolean isCancelled();

    void setCancelled(boolean cancelled);
}
