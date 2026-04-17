import type { AdError } from './types';

export function normalizeAdError(error: unknown, fallbackCode = 'nap_ssp_error'): AdError {
  if (error && typeof error === 'object') {
    const maybeError = error as {
      code?: unknown;
      message?: unknown;
      nativeCode?: unknown;
      nativeDomain?: unknown;
      details?: unknown;
      userInfo?: unknown;
    };

    const message =
      typeof maybeError.message === 'string'
        ? maybeError.message
        : 'An unknown nap ssp error occurred.';

    const details: Record<string, unknown> = {};
    if (maybeError.details && typeof maybeError.details === 'object') {
      details.details = maybeError.details as Record<string, unknown>;
    }
    if (maybeError.userInfo && typeof maybeError.userInfo === 'object') {
      details.userInfo = maybeError.userInfo as Record<string, unknown>;
    }

    return {
      code: typeof maybeError.code === 'string' ? maybeError.code : fallbackCode,
      message,
      nativeCode: typeof maybeError.nativeCode === 'string' || typeof maybeError.nativeCode === 'number' ? maybeError.nativeCode : undefined,
      nativeDomain: typeof maybeError.nativeDomain === 'string' ? maybeError.nativeDomain : undefined,
      details: Object.keys(details).length > 0 ? details : undefined,
    };
  }

  if (typeof error === 'string') {
    return {
      code: fallbackCode,
      message: error,
    };
  }

  return {
    code: fallbackCode,
    message: 'An unknown nap ssp error occurred.',
  };
}
