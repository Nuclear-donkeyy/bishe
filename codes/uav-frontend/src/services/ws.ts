const WS_BASE = 'ws://localhost:8080/ws/uav-telemetry';

export function connectTelemetrySocket(
  onMessage: (payload: any) => void,
  onError?: () => void
): WebSocket {
  const url = WS_BASE;
  const ws = new WebSocket(url);

  ws.onmessage = event => {
    try {
      const data = JSON.parse(event.data);
      onMessage(data);
    } catch (e) {
      // ignore parse errors
    }
  };

  ws.onerror = () => {
    onError?.();
  };
  ws.onclose = () => {
    onError?.();
  };

  return ws;
}
