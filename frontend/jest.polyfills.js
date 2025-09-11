// Polyfills for Jest testing environment

// TextEncoder/TextDecoder polyfill
const { TextEncoder, TextDecoder } = require('util');

if (typeof global.TextEncoder === 'undefined') {
  global.TextEncoder = TextEncoder;
}

if (typeof global.TextDecoder === 'undefined') {
  global.TextDecoder = TextDecoder;
}

// AbortController polyfill
if (typeof global.AbortController === 'undefined') {
  global.AbortController = class AbortController {
    constructor() {
      this.signal = {
        aborted: false,
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
        dispatchEvent: jest.fn()
      };
    }
    
    abort() {
      this.signal.aborted = true;
    }
  };
}

// Request/Response polyfills for fetch
if (typeof global.Request === 'undefined') {
  global.Request = class Request {
    constructor(input, init = {}) {
      this.url = typeof input === 'string' ? input : input.url;
      this.method = init.method || 'GET';
      this.headers = new Map(Object.entries(init.headers || {}));
      this.body = init.body;
    }
  };
}

if (typeof global.Response === 'undefined') {
  global.Response = class Response {
    constructor(body, init = {}) {
      this.body = body;
      this.status = init.status || 200;
      this.statusText = init.statusText || 'OK';
      this.ok = this.status >= 200 && this.status < 300;
      this.headers = new Map(Object.entries(init.headers || {}));
    }
    
    async json() {
      return typeof this.body === 'string' ? JSON.parse(this.body) : this.body;
    }
    
    async text() {
      return typeof this.body === 'string' ? this.body : JSON.stringify(this.body);
    }
    
    async blob() {
      return new Blob([this.body]);
    }
    
    async arrayBuffer() {
      const text = await this.text();
      return new TextEncoder().encode(text).buffer;
    }
  };
}

// Headers polyfill
if (typeof global.Headers === 'undefined') {
  global.Headers = class Headers {
    constructor(init = {}) {
      this._headers = new Map();
      
      if (init) {
        if (init instanceof Headers) {
          init.forEach((value, key) => this.set(key, value));
        } else if (Array.isArray(init)) {
          init.forEach(([key, value]) => this.set(key, value));
        } else {
          Object.entries(init).forEach(([key, value]) => this.set(key, value));
        }
      }
    }
    
    append(name, value) {
      const existing = this._headers.get(name.toLowerCase());
      this._headers.set(name.toLowerCase(), existing ? `${existing}, ${value}` : value);
    }
    
    delete(name) {
      this._headers.delete(name.toLowerCase());
    }
    
    get(name) {
      return this._headers.get(name.toLowerCase()) || null;
    }
    
    has(name) {
      return this._headers.has(name.toLowerCase());
    }
    
    set(name, value) {
      this._headers.set(name.toLowerCase(), value);
    }
    
    forEach(callback, thisArg) {
      this._headers.forEach((value, key) => {
        callback.call(thisArg, value, key, this);
      });
    }
    
    keys() {
      return this._headers.keys();
    }
    
    values() {
      return this._headers.values();
    }
    
    entries() {
      return this._headers.entries();
    }
    
    [Symbol.iterator]() {
      return this._headers.entries();
    }
  };
}

// FormData polyfill
if (typeof global.FormData === 'undefined') {
  global.FormData = class FormData {
    constructor() {
      this._data = new Map();
    }
    
    append(name, value, filename) {
      const existing = this._data.get(name);
      if (existing) {
        if (Array.isArray(existing)) {
          existing.push(value);
        } else {
          this._data.set(name, [existing, value]);
        }
      } else {
        this._data.set(name, value);
      }
    }
    
    delete(name) {
      this._data.delete(name);
    }
    
    get(name) {
      const value = this._data.get(name);
      return Array.isArray(value) ? value[0] : value;
    }
    
    getAll(name) {
      const value = this._data.get(name);
      return Array.isArray(value) ? value : value ? [value] : [];
    }
    
    has(name) {
      return this._data.has(name);
    }
    
    set(name, value, filename) {
      this._data.set(name, value);
    }
    
    forEach(callback, thisArg) {
      this._data.forEach((value, key) => {
        if (Array.isArray(value)) {
          value.forEach(v => callback.call(thisArg, v, key, this));
        } else {
          callback.call(thisArg, value, key, this);
        }
      });
    }
    
    keys() {
      return this._data.keys();
    }
    
    values() {
      const values = [];
      this._data.forEach(value => {
        if (Array.isArray(value)) {
          values.push(...value);
        } else {
          values.push(value);
        }
      });
      return values[Symbol.iterator]();
    }
    
    entries() {
      const entries = [];
      this._data.forEach((value, key) => {
        if (Array.isArray(value)) {
          value.forEach(v => entries.push([key, v]));
        } else {
          entries.push([key, value]);
        }
      });
      return entries[Symbol.iterator]();
    }
    
    [Symbol.iterator]() {
      return this.entries();
    }
  };
}

