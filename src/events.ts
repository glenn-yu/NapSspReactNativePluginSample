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
  private nativeEmitters = new Map<string, NativeEventEmitter>();
  // One nativeEmitter subscription per module+event pair
  private nativeSubscriptions = new Map<string, ReturnType<NativeEventEmitter['addListener']>>();

  setup(nativeModuleName: string): void {
    if (this.nativeEmitters.has(nativeModuleName)) {
      return;
    }

    const nativeModule = NativeModules[nativeModuleName];
    if (!nativeModule) {
      return;
    }

    const emitter = new NativeEventEmitter(nativeModule);
    this.nativeEmitters.set(nativeModuleName, emitter);
  }

  addListener(eventName: string, handler: EventHandler<any>): () => void {
    const cleanup = this.on(eventName, handler);

    for (const [moduleName, emitter] of this.nativeEmitters.entries()) {
      const key = `${moduleName}:${eventName}`;
      if (!this.nativeSubscriptions.has(key)) {
        const sub = emitter.addListener(eventName, (payload: any) => {
          this.emit(eventName, payload);
        });
        this.nativeSubscriptions.set(key, sub);
      }
    }

    return () => {
      cleanup();
      if (!this.hasListeners(eventName)) {
        for (const [key, sub] of this.nativeSubscriptions.entries()) {
          if (key.endsWith(`:${eventName}`)) {
            sub.remove();
            this.nativeSubscriptions.delete(key);
          }
        }
      }
    };
  }
}

export const globalEvents = new GlobalEventEmitter();
