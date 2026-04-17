export type EventHandler<TPayload> = (payload: TPayload) => void;

export class TypedEventEmitter<TEvents extends Record<string, any>> {
  private readonly listeners = new Map<keyof TEvents, Set<EventHandler<any>>>();

  on<K extends keyof TEvents>(event: K, handler: EventHandler<TEvents[K]>): () => void {
    const current = this.listeners.get(event) ?? new Set<EventHandler<TEvents[K]>>();
    current.add(handler);
    this.listeners.set(event, current as Set<EventHandler<any>>);

    return () => {
      const set = this.listeners.get(event);
      if (!set) {
        return;
      }
      set.delete(handler as EventHandler<any>);
      if (set.size === 0) {
        this.listeners.delete(event);
      }
    };
  }

  emit<K extends keyof TEvents>(event: K, payload: TEvents[K]): void {
    const set = this.listeners.get(event);
    if (!set) {
      return;
    }

    for (const handler of set) {
      handler(payload);
    }
  }

  removeAllListeners(): void {
    this.listeners.clear();
  }
}
