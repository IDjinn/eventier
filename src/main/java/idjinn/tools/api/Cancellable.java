package idjinn.tools.api;

public interface Cancellable {
    boolean isCancelled();

    void setCancelled(boolean cancelled);
}
