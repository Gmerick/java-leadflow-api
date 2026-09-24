"""Demonstração HTTP opcional: Python 3, somente biblioteca padrão.
Inicie o JAR em outro terminal antes de executar este arquivo.
Cada execução cria novos registros fictícios no banco da API indicada.
"""
import json
import sys
import urllib.error
import urllib.request
import uuid

BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8082").rstrip("/")
# O demo deve usar acesso local/túnel de laboratório, sem proxy intermediário.
http = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def call(method, path, payload=None, expected=200):
    data = None if payload is None else json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(BASE + path, data=data, method=method,
                                     headers={"Content-Type": "application/json"})
    try:
        response = http.open(request, timeout=10)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        status = response.code
        result = json.loads(response.read().decode("utf-8"))
    print(f"{method} {path} -> {status}")
    if status != expected:
        raise RuntimeError(f"Esperado {expected}, recebido {status}: {result}")
    return result


def check(condition, message):
    if not condition:
        raise RuntimeError(message)


def main():
    token = uuid.uuid4().hex
    campaign = call("POST", "/api/campaigns", {
        "name": "Campanha laboratorio " + token[:6], "channel": "EMAIL", "cost": 100
    }, 201)
    suffix = int(token[:12], 16) % 100000000
    payload = {"campaignId": campaign["id"], "name": "Pessoa Exemplo A",
               "email": f"a-{token}@example.com", "phone": "119" + f"{suffix:08d}"}
    lead = call("POST", "/api/leads", payload, 201)
    call("POST", "/api/leads", payload, 409)
    second = dict(payload, name="Pessoa Exemplo B", email=f"b-{token}@example.com",
                  phone="119" + f"{(suffix + 1) % 100000000:08d}")
    call("POST", "/api/leads", second, 201)
    path = f"/api/leads/{lead['id']}/conversion"
    call("PATCH", path, {"revenue": 300})
    call("PATCH", path, {"revenue": 600}, 409)
    reports = call("GET", "/api/reports/campaigns")
    report = next(row for row in reports if row["campaignId"] == campaign["id"])
    expected = {"leads": 2, "conversions": 1, "conversionRatePercent": 50,
                "cost": 100, "revenue": 300, "costPerLead": 50, "roiPercent": 200}
    for key, value in expected.items():
        check(report[key] == value, f"Indicador divergente: {key}")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    print("DEMO OK: duplicidade, conversao unica e indicadores conferidos.")

if __name__ == "__main__":
    try:
        main()
    except (urllib.error.URLError, RuntimeError, ValueError, KeyError, StopIteration) as error:
        print(f"Falha na demonstracao: {error}. Confira se o JAR esta ativo e se a URL esta correta.", file=sys.stderr)
        sys.exit(1)