// URLSearchParams polyfill
if (typeof global.URLSearchParams === 'undefined') {
  global.URLSearchParams = class URLSearchParams {
    constructor(init = '') {
      this._params = new Map();
      
      if (typeof init === 'string') {
        if (init.startsWith('?')) {
          init = init.slice(1);
        }
        init.split('&').forEach(pair => {
          if (pair) {
            const [key, value = ''] = pair.split('=');
            this.append(decodeURIComponent(key), decodeURIComponent(value));
          }
        });
      } else if (init instanceof URLSearchParams) {
        init.forEach((value, key) => this.append(key, value));
      } else if (Array.isArray(init)) {
        init.forEach(([key, value]) => this.append(key, value));
      } else if (init && typeof init === 'object') {
        Object.entries(init).forEach(([key, value]) => this.append(key, value));
      }
    }
    
    append(name, value) {
      const existing = this._params.get(name);
      if (existing) {
        if (Array.isArray(existing)) {
          existing.push(String(value));
        } else {
          this._params.set(name, [existing, String(value)]);
        }
      } else {
        this._params.set(name, String(value));
      }
    }
    
    delete(name) {
      this._params.delete(name);
    }
    
    get(name) {
      const value = this._params.get(name);
      return Array.isArray(value) ? value[0] : value || null;
    }
    
    getAll(name) {
      const value = this._params.get(name);
      return Array.isArray(value) ? value : value ? [value] : [];
    }
    
    has(name) {
      return this._params.has(name);
    }
    
    set(name, value) {
      this._params.set(name, String(value));
    }
    
    sort() {
      const sorted = new Map([...this._params.entries()].sort());
      this._params = sorted;
    }
    
    toString() {
      const params = [];
      this._params.forEach((value, key) => {
        if (Array.isArray(value)) {
          value.forEach(v => {
            params.push(`${encodeURIComponent(key)}=${encodeURIComponent(v)}`);
          });
        } else {
          params.push(`${encodeURIComponent(key)}=${encodeURIComponent(value)}`);
        }
      });
      return params.join('&');
    }
    
    forEach(callback, thisArg) {
      this._params.forEach((value, key) => {
        if (Array.isArray(value)) {
          value.forEach(v => callback.call(thisArg, v, key, this));
        } else {
          callback.call(thisArg, value, key, this);
        }
      });
    }
    
    keys() {
      const keys = [];
      this._params.forEach((value, key) => {
        if (Array.isArray(value)) {
          value.forEach(() => keys.push(key));
        } else {
          keys.push(key);
        }
      });
      return keys[Symbol.iterator]();
    }
    
    values() {
      const values = [];
      this._params.forEach(value => {
        if (Array.isArray(value)) {
          values.push(...value);
        } else {
          values.push(value);
        }
      });
      return values[Symbol.iterator]();
    }
    
    entries() {
      const entries = [];
      this._params.forEach((value, key) => {
        if (Array.isArray(value)) {
          value.forEach(v => entries.push([key, v]));
        } else {
          entries.push([key, value]);
        }
      });
      return entries[Symbol.iterator]();
    }
    
    [Symbol.iterator]() {
      return this.entries();
    }
  };
}

// Performance API polyfill
if (typeof global.performance === 'undefined') {
  global.performance = {
    now: () => Date.now(),
    mark: jest.fn(),
    measure: jest.fn(),
    getEntriesByName: jest.fn(() => []),
    getEntriesByType: jest.fn(() => []),
    clearMarks: jest.fn(),
    clearMeasures: jest.fn()
  };
}

// CustomEvent polyfill
if (typeof global.CustomEvent === 'undefined') {
  global.CustomEvent = class CustomEvent {
    constructor(type, options = {}) {
      this.type = type;
      this.detail = options.detail;
      this.bubbles = Boolean(options.bubbles);
      this.cancelable = Boolean(options.cancelable);
      this.composed = Boolean(options.composed);
    }
  };
}

// Event polyfill
if (typeof global.Event === 'undefined') {
  global.Event = class Event {
    constructor(type, options = {}) {
      this.type = type;
      this.bubbles = Boolean(options.bubbles);
      this.cancelable = Boolean(options.cancelable);
      this.composed = Boolean(options.composed);
      this.defaultPrevented = false;
      this.preventDefault = () => {
        this.defaultPrevented = true;
      };
      this.stopPropagation = jest.fn();
      this.stopImmediatePropagation = jest.fn();
    }
  };
}

// DOMRect polyfill
if (typeof global.DOMRect === 'undefined') {
  global.DOMRect = class DOMRect {
    constructor(x = 0, y = 0, width = 0, height = 0) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.top = y;
      this.right = x + width;
      this.bottom = y + height;
      this.left = x;
    }
  };
}

// MutationObserver polyfill
if (typeof global.MutationObserver === 'undefined') {
  global.MutationObserver = class MutationObserver {
    constructor(callback) {
      this.callback = callback;
    }
    
    observe() {}
    disconnect() {}
    takeRecords() {
      return [];
    }
  };
}