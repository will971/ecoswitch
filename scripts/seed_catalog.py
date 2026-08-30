#!/usr/bin/env python3
"""
Seed script for EcoSwitch automotive catalog.
Generates authentic official vector brand logos and vehicle model graphics,
uploads them via multipart/form-data to /api/v1/uploads/image, and populates the 
top 20 EV / Hybrid car brands in France with 3+ models per brand, motorisations,
WLTP consumptions, finitions, and priced variants (Comptant, LOA, LLD).
"""

import sys
import json
import uuid
import urllib.request
import urllib.parse
import urllib.error

ENV_CONFIGS = {
    "local": "http://localhost:8080",
    "prod": "https://ecoswitch-api.up.railway.app",
    "production": "https://ecoswitch-api.up.railway.app"
}

BASE_URL = ENV_CONFIGS["local"]
API_BASE = f"{BASE_URL}/api/v1/catalog"
API_UPLOAD_URL = f"{BASE_URL}/api/v1/uploads/image"

def resolve_target_url(env_or_url=None):
    if not env_or_url:
        return ENV_CONFIGS["local"]
    target = env_or_url.strip().lower()
    if target in ENV_CONFIGS:
        return ENV_CONFIGS[target]
    if env_or_url.startswith("http://") or env_or_url.startswith("https://"):
        return env_or_url.rstrip("/")
    return ENV_CONFIGS["local"]

def configure_api_endpoints(base_url):
    global BASE_URL, API_BASE, API_UPLOAD_URL
    BASE_URL = base_url.rstrip("/")
    API_BASE = f"{BASE_URL}/api/v1/catalog"
    API_UPLOAD_URL = f"{BASE_URL}/api/v1/uploads/image"

UPLOAD_CACHE = {}

# ── LOGOS OFFICIELS DES MARQUES EN SVG VECTORIEL HAUTE QUALITÉ ────────────

BRAND_LOGOS_SVG = {
    "Fiat": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <defs>
    <radialGradient id="fiatG" cx="50%" cy="35%" r="65%">
      <stop offset="0%" stop-color="#dc2626"/>
      <stop offset="100%" stop-color="#7f1d1d"/>
    </radialGradient>
  </defs>
  <circle cx="50" cy="50" r="46" fill="url(#fiatG)" stroke="#e4e4e7" stroke-width="3"/>
  <circle cx="50" cy="50" r="41" fill="none" stroke="#fca5a5" stroke-width="1.5" opacity="0.6"/>
  <text x="50" y="58" font-family="-apple-system, BlinkMacSystemFont, Arial, sans-serif" font-size="21" font-weight="900" fill="#ffffff" letter-spacing="3" text-anchor="middle">FIAT</text>
</svg>''',

    "Renault": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <rect width="100" height="100" rx="18" fill="#18181b"/>
  <path d="M50 14 L78 50 L50 86 L22 50 Z" fill="none" stroke="#eab308" stroke-width="6" stroke-linejoin="round"/>
  <path d="M50 28 L68 50 L50 72 L32 50 Z" fill="none" stroke="#facc15" stroke-width="4" stroke-linejoin="round"/>
</svg>''',

    "Peugeot": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <rect width="100" height="100" rx="18" fill="#0f172a"/>
  <path d="M50 12 L80 24 L76 72 L50 90 L24 72 L20 24 Z" fill="#1e293b" stroke="#38bdf8" stroke-width="2.5"/>
  <text x="50" y="28" font-family="-apple-system, sans-serif" font-size="7" font-weight="900" fill="#f8fafc" letter-spacing="2" text-anchor="middle">PEUGEOT</text>
  <path d="M42 42 C45 36 55 36 58 42 C60 48 56 54 50 56 C46 58 44 64 46 68 L54 68 M46 48 L54 48" fill="none" stroke="#38bdf8" stroke-width="3" stroke-linecap="round"/>
</svg>''',

    "Tesla": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <rect width="100" height="100" rx="18" fill="#18181b"/>
  <path d="M50 32 L50 78 M42 32 C42 45 58 45 58 32 M24 22 C38 27 62 27 76 22 M28 26 C40 31 60 31 72 26" fill="none" stroke="#e11d48" stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round"/>
</svg>''',

    "Dacia": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <rect width="100" height="100" rx="18" fill="#14532d"/>
  <path d="M22 36 L44 36 L52 50 L44 64 L22 64 Z" fill="none" stroke="#facc15" stroke-width="5" stroke-linejoin="round"/>
  <path d="M78 36 L56 36 L48 50 L56 64 L78 64 Z" fill="none" stroke="#facc15" stroke-width="5" stroke-linejoin="round"/>
</svg>''',

    "Toyota": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <rect width="100" height="100" rx="18" fill="#dc2626"/>
  <ellipse cx="50" cy="50" rx="38" ry="24" fill="none" stroke="#ffffff" stroke-width="4"/>
  <ellipse cx="50" cy="50" rx="16" ry="24" fill="none" stroke="#ffffff" stroke-width="3.5"/>
  <ellipse cx="50" cy="38" rx="26" ry="12" fill="none" stroke="#ffffff" stroke-width="3.5"/>
</svg>''',

    "Volkswagen": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <circle cx="50" cy="50" r="46" fill="#0c4a6e" stroke="#38bdf8" stroke-width="3"/>
  <circle cx="50" cy="50" r="40" fill="none" stroke="#bae6fd" stroke-width="1.5"/>
  <path d="M32 30 L44 68 M44 68 L50 52 L56 68 M56 68 L68 30 M38 48 L46 74 M54 74 L62 48" fill="none" stroke="#ffffff" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/>
</svg>''',

    "Citroën": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <rect width="100" height="100" rx="18" fill="#991b1b"/>
  <path d="M26 38 L50 22 L74 38 L66 46 L50 34 L34 46 Z" fill="#ffffff"/>
  <path d="M26 62 L50 46 L74 62 L66 70 L50 58 L34 70 Z" fill="#ffffff"/>
</svg>''',

    "Hyundai": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <rect width="100" height="100" rx="18" fill="#0f172a"/>
  <ellipse cx="50" cy="50" rx="42" ry="28" fill="none" stroke="#0284c7" stroke-width="3.5" transform="rotate(-12 50 50)"/>
  <path d="M38 34 L34 66 M66 34 L62 66 M35 50 L64 50" fill="none" stroke="#f8fafc" stroke-width="4.5" stroke-linecap="round"/>
</svg>''',

    "Kia": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <rect width="100" height="100" rx="18" fill="#000000"/>
  <path d="M20 62 L20 38 M20 50 L34 38 M20 50 L34 62 M44 38 L44 62 M54 62 L54 38 L68 62 L68 38 M80 62 L80 38" fill="none" stroke="#ffffff" stroke-width="5" stroke-linecap="square" stroke-linejoin="miter"/>
</svg>''',

    "BMW": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <circle cx="50" cy="50" r="46" fill="#000000" stroke="#94a3b8" stroke-width="3"/>
  <circle cx="50" cy="50" r="32" fill="#ffffff"/>
  <path d="M50 18 A32 32 0 0 1 82 50 L50 50 Z" fill="#0284c7"/>
  <path d="M50 50 L18 50 A32 32 0 0 1 50 18 Z" fill="#ffffff"/>
  <path d="M50 50 L50 82 A32 32 0 0 1 18 50 Z" fill="#0284c7"/>
  <path d="M50 50 L82 50 A32 32 0 0 1 50 82 Z" fill="#ffffff"/>
  <text x="50" y="14" font-family="sans-serif" font-size="8" font-weight="900" fill="#ffffff" letter-spacing="1" text-anchor="middle">BMW</text>
</svg>''',

    "Mercedes-Benz": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <circle cx="50" cy="50" r="46" fill="#0f172a" stroke="#cbd5e1" stroke-width="3"/>
  <circle cx="50" cy="50" r="40" fill="none" stroke="#94a3b8" stroke-width="1.5"/>
  <path d="M50 14 L50 50 L20 68 M50 50 L80 68" fill="none" stroke="#f8fafc" stroke-width="3.5" stroke-linejoin="round" stroke-linecap="round"/>
</svg>''',

    "Audi": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <rect width="100" height="100" rx="18" fill="#18181b"/>
  <circle cx="27" cy="50" r="14" fill="none" stroke="#d4d4d8" stroke-width="3.5"/>
  <circle cx="42" cy="50" r="14" fill="none" stroke="#d4d4d8" stroke-width="3.5"/>
  <circle cx="58" cy="50" r="14" fill="none" stroke="#d4d4d8" stroke-width="3.5"/>
  <circle cx="73" cy="50" r="14" fill="none" stroke="#d4d4d8" stroke-width="3.5"/>
</svg>''',

    "MG Motor": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <path d="M50 10 L88 28 L88 72 L50 90 L12 72 L12 28 Z" fill="#991b1b" stroke="#f8fafc" stroke-width="3.5"/>
  <text x="50" y="58" font-family="sans-serif" font-size="28" font-weight="900" fill="#ffffff" letter-spacing="2" text-anchor="middle">MG</text>
</svg>''',

    "Volvo": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <circle cx="46" cy="54" r="34" fill="#0f172a" stroke="#cbd5e1" stroke-width="4"/>
  <path d="M70 30 L86 14 M74 14 L86 14 L86 26" fill="none" stroke="#cbd5e1" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>
  <rect x="20" y="47" width="52" height="14" fill="#1e3a8a"/>
  <text x="46" y="58" font-family="sans-serif" font-size="9" font-weight="900" fill="#ffffff" letter-spacing="2" text-anchor="middle">VOLVO</text>
</svg>''',

    "Nissan": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <circle cx="50" cy="50" r="42" fill="#0f172a" stroke="#94a3b8" stroke-width="3.5"/>
  <rect x="12" y="42" width="76" height="16" fill="#1e293b" stroke="#e2e8f0" stroke-width="2"/>
  <text x="50" y="54" font-family="sans-serif" font-size="10" font-weight="900" fill="#ffffff" letter-spacing="2" text-anchor="middle">NISSAN</text>
</svg>''',

    "Skoda": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <circle cx="50" cy="50" r="46" fill="#064e3b" stroke="#34d399" stroke-width="3"/>
  <circle cx="50" cy="50" r="38" fill="none" stroke="#a7f3d0" stroke-width="1.5"/>
  <path d="M42 32 C58 26 68 38 68 50 C68 62 58 74 42 68 L50 50 Z" fill="#34d399"/>
  <circle cx="44" cy="46" r="3" fill="#064e3b"/>
</svg>''',

    "Cupra": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <rect width="100" height="100" rx="18" fill="#18181b"/>
  <path d="M26 30 L50 56 L74 30 M38 52 L50 68 L62 52 M50 68 L50 82" fill="none" stroke="#d97706" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"/>
</svg>''',

    "BYD": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <rect width="100" height="100" rx="18" fill="#0f172a"/>
  <ellipse cx="50" cy="50" rx="44" ry="26" fill="none" stroke="#e2e8f0" stroke-width="3"/>
  <text x="50" y="56" font-family="sans-serif" font-size="16" font-weight="900" fill="#f8fafc" letter-spacing="3" text-anchor="middle">BYD</text>
</svg>''',

    "Ford": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <ellipse cx="50" cy="50" rx="46" ry="28" fill="#1e3a8a" stroke="#cbd5e1" stroke-width="3"/>
  <ellipse cx="50" cy="50" rx="41" ry="23" fill="none" stroke="#93c5fd" stroke-width="1.5"/>
  <text x="50" y="57" font-family="'Brush Script MT', 'Snell Roundhand', cursive, sans-serif" font-size="26" font-weight="bold" font-style="italic" fill="#ffffff" text-anchor="middle">Ford</text>
</svg>'''
}

