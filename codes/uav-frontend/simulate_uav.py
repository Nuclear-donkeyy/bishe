import argparse
import json
import math
import threading
import time
from typing import List, Tuple, Optional

from paho.mqtt import client as mqtt

# MQTT broker config
BROKER_HOST = "localhost"
BROKER_PORT = 1883


class UavSimulator:
    def __init__(self, uavcode: str, init_batt: float, init_lat: float, init_lng: float):
        self.uavcode = uavcode
        self.battery = init_batt
        self.lat = init_lat
        self.lng = init_lng
        self.alt = 0.0
        self.state = "IDLE"  # IDLE / EXECUTING / RETURNING
        self.mission_id: Optional[str] = None
        self.route: List[Tuple[float, float]] = []
        self.route_index = 0
        self.speed_mps = 30.0  # 飞行速度，约 30m/s
        self.interval = 0.5    # 发送周期
        self.lock = threading.Lock()

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
        if self.state not in ("EXECUTING", "RETURNING") or not self.route:
            return
        if self.route_index >= len(self.route):
            self.state = "RETURNING"
            return
        target_lat, target_lng = self.route[self.route_index]
        # 简单直线插值，移动 speed_mps 对应的大约经纬度偏移（粗略，够用）
        step_deg = self.speed_mps / 111_000  # 每度约111km
        dlat = target_lat - self.lat
        dlng = target_lng - self.lng
        dist = math.hypot(dlat, dlng)
        if dist < step_deg:
            # 到达该航点
            self.lat, self.lng = target_lat, target_lng
            self.route_index += 1
            if self.route_index >= len(self.route):
                self.state = "RETURNING"
        else:
            self.lat += dlat / dist * step_deg
            self.lng += dlng / dist * step_deg
        # 电量下降
        self.battery = max(0, self.battery - 0.05)

    def _build_payload(self):
        # 构造传感器数据占位，可根据任务类型映射指标；此处演示为简易对象
        sensors = {}
        return {
            "uavCode": self.uavcode,
            "missionId": self.mission_id,
            "status": self.state,
            "lat": self.lat,
            "lng": self.lng,
            "battery": round(self.battery, 1),
            "sensors": sensors,
            "ts": time.time(),
        }

    def run(self):
        self.client.connect(BROKER_HOST, BROKER_PORT, keepalive=60)
        self.client.loop_start()
        # wait connection
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
                    # 在 RETURNING 时每轮都检查是否回到起点，距离小于 1 米则视为返航成功并停止移动
                    if self.state == "RETURNING":
                        if self.route:
                            home_lat, home_lng = self.route[0]
                            dist_home_m = math.hypot(self.lat - home_lat, self.lng - home_lng) * 111_000
                            if dist_home_m < 1.0:
                                self.state = "IDLE"
                                self.mission_id = None
                                self.route = []
                                self.route_index = 0
                        else:
                            self.state = "IDLE"
                            self.mission_id = None
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
    parser.add_argument("uavcode", help="无人机编号，例如 001 或 UAV001")
    parser.add_argument("battery", type=float, help="初始电量百分比")
    parser.add_argument("lat", type=float, help="初始纬度")
    parser.add_argument("lng", type=float, help="初始经度")
    args = parser.parse_args()

    sim = UavSimulator(args.uavcode, args.battery, args.lat, args.lng)
    sim.run()


if __name__ == "__main__":
    main()
