type TelemetryPayload = {
  uavCode?: string;
  batteryPercent?: number;
  status?: string;
  lat?: number;
  lng?: number;
  alt?: number;
  [key: string]: any;
};

type TelemetrySocketOptions = {
  onMessage: (payload: TelemetryPayload) => void;
  onConnect?: () => void;
  onDisconnect?: () => void;
  /**
   * Additional uavCodes to subscribe to under `/topic/uav-telemetry/{uavCode}`.
   * The base topic `/topic/uav-telemetry` is always subscribed.
   */
  uavCodes?: string[];
};

type Subscription = {
  id: string;
  destination: string;
};

export type TelemetryStompClient = {
  deactivate: () => void;
};

const WS_BASE = 'http://localhost:8080/ws/uav-telemetry';
const RECONNECT_DELAY = 4000;

const createSockJsUrl = () => {
  const server = Math.floor(Math.random() * 1000)
    .toString()
    .padStart(3, '0');
  const session = Math.random().toString(36).substring(2, 10);
  return `${WS_BASE.replace(/^http/, 'ws')}/${server}/${session}/websocket`;
};

const parseStompFrame = (frame: string) => {
  const nullIndex = frame.indexOf('\0');
  const trimmed = nullIndex >= 0 ? frame.substring(0, nullIndex) : frame;
  const lines = trimmed.split('\n');
  const command = lines.shift() || '';
  const headers: Record<string, string> = {};

  while (lines.length) {
    const line = lines.shift();
    if (line === '') break;
    const [key, ...rest] = (line || '').split(':');
    headers[key] = rest.join(':');
  }

  const body = lines.join('\n');
  return { command, headers, body };
};

class SockJsStompClient {
  private socket?: WebSocket;
  private subscriptions: Subscription[] = [];
  private connected = false;
  private shouldReconnect = true;
  private reconnectTimer?: ReturnType<typeof setTimeout>;

  constructor(private readonly options: TelemetrySocketOptions) {}

  activate() {
    this.shouldReconnect = true;
    this.startConnection();
  }

  deactivate() {
    this.shouldReconnect = false;
    clearTimeout(this.reconnectTimer);
    this.socket?.close();
  }

  private startConnection() {
    const url = createSockJsUrl();
    this.socket = new WebSocket(url);

    this.socket.onopen = () => {
      this.sendStompFrame('CONNECT', {
        'accept-version': '1.2',
        'heart-beat': '10000,10000'
      });
    };

    this.socket.onmessage = event => {
      const data = event.data as string;
      if (data === 'o') return; // SockJS open frame

      if (data.startsWith('a')) {
        let messages: string[] = [];
        try {
          messages = JSON.parse(data.slice(1));
        } catch {
          return;
        }
        messages.forEach(frame => this.handleStompFrame(frame));
      }
    };

    this.socket.onerror = () => this.handleDisconnect();
    this.socket.onclose = () => this.handleDisconnect();
  }

  private handleDisconnect() {
    if (this.connected) {
      this.connected = false;
      this.options.onDisconnect?.();
    }

    if (this.shouldReconnect) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = setTimeout(() => this.startConnection(), RECONNECT_DELAY);
    }
  }

  private handleStompFrame(frame: string) {
    const { command, headers, body } = parseStompFrame(frame);

    if (command === 'CONNECTED') {
      this.connected = true;
      this.options.onConnect?.();
      this.resubscribe();
      return;
    }

    if (command === 'MESSAGE') {
      try {
        const payload = JSON.parse(body || '{}');
        this.options.onMessage(payload);
      } catch {
        // ignore parse errors
      }
    }
  }

  private resubscribe() {
    const topics = new Set<string>(['/topic/uav-telemetry']);
    (this.options.uavCodes || []).forEach(code => topics.add(`/topic/uav-telemetry/${code}`));

    this.subscriptions = Array.from(topics).map((destination, index) => ({
      id: `sub-${index}`,
      destination
    }));

    this.subscriptions.forEach(sub =>
      this.sendStompFrame('SUBSCRIBE', {
        id: sub.id,
        destination: sub.destination,
        ack: 'auto'
      })
    );
  }

  private sendStompFrame(command: string, headers: Record<string, string>) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) return;

    const lines = [command];
    Object.entries(headers).forEach(([key, value]) => lines.push(`${key}:${value}`));
    lines.push('', '');
    const frame = `${lines.join('\n')}\0`;
    this.socket.send(JSON.stringify([frame]));
  }
}

export function connectTelemetrySocket(options: TelemetrySocketOptions): TelemetryStompClient {
  const client = new SockJsStompClient(options);
  client.activate();
  return client;
}