def generate_model_svg(brand_name, model_name, category="Berline", is_ev=True):
    """
    Génère un visuel de véhicule haute définition en SVG vectoriel avec
    silhouette selon la catégorie (Citadine, SUV, Berline, Break, Crossover)
    et palette du constructeur.
    """
    # Palette de couleur de silhouette
    color_map = {
        "Fiat": ("#0284c7", "#38bdf8", "#7f1d1d"),
        "Renault": ("#eab308", "#fde047", "#18181b"),
        "Tesla": ("#e11d48", "#fb7185", "#18181b"),
        "Peugeot": ("#0284c7", "#38bdf8", "#0f172a"),
        "Dacia": ("#15803d", "#4ade80", "#14532d"),
        "Toyota": ("#dc2626", "#f87171", "#7f1d1d"),
        "Volkswagen": ("#0284c7", "#38bdf8", "#0c4a6e"),
        "Citroën": ("#dc2626", "#f87171", "#991b1b"),
        "Hyundai": ("#0369a1", "#38bdf8", "#0f172a"),
        "Kia": ("#475569", "#94a3b8", "#000000"),
        "BMW": ("#0284c7", "#38bdf8", "#000000"),
        "Mercedes-Benz": ("#64748b", "#cbd5e1", "#0f172a"),
        "Audi": ("#475569", "#cbd5e1", "#18181b"),
        "MG Motor": ("#b91c1c", "#f87171", "#7f1d1d"),
        "Volvo": ("#1e3a8a", "#60a5fa", "#0f172a"),
        "Nissan": ("#b91c1c", "#f87171", "#0f172a"),
        "Skoda": ("#059669", "#34d399", "#064e3b"),
        "Cupra": ("#d97706", "#fbbf24", "#18181b"),
        "BYD": ("#0284c7", "#38bdf8", "#0f172a"),
        "Ford": ("#1d4ed8", "#60a5fa", "#1e3a8a")
    }
    c_prim, c_light, c_bg = color_map.get(brand_name, ("#0d9488", "#2dd4bf", "#18181b"))

    # Profil silhouette carrosserie
    if "SUV" in category or "CUV" in category or "Crossover" in category:
        body_path = "M 45 115 C 50 85, 75 60, 105 55 L 185 55 C 215 58, 245 78, 255 115 Z"
        window_path = "M 110 60 L 175 60 C 195 62, 210 75, 215 90 L 98 90 C 102 75, 106 62, 110 60 Z"
        w_x1, w_x2, w_y = 85, 220, 115
    elif "Citadine" in category or "Panda" in model_name or "500" in model_name:
        body_path = "M 55 118 C 60 90, 80 72, 105 68 L 170 68 C 198 72, 230 88, 245 118 Z"
        window_path = "M 110 72 L 160 72 C 180 75, 192 86, 198 96 L 100 96 C 104 84, 107 74, 110 72 Z"
        w_x1, w_x2, w_y = 90, 210, 118
    elif "Break" in category:
        body_path = "M 40 118 C 45 92, 70 70, 95 65 L 215 65 C 235 70, 255 90, 260 118 Z"
        window_path = "M 100 70 L 205 70 C 220 74, 228 86, 230 96 L 90 96 C 94 84, 97 74, 100 70 Z"
        w_x1, w_x2, w_y = 80, 225, 118
    else: # Berline / Compacte
        body_path = "M 45 118 C 50 92, 75 68, 105 62 L 180 62 C 215 65, 245 88, 255 118 Z"
        window_path = "M 110 66 L 170 66 C 192 68, 205 80, 210 94 L 98 94 C 102 82, 106 70, 110 66 Z"
        w_x1, w_x2, w_y = 85, 220, 118

    fuel_label = "⚡ 100% ÉLEC" if is_ev else "🍃 HYBRIDE"
    fuel_color = "#34d399" if is_ev else "#38bdf8"

    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 300 160">
  <defs>
    <linearGradient id="bgGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#18181b"/>
      <stop offset="100%" stop-color="#27272a"/>
    </linearGradient>
    <linearGradient id="bodyGrad" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="{c_prim}"/>
      <stop offset="100%" stop-color="{c_light}"/>
    </linearGradient>
  </defs>
  <rect width="300" height="160" rx="14" fill="url(#bgGrad)"/>
  <line x1="15" y1="125" x2="285" y2="125" stroke="#3f3f46" stroke-width="1.5" stroke-dasharray="6,6"/>
  
  <!-- Silhouette Carrosserie -->
  <path d="{body_path}" fill="url(#bodyGrad)" opacity="0.95"/>
  <path d="{window_path}" fill="#09090b" opacity="0.85"/>
  
  <!-- Roues Stylisées -->
  <circle cx="{w_x1}" cy="{w_y}" r="17" fill="#09090b" stroke="#71717a" stroke-width="3.5"/>
  <circle cx="{w_x1}" cy="{w_y}" r="7" fill="#d4d4d8"/>
  <circle cx="{w_x2}" cy="{w_y}" r="17" fill="#09090b" stroke="#71717a" stroke-width="3.5"/>
  <circle cx="{w_x2}" cy="{w_y}" r="7" fill="#d4d4d8"/>

  <!-- Badge Modèle -->
  <rect x="14" y="12" width="110" height="20" rx="5" fill="{c_bg}"/>
  <text x="69" y="26" font-family="-apple-system, BlinkMacSystemFont, Arial, sans-serif" font-size="10" font-weight="900" fill="#ffffff" text-anchor="middle">{brand_name} {model_name[:10]}</text>
  
  <!-- Badge Énergie -->
  <rect x="212" y="12" width="74" height="20" rx="10" fill="rgba(0, 0, 0, 0.4)" stroke="{fuel_color}" stroke-width="1"/>
  <text x="249" y="26" font-family="sans-serif" font-size="9" font-weight="700" fill="{fuel_color}" text-anchor="middle">{fuel_label}</text>
  
  <text x="150" y="148" font-family="sans-serif" font-size="10" font-weight="600" fill="#a1a1aa" text-anchor="middle">{brand_name} {model_name} • {category}</text>
