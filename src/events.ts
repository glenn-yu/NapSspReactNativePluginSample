import { NativeEventEmitter, NativeModules } from 'react-native';

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

  hasListeners(event: string): boolean {
    const set = this.listeners.get(event as keyof TEvents);
    return set != null && set.size > 0;
  }
}

/**
 * Global event emitter to handle native events from RCTDeviceEventEmitter.
 */
class GlobalEventEmitter extends TypedEventEmitter<any> {
  private nativeEmitter?: NativeEventEmitter;
  // One nativeEmitter subscription per event name (avoids duplicate firings)
  private nativeSubscriptions = new Map<string, ReturnType<NativeEventEmitter['addListener']>>();

  setup(nativeModuleName: string): void {
    if (this.nativeEmitter) {
      return;
    }

    const nativeModule = NativeModules[nativeModuleName];
    if (!nativeModule) {
      return;
    }

    this.nativeEmitter = new NativeEventEmitter(nativeModule);
  }

  addListener(eventName: string, handler: EventHandler<any>): () => void {
    const cleanup = this.on(eventName, handler);

    // Register with nativeEmitter only once per event name
    if (this.nativeEmitter && !this.nativeSubscriptions.has(eventName)) {
      const sub = this.nativeEmitter.addListener(eventName, (payload: any) => {
        this.emit(eventName, payload);
      });
      this.nativeSubscriptions.set(eventName, sub);
    }

    return () => {
      cleanup();
      // Remove nativeEmitter subscription when no JS listeners remain
      if (!this.hasListeners(eventName)) {
        this.nativeSubscriptions.get(eventName)?.remove();
        this.nativeSubscriptions.delete(eventName);
      }
    };
  }
}

export const globalEvents = new GlobalEventEmitter();
