import argparse
import json
import math
import threading
import time
from typing import List, Optional, Tuple

from paho.mqtt import client as mqtt

# MQTT broker config
BROKER_HOST = "localhost"
BROKER_PORT = 1883


class UavSimulator:
    def __init__(self, uavcode: str, init_batt: float, init_lat: float, init_lng: float, sensors: str = ""):
        self.uavcode = uavcode
        self.battery = init_batt
        self.lat = init_lat
        self.lng = init_lng
        self.home_lat = init_lat
        self.home_lng = init_lng
        self.alt = 0.0
        self.state = "IDLE"  # IDLE / EXECUTING / RETURNING
        self.mission_id: Optional[str] = None
        self.route: List[Tuple[float, float]] = []
        self.route_index = 0
        self.speed_mps = 30.0  # 飞行速度，约 30m/s
        self.interval = 0.5  # 发送周期
        self.lock = threading.Lock()

        self.sensor_keys = [s.strip() for s in sensors.split(",") if s.strip()]
        self.sensor_state = {key: 50.0 for key in self.sensor_keys}

        self.client = mqtt.Client(client_id=f"UAV-{uavcode}", protocol=mqtt.MQTTv311)
        self.client.on_connect = self._on_connect
        self.client.on_disconnect = self._on_disconnect
        self.client.on_message = self._on_message
        self.connected = False

        self.topic_telemetry = f"uav/{uavcode}/telemetry"
        self.topic_command = f"uav/{uavcode}/command"

    # MQTT callbacks
    def _on_connect(self, client, userdata, flags, rc):
        self.connected = True
        print(f"[MQTT] connected rc={rc}")
        client.subscribe(self.topic_command)
        print(f"[MQTT] subscribed {self.topic_command}")

    def _on_disconnect(self, client, userdata, rc):
        self.connected = False
        print(f"[MQTT] disconnected rc={rc}")

    def _on_message(self, client, userdata, msg):
        try:
            payload = json.loads(msg.payload.decode("utf-8"))
        except Exception as e:
            print(f"[CMD] bad payload: {e}")
            return
        if payload.get("type") == "mission.start":
            route = payload.get("route") or []
            if len(route) < 2:
                print("[CMD] route too short, ignore")
                return
            mission_code = payload.get("missionCode") or payload.get("missionId")
            with self.lock:
                self.route = [(float(p[0]), float(p[1])) for p in route]
                self.route_index = 0
                self.mission_id = mission_code
                self.state = "EXECUTING"
            print(f"[CMD] received mission.start mission={mission_code}, points={len(route)}")
        elif payload.get("type") == "interrupt":
            print("[CMD] received interrupt, switching to RETURNING")
            with self.lock:
                self.state = "RETURNING"

    # Simulation step
    def _step_route(self):
        step_deg = self.speed_mps / 111_000  # 每度约111km
        if self.state not in ("EXECUTING", "RETURNING") or not self.route:
            if self.state == "RETURNING":
                target_lat, target_lng = self.home_lat, self.home_lng
            else:
                return
        elif self.state == "RETURNING":
            target_lat, target_lng = self.home_lat, self.home_lng
        else:
            if self.route_index >= len(self.route):
                self.state = "RETURNING"
                return
            target_lat, target_lng = self.route[self.route_index]

        dlat = target_lat - self.lat
        dlng = target_lng - self.lng
        dist = math.hypot(dlat, dlng)
        if dist < step_deg:
            self.lat, self.lng = target_lat, target_lng
            if self.state == "RETURNING":
                # run() 会检测是否到家并切 IDLE
                pass
            else:
                self.route_index += 1
                if self.route_index >= len(self.route):
                    self.state = "RETURNING"
        else:
            self.lat += dlat / dist * step_deg
            self.lng += dlng / dist * step_deg
        self.battery = max(0, self.battery - 0.05)

    def _build_payload(self):
        # 构建传感器数据（data），模拟数值上下波动
        sensors = {}
        for key in self.sensor_keys:
            base = self.sensor_state.get(key, 50.0)
            drift = math.sin(time.time()) * 2
            val = max(0, base + drift)
            self.sensor_state[key] = val
            sensors[key] = round(val, 2)
        return {
            "uavCode": self.uavcode,
            "missionId": self.mission_id,
            "status": self.state,
            "lat": self.lat,
            "lng": self.lng,
            "battery": round(self.battery, 1),
            "sensors": sensors,
            "data": sensors,
            "ts": time.time(),
        }

    def run(self):
        self.client.connect(BROKER_HOST, BROKER_PORT, keepalive=60)
        self.client.loop_start()
        for _ in range(50):
            if self.connected:
                break
            time.sleep(0.1)
        if not self.connected:
            print("[MQTT] connect timeout")
            return
        print(f"[SIM] publishing telemetry to {self.topic_telemetry}, command topic {self.topic_command}")
        try:
            while True:
                with self.lock:
                    if self.state == "RETURNING":
                        dist_home_m = math.hypot(self.lat - self.home_lat, self.lng - self.home_lng) * 111_000
                        if dist_home_m < 1.0:
                            self.state = "IDLE"
                            self.mission_id = None
                            self.route = []
                            self.route_index = 0
                    if self.state != "IDLE":
                        self._step_route()
                    payload = self._build_payload()
                self.client.publish(self.topic_telemetry, json.dumps(payload), qos=0)
                time.sleep(self.interval)
        except KeyboardInterrupt:
            print("[SIM] interrupted")
        finally:
            self.client.loop_stop()
            self.client.disconnect()


def main():
    parser = argparse.ArgumentParser(description="UAV telemetry & command simulator")
    parser.add_argument("uavcode", help="无人机编号，例如 001 或UAV001")
    parser.add_argument("battery", type=float, help="初始电量百分比")
    parser.add_argument("lat", type=float, help="初始纬度")
    parser.add_argument("lng", type=float, help="初始经度")
    parser.add_argument(
        "--sensors",
        type=str,
        default="",
        help="传感器指标列表，逗号分隔，对应 telemetry.data 的 key"
    )
    args = parser.parse_args()

    sim = UavSimulator(args.uavcode, args.battery, args.lat, args.lng, args.sensors)
    sim.run()


if __name__ == "__main__":
    main()
