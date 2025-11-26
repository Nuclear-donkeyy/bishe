import time
import json
import math
import random
import argparse
from paho.mqtt import client as mqtt

BROKER_HOST = "localhost"   # Docker 映射出来的 1883
BROKER_PORT = 1883

connected = False  # 连接标记，在 on_connect 里置为 True


def on_connect(client, userdata, flags, rc):
    global connected
    if rc == 0:
        connected = True
        print("✅ 已连接到 MQTT Broker")
    else:
        print(f"❌ 连接失败，返回码 rc={rc}")


def on_disconnect(client, userdata, rc):
    global connected
    connected = False
    print(f"⚠️ 连接断开，rc={rc}")


def main():
    parser = argparse.ArgumentParser(description="UAV circle telemetry simulator")
    parser.add_argument("uavcode", help="无人机唯一标识，例如 001 或 UAV001")
    parser.add_argument("--radius", type=float, default=10.0, help="圆周半径，默认 10")
    parser.add_argument("--omega", type=float, default=1.0, help="角速度 rad/s，默认 1")
    parser.add_argument("--interval", type=float, default=0.1, help="发送间隔秒，默认 0.1s")
    args = parser.parse_args()

    uavcode = args.uavcode
    radius = args.radius
    omega = args.omega          # 角速度
    interval = args.interval    # 发送周期（秒）

    client_id = f"UAV-{uavcode}"
    topic_telemetry = f"uav/{uavcode}/telemetry"   # 注意不要前导 /，方便匹配 uav/+/telemetry

    client = mqtt.Client(
        client_id=client_id,
        protocol=mqtt.MQTTv311
    )

    client.on_connect = on_connect
    client.on_disconnect = on_disconnect

    print(f"🔌 正在连接到 MQTT Broker {BROKER_HOST}:{BROKER_PORT} ...")
    client.connect(BROKER_HOST, BROKER_PORT, keepalive=60)
    client.loop_start()

    # 等连接稳定一下
    for _ in range(50):  # 最多等 5 秒
        if connected:
            break
        time.sleep(0.1)

    if not connected:
        print("❌ 在超时时间内未能连接到 MQTT Broker，退出。")
        client.loop_stop()
        client.disconnect()
        return

    print(f"🚁 UAV {uavcode} 开始在主题 {topic_telemetry} 上发布圆周遥测数据...")
    start_ts = time.time()

    try:
        while True:
            t = time.time() - start_ts    # 从起飞到现在的时间（秒）
            theta = omega * t             # 角度 = ω * t

            x = radius * math.cos(theta)
            y = radius * math.sin(theta)

            payload = {
                "uavCode": uavcode,
                "x": x,
                "y": y,
                "battery": random.randint(50, 100),
                "ts": time.time()
            }

            # QoS=0 就够了，追求频率不追求每一帧可靠性
            result = client.publish(topic_telemetry, json.dumps(payload), qos=0)
            status = result[0]
            if status == 0:
                print(f"📤 {topic_telemetry} -> {payload}")
            else:
                print(f"❌ 发布失败，status={status}")

            time.sleep(interval)  # 100ms = 0.1s
    except KeyboardInterrupt:
        print("🛑 收到中断信号，准备退出...")
    finally:
        client.loop_stop()
        client.disconnect()
        print("👋 已断开与 MQTT Broker 的连接。")


if __name__ == "__main__":
    main()