</svg>'''

def upload_image_bytes(svg_content, filename, folder="brands"):
    """
    Téléverse le SVG généré ou l'image vers l'API REST
    """
    cache_key = f"{folder}:{filename}"
    if cache_key in UPLOAD_CACHE:
        return UPLOAD_CACHE[cache_key]

    boundary = f"----WebKitBoundary{uuid.uuid4().hex}"
    body = bytearray()
    
    data = svg_content.encode("utf-8") if isinstance(svg_content, str) else svg_content
    safe_filename = f"{filename}.svg" if not filename.endswith(".svg") else filename

    body.extend(f"--{boundary}\r\n".encode("utf-8"))
    body.extend(f'Content-Disposition: form-data; name="file"; filename="{safe_filename}"\r\n'.encode("utf-8"))
    body.extend(b'Content-Type: image/svg+xml\r\n\r\n')
    body.extend(data)
    body.extend(b"\r\n")
    body.extend(f"--{boundary}--\r\n".encode("utf-8"))

    req = urllib.request.Request(
        f"{API_UPLOAD_URL}?folder={folder}",
        data=bytes(body),
        headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
        method="POST"
    )

    try:
        with urllib.request.urlopen(req) as resp:
            res_json = json.loads(resp.read().decode("utf-8"))
            url = res_json.get("url", "")
            UPLOAD_CACHE[cache_key] = url
            return url
    except Exception as ex:
        print(f"        [!] Erreur lors de l'upload de {safe_filename} : {ex}", file=sys.stderr)
        return ""

def api_post(endpoint, data=None, params=None):
    url = f"{API_BASE}{endpoint}"
    if params:
        query_string = urllib.parse.urlencode(params)
        url += f"?{query_string}"
    
    headers = {"Content-Type": "application/json"}
    body = json.dumps(data).encode("utf-8") if data is not None else b"{}"
    
    req = urllib.request.Request(url, data=body, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        err_msg = e.read().decode("utf-8")
        print(f"[HTTP {e.code}] Error on {url}: {err_msg}", file=sys.stderr)
        raise

def api_get(endpoint, params=None):
    url = f"{API_BASE}{endpoint}"
    if params:
        query_string = urllib.parse.urlencode(params)
        url += f"?{query_string}"
    
    req = urllib.request.Request(url, headers={"Accept": "application/json"}, method="GET")
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        err_msg = e.read().decode("utf-8")
        print(f"[HTTP {e.code}] Error on {url}: {err_msg}", file=sys.stderr)
        raise

CATALOG_DATA = [
    # 1. RENAULT
    {
        "brand": "Renault",
        "models": [
            {
                "name": "Megane E-Tech",
                "category": "Compacte",
                "motorisations": [
                    {"name": "EV40 Boost Charge 130 ch", "fuelType": "ELECTRIC", "consumptionWltp": 15.4, "powerHp": 130, "batteryCapacityKwh": 40.0},
                    {"name": "EV60 Optimum Charge 220 ch", "fuelType": "ELECTRIC", "consumptionWltp": 16.1, "powerHp": 220, "batteryCapacityKwh": 60.0}
                ],
                "finitions": ["Equilibre", "Techno", "Iconic"],
                "variants": [
                    {"finition": "Equilibre", "motorisation": "EV40 Boost Charge 130 ch", "price": 34000.0, "loa": 260.0, "lld": 240.0, "insurance": 650.0, "maintenance": 250.0, "resale": 16000.0},
                    {"finition": "Techno", "motorisation": "EV60 Optimum Charge 220 ch", "price": 40000.0, "loa": 310.0, "lld": 290.0, "insurance": 720.0, "maintenance": 260.0, "resale": 19500.0},
                    {"finition": "Iconic", "motorisation": "EV60 Optimum Charge 220 ch", "price": 43000.0, "loa": 345.0, "lld": 320.0, "insurance": 760.0, "maintenance": 270.0, "resale": 21000.0}
                ]
            },
            {
                "name": "Scenic E-Tech",
                "category": "SUV",
                "motorisations": [
                    {"name": "Autonomie Confort 170 ch", "fuelType": "ELECTRIC", "consumptionWltp": 16.3, "powerHp": 170, "batteryCapacityKwh": 60.0},
                    {"name": "Grande Autonomie 220 ch", "fuelType": "ELECTRIC", "consumptionWltp": 16.8, "powerHp": 220, "batteryCapacityKwh": 87.0}
                ],
                "finitions": ["Evolution", "Techno", "Esprit Alpine", "Iconic"],
                "variants": [
                    {"finition": "Evolution", "motorisation": "Autonomie Confort 170 ch", "price": 39990.0, "loa": 320.0, "lld": 300.0, "insurance": 700.0, "maintenance": 260.0, "resale": 19000.0},
                    {"finition": "Techno", "motorisation": "Grande Autonomie 220 ch", "price": 46990.0, "loa": 390.0, "lld": 370.0, "insurance": 780.0, "maintenance": 280.0, "resale": 23000.0},
                    {"finition": "Esprit Alpine", "motorisation": "Grande Autonomie 220 ch", "price": 49490.0, "loa": 420.0, "lld": 395.0, "insurance": 810.0, "maintenance": 290.0, "resale": 24500.0},
                    {"finition": "Iconic", "motorisation": "Grande Autonomie 220 ch", "price": 52490.0, "loa": 450.0, "lld": 425.0, "insurance": 850.0, "maintenance": 300.0, "resale": 26000.0}
                ]
            },
            {
                "name": "R5 E-Tech",
                "category": "Citadine",
                "motorisations": [
                    {"name": "Autonomie Urbaine 120 ch", "fuelType": "ELECTRIC", "consumptionWltp": 14.8, "powerHp": 120, "batteryCapacityKwh": 40.0},
                    {"name": "Autonomie Confort 150 ch", "fuelType": "ELECTRIC", "consumptionWltp": 15.2, "powerHp": 150, "batteryCapacityKwh": 52.0}
                ],
                "finitions": ["Evolution", "Techno", "Iconic Cinq"],
                "variants": [
                    {"finition": "Evolution", "motorisation": "Autonomie Urbaine 120 ch", "price": 25000.0, "loa": 180.0, "lld": 165.0, "insurance": 520.0, "maintenance": 200.0, "resale": 13000.0},
                    {"finition": "Techno", "motorisation": "Autonomie Confort 150 ch", "price": 31490.0, "loa": 240.0, "lld": 220.0, "insurance": 580.0, "maintenance": 220.0, "resale": 16500.0},
                    {"finition": "Iconic Cinq", "motorisation": "Autonomie Confort 150 ch", "price": 33490.0, "loa": 265.0, "lld": 245.0, "insurance": 610.0, "maintenance": 230.0, "resale": 17800.0}
                ]
            },
            {
                "name": "Clio E-Tech Hybrid",
                "category": "Citadine",
                "motorisations": [
                    {"name": "E-Tech Full Hybrid 145", "fuelType": "HYBRID", "consumptionWltp": 4.2, "powerHp": 145, "batteryCapacityKwh": 1.2}
                ],
                "finitions": ["Evolution", "Techno", "Esprit Alpine"],
                "variants": [
                    {"finition": "Evolution", "motorisation": "E-Tech Full Hybrid 145", "price": 23800.0, "loa": 185.0, "lld": 170.0, "insurance": 550.0, "maintenance": 360.0, "resale": 12000.0},
                    {"finition": "Techno", "motorisation": "E-Tech Full Hybrid 145", "price": 25800.0, "loa": 210.0, "lld": 195.0, "insurance": 580.0, "maintenance": 370.0, "resale": 13500.0},
                    {"finition": "Esprit Alpine", "motorisation": "E-Tech Full Hybrid 145", "price": 27600.0, "loa": 235.0, "lld": 215.0, "insurance": 610.0, "maintenance": 380.0, "resale": 14500.0}
                ]
            }
        ]
    },

    # 2. PEUGEOT
    {
        "brand": "Peugeot",
        "models": [
            {
                "name": "e-208",
                "category": "Citadine",
                "motorisations": [
                    {"name": "Électrique 156 ch (54 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 14.4, "powerHp": 156, "batteryCapacityKwh": 54.0},
                    {"name": "Hybrid 100 e-DCS6", "fuelType": "HYBRID", "consumptionWltp": 4.5, "powerHp": 100, "batteryCapacityKwh": 0.9}
                ],
                "finitions": ["Style", "Allure", "GT"],
                "variants": [
                    {"finition": "Style", "motorisation": "Électrique 156 ch (54 kWh)", "price": 33550.0, "loa": 230.0, "lld": 210.0, "insurance": 580.0, "maintenance": 230.0, "resale": 16000.0},
                    {"finition": "Allure", "motorisation": "Électrique 156 ch (54 kWh)", "price": 35100.0, "loa": 250.0, "lld": 230.0, "insurance": 600.0, "maintenance": 240.0, "resale": 17200.0},
                    {"finition": "GT", "motorisation": "Électrique 156 ch (54 kWh)", "price": 37300.0, "loa": 280.0, "lld": 260.0, "insurance": 640.0, "maintenance": 250.0, "resale": 18500.0},
                    {"finition": "Style", "motorisation": "Hybrid 100 e-DCS6", "price": 24200.0, "loa": 190.0, "lld": 175.0, "insurance": 550.0, "maintenance": 360.0, "resale": 12500.0},
                    {"finition": "GT", "motorisation": "Hybrid 100 e-DCS6", "price": 27800.0, "loa": 230.0, "lld": 210.0, "insurance": 590.0, "maintenance": 380.0, "resale": 14500.0}
                ]
            },
            {
                "name": "e-2008",
                "category": "SUV",
                "motorisations": [
                    {"name": "Électrique 156 ch (54 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 15.3, "powerHp": 156, "batteryCapacityKwh": 54.0},
                    {"name": "Hybrid 136 e-DCS6", "fuelType": "HYBRID", "consumptionWltp": 4.9, "powerHp": 136, "batteryCapacityKwh": 0.9}
                ],
                "finitions": ["Allure", "GT"],
                "variants": [
                    {"finition": "Allure", "motorisation": "Électrique 156 ch (54 kWh)", "price": 39250.0, "loa": 290.0, "lld": 270.0, "insurance": 660.0, "maintenance": 250.0, "resale": 19000.0},
                    {"finition": "GT", "motorisation": "Électrique 156 ch (54 kWh)", "price": 41450.0, "loa": 320.0, "lld": 295.0, "insurance": 700.0, "maintenance": 260.0, "resale": 20500.0},
                    {"finition": "GT", "motorisation": "Hybrid 136 e-DCS6", "price": 32900.0, "loa": 265.0, "lld": 245.0, "insurance": 620.0, "maintenance": 390.0, "resale": 16000.0}
                ]
            },
            {
                "name": "e-3008",
                "category": "SUV",
                "motorisations": [
                    {"name": "Électrique 210 ch (73 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 16.7, "powerHp": 210, "batteryCapacityKwh": 73.0},
                    {"name": "Hybrid 136 e-DCS6", "fuelType": "HYBRID", "consumptionWltp": 5.5, "powerHp": 136, "batteryCapacityKwh": 0.9}
                ],
                "finitions": ["Allure", "GT"],
                "variants": [
                    {"finition": "Allure", "motorisation": "Électrique 210 ch (73 kWh)", "price": 44990.0, "loa": 360.0, "lld": 335.0, "insurance": 750.0, "maintenance": 280.0, "resale": 22000.0},
                    {"finition": "GT", "motorisation": "Électrique 210 ch (73 kWh)", "price": 49490.0, "loa": 410.0, "lld": 385.0, "insurance": 820.0, "maintenance": 290.0, "resale": 24500.0},
                    {"finition": "GT", "motorisation": "Hybrid 136 e-DCS6", "price": 42990.0, "loa": 345.0, "lld": 320.0, "insurance": 740.0, "maintenance": 420.0, "resale": 21000.0}
                ]
            }
        ]
    },

    # 3. TESLA
    {
        "brand": "Tesla",
        "models": [
            {
                "name": "Model 3",
                "category": "Berline",
                "motorisations": [
                    {"name": "Propulsion RWD", "fuelType": "ELECTRIC", "consumptionWltp": 13.2, "powerHp": 283, "batteryCapacityKwh": 60.0},
                    {"name": "Grande Autonomie AWD", "fuelType": "ELECTRIC", "consumptionWltp": 14.0, "powerHp": 498, "batteryCapacityKwh": 78.0},
                    {"name": "Performance AWD", "fuelType": "ELECTRIC", "consumptionWltp": 16.7, "powerHp": 627, "batteryCapacityKwh": 78.0}
                ],
                "finitions": ["Standard", "Long Range", "Performance"],
                "variants": [
                    {"finition": "Standard", "motorisation": "Propulsion RWD", "price": 41490.0, "loa": 320.0, "lld": 299.0, "insurance": 850.0, "maintenance": 220.0, "resale": 22000.0},
                    {"finition": "Long Range", "motorisation": "Grande Autonomie AWD", "price": 50990.0, "loa": 420.0, "lld": 395.0, "insurance": 950.0, "maintenance": 250.0, "resale": 27500.0},
                    {"finition": "Performance", "motorisation": "Performance AWD", "price": 57490.0, "loa": 510.0, "lld": 480.0, "insurance": 1100.0, "maintenance": 280.0, "resale": 31000.0}
                ]
            },
            {
                "name": "Model Y",
                "category": "SUV",
                "motorisations": [
                    {"name": "Propulsion RWD", "fuelType": "ELECTRIC", "consumptionWltp": 15.7, "powerHp": 299, "batteryCapacityKwh": 60.0},
                    {"name": "Grande Autonomie AWD", "fuelType": "ELECTRIC", "consumptionWltp": 16.9, "powerHp": 514, "batteryCapacityKwh": 78.0}
                ],
                "finitions": ["Standard", "Long Range"],
                "variants": [
                    {"finition": "Standard", "motorisation": "Propulsion RWD", "price": 44990.0, "loa": 360.0, "lld": 330.0, "insurance": 900.0, "maintenance": 240.0, "resale": 24000.0},
                    {"finition": "Long Range", "motorisation": "Grande Autonomie AWD", "price": 52990.0, "loa": 450.0, "lld": 415.0, "insurance": 980.0, "maintenance": 260.0, "resale": 28500.0}
                ]
            },
            {
                "name": "Model S",
                "category": "Berline",
                "motorisations": [
                    {"name": "Dual Motor AWD 670 ch", "fuelType": "ELECTRIC", "consumptionWltp": 17.5, "powerHp": 670, "batteryCapacityKwh": 100.0},
                    {"name": "Tri-Motor Plaid 1020 ch", "fuelType": "ELECTRIC", "consumptionWltp": 18.7, "powerHp": 1020, "batteryCapacityKwh": 100.0}
                ],
                "finitions": ["Grande Autonomie", "Plaid"],
                "variants": [
                    {"finition": "Grande Autonomie", "motorisation": "Dual Motor AWD 670 ch", "price": 95990.0, "loa": 890.0, "lld": 820.0, "insurance": 1400.0, "maintenance": 350.0, "resale": 50000.0},
                    {"finition": "Plaid", "motorisation": "Tri-Motor Plaid 1020 ch", "price": 110990.0, "loa": 1090.0, "lld": 990.0, "insurance": 1800.0, "maintenance": 450.0, "resale": 60000.0}
                ]
            }
        ]
    },

    # 4. DACIA
    {
        "brand": "Dacia",
        "models": [
            {
                "name": "Spring",
                "category": "Citadine",
                "motorisations": [
                    {"name": "Electric 45 ch", "fuelType": "ELECTRIC", "consumptionWltp": 13.9, "powerHp": 45, "batteryCapacityKwh": 26.8},
                    {"name": "Electric 65 ch", "fuelType": "ELECTRIC", "consumptionWltp": 14.5, "powerHp": 65, "batteryCapacityKwh": 26.8}
                ],
                "finitions": ["Essential", "Extreme"],
                "variants": [
                    {"finition": "Essential", "motorisation": "Electric 45 ch", "price": 18900.0, "loa": 120.0, "lld": 99.0, "insurance": 420.0, "maintenance": 180.0, "resale": 9500.0},
                    {"finition": "Extreme", "motorisation": "Electric 65 ch", "price": 20900.0, "loa": 145.0, "lld": 125.0, "insurance": 460.0, "maintenance": 190.0, "resale": 11000.0}
                ]
            },
            {
                "name": "Duster Hybrid",
                "category": "SUV",
                "motorisations": [
                    {"name": "Hybrid 140 ch", "fuelType": "HYBRID", "consumptionWltp": 4.9, "powerHp": 140, "batteryCapacityKwh": 1.2}
                ],
                "finitions": ["Expression", "Journey"],
                "variants": [
                    {"finition": "Expression", "motorisation": "Hybrid 140 ch", "price": 26600.0, "loa": 210.0, "lld": 190.0, "insurance": 520.0, "maintenance": 340.0, "resale": 14000.0},
                    {"finition": "Journey", "motorisation": "Hybrid 140 ch", "price": 28100.0, "loa": 235.0, "lld": 210.0, "insurance": 560.0, "maintenance": 350.0, "resale": 15500.0}
                ]
            },
            {
                "name": "Jogger Hybrid",
                "category": "Break",
                "motorisations": [
                    {"name": "Hybrid 140 ch", "fuelType": "HYBRID", "consumptionWltp": 4.8, "powerHp": 140, "batteryCapacityKwh": 1.2}
                ],
                "finitions": ["Expression", "Extreme"],
                "variants": [
                    {"finition": "Expression", "motorisation": "Hybrid 140 ch", "price": 25200.0, "loa": 200.0, "lld": 180.0, "insurance": 510.0, "maintenance": 330.0, "resale": 13000.0},
                    {"finition": "Extreme", "motorisation": "Hybrid 140 ch", "price": 27200.0, "loa": 225.0, "lld": 205.0, "insurance": 540.0, "maintenance": 340.0, "resale": 14500.0}
                ]
            }
        ]
    },

    # 5. TOYOTA
    {
        "brand": "Toyota",
        "models": [
            {
                "name": "Yaris Hybrid",
                "category": "Citadine",
                "motorisations": [
                    {"name": "116h Dynamic Force", "fuelType": "HYBRID", "consumptionWltp": 3.8, "powerHp": 116, "batteryCapacityKwh": 0.8},
                    {"name": "130h Dynamic Force", "fuelType": "HYBRID", "consumptionWltp": 4.2, "powerHp": 130, "batteryCapacityKwh": 0.8}
                ],
                "finitions": ["Dynamic", "GR Sport"],
                "variants": [
                    {"finition": "Dynamic", "motorisation": "116h Dynamic Force", "price": 23950.0, "loa": 189.0, "lld": 175.0, "insurance": 540.0, "maintenance": 350.0, "resale": 13000.0},
                    {"finition": "GR Sport", "motorisation": "130h Dynamic Force", "price": 28450.0, "loa": 239.0, "lld": 219.0, "insurance": 600.0, "maintenance": 370.0, "resale": 16000.0}
                ]
            },
            {
                "name": "Yaris Cross",
                "category": "SUV",
                "motorisations": [
                    {"name": "116h Hybrid 2WD", "fuelType": "HYBRID", "consumptionWltp": 4.4, "powerHp": 116, "batteryCapacityKwh": 0.8},
                    {"name": "130h Hybrid AWD-i", "fuelType": "HYBRID", "consumptionWltp": 4.8, "powerHp": 130, "batteryCapacityKwh": 0.8}
                ],
                "finitions": ["Dynamic", "Collection"],
                "variants": [
                    {"finition": "Dynamic", "motorisation": "116h Hybrid 2WD", "price": 28200.0, "loa": 220.0, "lld": 200.0, "insurance": 580.0, "maintenance": 360.0, "resale": 15500.0},
                    {"finition": "Collection", "motorisation": "130h Hybrid AWD-i", "price": 34700.0, "loa": 290.0, "lld": 270.0, "insurance": 650.0, "maintenance": 390.0, "resale": 19000.0}
                ]
            },
            {
                "name": "bZ4X",
                "category": "SUV",
                "motorisations": [
                    {"name": "Pure 204 ch 2WD", "fuelType": "ELECTRIC", "consumptionWltp": 16.7, "powerHp": 204, "batteryCapacityKwh": 71.4}
                ],
                "finitions": ["Pure", "Origin"],
                "variants": [
                    {"finition": "Pure", "motorisation": "Pure 204 ch 2WD", "price": 39900.0, "loa": 340.0, "lld": 310.0, "insurance": 720.0, "maintenance": 260.0, "resale": 20000.0},
                    {"finition": "Origin", "motorisation": "Pure 204 ch 2WD", "price": 45500.0, "loa": 395.0, "lld": 370.0, "insurance": 790.0, "maintenance": 270.0, "resale": 23000.0}
                ]
            }
        ]
    },

    # 6. VOLKSWAGEN
    {
        "brand": "Volkswagen",
        "models": [
            {
                "name": "ID.3",
                "category": "Compacte",
                "motorisations": [
                    {"name": "Pro 204 ch (59 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 15.3, "powerHp": 204, "batteryCapacityKwh": 59.0},
                    {"name": "Pro S 204 ch (77 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 15.7, "powerHp": 204, "batteryCapacityKwh": 77.0}
                ],
                "finitions": ["Life Max", "Style"],
                "variants": [
                    {"finition": "Life Max", "motorisation": "Pro 204 ch (59 kWh)", "price": 37990.0, "loa": 299.0, "lld": 279.0, "insurance": 680.0, "maintenance": 250.0, "resale": 19000.0},
                    {"finition": "Style", "motorisation": "Pro S 204 ch (77 kWh)", "price": 44310.0, "loa": 370.0, "lld": 345.0, "insurance": 760.0, "maintenance": 270.0, "resale": 23000.0}
                ]
            },
            {
                "name": "ID.4",
                "category": "SUV",
                "motorisations": [
                    {"name": "Pro 286 ch (77 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 16.2, "powerHp": 286, "batteryCapacityKwh": 77.0},
                    {"name": "GTX 340 ch AWD", "fuelType": "ELECTRIC", "consumptionWltp": 17.5, "powerHp": 340, "batteryCapacityKwh": 77.0}
                ],
                "finitions": ["Life Max", "GTX"],
                "variants": [
                    {"finition": "Life Max", "motorisation": "Pro 286 ch (77 kWh)", "price": 45990.0, "loa": 380.0, "lld": 350.0, "insurance": 790.0, "maintenance": 280.0, "resale": 24000.0},
                    {"finition": "GTX", "motorisation": "GTX 340 ch AWD", "price": 57900.0, "loa": 520.0, "lld": 485.0, "insurance": 960.0, "maintenance": 320.0, "resale": 30500.0}
                ]
            },
            {
                "name": "Golf eHybrid",
                "category": "Compacte",
                "motorisations": [
                    {"name": "1.5 TSI eHybrid 204 ch", "fuelType": "HYBRID", "consumptionWltp": 0.9, "powerHp": 204, "batteryCapacityKwh": 19.7}
                ],
                "finitions": ["Style", "GTE"],
                "variants": [
                    {"finition": "Style", "motorisation": "1.5 TSI eHybrid 204 ch", "price": 43500.0, "loa": 360.0, "lld": 330.0, "insurance": 730.0, "maintenance": 410.0, "resale": 22000.0},
                    {"finition": "GTE", "motorisation": "1.5 TSI eHybrid 204 ch", "price": 48200.0, "loa": 410.0, "lld": 380.0, "insurance": 820.0, "maintenance": 440.0, "resale": 25000.0}
                ]
            }
        ]
    },

    # 7. CITROËN
    {
        "brand": "Citroën",
        "models": [
            {
                "name": "ë-C3",
                "category": "Citadine",
                "motorisations": [
                    {"name": "Électrique 113 ch (44 kWh LFP)", "fuelType": "ELECTRIC", "consumptionWltp": 14.1, "powerHp": 113, "batteryCapacityKwh": 44.0}
                ],
                "finitions": ["You", "Max"],
                "variants": [
                    {"finition": "You", "motorisation": "Électrique 113 ch (44 kWh LFP)", "price": 23300.0, "loa": 149.0, "lld": 129.0, "insurance": 480.0, "maintenance": 200.0, "resale": 12000.0},
                    {"finition": "Max", "motorisation": "Électrique 113 ch (44 kWh LFP)", "price": 27800.0, "loa": 189.0, "lld": 169.0, "insurance": 520.0, "maintenance": 210.0, "resale": 14500.0}
                ]
            },
            {
                "name": "ë-C4",
                "category": "Berline",
                "motorisations": [
                    {"name": "Électrique 136 ch (50 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 15.3, "powerHp": 136, "batteryCapacityKwh": 50.0},
                    {"name": "Électrique 156 ch (54 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 14.8, "powerHp": 156, "batteryCapacityKwh": 54.0}
                ],
                "finitions": ["Plus", "Max"],
                "variants": [
                    {"finition": "Plus", "motorisation": "Électrique 136 ch (50 kWh)", "price": 35740.0, "loa": 259.0, "lld": 239.0, "insurance": 610.0, "maintenance": 230.0, "resale": 17500.0},
                    {"finition": "Max", "motorisation": "Électrique 156 ch (54 kWh)", "price": 39300.0, "loa": 299.0, "lld": 279.0, "insurance": 660.0, "maintenance": 240.0, "resale": 19500.0}
                ]
            },
            {
                "name": "C5 Aircross Hybrid",
                "category": "SUV",
                "motorisations": [
                    {"name": "Hybrid 136 ë-DCS6", "fuelType": "HYBRID", "consumptionWltp": 5.4, "powerHp": 136, "batteryCapacityKwh": 0.9}
                ],
                "finitions": ["Plus", "Max"],
                "variants": [
                    {"finition": "Plus", "motorisation": "Hybrid 136 ë-DCS6", "price": 37800.0, "loa": 290.0, "lld": 270.0, "insurance": 680.0, "maintenance": 410.0, "resale": 18500.0},
                    {"finition": "Max", "motorisation": "Hybrid 136 ë-DCS6", "price": 41200.0, "loa": 330.0, "lld": 305.0, "insurance": 720.0, "maintenance": 430.0, "resale": 20500.0}
                ]
            }
        ]
    },

    # 8. HYUNDAI
    {
        "brand": "Hyundai",
        "models": [
            {
                "name": "Ioniq 5",
                "category": "Crossover",
                "motorisations": [
                    {"name": "Intuitive 229 ch (84 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 16.8, "powerHp": 229, "batteryCapacityKwh": 84.0},
                    {"name": "HTRAC AWD 325 ch (84 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 18.2, "powerHp": 325, "batteryCapacityKwh": 84.0}
                ],
                "finitions": ["Intuitive", "Executive"],
                "variants": [
                    {"finition": "Intuitive", "motorisation": "Intuitive 229 ch (84 kWh)", "price": 46800.0, "loa": 380.0, "lld": 350.0, "insurance": 790.0, "maintenance": 270.0, "resale": 24000.0},
                    {"finition": "Executive", "motorisation": "HTRAC AWD 325 ch (84 kWh)", "price": 60500.0, "loa": 530.0, "lld": 490.0, "insurance": 960.0, "maintenance": 310.0, "resale": 32000.0}
                ]
            },
            {
                "name": "Kona Electric",
                "category": "SUV",
                "motorisations": [
                    {"name": "Standard 156 ch (48.4 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 14.6, "powerHp": 156, "batteryCapacityKwh": 48.4},
                    {"name": "Long Range 217 ch (65.4 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 15.1, "powerHp": 217, "batteryCapacityKwh": 65.4}
                ],
                "finitions": ["Intuitive", "Creative"],
                "variants": [
                    {"finition": "Intuitive", "motorisation": "Standard 156 ch (48.4 kWh)", "price": 37900.0, "loa": 290.0, "lld": 270.0, "insurance": 640.0, "maintenance": 240.0, "resale": 19000.0},
                    {"finition": "Creative", "motorisation": "Long Range 217 ch (65.4 kWh)", "price": 44400.0, "loa": 360.0, "lld": 335.0, "insurance": 730.0, "maintenance": 260.0, "resale": 23000.0}
                ]
            },
            {
                "name": "Tucson Hybrid",
                "category": "SUV",
                "motorisations": [
                    {"name": "1.6 T-GDI Hybrid 215 ch", "fuelType": "HYBRID", "consumptionWltp": 5.6, "powerHp": 215, "batteryCapacityKwh": 1.49}
                ],
                "finitions": ["Intuitive", "N Line Executive"],
                "variants": [
                    {"finition": "Intuitive", "motorisation": "1.6 T-GDI Hybrid 215 ch", "price": 38900.0, "loa": 310.0, "lld": 290.0, "insurance": 690.0, "maintenance": 420.0, "resale": 20000.0},
                    {"finition": "N Line Executive", "motorisation": "1.6 T-GDI Hybrid 215 ch", "price": 46800.0, "loa": 395.0, "lld": 365.0, "insurance": 780.0, "maintenance": 450.0, "resale": 24000.0}
                ]
            }
        ]
    },

    # 9. KIA
    {
        "brand": "Kia",
        "models": [
            {
                "name": "EV6",
                "category": "Crossover",
                "motorisations": [
                    {"name": "229 ch 2WD (77.4 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 16.5, "powerHp": 229, "batteryCapacityKwh": 77.4},
                    {"name": "325 ch AWD (77.4 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 17.8, "powerHp": 325, "batteryCapacityKwh": 77.4}
                ],
                "finitions": ["Air Design", "GT-line"],
                "variants": [
                    {"finition": "Air Design", "motorisation": "229 ch 2WD (77.4 kWh)", "price": 49690.0, "loa": 390.0, "lld": 365.0, "insurance": 820.0, "maintenance": 280.0, "resale": 26000.0},
                    {"finition": "GT-line", "motorisation": "325 ch AWD (77.4 kWh)", "price": 57690.0, "loa": 480.0, "lld": 445.0, "insurance": 920.0, "maintenance": 310.0, "resale": 30500.0}
                ]
            },
            {
                "name": "Niro EV",
                "category": "Crossover",
                "motorisations": [
                    {"name": "Électrique 204 ch (64.8 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 16.2, "powerHp": 204, "batteryCapacityKwh": 64.8}
                ],
                "finitions": ["Motion", "Active", "Premium"],
                "variants": [
                    {"finition": "Motion", "motorisation": "Électrique 204 ch (64.8 kWh)", "price": 40490.0, "loa": 310.0, "lld": 290.0, "insurance": 690.0, "maintenance": 250.0, "resale": 20000.0},
                    {"finition": "Premium", "motorisation": "Électrique 204 ch (64.8 kWh)", "price": 47090.0, "loa": 380.0, "lld": 355.0, "insurance": 770.0, "maintenance": 270.0, "resale": 24000.0}
                ]
            },
            {
                "name": "Sportage Hybrid",
                "category": "SUV",
                "motorisations": [
                    {"name": "1.6 T-GDi Hybrid 215 ch", "fuelType": "HYBRID", "consumptionWltp": 5.6, "powerHp": 215, "batteryCapacityKwh": 1.49}
                ],
                "finitions": ["Active", "GT-line Premium"],
                "variants": [
                    {"finition": "Active", "motorisation": "1.6 T-GDi Hybrid 215 ch", "price": 39990.0, "loa": 320.0, "lld": 295.0, "insurance": 700.0, "maintenance": 420.0, "resale": 20500.0},
                    {"finition": "GT-line Premium", "motorisation": "1.6 T-GDi Hybrid 215 ch", "price": 47490.0, "loa": 395.0, "lld": 370.0, "insurance": 790.0, "maintenance": 450.0, "resale": 24500.0}
                ]
            }
        ]
    },

    # 10. BMW
    {
        "brand": "BMW",
        "models": [
            {
                "name": "i4 Gran Coupé",
                "category": "Berline",
                "motorisations": [
                    {"name": "eDrive35 (67 kWh - 286 ch)", "fuelType": "ELECTRIC", "consumptionWltp": 15.8, "powerHp": 286, "batteryCapacityKwh": 67.0},
                    {"name": "eDrive40 (81 kWh - 340 ch)", "fuelType": "ELECTRIC", "consumptionWltp": 16.1, "powerHp": 340, "batteryCapacityKwh": 81.0}
                ],
                "finitions": ["Base", "M Sport"],
                "variants": [
                    {"finition": "Base", "motorisation": "eDrive35 (67 kWh - 286 ch)", "price": 57550.0, "loa": 490.0, "lld": 450.0, "insurance": 1050.0, "maintenance": 320.0, "resale": 31000.0},
                    {"finition": "M Sport", "motorisation": "eDrive40 (81 kWh - 340 ch)", "price": 68200.0, "loa": 590.0, "lld": 550.0, "insurance": 1250.0, "maintenance": 360.0, "resale": 38000.0}
                ]
            },
            {
                "name": "iX1",
                "category": "SUV",
                "motorisations": [
                    {"name": "eDrive20 (64.7 kWh - 204 ch)", "fuelType": "ELECTRIC", "consumptionWltp": 15.4, "powerHp": 204, "batteryCapacityKwh": 64.7},
                    {"name": "xDrive30 (64.7 kWh - 313 ch)", "fuelType": "ELECTRIC", "consumptionWltp": 16.8, "powerHp": 313, "batteryCapacityKwh": 64.7}
                ],
                "finitions": ["Base", "M Sport"],
                "variants": [
                    {"finition": "Base", "motorisation": "eDrive20 (64.7 kWh - 204 ch)", "price": 46900.0, "loa": 390.0, "lld": 360.0, "insurance": 820.0, "maintenance": 280.0, "resale": 25000.0},
                    {"finition": "M Sport", "motorisation": "xDrive30 (64.7 kWh - 313 ch)", "price": 60550.0, "loa": 530.0, "lld": 490.0, "insurance": 1050.0, "maintenance": 330.0, "resale": 33000.0}
                ]
            },
            {
                "name": "Série 3 330e",
                "category": "Berline",
                "motorisations": [
                    {"name": "330e PHEV 292 ch", "fuelType": "HYBRID", "consumptionWltp": 1.4, "powerHp": 292, "batteryCapacityKwh": 19.5}
                ],
                "finitions": ["Base", "M Sport"],
                "variants": [
                    {"finition": "Base", "motorisation": "330e PHEV 292 ch", "price": 59900.0, "loa": 520.0, "lld": 480.0, "insurance": 1100.0, "maintenance": 490.0, "resale": 32000.0},
                    {"finition": "M Sport", "motorisation": "330e PHEV 292 ch", "price": 65450.0, "loa": 570.0, "lld": 530.0, "insurance": 1200.0, "maintenance": 520.0, "resale": 36000.0}
                ]
            }
        ]
    },

    # 11. MERCEDES-BENZ
    {
        "brand": "Mercedes-Benz",
        "models": [
            {
                "name": "EQA",
                "category": "SUV",
                "motorisations": [
                    {"name": "250+ (70.5 kWh - 190 ch)", "fuelType": "ELECTRIC", "consumptionWltp": 15.4, "powerHp": 190, "batteryCapacityKwh": 70.5}
                ],
                "finitions": ["Progressive Line", "AMG Line"],
                "variants": [
                    {"finition": "Progressive Line", "motorisation": "250+ (70.5 kWh - 190 ch)", "price": 46950.0, "loa": 395.0, "lld": 370.0, "insurance": 850.0, "maintenance": 290.0, "resale": 25000.0},
                    {"finition": "AMG Line", "motorisation": "250+ (70.5 kWh - 190 ch)", "price": 50950.0, "loa": 440.0, "lld": 410.0, "insurance": 920.0, "maintenance": 310.0, "resale": 27500.0}
                ]
            },
            {
                "name": "EQE Berline",
                "category": "Berline",
                "motorisations": [
                    {"name": "300 (89 kWh - 245 ch)", "fuelType": "ELECTRIC", "consumptionWltp": 16.5, "powerHp": 245, "batteryCapacityKwh": 89.0},
                    {"name": "350+ (96 kWh - 292 ch)", "fuelType": "ELECTRIC", "consumptionWltp": 16.2, "powerHp": 292, "batteryCapacityKwh": 96.0}
                ],
                "finitions": ["Electric Art", "AMG Line"],
                "variants": [
                    {"finition": "Electric Art", "motorisation": "300 (89 kWh - 245 ch)", "price": 69900.0, "loa": 650.0, "lld": 600.0, "insurance": 1250.0, "maintenance": 380.0, "resale": 38000.0},
                    {"finition": "AMG Line", "motorisation": "350+ (96 kWh - 292 ch)", "price": 79200.0, "loa": 740.0, "lld": 690.0, "insurance": 1400.0, "maintenance": 420.0, "resale": 44000.0}
                ]
            },
            {
                "name": "Classe A 250 e",
                "category": "Compacte",
                "motorisations": [
                    {"name": "250 e Plug-in Hybrid 218 ch", "fuelType": "HYBRID", "consumptionWltp": 1.1, "powerHp": 218, "batteryCapacityKwh": 15.6}
                ],
                "finitions": ["Progressive Line", "AMG Line"],
                "variants": [
                    {"finition": "Progressive Line", "motorisation": "250 e Plug-in Hybrid 218 ch", "price": 46500.0, "loa": 395.0, "lld": 365.0, "insurance": 820.0, "maintenance": 440.0, "resale": 24500.0},
                    {"finition": "AMG Line", "motorisation": "250 e Plug-in Hybrid 218 ch", "price": 50150.0, "loa": 435.0, "lld": 405.0, "insurance": 890.0, "maintenance": 460.0, "resale": 27000.0}
                ]
            }
        ]
    },

    # 12. AUDI
    {
        "brand": "Audi",
        "models": [
            {
                "name": "Q4 e-tron",
                "category": "SUV",
                "motorisations": [
                    {"name": "45 e-tron (77 kWh - 286 ch)", "fuelType": "ELECTRIC", "consumptionWltp": 16.6, "powerHp": 286, "batteryCapacityKwh": 77.0},
                    {"name": "55 e-tron quattro (77 kWh - 340 ch)", "fuelType": "ELECTRIC", "consumptionWltp": 17.8, "powerHp": 340, "batteryCapacityKwh": 77.0}
                ],
                "finitions": ["Design", "S line"],
                "variants": [
                    {"finition": "Design", "motorisation": "45 e-tron (77 kWh - 286 ch)", "price": 46900.0, "loa": 399.0, "lld": 370.0, "insurance": 820.0, "maintenance": 280.0, "resale": 26000.0},
                    {"finition": "S line", "motorisation": "55 e-tron quattro (77 kWh - 340 ch)", "price": 63900.0, "loa": 560.0, "lld": 520.0, "insurance": 1050.0, "maintenance": 330.0, "resale": 35000.0}
                ]
            },
            {
                "name": "Q8 e-tron",
                "category": "SUV",
                "motorisations": [
                    {"name": "55 e-tron quattro (106 kWh - 408 ch)", "fuelType": "ELECTRIC", "consumptionWltp": 20.6, "powerHp": 408, "batteryCapacityKwh": 106.0}
                ],
                "finitions": ["S line", "Avus"],
                "variants": [
                    {"finition": "S line", "motorisation": "55 e-tron quattro (106 kWh - 408 ch)", "price": 96700.0, "loa": 890.0, "lld": 820.0, "insurance": 1450.0, "maintenance": 420.0, "resale": 52000.0},
                    {"finition": "Avus", "motorisation": "55 e-tron quattro (106 kWh - 408 ch)", "price": 105500.0, "loa": 980.0, "lld": 910.0, "insurance": 1600.0, "maintenance": 450.0, "resale": 58000.0}
                ]
            },
            {
                "name": "A3 Sportback TFSI e",
                "category": "Compacte",
                "motorisations": [
                    {"name": "40 TFSI e 204 ch S tronic", "fuelType": "HYBRID", "consumptionWltp": 1.1, "powerHp": 204, "batteryCapacityKwh": 19.7}
                ],
                "finitions": ["Design", "S line"],
                "variants": [
                    {"finition": "Design", "motorisation": "40 TFSI e 204 ch S tronic", "price": 44800.0, "loa": 370.0, "lld": 340.0, "insurance": 760.0, "maintenance": 420.0, "resale": 23500.0},
                    {"finition": "S line", "motorisation": "40 TFSI e 204 ch S tronic", "price": 49300.0, "loa": 420.0, "lld": 385.0, "insurance": 830.0, "maintenance": 450.0, "resale": 26500.0}
                ]
            }
        ]
    },

    # 13. MG MOTOR
    {
        "brand": "MG Motor",
        "models": [
            {
                "name": "MG4 Electric",
                "category": "Compacte",
                "motorisations": [
                    {"name": "Standard 170 ch (51 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 17.0, "powerHp": 170, "batteryCapacityKwh": 51.0},
                    {"name": "Comfort 204 ch (64 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 16.0, "powerHp": 204, "batteryCapacityKwh": 64.0},
                    {"name": "XPOWER AWD 435 ch (64 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 18.7, "powerHp": 435, "batteryCapacityKwh": 64.0}
                ],
                "finitions": ["Standard", "Comfort", "Luxury", "XPOWER"],
                "variants": [
                    {"finition": "Standard", "motorisation": "Standard 170 ch (51 kWh)", "price": 29990.0, "loa": 199.0, "lld": 179.0, "insurance": 560.0, "maintenance": 210.0, "resale": 14000.0},
                    {"finition": "Comfort", "motorisation": "Comfort 204 ch (64 kWh)", "price": 33990.0, "loa": 239.0, "lld": 219.0, "insurance": 620.0, "maintenance": 230.0, "resale": 16500.0},
                    {"finition": "Luxury", "motorisation": "Comfort 204 ch (64 kWh)", "price": 35990.0, "loa": 259.0, "lld": 239.0, "insurance": 650.0, "maintenance": 240.0, "resale": 18000.0},
                    {"finition": "XPOWER", "motorisation": "XPOWER AWD 435 ch (64 kWh)", "price": 40490.0, "loa": 320.0, "lld": 295.0, "insurance": 820.0, "maintenance": 270.0, "resale": 21000.0}
                ]
            },
            {
                "name": "MG ZS EV",
                "category": "SUV",
                "motorisations": [
                    {"name": "Autonomie Étendue 156 ch (70 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 17.8, "powerHp": 156, "batteryCapacityKwh": 70.0}
                ],
                "finitions": ["Comfort", "Luxury"],
                "variants": [
                    {"finition": "Comfort", "motorisation": "Autonomie Étendue 156 ch (70 kWh)", "price": 37990.0, "loa": 270.0, "lld": 249.0, "insurance": 640.0, "maintenance": 240.0, "resale": 18500.0},
                    {"finition": "Luxury", "motorisation": "Autonomie Étendue 156 ch (70 kWh)", "price": 39990.0, "loa": 295.0, "lld": 270.0, "insurance": 680.0, "maintenance": 250.0, "resale": 20000.0}
                ]
            },
            {
                "name": "MG3 Hybrid+",
                "category": "Citadine",
                "motorisations": [
                    {"name": "Hybrid+ 195 ch", "fuelType": "HYBRID", "consumptionWltp": 4.4, "powerHp": 195, "batteryCapacityKwh": 1.83}
                ],
                "finitions": ["Standard", "Luxury"],
                "variants": [
                    {"finition": "Standard", "motorisation": "Hybrid+ 195 ch", "price": 19990.0, "loa": 149.0, "lld": 135.0, "insurance": 470.0, "maintenance": 310.0, "resale": 10500.0},
                    {"finition": "Luxury", "motorisation": "Hybrid+ 195 ch", "price": 23490.0, "loa": 179.0, "lld": 160.0, "insurance": 520.0, "maintenance": 330.0, "resale": 12500.0}
                ]
            }
        ]
    },

    # 14. FIAT
    {
        "brand": "Fiat",
        "models": [
            {
                "name": "500e",
                "category": "Citadine",
                "motorisations": [
                    {"name": "Électrique 95 ch (24 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 13.0, "powerHp": 95, "batteryCapacityKwh": 23.8},
                    {"name": "Électrique 118 ch (42 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 14.0, "powerHp": 118, "batteryCapacityKwh": 42.0}
                ],
                "finitions": ["Pop", "La Prima"],
                "variants": [
                    {"finition": "Pop", "motorisation": "Électrique 95 ch (24 kWh)", "price": 30400.0, "loa": 179.0, "lld": 159.0, "insurance": 520.0, "maintenance": 200.0, "resale": 14500.0},
                    {"finition": "La Prima", "motorisation": "Électrique 118 ch (42 kWh)", "price": 37900.0, "loa": 269.0, "lld": 249.0, "insurance": 610.0, "maintenance": 220.0, "resale": 18500.0}
                ]
            },
            {
                "name": "600e",
                "category": "Crossover",
                "motorisations": [
                    {"name": "Électrique 156 ch (54 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 15.1, "powerHp": 156, "batteryCapacityKwh": 54.0},
                    {"name": "Hybrid 100 ch", "fuelType": "HYBRID", "consumptionWltp": 4.9, "powerHp": 100, "batteryCapacityKwh": 0.9}
                ],
                "finitions": ["Red", "La Prima"],
                "variants": [
                    {"finition": "Red", "motorisation": "Électrique 156 ch (54 kWh)", "price": 35900.0, "loa": 249.0, "lld": 229.0, "insurance": 620.0, "maintenance": 230.0, "resale": 18000.0},
                    {"finition": "La Prima", "motorisation": "Électrique 156 ch (54 kWh)", "price": 40900.0, "loa": 299.0, "lld": 279.0, "insurance": 680.0, "maintenance": 240.0, "resale": 20500.0},
                    {"finition": "Red", "motorisation": "Hybrid 100 ch", "price": 24900.0, "loa": 189.0, "lld": 169.0, "insurance": 540.0, "maintenance": 360.0, "resale": 13000.0}
                ]
            },
            {
                "name": "Panda Hybrid",
                "category": "Citadine",
                "motorisations": [
                    {"name": "1.0 GSE Hybrid 70 ch", "fuelType": "HYBRID", "consumptionWltp": 4.8, "powerHp": 70, "batteryCapacityKwh": 0.5}
                ],
                "finitions": ["Base", "Cross"],
                "variants": [
                    {"finition": "Base", "motorisation": "1.0 GSE Hybrid 70 ch", "price": 15900.0, "loa": 109.0, "lld": 99.0, "insurance": 420.0, "maintenance": 300.0, "resale": 8500.0},
                    {"finition": "Cross", "motorisation": "1.0 GSE Hybrid 70 ch", "price": 18500.0, "loa": 139.0, "lld": 125.0, "insurance": 460.0, "maintenance": 320.0, "resale": 10000.0}
                ]
            }
        ]
    },

    # 15. VOLVO
    {
        "brand": "Volvo",
        "models": [
            {
                "name": "EX30",
                "category": "SUV",
                "motorisations": [
                    {"name": "Single Motor (51 kWh LFP - 272 ch)", "fuelType": "ELECTRIC", "consumptionWltp": 16.7, "powerHp": 272, "batteryCapacityKwh": 51.0},
                    {"name": "Single Motor Extended Range (69 kWh - 272 ch)", "fuelType": "ELECTRIC", "consumptionWltp": 17.0, "powerHp": 272, "batteryCapacityKwh": 69.0},
                    {"name": "Twin Motor Performance (69 kWh - 428 ch)", "fuelType": "ELECTRIC", "consumptionWltp": 17.5, "powerHp": 428, "batteryCapacityKwh": 69.0}
                ],
                "finitions": ["Core", "Plus", "Ultra"],
                "variants": [
                    {"finition": "Core", "motorisation": "Single Motor (51 kWh LFP - 272 ch)", "price": 37500.0, "loa": 290.0, "lld": 270.0, "insurance": 690.0, "maintenance": 250.0, "resale": 20000.0},
                    {"finition": "Plus", "motorisation": "Single Motor Extended Range (69 kWh - 272 ch)", "price": 45000.0, "loa": 370.0, "lld": 345.0, "insurance": 780.0, "maintenance": 270.0, "resale": 24000.0},
                    {"finition": "Ultra", "motorisation": "Twin Motor Performance (69 kWh - 428 ch)", "price": 52200.0, "loa": 450.0, "lld": 420.0, "insurance": 910.0, "maintenance": 300.0, "resale": 28000.0}
                ]
            },
            {
                "name": "EX40 Recharge",
                "category": "SUV",
                "motorisations": [
                    {"name": "Single Motor RWD (69 kWh - 238 ch)", "fuelType": "ELECTRIC", "consumptionWltp": 16.6, "powerHp": 238, "batteryCapacityKwh": 69.0}
                ],
                "finitions": ["Plus", "Ultimate"],
                "variants": [
                    {"finition": "Plus", "motorisation": "Single Motor RWD (69 kWh - 238 ch)", "price": 46990.0, "loa": 395.0, "lld": 370.0, "insurance": 820.0, "maintenance": 280.0, "resale": 25000.0},
                    {"finition": "Ultimate", "motorisation": "Single Motor RWD (69 kWh - 238 ch)", "price": 53600.0, "loa": 460.0, "lld": 430.0, "insurance": 890.0, "maintenance": 300.0, "resale": 28500.0}
                ]
            },
            {
                "name": "XC60 T6 Recharge",
                "category": "SUV",
                "motorisations": [
                    {"name": "T6 AWD Plug-in Hybrid 350 ch", "fuelType": "HYBRID", "consumptionWltp": 1.0, "powerHp": 350, "batteryCapacityKwh": 18.8}
                ],
                "finitions": ["Plus Dark", "Ultimate Dark"],
                "variants": [
                    {"finition": "Plus Dark", "motorisation": "T6 AWD Plug-in Hybrid 350 ch", "price": 73600.0, "loa": 690.0, "lld": 640.0, "insurance": 1280.0, "maintenance": 520.0, "resale": 40000.0},
                    {"finition": "Ultimate Dark", "motorisation": "T6 AWD Plug-in Hybrid 350 ch", "price": 81900.0, "loa": 780.0, "lld": 730.0, "insurance": 1420.0, "maintenance": 560.0, "resale": 45000.0}
                ]
            }
        ]
    },

    # 16. NISSAN
    {
        "brand": "Nissan",
        "models": [
            {
                "name": "Ariya",
                "category": "Crossover",
                "motorisations": [
                    {"name": "Advance 218 ch (63 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 17.6, "powerHp": 218, "batteryCapacityKwh": 63.0},
                    {"name": "Evolve 242 ch (87 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 18.2, "powerHp": 242, "batteryCapacityKwh": 87.0}
                ],
                "finitions": ["Advance", "Evolve"],
                "variants": [
                    {"finition": "Advance", "motorisation": "Advance 218 ch (63 kWh)", "price": 39900.0, "loa": 320.0, "lld": 299.0, "insurance": 730.0, "maintenance": 260.0, "resale": 20500.0},
                    {"finition": "Evolve", "motorisation": "Evolve 242 ch (87 kWh)", "price": 50400.0, "loa": 440.0, "lld": 410.0, "insurance": 860.0, "maintenance": 290.0, "resale": 27000.0}
                ]
            },
            {
                "name": "Leaf",
                "category": "Compacte",
                "motorisations": [
                    {"name": "Acenta 150 ch (40 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 17.1, "powerHp": 150, "batteryCapacityKwh": 40.0}
                ],
                "finitions": ["Acenta", "N-Connecta"],
                "variants": [
                    {"finition": "Acenta", "motorisation": "Acenta 150 ch (40 kWh)", "price": 36900.0, "loa": 269.0, "lld": 249.0, "insurance": 620.0, "maintenance": 240.0, "resale": 16000.0},
                    {"finition": "N-Connecta", "motorisation": "Acenta 150 ch (40 kWh)", "price": 38600.0, "loa": 289.0, "lld": 269.0, "insurance": 650.0, "maintenance": 250.0, "resale": 17500.0}
                ]
            },
            {
                "name": "Qashqai e-POWER",
                "category": "SUV",
                "motorisations": [
                    {"name": "e-POWER 190 ch", "fuelType": "HYBRID", "consumptionWltp": 5.1, "powerHp": 190, "batteryCapacityKwh": 2.1}
                ],
                "finitions": ["Acenta", "Tekna"],
                "variants": [
                    {"finition": "Acenta", "motorisation": "e-POWER 190 ch", "price": 38700.0, "loa": 310.0, "lld": 290.0, "insurance": 680.0, "maintenance": 410.0, "resale": 20000.0},
                    {"finition": "Tekna", "motorisation": "e-POWER 190 ch", "price": 43900.0, "loa": 365.0, "lld": 340.0, "insurance": 750.0, "maintenance": 440.0, "resale": 23500.0}
                ]
            }
        ]
    },

    # 17. SKODA
    {
        "brand": "Skoda",
        "models": [
            {
                "name": "Enyaq iV",
                "category": "SUV",
                "motorisations": [
                    {"name": "85 (77 kWh - 286 ch)", "fuelType": "ELECTRIC", "consumptionWltp": 14.8, "powerHp": 286, "batteryCapacityKwh": 77.0},
                    {"name": "85x 4x4 (77 kWh - 286 ch)", "fuelType": "ELECTRIC", "consumptionWltp": 15.8, "powerHp": 286, "batteryCapacityKwh": 77.0}
                ],
                "finitions": ["Selection", "Sportline"],
                "variants": [
                    {"finition": "Selection", "motorisation": "85 (77 kWh - 286 ch)", "price": 46990.0, "loa": 380.0, "lld": 350.0, "insurance": 760.0, "maintenance": 270.0, "resale": 24500.0},
                    {"finition": "Sportline", "motorisation": "85x 4x4 (77 kWh - 286 ch)", "price": 56980.0, "loa": 480.0, "lld": 445.0, "insurance": 890.0, "maintenance": 310.0, "resale": 29500.0}
                ]
            },
            {
                "name": "Enyaq Coupé",
                "category": "SUV Coupé",
                "motorisations": [
                    {"name": "85 (77 kWh - 286 ch)", "fuelType": "ELECTRIC", "consumptionWltp": 14.4, "powerHp": 286, "batteryCapacityKwh": 77.0},
                    {"name": "RS 4x4 (77 kWh - 340 ch)", "fuelType": "ELECTRIC", "consumptionWltp": 16.1, "powerHp": 340, "batteryCapacityKwh": 77.0}
                ],
                "finitions": ["Selection", "RS"],
                "variants": [
                    {"finition": "Selection", "motorisation": "85 (77 kWh - 286 ch)", "price": 49990.0, "loa": 410.0, "lld": 380.0, "insurance": 790.0, "maintenance": 280.0, "resale": 26000.0},
                    {"finition": "RS", "motorisation": "RS 4x4 (77 kWh - 340 ch)", "price": 63780.0, "loa": 550.0, "lld": 510.0, "insurance": 1020.0, "maintenance": 340.0, "resale": 33500.0}
                ]
            },
            {
                "name": "Octavia Combi iV",
                "category": "Break",
                "motorisations": [
                    {"name": "1.5 TSI PHEV 204 ch DSG", "fuelType": "HYBRID", "consumptionWltp": 1.0, "powerHp": 204, "batteryCapacityKwh": 19.7}
                ],
                "finitions": ["Selection", "Sportline"],
                "variants": [
                    {"finition": "Selection", "motorisation": "1.5 TSI PHEV 204 ch DSG", "price": 41500.0, "loa": 340.0, "lld": 315.0, "insurance": 700.0, "maintenance": 390.0, "resale": 21500.0},
                    {"finition": "Sportline", "motorisation": "1.5 TSI PHEV 204 ch DSG", "price": 46200.0, "loa": 390.0, "lld": 360.0, "insurance": 770.0, "maintenance": 420.0, "resale": 24500.0}
                ]
            }
        ]
    },

    # 18. CUPRA
    {
        "brand": "Cupra",
        "models": [
            {
                "name": "Born",
                "category": "Compacte",
                "motorisations": [
                    {"name": "V 204 ch (58 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 15.5, "powerHp": 204, "batteryCapacityKwh": 58.0},
                    {"name": "e-Boost 231 ch (77 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 15.9, "powerHp": 231, "batteryCapacityKwh": 77.0},
                    {"name": "VZ 326 ch (79 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 16.4, "powerHp": 326, "batteryCapacityKwh": 79.0}
                ],
                "finitions": ["V", "VZ"],
                "variants": [
                    {"finition": "V", "motorisation": "V 204 ch (58 kWh)", "price": 39990.0, "loa": 299.0, "lld": 279.0, "insurance": 710.0, "maintenance": 250.0, "resale": 20500.0},
                    {"finition": "V", "motorisation": "e-Boost 231 ch (77 kWh)", "price": 45490.0, "loa": 360.0, "lld": 335.0, "insurance": 780.0, "maintenance": 270.0, "resale": 23500.0},
                    {"finition": "VZ", "motorisation": "VZ 326 ch (79 kWh)", "price": 48990.0, "loa": 410.0, "lld": 380.0, "insurance": 890.0, "maintenance": 290.0, "resale": 26000.0}
                ]
            },
            {
                "name": "Formentor e-HYBRID",
                "category": "CUV",
                "motorisations": [
                    {"name": "1.5 e-HYBRID 204 ch DSG", "fuelType": "HYBRID", "consumptionWltp": 1.1, "powerHp": 204, "batteryCapacityKwh": 19.7},
                    {"name": "VZ 1.5 e-HYBRID 272 ch DSG", "fuelType": "HYBRID", "consumptionWltp": 1.3, "powerHp": 272, "batteryCapacityKwh": 19.7}
                ],
                "finitions": ["V", "VZ"],
                "variants": [
                    {"finition": "V", "motorisation": "1.5 e-HYBRID 204 ch DSG", "price": 44900.0, "loa": 360.0, "lld": 330.0, "insurance": 760.0, "maintenance": 420.0, "resale": 23500.0},
                    {"finition": "VZ", "motorisation": "VZ 1.5 e-HYBRID 272 ch DSG", "price": 54200.0, "loa": 450.0, "lld": 420.0, "insurance": 920.0, "maintenance": 460.0, "resale": 29000.0}
                ]
            },
            {
                "name": "Tavascan",
                "category": "SUV Coupé",
                "motorisations": [
                    {"name": "Endurance 286 ch (77 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 15.6, "powerHp": 286, "batteryCapacityKwh": 77.0},
                    {"name": "VZ 340 ch AWD (77 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 16.8, "powerHp": 340, "batteryCapacityKwh": 77.0}
                ],
                "finitions": ["Endurance", "VZ"],
                "variants": [
                    {"finition": "Endurance", "motorisation": "Endurance 286 ch (77 kWh)", "price": 46990.0, "loa": 390.0, "lld": 360.0, "insurance": 820.0, "maintenance": 280.0, "resale": 25500.0},
                    {"finition": "VZ", "motorisation": "VZ 340 ch AWD (77 kWh)", "price": 57990.0, "loa": 490.0, "lld": 455.0, "insurance": 960.0, "maintenance": 320.0, "resale": 31500.0}
                ]
            }
        ]
    },

    # 19. BYD
    {
        "brand": "BYD",
        "models": [
            {
                "name": "Atto 3",
                "category": "SUV",
                "motorisations": [
                    {"name": "Comfort 204 ch (60.4 kWh Blade)", "fuelType": "ELECTRIC", "consumptionWltp": 16.0, "powerHp": 204, "batteryCapacityKwh": 60.4}
                ],
                "finitions": ["Comfort", "Design"],
                "variants": [
                    {"finition": "Comfort", "motorisation": "Comfort 204 ch (60.4 kWh Blade)", "price": 37990.0, "loa": 290.0, "lld": 269.0, "insurance": 650.0, "maintenance": 230.0, "resale": 19000.0},
                    {"finition": "Design", "motorisation": "Comfort 204 ch (60.4 kWh Blade)", "price": 39990.0, "loa": 315.0, "lld": 290.0, "insurance": 690.0, "maintenance": 240.0, "resale": 20500.0}
                ]
            },
            {
                "name": "Dolphin",
                "category": "Compacte",
                "motorisations": [
                    {"name": "Comfort 204 ch (60.4 kWh Blade)", "fuelType": "ELECTRIC", "consumptionWltp": 15.9, "powerHp": 204, "batteryCapacityKwh": 60.4}
                ],
                "finitions": ["Comfort", "Design"],
                "variants": [
                    {"finition": "Comfort", "motorisation": "Comfort 204 ch (60.4 kWh Blade)", "price": 33990.0, "loa": 240.0, "lld": 220.0, "insurance": 590.0, "maintenance": 220.0, "resale": 16500.0},
                    {"finition": "Design", "motorisation": "Comfort 204 ch (60.4 kWh Blade)", "price": 35990.0, "loa": 265.0, "lld": 245.0, "insurance": 620.0, "maintenance": 230.0, "resale": 18000.0}
                ]
            },
            {
                "name": "Seal",
                "category": "Berline",
                "motorisations": [
                    {"name": "Design RWD 313 ch (82.5 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 16.6, "powerHp": 313, "batteryCapacityKwh": 82.5},
                    {"name": "Excellence AWD 530 ch (82.5 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 18.2, "powerHp": 530, "batteryCapacityKwh": 82.5}
                ],
                "finitions": ["Design", "Excellence"],
                "variants": [
                    {"finition": "Design", "motorisation": "Design RWD 313 ch (82.5 kWh)", "price": 46990.0, "loa": 390.0, "lld": 360.0, "insurance": 850.0, "maintenance": 270.0, "resale": 25500.0},
                    {"finition": "Excellence", "motorisation": "Excellence AWD 530 ch (82.5 kWh)", "price": 49990.0, "loa": 430.0, "lld": 399.0, "insurance": 960.0, "maintenance": 290.0, "resale": 27500.0}
                ]
            }
        ]
    },

    # 20. FORD
    {
        "brand": "Ford",
        "models": [
            {
                "name": "Mustang Mach-E",
                "category": "SUV",
                "motorisations": [
                    {"name": "RWD Standard Range 269 ch (72 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 17.3, "powerHp": 269, "batteryCapacityKwh": 72.0},
                    {"name": "RWD Extended Range 294 ch (91 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 17.8, "powerHp": 294, "batteryCapacityKwh": 91.0}
                ],
                "finitions": ["Mach-E", "Premium"],
                "variants": [
                    {"finition": "Mach-E", "motorisation": "RWD Standard Range 269 ch (72 kWh)", "price": 46990.0, "loa": 395.0, "lld": 365.0, "insurance": 820.0, "maintenance": 280.0, "resale": 25000.0},
                    {"finition": "Premium", "motorisation": "RWD Extended Range 294 ch (91 kWh)", "price": 54990.0, "loa": 470.0, "lld": 435.0, "insurance": 910.0, "maintenance": 300.0, "resale": 29500.0}
                ]
            },
            {
                "name": "Puma EcoBoost Hybrid",
                "category": "SUV",
                "motorisations": [
                    {"name": "1.0 EcoBoost Hybrid 125 ch mHEV", "fuelType": "HYBRID", "consumptionWltp": 5.4, "powerHp": 125, "batteryCapacityKwh": 0.4}
                ],
                "finitions": ["Titanium", "ST-Line X"],
                "variants": [
                    {"finition": "Titanium", "motorisation": "1.0 EcoBoost Hybrid 125 ch mHEV", "price": 27490.0, "loa": 210.0, "lld": 190.0, "insurance": 560.0, "maintenance": 380.0, "resale": 14500.0},
                    {"finition": "ST-Line X", "motorisation": "1.0 EcoBoost Hybrid 125 ch mHEV", "price": 31390.0, "loa": 255.0, "lld": 235.0, "insurance": 610.0, "maintenance": 400.0, "resale": 16800.0}
                ]
            },
            {
                "name": "Kuga PHEV",
                "category": "SUV",
                "motorisations": [
                    {"name": "2.5 Duratec PHEV 243 ch", "fuelType": "HYBRID", "consumptionWltp": 1.2, "powerHp": 243, "batteryCapacityKwh": 14.4}
                ],
                "finitions": ["Titanium", "ST-Line X"],
                "variants": [
                    {"finition": "Titanium", "motorisation": "2.5 Duratec PHEV 243 ch", "price": 43500.0, "loa": 360.0, "lld": 330.0, "insurance": 760.0, "maintenance": 440.0, "resale": 23000.0},
                    {"finition": "ST-Line X", "motorisation": "2.5 Duratec PHEV 243 ch", "price": 48500.0, "loa": 410.0, "lld": 380.0, "insurance": 830.0, "maintenance": 470.0, "resale": 26000.0}
                ]
            }
        ]
    }
]

def seed_catalog(target_url=None):
    if target_url:
        configure_api_endpoints(target_url)

    print(f"[*] Seeding catalog on {API_BASE} with authentic SVGs and accurate models...")
    print(f"[*] Upload endpoint: {API_UPLOAD_URL}")
    
    total_brands = 0
    total_models = 0
    total_motorisations = 0
    total_finitions = 0
    total_variants = 0
    
    for brand_data in CATALOG_DATA:
        brand_name = brand_data["brand"]
        
        # 1. Téléverser le logo SVG officiel de la marque
        brand_svg = BRAND_LOGOS_SVG.get(brand_name, f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="50" cy="50" r="46" fill="#18181b"/><text x="50" y="58" font-size="20" font-weight="bold" fill="#ffffff" text-anchor="middle">{brand_name[:3]}</text></svg>''')
        uploaded_logo = upload_image_bytes(brand_svg, f"logo_{brand_name.lower().replace(' ', '_')}", folder="brands")
        print(f"[Upload] Logo '{brand_name}' téléversé -> {uploaded_logo}")

        # 2. Créer ou mettre à jour la Marque
        try:
            brand_resp = api_post("/brands", {"name": brand_name, "logoUrl": uploaded_logo})
            brand_id = brand_resp["id"]
            print(f"[+] Created Brand #{brand_id}: {brand_name}")
            total_brands += 1
        except Exception:
            brands_list = api_get("/brands")
            brand_obj = next((b for b in brands_list if b["name"].lower() == brand_name.lower()), None)
            if not brand_obj:
                print(f"[!] Could not create or find brand: {brand_name}")
                continue
            brand_id = brand_obj["id"]
            print(f"[*] Found existing Brand #{brand_id}: {brand_name}")
        
        # 3. Modèles
        for model_data in brand_data.get("models", []):
            model_name = model_data["name"]
            model_cat = model_data.get("category", "Berline")
            is_ev = any(m.get("fuelType") == "ELECTRIC" for m in model_data.get("motorisations", []))
            
            # Générer et téléverser la silhouette exacte du modèle
            model_svg = generate_model_svg(brand_name, model_name, model_cat, is_ev=is_ev)
            uploaded_model_img = upload_image_bytes(model_svg, f"model_{brand_name}_{model_name}".lower().replace(' ', '_'), folder="models")
            print(f"    [Upload] Photo modèle '{model_name}' ({model_cat}) téléversée -> {uploaded_model_img}")

            try:
                model_resp = api_post(
                    "/models",
                    {"name": model_name, "imageUrl": uploaded_model_img, "category": model_cat},
                    params={"brandId": brand_id}
                )
                model_id = model_resp["id"]
                print(f"    -> [+] Created Model #{model_id}: {model_name} ({model_cat})")
                total_models += 1
            except Exception:
                models_list = api_get("/models", params={"brandId": brand_id})
                model_obj = next((m for m in models_list if m["name"].lower() == model_name.lower()), None)
                if not model_obj:
                    print(f"    -> [!] Could not create/find model: {model_name}")
                    continue
                model_id = model_obj["id"]
                print(f"    -> [*] Found existing Model #{model_id}: {model_name}")
            
            # 4. Motorisations
            mot_id_map = {}
            for mot_data in model_data.get("motorisations", []):
                mot_name = mot_data["name"]
                try:
                    mot_resp = api_post("/motorisations", mot_data, params={"modelId": model_id})
                    mot_id = mot_resp["id"]
                    mot_id_map[mot_name] = mot_id
                    print(f"        [+] Motorisation #{mot_id}: {mot_name} (WLTP: {mot_data['consumptionWltp']})")
                    total_motorisations += 1
                except Exception:
                    mot_list = api_get("/motorisations", params={"modelId": model_id})
                    mot_obj = next((m for m in mot_list if m["name"].lower() == mot_name.lower()), None)
                    if mot_obj:
                        mot_id_map[mot_name] = mot_obj["id"]
            
            # 5. Finitions
            fin_id_map = {}
            for fin_name in model_data.get("finitions", []):
                fin_svg = generate_model_svg(brand_name, f"{model_name} {fin_name}", model_cat, is_ev=is_ev)
                uploaded_fin_img = upload_image_bytes(fin_svg, f"fin_{brand_name}_{model_name}_{fin_name}".lower().replace(' ', '_'), folder="finitions")
                
                fin_payload = {
                    "name": fin_name,
                    "imageUrl": uploaded_fin_img
                }
                
                try:
                    fin_resp = api_post("/finitions", fin_payload, params={"modelId": model_id})
                    fin_id = fin_resp["id"]
                    fin_id_map[fin_name] = fin_id
                    print(f"        [+] Finition #{fin_id}: {fin_name}")
                    total_finitions += 1
                except Exception:
                    fin_list = api_get("/finitions", params={"modelId": model_id})
                    fin_obj = next((f for f in fin_list if f["name"].lower() == fin_name.lower()), None)
                    if fin_obj:
                        fin_id_map[fin_name] = fin_obj["id"]
            
            # 6. Variantes tarifées
            for var_data in model_data.get("variants", []):
                fin_name = var_data["finition"]
                mot_name = var_data["motorisation"]
                
                fin_id = fin_id_map.get(fin_name)
                mot_id = mot_id_map.get(mot_name)
                
                if not fin_id or not mot_id:
                    continue
                
                variant_payload = {
                    "purchasePrice": var_data.get("price", 0.0),
                    "monthlyLoa": var_data.get("loa"),
                    "monthlyLld": var_data.get("lld"),
                    "defaultInsuranceCost": var_data.get("insurance"),
                    "defaultMaintenanceCost": var_data.get("maintenance"),
                    "estimatedResaleValue": var_data.get("resale")
                }
                
                try:
                    var_resp = api_post("/variants", variant_payload, params={"finitionId": fin_id, "motorisationId": mot_id})
                    print(f"            [$] Variant #{var_resp['id']}: {fin_name} x {mot_name} -> {var_data.get('price')} €")
                    total_variants += 1
                except Exception as ex:
                    pass

    print("\n" + "="*60)
    print(f"[✓] Seeding completed with authentic manufacturer SVGs!")
    print(f"    - Brands: {total_brands}")
    print(f"    - Models: {total_models}")
    print(f"    - Motorisations: {total_motorisations}")
    print(f"    - Finitions: {total_finitions}")
    print(f"    - Priced Variants: {total_variants}")
    print("="*60)

def main():
    import argparse
    parser = argparse.ArgumentParser(
        description="EcoSwitch Automotive Catalog Seeder",
        epilog="Exemples:\n  python3 scripts/seed_catalog.py\n  python3 scripts/seed_catalog.py prod\n  python3 scripts/seed_catalog.py --url https://ecoswitch-api.up.railway.app",
        formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument(
        "env",
        nargs="?",
        default="local",
        help="Environnement cible ('local', 'prod' ou URL personnalisée). Défaut: 'local'"
    )
    parser.add_argument(
        "--url",
        "-u",
        help="URL de base directe de l'API (écrase l'argument env)"
    )
    args = parser.parse_args()

    target_url = args.url if args.url else resolve_target_url(args.env)
    env_name = args.env.upper() if args.env.lower() in ENV_CONFIGS else "CUSTOM"
    print("=" * 60)
    print(f"[*] EcoSwitch Catalog Seeder")
    print(f"[*] Environnement cible : {env_name}")
    print(f"[*] URL de l'API        : {target_url}")
    print("=" * 60)
    seed_catalog(target_url)

if __name__ == "__main__":
    main()
