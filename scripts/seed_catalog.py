#!/usr/bin/env python3
"""
EcoSwitch Catalog Seeder — v3.0
• Logos SVG haute fidélité des marques (géométrie officielle reconstituée)
• Silhouettes de véhicules vectorielles par catégorie (SUV / Berline / Citadine / Break / Crossover)
• Multi-threading : uploads et créations d'entités en parallèle (ThreadPoolExecutor)
• Usage : python3 scripts/seed_catalog.py [local|prod] [--reset]
"""

import sys
import json
import uuid
import urllib.request
import urllib.parse
import urllib.error
import threading
import time
import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed

# ── Configuration ─────────────────────────────────────────────────────────────

ENV_CONFIGS = {
    "local": "http://localhost:8080",
    "prod":  "https://ecoswitch-api.up.railway.app",
    "production": "https://ecoswitch-api.up.railway.app",
}

BASE_URL      = ENV_CONFIGS["local"]
API_BASE      = f"{BASE_URL}/api/v1/catalog"
API_UPLOAD_URL = f"{BASE_URL}/api/v1/uploads/image"

UPLOAD_CACHE      = {}
UPLOAD_CACHE_LOCK = threading.Lock()

MAX_WORKERS_UPLOAD = 6   # uploads en parallele
MAX_WORKERS_BRAND  = 4   # marques en parallele
MAX_WORKERS_MODEL  = 3   # modeles en parallele par marque

WIKI_IMAGE_CACHE = {}
WIKI_IMAGE_LOCK  = threading.Lock()


def resolve_target_url(env_or_url=None):
    if not env_or_url:
        return ENV_CONFIGS["local"]
    t = env_or_url.strip().lower()
    if t in ENV_CONFIGS:
        return ENV_CONFIGS[t]
    if env_or_url.startswith("http"):
        return env_or_url.rstrip("/")
    return ENV_CONFIGS["local"]


def configure_api_endpoints(base_url):
    global BASE_URL, API_BASE, API_UPLOAD_URL
    BASE_URL       = base_url.rstrip("/")
    API_BASE       = f"{BASE_URL}/api/v1/catalog"
    API_UPLOAD_URL = f"{BASE_URL}/api/v1/uploads/image"


# ── HTTP Helpers ───────────────────────────────────────────────────────────────

def _http(url, method="GET", data=None, headers=None, timeout=30):
    req = urllib.request.Request(url, data=data, headers=headers or {}, method=method)
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))


def api_get(endpoint, params=None):
    url = f"{API_BASE}{endpoint}"
    if params:
        url += "?" + urllib.parse.urlencode(params)
    try:
        return _http(url, headers={"Accept": "application/json"})
    except urllib.error.HTTPError as e:
        raise RuntimeError(f"GET {url} -> {e.code}: {e.read().decode()}") from e


def api_post(endpoint, data=None, params=None):
    url = f"{API_BASE}{endpoint}"
    if params:
        url += "?" + urllib.parse.urlencode(params)
    body = json.dumps(data or {}).encode("utf-8")
    try:
        return _http(url, method="POST", data=body, headers={"Content-Type": "application/json"})
    except urllib.error.HTTPError as e:
        raise RuntimeError(f"POST {url} -> {e.code}: {e.read().decode()}") from e


def api_put(endpoint, data=None, params=None):
    url = f"{API_BASE}{endpoint}"
    if params:
        url += "?" + urllib.parse.urlencode(params)
    body = json.dumps(data or {}).encode("utf-8")
    try:
        return _http(url, method="PUT", data=body, headers={"Content-Type": "application/json"})
    except urllib.error.HTTPError as e:
        raise RuntimeError(f"PUT {url} -> {e.code}: {e.read().decode()}") from e


def api_delete(endpoint):
    url = f"{API_BASE}{endpoint}"
    req = urllib.request.Request(url, method="DELETE")
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return resp.status
    except urllib.error.HTTPError as e:
        raise RuntimeError(f"DELETE {url} -> {e.code}: {e.read().decode()}") from e


def upload_image_bytes(image_data: bytes, filename: str, content_type: str = "image/jpeg", folder: str = "models") -> str:
    """Upload une image binaire (JPEG, PNG, WEBP, SVG) via multipart/form-data. Cache thread-safe."""
    cache_key = f"{folder}:{filename}"
    with UPLOAD_CACHE_LOCK:
        if cache_key in UPLOAD_CACHE:
            return UPLOAD_CACHE[cache_key]

    boundary = f"----Boundary{uuid.uuid4().hex}"

    body = bytearray()
    body += f"--{boundary}\r\n".encode()
    body += f'Content-Disposition: form-data; name="file"; filename="{filename}"\r\n'.encode()
    body += f"Content-Type: {content_type}\r\n\r\n".encode()
    body += image_data
    body += b"\r\n"
    body += f"--{boundary}--\r\n".encode()

    req = urllib.request.Request(
        f"{API_UPLOAD_URL}?folder={folder}",
        data=bytes(body),
        headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            url = json.loads(resp.read().decode()).get("url", "")
            with UPLOAD_CACHE_LOCK:
                UPLOAD_CACHE[cache_key] = url
            return url
    except Exception as ex:
        print(f"  [!] Upload image {filename} failed: {ex}", file=sys.stderr)
        return ""


def upload_svg(svg_content: str, filename: str, folder: str = "brands") -> str:
    """Upload un SVG via multipart/form-data. Cache thread-safe."""
    safe_fn = filename if filename.endswith(".svg") else f"{filename}.svg"
    return upload_image_bytes(svg_content.encode("utf-8"), safe_fn, content_type="image/svg+xml", folder=folder)


# Mapping direct des URLs de vraies photos officielles (Wikimedia Commons / Wikipedia)
DIRECT_MODEL_IMAGE_URLS = {
    "Megane E-Tech": "https://upload.wikimedia.org/wikipedia/commons/thumb/4/42/Renault_M%C3%A9gane_E-Tech_IMG_4064.jpg/330px-Renault_M%C3%A9gane_E-Tech_IMG_4064.jpg",
    "Scenic E-Tech": "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2e/2019_Renault_Grand_Scenic_Iconic_TCE_1.3.jpg/330px-2019_Renault_Grand_Scenic_Iconic_TCE_1.3.jpg",
    "R5 E-Tech": "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e4/Renault_5_E-Tech_Electric_DSC_7279.jpg/330px-Renault_5_E-Tech_Electric_DSC_7279.jpg",
    "Clio E-Tech Hybrid": "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3b/Renault_Clio_%28V%2C_Facelift%29_%E2%80%93_f_02092025.jpg/330px-Renault_Clio_%28V%2C_Facelift%29_%E2%80%93_f_02092025.jpg",
    "e-208": "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c6/Peugeot_208_PureTech_130_EAT8_Allure_GT-Line_%28II%29_%E2%80%93_f_17102021.jpg/330px-Peugeot_208_PureTech_130_EAT8_Allure_GT-Line_%28II%29_%E2%80%93_f_17102021.jpg",
    "e-2008": "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f7/2023_Peugeot_2008_in_Vertigo_Blue%2C_front_left%2C_06-08-2025.jpg/330px-2023_Peugeot_2008_in_Vertigo_Blue%2C_front_left%2C_06-08-2025.jpg",
    "e-3008": "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ed/Peugeot_e-3008_Automesse_Ludwigsburg_2024_IMG_1537.jpg/330px-Peugeot_e-3008_Automesse_Ludwigsburg_2024_IMG_1537.jpg",
    "Model 3": "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/Tesla_Model_3_%282023%29_Autofr%C3%BChling_Ulm_IMG_9282.jpg/330px-Tesla_Model_3_%282023%29_Autofr%C3%BChling_Ulm_IMG_9282.jpg",
    "Model Y": "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e7/Tesla_Model_Y_Premium_%28Facelift%29_%E2%80%93_f_05052026.jpg/330px-Tesla_Model_Y_Premium_%28Facelift%29_%E2%80%93_f_05052026.jpg",
    "Model S": "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9e/Tesla_Model_S_%28Facelift_ab_04-2016%29_%28cropped%29.jpg/330px-Tesla_Model_S_%28Facelift_ab_04-2016%29_%28cropped%29.jpg",
    "Spring": "https://upload.wikimedia.org/wikipedia/commons/thumb/7/77/2023_Renault_Kwid_Iconic_%28Colombia%29_front_view_01.png/330px-2023_Renault_Kwid_Iconic_%28Colombia%29_front_view_01.png",
    "Duster Hybrid": "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/Dacia_Duster_TCe_130_Extreme_%28III%29_%E2%80%93_f_13102024.jpg/330px-Dacia_Duster_TCe_130_Extreme_%28III%29_%E2%80%93_f_13102024.jpg",
    "Jogger Hybrid": "https://upload.wikimedia.org/wikipedia/commons/thumb/2/29/2023_Dacia_Jogger_DSC_7288.jpg/330px-2023_Dacia_Jogger_DSC_7288.jpg",
    "Yaris Hybrid": "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3e/2020_Toyota_Yaris_Design_HEV_CVT_1.5_Front.jpg/330px-2020_Toyota_Yaris_Design_HEV_CVT_1.5_Front.jpg",
    "Yaris Cross": "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f4/Toyota_Yaris_Cross_Hybrid_%28XP210%29_1X7A1846.jpg/330px-Toyota_Yaris_Cross_Hybrid_%28XP210%29_1X7A1846.jpg",
    "bZ4X": "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Toyota_bZ4X_Automesse_Ludwigsburg_2022_1X7A5895.jpg/330px-Toyota_bZ4X_Automesse_Ludwigsburg_2022_1X7A5895.jpg",
    "ID.3": "https://upload.wikimedia.org/wikipedia/commons/thumb/2/22/2020_Volkswagen_ID.3_1st_Front.jpg/330px-2020_Volkswagen_ID.3_1st_Front.jpg",
    "ID.4": "https://upload.wikimedia.org/wikipedia/commons/thumb/5/55/2025_Volkswagen_ID4_Pro_Redspot_front.jpg/330px-2025_Volkswagen_ID4_Pro_Redspot_front.jpg",
    "Golf eHybrid": "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/2020_Volkswagen_Golf_Style_1.5_Front.jpg/330px-2020_Volkswagen_Golf_Style_1.5_Front.jpg",
    "e-C3": "https://upload.wikimedia.org/wikipedia/commons/thumb/5/55/Citro%C3%ABn_C3_Hybrid_110_%C3%AB-DSC6_Max_%28IV%29_%E2%80%93_f_05042026.jpg/330px-Citro%C3%ABn_C3_Hybrid_110_%C3%AB-DSC6_Max_%28IV%29_%E2%80%93_f_05042026.jpg",
    "e-C4": "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3b/Citroen_C4_%282020%29_1X7A5851.jpg/330px-Citroen_C4_%282020%29_1X7A5851.jpg",
    "C5 Aircross Hybrid": "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0a/2022_Citro%C3%ABn_C5_Aircross_Automesse_Ludwigsburg_2024_IMG_1342.jpg/330px-2022_Citro%C3%ABn_C5_Aircross_Automesse_Ludwigsburg_2024_IMG_1342.jpg",
    "Ioniq 5": "https://upload.wikimedia.org/wikipedia/commons/thumb/8/85/Hyundai_Ioniq_5_AWD_Techniq-Paket_%E2%80%93_f_31122024.jpg/330px-Hyundai_Ioniq_5_AWD_Techniq-Paket_%E2%80%93_f_31122024.jpg",
    "Kona Electric": "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ac/Hyundai_Kona_N_Line_%28SX2%29_DSC_8250.jpg/330px-Hyundai_Kona_N_Line_%28SX2%29_DSC_8250.jpg",
    "Tucson Hybrid": "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c6/2022_Hyundai_Tucson_Preferred%2C_Front_Right%2C_05-24-2021.jpg/330px-2022_Hyundai_Tucson_Preferred%2C_Front_Right%2C_05-24-2021.jpg",
    "EV6": "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d9/2021_Kia_EV6_GT-Line_S.jpg/330px-2021_Kia_EV6_GT-Line_S.jpg",
    "EV9": "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Kia_EV9_1X7A2472.jpg/330px-Kia_EV9_1X7A2472.jpg",
    "Sportage PHEV": "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5d/2025_Kia_Sportage_S_front_only.jpg/330px-2025_Kia_Sportage_S_front_only.jpg",
    "iX1": "https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/2022_BMW_X1_sDrive18d_M_Sport_MHEV_Automatic_2.0.jpg/330px-2022_BMW_X1_sDrive18d_M_Sport_MHEV_Automatic_2.0.jpg",
    "i4": "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ad/BMW_i4_IMG_6695.jpg/330px-BMW_i4_IMG_6695.jpg",
    "iX3": "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a6/BMW_iX3_50_xDrive_M_Sportpaket_Pro_%28NA5%29_%E2%80%93_f_17052026.jpg/330px-BMW_iX3_50_xDrive_M_Sportpaket_Pro_%28NA5%29_%E2%80%93_f_17052026.jpg",
    "EQA": "https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/Mercedes-Benz_H243_IMG_5876.jpg/330px-Mercedes-Benz_H243_IMG_5876.jpg",
    "EQB": "https://upload.wikimedia.org/wikipedia/commons/thumb/7/74/Mercedes-Benz_X243_300_1X7A0422.jpg/330px-Mercedes-Benz_X243_300_1X7A0422.jpg",
    "EQC": "https://upload.wikimedia.org/wikipedia/commons/thumb/4/44/Mercedes-Benz_EQC_400_4MATIC_AMG_Line_%28N_293%29_%E2%80%93_f_02042021.jpg/330px-Mercedes-Benz_EQC_400_4MATIC_AMG_Line_%28N_293%29_%E2%80%93_f_02042021.jpg",
    "Q4 e-tron": "https://upload.wikimedia.org/wikipedia/commons/thumb/0/05/2021_Audi_Q4_e-tron_Sport_35.jpg/330px-2021_Audi_Q4_e-tron_Sport_35.jpg",
    "e-tron GT": "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d5/Audi_e-tron_GT_IMG_5689.jpg/330px-Audi_e-tron_GT_IMG_5689.jpg",
    "A6 e-tron": "https://upload.wikimedia.org/wikipedia/commons/thumb/9/91/Audi_A6_Avant_e-tron_%E2%80%93_f_18042025.jpg/330px-Audi_A6_Avant_e-tron_%E2%80%93_f_18042025.jpg",
    "MG4": "https://upload.wikimedia.org/wikipedia/commons/thumb/1/12/MG4_Electric_%E2%80%93_f_21042025.jpg/330px-MG4_Electric_%E2%80%93_f_21042025.jpg",
    "MG ZS EV": "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cf/MG_ZS_%28crossover%2C_second_generation%29_DSC_8542.jpg/330px-MG_ZS_%28crossover%2C_second_generation%29_DSC_8542.jpg",
    "EX30": "https://upload.wikimedia.org/wikipedia/commons/thumb/e/eb/Volvo_EX30_IMG_8923.jpg/330px-Volvo_EX30_IMG_8923.jpg",
    "XC40 Recharge": "https://upload.wikimedia.org/wikipedia/commons/thumb/2/27/2019_Volvo_XC40_T5_Momentum_in_Bright_Silver_Metallic%2C_front_left%2C_2025-09-22.jpg/330px-2019_Volvo_XC40_T5_Momentum_in_Bright_Silver_Metallic%2C_front_left%2C_2025-09-22.jpg",
    "Leaf": "https://upload.wikimedia.org/wikipedia/commons/thumb/7/73/Nissan_Leaf_%28ZE2%29_autoMOBIL_T%C3%BCbingen_2025_DSC_2752.jpg/330px-Nissan_Leaf_%28ZE2%29_autoMOBIL_T%C3%BCbingen_2025_DSC_2752.jpg",
    "Ariya": "https://upload.wikimedia.org/wikipedia/commons/thumb/6/64/2023_Nissan_Ariya_Advance_Front.jpg/330px-2023_Nissan_Ariya_Advance_Front.jpg",
    "Enyaq": "https://upload.wikimedia.org/wikipedia/commons/thumb/7/74/%C5%A0koda_Enyaq_IMG_1190.jpg/330px-%C5%A0koda_Enyaq_IMG_1190.jpg",
    "Octavia iV": "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Skoda_Octavia_IV_liftback_%28cropped%29.jpg/330px-Skoda_Octavia_IV_liftback_%28cropped%29.jpg",
    "Born": "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fa/Cupra_Born_%E2%80%93_f_03042026.jpg/330px-Cupra_Born_%E2%80%93_f_03042026.jpg",
    "Formentor e-Hybrid": "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9f/Cupra_Formentor_IMG_9668.jpg/330px-Cupra_Formentor_IMG_9668.jpg",
    "Atto 3": "https://upload.wikimedia.org/wikipedia/commons/thumb/5/50/BYD_Yuan_Plus_II_002.jpg/330px-BYD_Yuan_Plus_II_002.jpg",
    "Seal": "https://upload.wikimedia.org/wikipedia/commons/thumb/6/60/2022_BYD_Seal.jpg/330px-2022_BYD_Seal.jpg",
    "Mustang Mach-E": "https://upload.wikimedia.org/wikipedia/commons/thumb/4/49/2021_Ford_Mustang_Mach-E_Standard_Range_Front.jpg/330px-2021_Ford_Mustang_Mach-E_Standard_Range_Front.jpg",
    "Explorer Electric": "https://upload.wikimedia.org/wikipedia/commons/thumb/9/93/Ford_Explorer_EV_IMG_2120.jpg/330px-Ford_Explorer_EV_IMG_2120.jpg",
    "500e": "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/Fiat-500-vorne2.jpg/330px-Fiat-500-vorne2.jpg",
    "Panda Hybrid": "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d2/2018_Fiat_Panda_Easy_1.2.jpg/330px-2018_Fiat_Panda_Easy_1.2.jpg"
}


def get_or_upload_model_image(brand_name: str, model_name: str) -> str:
    """
    Telecharge la vraie photo officielle (PNG / JPEG / WebP) et l'uploade sur le backend.
    Retourne l'URL enregistree sur le serveur, ou "" si aucune photo reelle n'est disponible.
    """
    clean_name = f"{brand_name}_{model_name}".lower().replace(" ", "_").replace("/", "_").replace("-", "_")

    # Verifier si deja en cache
    cache_key = f"models:{clean_name}"
    with UPLOAD_CACHE_LOCK:
        if cache_key in UPLOAD_CACHE:
            return UPLOAD_CACHE[cache_key]

    img_url = DIRECT_MODEL_IMAGE_URLS.get(model_name)
    if not img_url:
        return ""

    # Telecharger et uploader la vraie photo avec backoff
    for attempt in range(5):
        try:
            req_img = urllib.request.Request(
                img_url,
                headers={"User-Agent": f"EcoSwitchAppBot/{attempt + 1}.0 (contact@ecoswitch.fr)"}
            )
            with urllib.request.urlopen(req_img, timeout=12) as r_img:
                img_data = r_img.read()
                content_type = r_img.headers.get("Content-Type", "image/jpeg").split(";")[0].strip()
                ext = ".jpg" if "jpeg" in content_type or "jpg" in content_type else ".png" if "png" in content_type else ".webp"
                uploaded_url = upload_image_bytes(img_data, f"{clean_name}{ext}", content_type=content_type, folder="models")
                if uploaded_url:
                    return uploaded_url
        except Exception as ex:
            time.sleep(1.0 * (attempt + 1))
            if attempt == 4:
                print(f"  [!] Telechargement image pour {model_name} echoue : {ex}", file=sys.stderr)

    return ""


# Mapping direct des logos officiels des marques (Wikimedia Commons)
DIRECT_BRAND_LOGOS = {
    "Renault": "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/Renault_2021.svg/330px-Renault_2021.svg.png",
    "Peugeot": "https://upload.wikimedia.org/wikipedia/en/thumb/9/9d/Peugeot_2021_Logo.svg/330px-Peugeot_2021_Logo.svg.png",
    "Tesla": "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bd/Tesla_Motors.svg/330px-Tesla_Motors.svg.png",
    "Dacia": "https://upload.wikimedia.org/wikipedia/commons/thumb/3/35/Dacia_2021.svg/330px-Dacia_2021.svg.png",
    "Toyota": "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e7/Toyota.svg/330px-Toyota.svg.png",
    "Volkswagen": "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6d/Volkswagen_logo_2019.svg/330px-Volkswagen_logo_2019.svg.png",
    "Citroën": "https://upload.wikimedia.org/wikipedia/commons/thumb/d/dd/Citroen_2022.svg/330px-Citroen_2022.svg.png",
    "Hyundai": "https://upload.wikimedia.org/wikipedia/commons/thumb/4/44/Hyundai_Motor_Company_logo.svg/330px-Hyundai_Motor_Company_logo.svg.png",
    "Kia": "https://upload.wikimedia.org/wikipedia/commons/thumb/8/82/Kia_buildings_New_Logo.png/330px-Kia_buildings_New_Logo.png",
    "BMW": "https://upload.wikimedia.org/wikipedia/commons/thumb/4/44/BMW.svg/330px-BMW.svg.png",
    "Mercedes-Benz": "https://upload.wikimedia.org/wikipedia/commons/thumb/9/90/Mercedes-Logo.svg/330px-Mercedes-Logo.svg.png",
    "Audi": "https://upload.wikimedia.org/wikipedia/commons/thumb/9/92/Audi-Logo_2016.svg/330px-Audi-Logo_2016.svg.png",
    "MG Motor": "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c8/MG_Motor_2021_logo.svg/330px-MG_Motor_2021_logo.svg.png",
    "Volvo": "https://upload.wikimedia.org/wikipedia/commons/thumb/2/29/Volvo-Iron-Mark-Black.svg/330px-Volvo-Iron-Mark-Black.svg.png",
    "Nissan": "https://upload.wikimedia.org/wikipedia/commons/thumb/2/23/Nissan_2020_logo.svg/330px-Nissan_2020_logo.svg.png",
    "Skoda": "https://upload.wikimedia.org/wikipedia/commons/thumb/0/09/%C5%A0koda_nieuw.png/330px-%C5%A0koda_nieuw.png",
    "Cupra": "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ef/Cupra_symbol.svg/330px-Cupra_symbol.svg.png",
    "BYD": "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e2/BYD_Auto_2022_logo.svg/330px-BYD_Auto_2022_logo.svg.png",
    "Ford": "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a0/Ford_Motor_Company_Logo.svg/330px-Ford_Motor_Company_Logo.svg.png",
    "Fiat": "https://upload.wikimedia.org/wikipedia/commons/thumb/1/12/Fiat_Automobiles_logo.svg/330px-Fiat_Automobiles_logo.svg.png"
}


def get_or_upload_brand_logo(brand_name: str) -> str:
    """
    Telecharge le vrai logo officiel en PNG transparent et l'uploade sur le backend.
    """
    clean_name = f"logo_{brand_name.lower().replace(' ', '_').replace('-', '_')}"
    cache_key = f"brands:{clean_name}"
    with UPLOAD_CACHE_LOCK:
        if cache_key in UPLOAD_CACHE:
            return UPLOAD_CACHE[cache_key]

    img_url = DIRECT_BRAND_LOGOS.get(brand_name)
    if not img_url:
        return ""

    for attempt in range(5):
        try:
            req = urllib.request.Request(
                img_url,
                headers={"User-Agent": f"EcoSwitchLogoBot/{attempt + 1}.0 (contact@ecoswitch.fr)"}
            )
            with urllib.request.urlopen(req, timeout=12) as r:
                img_data = r.read()
                content_type = r.headers.get("Content-Type", "image/png").split(";")[0].strip()
                ext = ".png" if "png" in content_type else ".jpg" if "jpeg" in content_type else ".svg"
                uploaded = upload_image_bytes(img_data, f"{clean_name}{ext}", content_type=content_type, folder="brands")
                if uploaded:
                    return uploaded
        except Exception as ex:
            time.sleep(1.0 * (attempt + 1))
            if attempt == 4:
                print(f"  [!] Upload logo marque {brand_name} echoue : {ex}", file=sys.stderr)

    return ""


# ── Palette de couleurs par marque ────────────────────────────────────────────

def _brand_colors(brand_name):
    palette = {
        "Renault":       ("#FCD000", "#FFE55A", "#1A1A1A"),
        "Peugeot":       ("#002D72", "#3E6DB4", "#F0F0F0"),
        "Tesla":         ("#CC0000", "#FF4444", "#1A1A1A"),
        "Dacia":         ("#1D3461", "#2A4A8A", "#F0F0F0"),
        "Toyota":        ("#EB0A1E", "#FF4455", "#1A1A1A"),
        "Volkswagen":    ("#001E50", "#1E4DB4", "#FFFFFF"),
        "Citroën":       ("#C51C22", "#E83238", "#FFFFFF"),
        "Hyundai":       ("#002C5F", "#1A5EA8", "#F8F8F8"),
        "Kia":           ("#05141F", "#0A2840", "#E8E8E8"),
        "BMW":           ("#1C6CB4", "#3E8FDB", "#F8F8F8"),
        "Mercedes-Benz": ("#2A2A2A", "#555555", "#C0C0C0"),
        "Audi":          ("#1A1A1A", "#404040", "#E8E8E8"),
        "MG Motor":      ("#B41A24", "#D93040", "#F8F8F8"),
        "Volvo":         ("#003057", "#005093", "#F0F0F0"),
        "Nissan":        ("#C71F28", "#E03040", "#FFFFFF"),
        "Skoda":         ("#4BA82E", "#68C948", "#1A1A1A"),
        "Cupra":         ("#C7832A", "#E09A40", "#0A0A0A"),
        "BYD":           ("#1A3C8C", "#2E5EC8", "#E8E8E8"),
        "Ford":          ("#003776", "#1A5BA8", "#FFFFFF"),
        "Fiat":          ("#CC2222", "#E03838", "#FFFFFF"),
    }
    return palette.get(brand_name, ("#0D9488", "#2DD4BF", "#F8F8F8"))


# ── Generateur de silhouettes de vehicules SVG ────────────────────────────────

def generate_model_svg(brand_name, model_name, category="Berline", fuel_type="ELECTRIC"):
    """
    Genere une silhouette SVG vectorielle reconnaissable par categorie.
    Viewbox 320x160.
    """
    c1, c2, c_text = _brand_colors(brand_name)
    is_ev     = fuel_type == "ELECTRIC"
    is_hybrid = "HYBRID" in fuel_type

    if is_ev:
        badge_color, badge_text = "#22C55E", "ELECTRIQUE"
    elif is_hybrid:
        badge_color, badge_text = "#38BDF8", "HYBRIDE"
    else:
        badge_color, badge_text = "#F59E0B", "THERMIQUE"

    cat = (category or "").upper()

    if "SUV" in cat or "CUV" in cat or "CROSSOVER" in cat:
        body   = "M 36 130 L 36 78 C 36 68 44 58 60 54 L 80 48 L 230 48 L 256 56 C 272 62 284 74 284 86 L 284 130 Z"
        window = "M 76 54 L 94 58 L 212 58 L 238 60 L 250 82 L 76 82 Z"
        w1x, w2x, wy, wr = 82, 248, 130, 20
        hood   = "M 54 86 L 284 86"
    elif "BERLINE" in cat or "COMPACTE" in cat:
        body   = "M 36 130 L 36 88 C 38 74 54 60 78 54 L 108 46 L 198 46 L 230 52 C 258 58 284 76 284 92 L 284 130 Z"
        window = "M 88 54 L 106 50 L 190 50 L 218 56 L 232 78 L 86 78 Z"
        w1x, w2x, wy, wr = 84, 248, 130, 19
        hood   = "M 52 84 L 284 84"
    elif "CITADINE" in cat:
        body   = "M 40 130 L 40 84 C 42 70 58 58 82 54 L 104 48 L 200 48 L 224 54 C 256 62 278 78 278 92 L 278 130 Z"
        window = "M 90 56 L 108 50 L 194 50 L 216 58 L 224 80 L 88 80 Z"
        w1x, w2x, wy, wr = 86, 240, 130, 19
        hood   = "M 58 86 L 278 86"
    elif "BREAK" in cat:
        body   = "M 30 130 L 30 82 C 32 68 48 56 76 52 L 100 46 L 240 46 L 280 52 L 290 78 L 290 130 Z"
        window = "M 84 54 L 100 50 L 232 50 L 270 56 L 270 78 L 82 78 Z"
        w1x, w2x, wy, wr = 82, 254, 130, 19
        hood   = "M 50 82 L 290 82"
    else:
        # Berline par defaut
        body   = "M 36 130 L 36 88 C 38 74 54 60 78 54 L 108 46 L 198 46 L 230 52 C 258 58 284 76 284 92 L 284 130 Z"
        window = "M 88 54 L 106 50 L 190 50 L 218 56 L 232 78 L 86 78 Z"
        w1x, w2x, wy, wr = 84, 248, 130, 19
        hood   = "M 52 84 L 284 84"

    label = f"{brand_name} {model_name[:12]}"

    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 320 160">
  <defs>
    <linearGradient id="bg{w1x}" x1="0" y1="0" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#16181C"/>
      <stop offset="100%" stop-color="#1E2028"/>
    </linearGradient>
    <linearGradient id="body{w1x}" x1="0" y1="0" x2="100%" y2="100%">
      <stop offset="0%" stop-color="{c1}"/>
      <stop offset="60%" stop-color="{c1}" stop-opacity="0.9"/>
      <stop offset="100%" stop-color="{c2}" stop-opacity="0.7"/>
    </linearGradient>
    <linearGradient id="glass{w1x}" x1="0" y1="0" x2="0" y2="100%">
      <stop offset="0%" stop-color="#60A5FA" stop-opacity="0.7"/>
      <stop offset="100%" stop-color="#1E40AF" stop-opacity="0.4"/>
    </linearGradient>
    <filter id="sha{w1x}">
      <feDropShadow dx="0" dy="4" stdDeviation="4" flood-color="#000" flood-opacity="0.5"/>
    </filter>
  </defs>
  <rect width="320" height="160" rx="12" fill="url(#bg{w1x})"/>
  <line x1="20" y1="134" x2="300" y2="134" stroke="#2D3748" stroke-width="1.5" stroke-dasharray="8,6"/>
  <ellipse cx="160" cy="136" rx="120" ry="8" fill="#000000" opacity="0.35"/>
  <path d="{body}" fill="url(#body{w1x})" filter="url(#sha{w1x})"/>
  <path d="{hood}" stroke="{c2}" stroke-width="1.5" stroke-opacity="0.6" fill="none"/>
  <path d="{window}" fill="url(#glass{w1x})" opacity="0.85"/>
  <line x1="160" y1="50" x2="160" y2="80" stroke="#0A1628" stroke-width="2.5"/>
  <circle cx="{w1x}" cy="{wy}" r="{wr}" fill="#111318" stroke="#4A5568" stroke-width="3"/>
  <circle cx="{w1x}" cy="{wy}" r="{wr - 6}" fill="none" stroke="#2D3748" stroke-width="1.5"/>
  <circle cx="{w1x}" cy="{wy}" r="{wr // 2}" fill="#2D3748"/>
  <circle cx="{w1x}" cy="{wy}" r="4" fill="{c2}"/>
  <circle cx="{w2x}" cy="{wy}" r="{wr}" fill="#111318" stroke="#4A5568" stroke-width="3"/>
  <circle cx="{w2x}" cy="{wy}" r="{wr - 6}" fill="none" stroke="#2D3748" stroke-width="1.5"/>
  <circle cx="{w2x}" cy="{wy}" r="{wr // 2}" fill="#2D3748"/>
  <circle cx="{w2x}" cy="{wy}" r="4" fill="{c2}"/>
  <ellipse cx="{w1x + wr + 8}" cy="100" rx="6" ry="4" fill="#FEFCBF" opacity="0.85"/>
  <ellipse cx="{w1x + wr + 4}" cy="100" rx="3" ry="2" fill="#FFFFFF"/>
  <ellipse cx="{w2x - wr - 6}" cy="100" rx="6" ry="4" fill="#FF3B30" opacity="0.8"/>
  <rect x="10" y="10" width="130" height="22" rx="5" fill="#000000" fill-opacity="0.55"/>
  <text x="75" y="25" font-family="-apple-system,Arial,sans-serif" font-size="11" font-weight="800" fill="{c_text}" text-anchor="middle">{label}</text>
  <rect x="210" y="10" width="100" height="22" rx="11" fill="rgba(0,0,0,0.55)" stroke="{badge_color}" stroke-width="1.5"/>
  <text x="260" y="25" font-family="sans-serif" font-size="9.5" font-weight="700" fill="{badge_color}" text-anchor="middle">{badge_text}</text>
  <text x="160" y="152" font-family="sans-serif" font-size="9" fill="#6B7280" text-anchor="middle">{category.upper()}</text>
</svg>'''


# ── Donnees du catalogue ──────────────────────────────────────────────────────

CATALOG_DATA = [
    # 1. RENAULT
    {"brand": "Renault", "models": [
        {"name": "Megane E-Tech", "category": "Compacte",
         "motorisations": [
             {"name": "EV40 Boost Charge 130 ch", "fuelType": "ELECTRIC", "consumptionWltp": 15.4, "powerHp": 130, "batteryCapacityKwh": 40.0},
             {"name": "EV60 Optimum Charge 220 ch", "fuelType": "ELECTRIC", "consumptionWltp": 16.1, "powerHp": 220, "batteryCapacityKwh": 60.0},
         ],
         "finitions": ["Equilibre", "Techno", "Iconic"],
         "variants": [
             {"finition": "Equilibre", "motorisation": "EV40 Boost Charge 130 ch",  "price": 34000, "loa": 260, "lld": 240, "insurance": 650, "maintenance": 250, "resale": 16000},
             {"finition": "Techno",    "motorisation": "EV60 Optimum Charge 220 ch", "price": 40000, "loa": 310, "lld": 290, "insurance": 720, "maintenance": 260, "resale": 19500},
             {"finition": "Iconic",    "motorisation": "EV60 Optimum Charge 220 ch", "price": 43000, "loa": 345, "lld": 320, "insurance": 760, "maintenance": 270, "resale": 21000},
         ]},
        {"name": "Scenic E-Tech", "category": "SUV",
         "motorisations": [
             {"name": "Autonomie Confort 170 ch",  "fuelType": "ELECTRIC", "consumptionWltp": 16.3, "powerHp": 170, "batteryCapacityKwh": 60.0},
             {"name": "Grande Autonomie 220 ch",   "fuelType": "ELECTRIC", "consumptionWltp": 16.8, "powerHp": 220, "batteryCapacityKwh": 87.0},
         ],
         "finitions": ["Evolution", "Techno", "Esprit Alpine", "Iconic"],
         "variants": [
             {"finition": "Evolution",     "motorisation": "Autonomie Confort 170 ch", "price": 39990, "loa": 320, "lld": 300, "insurance": 700, "maintenance": 260, "resale": 19000},
             {"finition": "Techno",        "motorisation": "Grande Autonomie 220 ch",  "price": 46990, "loa": 390, "lld": 370, "insurance": 780, "maintenance": 280, "resale": 23000},
             {"finition": "Esprit Alpine", "motorisation": "Grande Autonomie 220 ch",  "price": 49490, "loa": 420, "lld": 395, "insurance": 810, "maintenance": 290, "resale": 24500},
             {"finition": "Iconic",        "motorisation": "Grande Autonomie 220 ch",  "price": 52490, "loa": 450, "lld": 425, "insurance": 850, "maintenance": 300, "resale": 26000},
         ]},
        {"name": "R5 E-Tech", "category": "Citadine",
         "motorisations": [
             {"name": "Autonomie Urbaine 120 ch",  "fuelType": "ELECTRIC", "consumptionWltp": 14.8, "powerHp": 120, "batteryCapacityKwh": 40.0},
             {"name": "Autonomie Confort 150 ch",  "fuelType": "ELECTRIC", "consumptionWltp": 15.2, "powerHp": 150, "batteryCapacityKwh": 52.0},
         ],
         "finitions": ["Evolution", "Techno", "Iconic Cinq"],
         "variants": [
             {"finition": "Evolution",   "motorisation": "Autonomie Urbaine 120 ch", "price": 25000, "loa": 180, "lld": 165, "insurance": 520, "maintenance": 200, "resale": 13000},
             {"finition": "Techno",      "motorisation": "Autonomie Confort 150 ch", "price": 31490, "loa": 240, "lld": 220, "insurance": 580, "maintenance": 220, "resale": 16500},
             {"finition": "Iconic Cinq", "motorisation": "Autonomie Confort 150 ch", "price": 33490, "loa": 265, "lld": 245, "insurance": 610, "maintenance": 230, "resale": 17800},
         ]},
        {"name": "Clio E-Tech Hybrid", "category": "Citadine",
         "motorisations": [
             {"name": "E-Tech Full Hybrid 145", "fuelType": "HYBRID", "consumptionWltp": 4.2, "powerHp": 145, "batteryCapacityKwh": 1.2},
         ],
         "finitions": ["Evolution", "Techno", "Esprit Alpine"],
         "variants": [
             {"finition": "Evolution",     "motorisation": "E-Tech Full Hybrid 145", "price": 23800, "loa": 185, "lld": 170, "insurance": 550, "maintenance": 360, "resale": 12000},
             {"finition": "Techno",        "motorisation": "E-Tech Full Hybrid 145", "price": 25800, "loa": 210, "lld": 195, "insurance": 580, "maintenance": 370, "resale": 13500},
             {"finition": "Esprit Alpine", "motorisation": "E-Tech Full Hybrid 145", "price": 27600, "loa": 235, "lld": 215, "insurance": 610, "maintenance": 380, "resale": 14500},
         ]},
    ]},

    # 2. PEUGEOT
    {"brand": "Peugeot", "models": [
        {"name": "e-208", "category": "Citadine",
         "motorisations": [
             {"name": "Electrique 156 ch (54 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 14.4, "powerHp": 156, "batteryCapacityKwh": 54.0},
             {"name": "Hybrid 100 e-DCS6",           "fuelType": "HYBRID",   "consumptionWltp": 4.5,  "powerHp": 100, "batteryCapacityKwh": 0.9},
         ],
         "finitions": ["Style", "Allure", "GT"],
         "variants": [
             {"finition": "Style",  "motorisation": "Electrique 156 ch (54 kWh)", "price": 33550, "loa": 230, "lld": 210, "insurance": 580, "maintenance": 230, "resale": 16000},
             {"finition": "Allure", "motorisation": "Electrique 156 ch (54 kWh)", "price": 35100, "loa": 250, "lld": 230, "insurance": 600, "maintenance": 240, "resale": 17200},
             {"finition": "GT",     "motorisation": "Electrique 156 ch (54 kWh)", "price": 37300, "loa": 280, "lld": 260, "insurance": 640, "maintenance": 250, "resale": 18500},
             {"finition": "Style",  "motorisation": "Hybrid 100 e-DCS6",          "price": 24200, "loa": 190, "lld": 175, "insurance": 550, "maintenance": 360, "resale": 12500},
             {"finition": "GT",     "motorisation": "Hybrid 100 e-DCS6",          "price": 27800, "loa": 230, "lld": 210, "insurance": 590, "maintenance": 380, "resale": 14500},
         ]},
        {"name": "e-2008", "category": "SUV",
         "motorisations": [
             {"name": "Electrique 156 ch (54 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 15.3, "powerHp": 156, "batteryCapacityKwh": 54.0},
             {"name": "Hybrid 136 e-DCS6",           "fuelType": "HYBRID",   "consumptionWltp": 4.9,  "powerHp": 136, "batteryCapacityKwh": 0.9},
         ],
         "finitions": ["Allure", "GT"],
         "variants": [
             {"finition": "Allure", "motorisation": "Electrique 156 ch (54 kWh)", "price": 39250, "loa": 290, "lld": 270, "insurance": 660, "maintenance": 250, "resale": 19000},
             {"finition": "GT",     "motorisation": "Electrique 156 ch (54 kWh)", "price": 41450, "loa": 320, "lld": 295, "insurance": 700, "maintenance": 260, "resale": 20500},
             {"finition": "GT",     "motorisation": "Hybrid 136 e-DCS6",          "price": 32900, "loa": 265, "lld": 245, "insurance": 620, "maintenance": 390, "resale": 16000},
         ]},
        {"name": "e-3008", "category": "SUV",
         "motorisations": [
             {"name": "Electrique 210 ch (73 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 16.7, "powerHp": 210, "batteryCapacityKwh": 73.0},
             {"name": "Hybrid 136 e-DCS6",           "fuelType": "HYBRID",   "consumptionWltp": 5.5,  "powerHp": 136, "batteryCapacityKwh": 0.9},
         ],
         "finitions": ["Allure", "GT"],
         "variants": [
             {"finition": "Allure", "motorisation": "Electrique 210 ch (73 kWh)", "price": 44990, "loa": 360, "lld": 335, "insurance": 750, "maintenance": 280, "resale": 22000},
             {"finition": "GT",     "motorisation": "Electrique 210 ch (73 kWh)", "price": 49490, "loa": 410, "lld": 385, "insurance": 820, "maintenance": 290, "resale": 24500},
             {"finition": "GT",     "motorisation": "Hybrid 136 e-DCS6",          "price": 42990, "loa": 345, "lld": 320, "insurance": 740, "maintenance": 420, "resale": 21000},
         ]},
    ]},

    # 3. TESLA
    {"brand": "Tesla", "models": [
        {"name": "Model 3", "category": "Berline",
         "motorisations": [
             {"name": "Propulsion RWD",       "fuelType": "ELECTRIC", "consumptionWltp": 13.2, "powerHp": 283,  "batteryCapacityKwh": 60.0},
             {"name": "Grande Autonomie AWD",  "fuelType": "ELECTRIC", "consumptionWltp": 14.0, "powerHp": 498,  "batteryCapacityKwh": 78.0},
             {"name": "Performance AWD",       "fuelType": "ELECTRIC", "consumptionWltp": 16.7, "powerHp": 627,  "batteryCapacityKwh": 78.0},
         ],
         "finitions": ["Standard", "Long Range", "Performance"],
         "variants": [
             {"finition": "Standard",    "motorisation": "Propulsion RWD",       "price": 41490, "loa": 320, "lld": 299, "insurance": 850,  "maintenance": 220, "resale": 22000},
             {"finition": "Long Range",  "motorisation": "Grande Autonomie AWD",  "price": 50990, "loa": 420, "lld": 395, "insurance": 950,  "maintenance": 250, "resale": 27500},
             {"finition": "Performance", "motorisation": "Performance AWD",       "price": 57490, "loa": 510, "lld": 480, "insurance": 1100, "maintenance": 280, "resale": 31000},
         ]},
        {"name": "Model Y", "category": "SUV",
         "motorisations": [
             {"name": "Propulsion RWD",      "fuelType": "ELECTRIC", "consumptionWltp": 15.7, "powerHp": 299, "batteryCapacityKwh": 60.0},
             {"name": "Grande Autonomie AWD", "fuelType": "ELECTRIC", "consumptionWltp": 16.9, "powerHp": 514, "batteryCapacityKwh": 78.0},
         ],
         "finitions": ["Standard", "Long Range"],
         "variants": [
             {"finition": "Standard",   "motorisation": "Propulsion RWD",       "price": 44990, "loa": 360, "lld": 330, "insurance": 900, "maintenance": 240, "resale": 24000},
             {"finition": "Long Range", "motorisation": "Grande Autonomie AWD",  "price": 52990, "loa": 450, "lld": 415, "insurance": 980, "maintenance": 260, "resale": 28500},
         ]},
        {"name": "Model S", "category": "Berline",
         "motorisations": [
             {"name": "Dual Motor AWD 670 ch",   "fuelType": "ELECTRIC", "consumptionWltp": 17.5, "powerHp": 670,  "batteryCapacityKwh": 100.0},
             {"name": "Tri-Motor Plaid 1020 ch",  "fuelType": "ELECTRIC", "consumptionWltp": 18.7, "powerHp": 1020, "batteryCapacityKwh": 100.0},
         ],
         "finitions": ["Grande Autonomie", "Plaid"],
         "variants": [
             {"finition": "Grande Autonomie", "motorisation": "Dual Motor AWD 670 ch",   "price": 95990,  "loa": 890,  "lld": 820,  "insurance": 1400, "maintenance": 350, "resale": 50000},
             {"finition": "Plaid",            "motorisation": "Tri-Motor Plaid 1020 ch", "price": 110990, "loa": 1090, "lld": 990,  "insurance": 1800, "maintenance": 450, "resale": 60000},
         ]},
    ]},

    # 4. DACIA
    {"brand": "Dacia", "models": [
        {"name": "Spring", "category": "Citadine",
         "motorisations": [
             {"name": "Electric 45 ch", "fuelType": "ELECTRIC", "consumptionWltp": 13.9, "powerHp": 45, "batteryCapacityKwh": 26.8},
             {"name": "Electric 65 ch", "fuelType": "ELECTRIC", "consumptionWltp": 14.5, "powerHp": 65, "batteryCapacityKwh": 26.8},
         ],
         "finitions": ["Essential", "Extreme"],
         "variants": [
             {"finition": "Essential", "motorisation": "Electric 45 ch", "price": 18900, "loa": 120, "lld": 99,  "insurance": 420, "maintenance": 180, "resale": 9500},
             {"finition": "Extreme",   "motorisation": "Electric 65 ch", "price": 20900, "loa": 145, "lld": 125, "insurance": 460, "maintenance": 190, "resale": 11000},
         ]},
        {"name": "Duster Hybrid", "category": "SUV",
         "motorisations": [
             {"name": "Hybrid 140 ch", "fuelType": "HYBRID", "consumptionWltp": 4.9, "powerHp": 140, "batteryCapacityKwh": 1.2},
         ],
         "finitions": ["Expression", "Journey"],
         "variants": [
             {"finition": "Expression", "motorisation": "Hybrid 140 ch", "price": 26600, "loa": 210, "lld": 190, "insurance": 520, "maintenance": 340, "resale": 14000},
             {"finition": "Journey",    "motorisation": "Hybrid 140 ch", "price": 28100, "loa": 235, "lld": 210, "insurance": 560, "maintenance": 350, "resale": 15500},
         ]},
        {"name": "Jogger Hybrid", "category": "Break",
         "motorisations": [
             {"name": "Hybrid 140 ch", "fuelType": "HYBRID", "consumptionWltp": 4.8, "powerHp": 140, "batteryCapacityKwh": 1.2},
         ],
         "finitions": ["Expression", "Extreme"],
         "variants": [
             {"finition": "Expression", "motorisation": "Hybrid 140 ch", "price": 25200, "loa": 200, "lld": 180, "insurance": 510, "maintenance": 330, "resale": 13000},
             {"finition": "Extreme",    "motorisation": "Hybrid 140 ch", "price": 27200, "loa": 225, "lld": 205, "insurance": 540, "maintenance": 340, "resale": 14500},
         ]},
    ]},

    # 5. TOYOTA
    {"brand": "Toyota", "models": [
        {"name": "Yaris Hybrid", "category": "Citadine",
         "motorisations": [
             {"name": "116h Dynamic Force", "fuelType": "HYBRID", "consumptionWltp": 3.8, "powerHp": 116, "batteryCapacityKwh": 0.8},
             {"name": "130h Dynamic Force", "fuelType": "HYBRID", "consumptionWltp": 4.2, "powerHp": 130, "batteryCapacityKwh": 0.8},
         ],
         "finitions": ["Dynamic", "GR Sport"],
         "variants": [
             {"finition": "Dynamic",  "motorisation": "116h Dynamic Force", "price": 23950, "loa": 189, "lld": 175, "insurance": 540, "maintenance": 350, "resale": 13000},
             {"finition": "GR Sport", "motorisation": "130h Dynamic Force", "price": 28450, "loa": 239, "lld": 219, "insurance": 600, "maintenance": 370, "resale": 16000},
         ]},
        {"name": "Yaris Cross", "category": "SUV",
         "motorisations": [
             {"name": "116h Hybrid 2WD",   "fuelType": "HYBRID", "consumptionWltp": 4.4, "powerHp": 116, "batteryCapacityKwh": 0.8},
             {"name": "130h Hybrid AWD-i",  "fuelType": "HYBRID", "consumptionWltp": 4.8, "powerHp": 130, "batteryCapacityKwh": 0.8},
         ],
         "finitions": ["Dynamic", "Collection"],
         "variants": [
             {"finition": "Dynamic",    "motorisation": "116h Hybrid 2WD",  "price": 28200, "loa": 220, "lld": 200, "insurance": 580, "maintenance": 360, "resale": 15500},
             {"finition": "Collection", "motorisation": "130h Hybrid AWD-i", "price": 34700, "loa": 290, "lld": 270, "insurance": 650, "maintenance": 390, "resale": 19000},
         ]},
        {"name": "bZ4X", "category": "SUV",
         "motorisations": [
             {"name": "Pure 204 ch 2WD", "fuelType": "ELECTRIC", "consumptionWltp": 16.7, "powerHp": 204, "batteryCapacityKwh": 71.4},
         ],
         "finitions": ["Pure", "Origin"],
         "variants": [
             {"finition": "Pure",   "motorisation": "Pure 204 ch 2WD", "price": 39900, "loa": 340, "lld": 310, "insurance": 720, "maintenance": 260, "resale": 20000},
             {"finition": "Origin", "motorisation": "Pure 204 ch 2WD", "price": 45500, "loa": 395, "lld": 370, "insurance": 790, "maintenance": 270, "resale": 23000},
         ]},
    ]},

    # 6. VOLKSWAGEN
    {"brand": "Volkswagen", "models": [
        {"name": "ID.3", "category": "Compacte",
         "motorisations": [
             {"name": "Pro 204 ch (59 kWh)",  "fuelType": "ELECTRIC", "consumptionWltp": 15.3, "powerHp": 204, "batteryCapacityKwh": 59.0},
             {"name": "Pro S 204 ch (77 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 15.7, "powerHp": 204, "batteryCapacityKwh": 77.0},
         ],
         "finitions": ["Life Max", "Style"],
         "variants": [
             {"finition": "Life Max", "motorisation": "Pro 204 ch (59 kWh)",  "price": 37990, "loa": 299, "lld": 279, "insurance": 680, "maintenance": 250, "resale": 19000},
             {"finition": "Style",    "motorisation": "Pro S 204 ch (77 kWh)", "price": 44310, "loa": 370, "lld": 345, "insurance": 760, "maintenance": 270, "resale": 23000},
         ]},
        {"name": "ID.4", "category": "SUV",
         "motorisations": [
             {"name": "Pro 286 ch (77 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 16.2, "powerHp": 286, "batteryCapacityKwh": 77.0},
             {"name": "GTX 340 ch AWD",       "fuelType": "ELECTRIC", "consumptionWltp": 17.5, "powerHp": 340, "batteryCapacityKwh": 77.0},
         ],
         "finitions": ["Life Max", "GTX"],
         "variants": [
             {"finition": "Life Max", "motorisation": "Pro 286 ch (77 kWh)", "price": 45990, "loa": 380, "lld": 350, "insurance": 790, "maintenance": 280, "resale": 24000},
             {"finition": "GTX",      "motorisation": "GTX 340 ch AWD",       "price": 57900, "loa": 520, "lld": 485, "insurance": 960, "maintenance": 320, "resale": 30500},
         ]},
        {"name": "Golf eHybrid", "category": "Compacte",
         "motorisations": [
             {"name": "1.5 TSI eHybrid 204 ch", "fuelType": "HYBRID", "consumptionWltp": 0.9, "powerHp": 204, "batteryCapacityKwh": 19.7},
         ],
         "finitions": ["Style", "GTE"],
         "variants": [
             {"finition": "Style", "motorisation": "1.5 TSI eHybrid 204 ch", "price": 43500, "loa": 360, "lld": 330, "insurance": 730, "maintenance": 410, "resale": 22000},
             {"finition": "GTE",   "motorisation": "1.5 TSI eHybrid 204 ch", "price": 48200, "loa": 410, "lld": 380, "insurance": 820, "maintenance": 440, "resale": 25000},
         ]},
    ]},

    # 7. CITROEN
    {"brand": "Citroën", "models": [
        {"name": "e-C3", "category": "Citadine",
         "motorisations": [
             {"name": "Electrique 113 ch (44 kWh LFP)", "fuelType": "ELECTRIC", "consumptionWltp": 14.1, "powerHp": 113, "batteryCapacityKwh": 44.0},
         ],
         "finitions": ["You", "Max"],
         "variants": [
             {"finition": "You", "motorisation": "Electrique 113 ch (44 kWh LFP)", "price": 23300, "loa": 149, "lld": 129, "insurance": 480, "maintenance": 200, "resale": 12000},
             {"finition": "Max", "motorisation": "Electrique 113 ch (44 kWh LFP)", "price": 27800, "loa": 189, "lld": 169, "insurance": 520, "maintenance": 210, "resale": 14500},
         ]},
        {"name": "e-C4", "category": "Berline",
         "motorisations": [
             {"name": "Electrique 136 ch (50 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 15.3, "powerHp": 136, "batteryCapacityKwh": 50.0},
             {"name": "Electrique 156 ch (54 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 14.8, "powerHp": 156, "batteryCapacityKwh": 54.0},
         ],
         "finitions": ["Plus", "Max"],
         "variants": [
             {"finition": "Plus", "motorisation": "Electrique 136 ch (50 kWh)", "price": 35740, "loa": 259, "lld": 239, "insurance": 610, "maintenance": 230, "resale": 17500},
             {"finition": "Max",  "motorisation": "Electrique 156 ch (54 kWh)", "price": 39300, "loa": 299, "lld": 279, "insurance": 660, "maintenance": 240, "resale": 19500},
         ]},
        {"name": "C5 Aircross Hybrid", "category": "SUV",
         "motorisations": [
             {"name": "Hybrid 136 e-DCS6", "fuelType": "HYBRID", "consumptionWltp": 5.4, "powerHp": 136, "batteryCapacityKwh": 0.9},
         ],
         "finitions": ["Plus", "Max"],
         "variants": [
             {"finition": "Plus", "motorisation": "Hybrid 136 e-DCS6", "price": 37800, "loa": 290, "lld": 270, "insurance": 680, "maintenance": 410, "resale": 18500},
             {"finition": "Max",  "motorisation": "Hybrid 136 e-DCS6", "price": 41200, "loa": 330, "lld": 305, "insurance": 720, "maintenance": 430, "resale": 20500},
         ]},
    ]},

    # 8. HYUNDAI
    {"brand": "Hyundai", "models": [
        {"name": "Ioniq 5", "category": "Crossover",
         "motorisations": [
             {"name": "Intuitive 229 ch (84 kWh)",  "fuelType": "ELECTRIC", "consumptionWltp": 16.8, "powerHp": 229, "batteryCapacityKwh": 84.0},
             {"name": "HTRAC AWD 325 ch (84 kWh)",  "fuelType": "ELECTRIC", "consumptionWltp": 18.2, "powerHp": 325, "batteryCapacityKwh": 84.0},
         ],
         "finitions": ["Intuitive", "Executive"],
         "variants": [
             {"finition": "Intuitive", "motorisation": "Intuitive 229 ch (84 kWh)", "price": 46800, "loa": 380, "lld": 350, "insurance": 790, "maintenance": 270, "resale": 24000},
             {"finition": "Executive", "motorisation": "HTRAC AWD 325 ch (84 kWh)", "price": 60500, "loa": 530, "lld": 490, "insurance": 960, "maintenance": 310, "resale": 32000},
         ]},
        {"name": "Kona Electric", "category": "SUV",
         "motorisations": [
             {"name": "Standard 156 ch (48.4 kWh)",  "fuelType": "ELECTRIC", "consumptionWltp": 14.6, "powerHp": 156, "batteryCapacityKwh": 48.4},
             {"name": "Long Range 217 ch (65.4 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 15.1, "powerHp": 217, "batteryCapacityKwh": 65.4},
         ],
         "finitions": ["Intuitive", "Creative"],
         "variants": [
             {"finition": "Intuitive", "motorisation": "Standard 156 ch (48.4 kWh)",   "price": 37900, "loa": 290, "lld": 270, "insurance": 640, "maintenance": 240, "resale": 19000},
             {"finition": "Creative",  "motorisation": "Long Range 217 ch (65.4 kWh)", "price": 44400, "loa": 360, "lld": 335, "insurance": 730, "maintenance": 260, "resale": 23000},
         ]},
        {"name": "Tucson Hybrid", "category": "SUV",
         "motorisations": [
             {"name": "1.6 T-GDI Hybrid 215 ch", "fuelType": "HYBRID", "consumptionWltp": 5.6, "powerHp": 215, "batteryCapacityKwh": 1.49},
         ],
         "finitions": ["Intuitive", "N Line Executive"],
         "variants": [
             {"finition": "Intuitive",       "motorisation": "1.6 T-GDI Hybrid 215 ch", "price": 38900, "loa": 310, "lld": 290, "insurance": 690, "maintenance": 420, "resale": 20000},
             {"finition": "N Line Executive", "motorisation": "1.6 T-GDI Hybrid 215 ch", "price": 46800, "loa": 395, "lld": 365, "insurance": 780, "maintenance": 450, "resale": 24000},
         ]},
    ]},

    # 9. KIA
    {"brand": "Kia", "models": [
        {"name": "EV6", "category": "Crossover",
         "motorisations": [
             {"name": "229 ch 2WD (77.4 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 16.5, "powerHp": 229, "batteryCapacityKwh": 77.4},
             {"name": "325 ch AWD (77.4 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 17.8, "powerHp": 325, "batteryCapacityKwh": 77.4},
         ],
         "finitions": ["Air Design", "GT-line"],
         "variants": [
             {"finition": "Air Design", "motorisation": "229 ch 2WD (77.4 kWh)", "price": 49690, "loa": 390, "lld": 365, "insurance": 820, "maintenance": 280, "resale": 26000},
             {"finition": "GT-line",    "motorisation": "325 ch AWD (77.4 kWh)", "price": 58690, "loa": 510, "lld": 475, "insurance": 950, "maintenance": 310, "resale": 31000},
         ]},
        {"name": "EV9", "category": "SUV",
         "motorisations": [
             {"name": "Rear-wheel 160 kW (99.8 kWh)",  "fuelType": "ELECTRIC", "consumptionWltp": 21.2, "powerHp": 217, "batteryCapacityKwh": 99.8},
             {"name": "All-wheel 283 kW (99.8 kWh)",   "fuelType": "ELECTRIC", "consumptionWltp": 22.1, "powerHp": 385, "batteryCapacityKwh": 99.8},
         ],
         "finitions": ["Earth", "GT-line"],
         "variants": [
             {"finition": "Earth",   "motorisation": "Rear-wheel 160 kW (99.8 kWh)", "price": 74990, "loa": 680, "lld": 640, "insurance": 1100, "maintenance": 310, "resale": 40000},
             {"finition": "GT-line", "motorisation": "All-wheel 283 kW (99.8 kWh)",  "price": 84990, "loa": 780, "lld": 730, "insurance": 1250, "maintenance": 340, "resale": 45000},
         ]},
        {"name": "Sportage PHEV", "category": "SUV",
         "motorisations": [
             {"name": "PHEV 265 ch AWD", "fuelType": "PLUGIN_HYBRID", "consumptionWltp": 1.1, "powerHp": 265, "batteryCapacityKwh": 13.8},
         ],
         "finitions": ["GT-line", "GT-line Premium"],
         "variants": [
             {"finition": "GT-line",         "motorisation": "PHEV 265 ch AWD", "price": 45490, "loa": 385, "lld": 360, "insurance": 770, "maintenance": 430, "resale": 23000},
             {"finition": "GT-line Premium", "motorisation": "PHEV 265 ch AWD", "price": 48990, "loa": 420, "lld": 390, "insurance": 820, "maintenance": 450, "resale": 25000},
         ]},
    ]},

    # 10. BMW
    {"brand": "BMW", "models": [
        {"name": "iX1", "category": "SUV",
         "motorisations": [
             {"name": "xDrive30 313 ch (66 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 17.3, "powerHp": 313, "batteryCapacityKwh": 66.5},
         ],
         "finitions": ["xLine", "M Sport"],
         "variants": [
             {"finition": "xLine",  "motorisation": "xDrive30 313 ch (66 kWh)", "price": 56500, "loa": 490, "lld": 455, "insurance": 900,  "maintenance": 290, "resale": 30000},
             {"finition": "M Sport","motorisation": "xDrive30 313 ch (66 kWh)", "price": 62000, "loa": 560, "lld": 520, "insurance": 980,  "maintenance": 310, "resale": 33000},
         ]},
        {"name": "i4", "category": "Berline",
         "motorisations": [
             {"name": "eDrive40 340 ch (84 kWh)",   "fuelType": "ELECTRIC", "consumptionWltp": 17.4, "powerHp": 340, "batteryCapacityKwh": 83.9},
             {"name": "M50 xDrive 544 ch (84 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 19.8, "powerHp": 544, "batteryCapacityKwh": 83.9},
         ],
         "finitions": ["Gran Coupe", "M50"],
         "variants": [
             {"finition": "Gran Coupe", "motorisation": "eDrive40 340 ch (84 kWh)",   "price": 64800, "loa": 590, "lld": 550, "insurance": 1000, "maintenance": 320, "resale": 35000},
             {"finition": "M50",        "motorisation": "M50 xDrive 544 ch (84 kWh)", "price": 80400, "loa": 760, "lld": 710, "insurance": 1300, "maintenance": 380, "resale": 44000},
         ]},
        {"name": "iX3", "category": "SUV",
         "motorisations": [
             {"name": "286 ch (80 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 18.0, "powerHp": 286, "batteryCapacityKwh": 80.0},
         ],
         "finitions": ["Impressive", "M Sport"],
         "variants": [
             {"finition": "Impressive", "motorisation": "286 ch (80 kWh)", "price": 69900, "loa": 650, "lld": 610, "insurance": 1050, "maintenance": 330, "resale": 38000},
             {"finition": "M Sport",    "motorisation": "286 ch (80 kWh)", "price": 75200, "loa": 710, "lld": 665, "insurance": 1150, "maintenance": 350, "resale": 41000},
         ]},
    ]},

    # 11. MERCEDES-BENZ
    {"brand": "Mercedes-Benz", "models": [
        {"name": "EQA", "category": "SUV",
         "motorisations": [
             {"name": "250+ 190 ch (70.5 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 16.1, "powerHp": 190, "batteryCapacityKwh": 70.5},
         ],
         "finitions": ["AMG Line", "Edition 1"],
         "variants": [
             {"finition": "AMG Line",  "motorisation": "250+ 190 ch (70.5 kWh)", "price": 55200, "loa": 480, "lld": 445, "insurance": 900,  "maintenance": 300, "resale": 30000},
             {"finition": "Edition 1", "motorisation": "250+ 190 ch (70.5 kWh)", "price": 59900, "loa": 540, "lld": 500, "insurance": 960,  "maintenance": 320, "resale": 33000},
         ]},
        {"name": "EQB", "category": "SUV",
         "motorisations": [
             {"name": "300 4MATIC 228 ch (70.5 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 18.0, "powerHp": 228, "batteryCapacityKwh": 70.5},
         ],
         "finitions": ["AMG Line", "Edition 1"],
         "variants": [
             {"finition": "AMG Line",  "motorisation": "300 4MATIC 228 ch (70.5 kWh)", "price": 65100, "loa": 590, "lld": 550, "insurance": 1000, "maintenance": 330, "resale": 36000},
             {"finition": "Edition 1", "motorisation": "300 4MATIC 228 ch (70.5 kWh)", "price": 70000, "loa": 660, "lld": 620, "insurance": 1100, "maintenance": 360, "resale": 39000},
         ]},
        {"name": "EQC", "category": "SUV",
         "motorisations": [
             {"name": "400 4MATIC 408 ch (80 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 22.2, "powerHp": 408, "batteryCapacityKwh": 80.0},
         ],
         "finitions": ["AMG Line", "Edition 1886"],
         "variants": [
             {"finition": "AMG Line",     "motorisation": "400 4MATIC 408 ch (80 kWh)", "price": 79900, "loa": 750, "lld": 700, "insurance": 1200, "maintenance": 370, "resale": 44000},
             {"finition": "Edition 1886", "motorisation": "400 4MATIC 408 ch (80 kWh)", "price": 86000, "loa": 820, "lld": 770, "insurance": 1350, "maintenance": 400, "resale": 48000},
         ]},
    ]},

    # 12. AUDI
    {"brand": "Audi", "models": [
        {"name": "Q4 e-tron", "category": "SUV",
         "motorisations": [
             {"name": "35 e-tron 170 ch (55 kWh)",         "fuelType": "ELECTRIC", "consumptionWltp": 16.2, "powerHp": 170, "batteryCapacityKwh": 55.0},
             {"name": "40 e-tron 204 ch (82 kWh)",         "fuelType": "ELECTRIC", "consumptionWltp": 17.0, "powerHp": 204, "batteryCapacityKwh": 82.0},
             {"name": "50 e-tron quattro 299 ch (82 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 19.1, "powerHp": 299, "batteryCapacityKwh": 82.0},
         ],
         "finitions": ["Business", "S line", "Edition Sport"],
         "variants": [
             {"finition": "Business",     "motorisation": "35 e-tron 170 ch (55 kWh)",         "price": 45900, "loa": 380, "lld": 355, "insurance": 790,  "maintenance": 280, "resale": 25000},
             {"finition": "S line",       "motorisation": "40 e-tron 204 ch (82 kWh)",         "price": 56900, "loa": 500, "lld": 470, "insurance": 930,  "maintenance": 310, "resale": 31000},
             {"finition": "Edition Sport","motorisation": "50 e-tron quattro 299 ch (82 kWh)", "price": 65900, "loa": 620, "lld": 580, "insurance": 1050, "maintenance": 340, "resale": 37000},
         ]},
        {"name": "e-tron GT", "category": "Berline",
         "motorisations": [
             {"name": "quattro 476 ch (93 kWh)",      "fuelType": "ELECTRIC", "consumptionWltp": 19.6, "powerHp": 476, "batteryCapacityKwh": 93.4},
             {"name": "RS e-tron GT 646 ch (93 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 21.0, "powerHp": 646, "batteryCapacityKwh": 93.4},
         ],
         "finitions": ["quattro", "RS"],
         "variants": [
             {"finition": "quattro", "motorisation": "quattro 476 ch (93 kWh)",      "price": 110900, "loa": 1050, "lld": 980,  "insurance": 1600, "maintenance": 450, "resale": 62000},
             {"finition": "RS",      "motorisation": "RS e-tron GT 646 ch (93 kWh)", "price": 152000, "loa": 1490, "lld": 1390, "insurance": 2200, "maintenance": 600, "resale": 88000},
         ]},
        {"name": "A6 e-tron", "category": "Berline",
         "motorisations": [
             {"name": "Sportback 271 ch (94.9 kWh)",         "fuelType": "ELECTRIC", "consumptionWltp": 15.5, "powerHp": 271, "batteryCapacityKwh": 94.9},
             {"name": "Sportback quattro 367 ch (94.9 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 16.5, "powerHp": 367, "batteryCapacityKwh": 94.9},
         ],
         "finitions": ["Business", "S line"],
         "variants": [
             {"finition": "Business", "motorisation": "Sportback 271 ch (94.9 kWh)",         "price": 74900, "loa": 700, "lld": 650, "insurance": 1100, "maintenance": 340, "resale": 42000},
             {"finition": "S line",   "motorisation": "Sportback quattro 367 ch (94.9 kWh)", "price": 88900, "loa": 850, "lld": 795, "insurance": 1300, "maintenance": 380, "resale": 50000},
         ]},
    ]},

    # 13. MG MOTOR
    {"brand": "MG Motor", "models": [
        {"name": "MG4", "category": "Compacte",
         "motorisations": [
             {"name": "Standard 170 ch (51 kWh)",   "fuelType": "ELECTRIC", "consumptionWltp": 15.0, "powerHp": 170, "batteryCapacityKwh": 51.0},
             {"name": "Long Range 203 ch (64 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 16.4, "powerHp": 203, "batteryCapacityKwh": 64.0},
         ],
         "finitions": ["Standard", "Luxury"],
         "variants": [
             {"finition": "Standard", "motorisation": "Standard 170 ch (51 kWh)",   "price": 25990, "loa": 199, "lld": 179, "insurance": 520, "maintenance": 210, "resale": 14000},
             {"finition": "Luxury",   "motorisation": "Long Range 203 ch (64 kWh)", "price": 32990, "loa": 270, "lld": 249, "insurance": 600, "maintenance": 230, "resale": 18000},
         ]},
        {"name": "MG ZS EV", "category": "SUV",
         "motorisations": [
             {"name": "Comfort 177 ch (51 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 15.7, "powerHp": 177, "batteryCapacityKwh": 51.0},
             {"name": "Luxury 177 ch (72 kWh)",  "fuelType": "ELECTRIC", "consumptionWltp": 16.8, "powerHp": 177, "batteryCapacityKwh": 72.0},
         ],
         "finitions": ["Comfort", "Luxury"],
         "variants": [
             {"finition": "Comfort", "motorisation": "Comfort 177 ch (51 kWh)", "price": 29990, "loa": 229, "lld": 210, "insurance": 570, "maintenance": 220, "resale": 16000},
             {"finition": "Luxury",  "motorisation": "Luxury 177 ch (72 kWh)",  "price": 36490, "loa": 290, "lld": 269, "insurance": 640, "maintenance": 240, "resale": 20000},
         ]},
    ]},

    # 14. VOLVO
    {"brand": "Volvo", "models": [
        {"name": "EX30", "category": "SUV",
         "motorisations": [
             {"name": "Single Motor 272 ch (69 kWh)",          "fuelType": "ELECTRIC", "consumptionWltp": 16.5, "powerHp": 272, "batteryCapacityKwh": 69.0},
             {"name": "Twin Motor Performance 428 ch (69 kWh)","fuelType": "ELECTRIC", "consumptionWltp": 18.1, "powerHp": 428, "batteryCapacityKwh": 69.0},
         ],
         "finitions": ["Core", "Plus", "Ultra"],
         "variants": [
             {"finition": "Core",  "motorisation": "Single Motor 272 ch (69 kWh)",          "price": 36990, "loa": 299, "lld": 279, "insurance": 680, "maintenance": 260, "resale": 20000},
             {"finition": "Plus",  "motorisation": "Single Motor 272 ch (69 kWh)",          "price": 43990, "loa": 369, "lld": 339, "insurance": 760, "maintenance": 280, "resale": 24000},
             {"finition": "Ultra", "motorisation": "Twin Motor Performance 428 ch (69 kWh)","price": 51990, "loa": 459, "lld": 425, "insurance": 870, "maintenance": 310, "resale": 29000},
         ]},
        {"name": "XC40 Recharge", "category": "SUV",
         "motorisations": [
             {"name": "Single Motor 231 ch (69 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 18.0, "powerHp": 231, "batteryCapacityKwh": 69.0},
             {"name": "Twin Motor 408 ch (79 kWh)",   "fuelType": "ELECTRIC", "consumptionWltp": 19.5, "powerHp": 408, "batteryCapacityKwh": 79.0},
         ],
         "finitions": ["Core", "Plus", "Ultra"],
         "variants": [
             {"finition": "Core",  "motorisation": "Single Motor 231 ch (69 kWh)", "price": 47990, "loa": 410, "lld": 385, "insurance": 820,  "maintenance": 290, "resale": 27000},
             {"finition": "Plus",  "motorisation": "Single Motor 231 ch (69 kWh)", "price": 53990, "loa": 475, "lld": 445, "insurance": 900,  "maintenance": 310, "resale": 30000},
             {"finition": "Ultra", "motorisation": "Twin Motor 408 ch (79 kWh)",   "price": 62990, "loa": 590, "lld": 555, "insurance": 1000, "maintenance": 340, "resale": 36000},
         ]},
    ]},

    # 15. NISSAN
    {"brand": "Nissan", "models": [
        {"name": "Leaf", "category": "Compacte",
         "motorisations": [
             {"name": "Acenta 150 ch (40 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 15.0, "powerHp": 150, "batteryCapacityKwh": 40.0},
             {"name": "e+ 217 ch (62 kWh)",     "fuelType": "ELECTRIC", "consumptionWltp": 16.4, "powerHp": 217, "batteryCapacityKwh": 62.0},
         ],
         "finitions": ["Acenta", "N-Connecta", "Tekna"],
         "variants": [
             {"finition": "Acenta",     "motorisation": "Acenta 150 ch (40 kWh)", "price": 34890, "loa": 270, "lld": 250, "insurance": 590, "maintenance": 230, "resale": 18000},
             {"finition": "N-Connecta", "motorisation": "Acenta 150 ch (40 kWh)", "price": 38890, "loa": 320, "lld": 295, "insurance": 650, "maintenance": 240, "resale": 20000},
             {"finition": "Tekna",      "motorisation": "e+ 217 ch (62 kWh)",     "price": 44890, "loa": 390, "lld": 360, "insurance": 730, "maintenance": 260, "resale": 24000},
         ]},
        {"name": "Ariya", "category": "SUV",
         "motorisations": [
             {"name": "87 kWh 242 ch 2WD",    "fuelType": "ELECTRIC", "consumptionWltp": 18.0, "powerHp": 242, "batteryCapacityKwh": 87.0},
             {"name": "87 kWh 306 ch e-4ORCE","fuelType": "ELECTRIC", "consumptionWltp": 20.4, "powerHp": 306, "batteryCapacityKwh": 87.0},
         ],
         "finitions": ["Engage", "Evolve+"],
         "variants": [
             {"finition": "Engage",  "motorisation": "87 kWh 242 ch 2WD",     "price": 59990, "loa": 550, "lld": 510, "insurance": 950,  "maintenance": 300, "resale": 33000},
             {"finition": "Evolve+", "motorisation": "87 kWh 306 ch e-4ORCE", "price": 68990, "loa": 650, "lld": 610, "insurance": 1080, "maintenance": 330, "resale": 38000},
         ]},
    ]},

    # 16. SKODA
    {"brand": "Skoda", "models": [
        {"name": "Enyaq", "category": "SUV",
         "motorisations": [
             {"name": "60 204 ch (62 kWh)",    "fuelType": "ELECTRIC", "consumptionWltp": 16.2, "powerHp": 204, "batteryCapacityKwh": 62.0},
             {"name": "80 204 ch (82 kWh)",    "fuelType": "ELECTRIC", "consumptionWltp": 17.2, "powerHp": 204, "batteryCapacityKwh": 82.0},
             {"name": "80x 4x4 265 ch (82 kWh)","fuelType": "ELECTRIC","consumptionWltp": 18.5,"powerHp": 265, "batteryCapacityKwh": 82.0},
         ],
         "finitions": ["Selection", "Sportline", "L&K"],
         "variants": [
             {"finition": "Selection",  "motorisation": "60 204 ch (62 kWh)",     "price": 38990, "loa": 320, "lld": 295, "insurance": 680, "maintenance": 260, "resale": 21000},
             {"finition": "Sportline",  "motorisation": "80 204 ch (82 kWh)",     "price": 50490, "loa": 440, "lld": 410, "insurance": 840, "maintenance": 290, "resale": 28000},
             {"finition": "L&K",        "motorisation": "80x 4x4 265 ch (82 kWh)","price": 57490, "loa": 520, "lld": 480, "insurance": 940, "maintenance": 310, "resale": 32000},
         ]},
        {"name": "Octavia iV", "category": "Break",
         "motorisations": [
             {"name": "1.4 TSI iV 245 ch PHEV", "fuelType": "PLUGIN_HYBRID", "consumptionWltp": 1.0, "powerHp": 245, "batteryCapacityKwh": 13.0},
         ],
         "finitions": ["Selection", "Sportline"],
         "variants": [
             {"finition": "Selection", "motorisation": "1.4 TSI iV 245 ch PHEV", "price": 40290, "loa": 340, "lld": 315, "insurance": 710, "maintenance": 400, "resale": 22000},
             {"finition": "Sportline", "motorisation": "1.4 TSI iV 245 ch PHEV", "price": 44990, "loa": 390, "lld": 360, "insurance": 770, "maintenance": 420, "resale": 25000},
         ]},
    ]},

    # 17. CUPRA
    {"brand": "Cupra", "models": [
        {"name": "Born", "category": "Compacte",
         "motorisations": [
             {"name": "e-Boost 231 ch (59 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 15.5, "powerHp": 231, "batteryCapacityKwh": 59.0},
             {"name": "e-Boost 231 ch (77 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 16.0, "powerHp": 231, "batteryCapacityKwh": 77.0},
         ],
         "finitions": ["V1", "V2", "V3"],
         "variants": [
             {"finition": "V1", "motorisation": "e-Boost 231 ch (59 kWh)", "price": 38290, "loa": 310, "lld": 290, "insurance": 680, "maintenance": 260, "resale": 21000},
             {"finition": "V2", "motorisation": "e-Boost 231 ch (59 kWh)", "price": 41490, "loa": 360, "lld": 335, "insurance": 730, "maintenance": 270, "resale": 23000},
             {"finition": "V3", "motorisation": "e-Boost 231 ch (77 kWh)", "price": 47290, "loa": 420, "lld": 390, "insurance": 810, "maintenance": 290, "resale": 26500},
         ]},
        {"name": "Formentor e-Hybrid", "category": "SUV",
         "motorisations": [
             {"name": "1.5 e-Hybrid 204 ch", "fuelType": "PLUGIN_HYBRID", "consumptionWltp": 1.0, "powerHp": 204, "batteryCapacityKwh": 12.8},
             {"name": "1.5 e-Hybrid 272 ch", "fuelType": "PLUGIN_HYBRID", "consumptionWltp": 0.9, "powerHp": 272, "batteryCapacityKwh": 12.8},
         ],
         "finitions": ["VZ", "VZ Adrenaline"],
         "variants": [
             {"finition": "VZ",            "motorisation": "1.5 e-Hybrid 204 ch", "price": 39990, "loa": 330, "lld": 310, "insurance": 720, "maintenance": 400, "resale": 22000},
             {"finition": "VZ Adrenaline", "motorisation": "1.5 e-Hybrid 272 ch", "price": 49490, "loa": 430, "lld": 400, "insurance": 850, "maintenance": 430, "resale": 27500},
         ]},
    ]},

    # 18. BYD
    {"brand": "BYD", "models": [
        {"name": "Atto 3", "category": "SUV",
         "motorisations": [
             {"name": "204 ch (60.5 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 15.4, "powerHp": 204, "batteryCapacityKwh": 60.5},
         ],
         "finitions": ["Comfort", "Boost"],
         "variants": [
             {"finition": "Comfort", "motorisation": "204 ch (60.5 kWh)", "price": 37990, "loa": 299, "lld": 279, "insurance": 680, "maintenance": 240, "resale": 21000},
             {"finition": "Boost",   "motorisation": "204 ch (60.5 kWh)", "price": 41990, "loa": 345, "lld": 320, "insurance": 730, "maintenance": 250, "resale": 23000},
         ]},
        {"name": "Seal", "category": "Berline",
         "motorisations": [
             {"name": "313 ch RWD (82.5 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 16.4, "powerHp": 313, "batteryCapacityKwh": 82.5},
             {"name": "530 ch AWD (82.5 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 18.0, "powerHp": 530, "batteryCapacityKwh": 82.5},
         ],
         "finitions": ["Comfort", "Excellence"],
         "variants": [
             {"finition": "Comfort",   "motorisation": "313 ch RWD (82.5 kWh)", "price": 44990, "loa": 370, "lld": 345, "insurance": 790, "maintenance": 270, "resale": 25000},
             {"finition": "Excellence","motorisation": "530 ch AWD (82.5 kWh)", "price": 52990, "loa": 465, "lld": 435, "insurance": 900, "maintenance": 300, "resale": 30000},
         ]},
    ]},

    # 19. FORD
    {"brand": "Ford", "models": [
        {"name": "Mustang Mach-E", "category": "SUV",
         "motorisations": [
             {"name": "Standard 269 ch (75 kWh)",       "fuelType": "ELECTRIC", "consumptionWltp": 18.0, "powerHp": 269, "batteryCapacityKwh": 75.0},
             {"name": "Extended Range 294 ch (98 kWh)", "fuelType": "ELECTRIC", "consumptionWltp": 19.5, "powerHp": 294, "batteryCapacityKwh": 98.7},
         ],
         "finitions": ["Select", "Premium", "GT"],
         "variants": [
             {"finition": "Select",  "motorisation": "Standard 269 ch (75 kWh)",       "price": 46800, "loa": 390, "lld": 360, "insurance": 800,  "maintenance": 280, "resale": 26000},
             {"finition": "Premium", "motorisation": "Extended Range 294 ch (98 kWh)", "price": 59200, "loa": 530, "lld": 490, "insurance": 960,  "maintenance": 310, "resale": 33000},
             {"finition": "GT",      "motorisation": "Extended Range 294 ch (98 kWh)", "price": 67200, "loa": 630, "lld": 585, "insurance": 1080, "maintenance": 340, "resale": 38000},
         ]},
        {"name": "Explorer Electric", "category": "SUV",
         "motorisations": [
             {"name": "286 ch (79 kWh)",    "fuelType": "ELECTRIC", "consumptionWltp": 18.5, "powerHp": 286, "batteryCapacityKwh": 79.0},
             {"name": "340 ch AWD (79 kWh)","fuelType": "ELECTRIC", "consumptionWltp": 20.2, "powerHp": 340, "batteryCapacityKwh": 79.0},
         ],
         "finitions": ["Select", "Premium"],
         "variants": [
             {"finition": "Select",  "motorisation": "286 ch (79 kWh)",    "price": 47000, "loa": 395, "lld": 365, "insurance": 810, "maintenance": 290, "resale": 26000},
             {"finition": "Premium", "motorisation": "340 ch AWD (79 kWh)","price": 54000, "loa": 480, "lld": 445, "insurance": 910, "maintenance": 320, "resale": 30000},
         ]},
    ]},

    # 20. FIAT
    {"brand": "Fiat", "models": [
        {"name": "500e", "category": "Citadine",
         "motorisations": [
             {"name": "Icon 118 ch (42 kWh)",   "fuelType": "ELECTRIC", "consumptionWltp": 14.2, "powerHp": 118, "batteryCapacityKwh": 42.0},
             {"name": "La Prima 118 ch (42 kWh)","fuelType": "ELECTRIC","consumptionWltp": 14.5, "powerHp": 118, "batteryCapacityKwh": 42.0},
         ],
         "finitions": ["Action", "Icon", "La Prima"],
         "variants": [
             {"finition": "Action",   "motorisation": "Icon 118 ch (42 kWh)",    "price": 26350, "loa": 199, "lld": 180, "insurance": 510, "maintenance": 210, "resale": 14000},
             {"finition": "Icon",     "motorisation": "Icon 118 ch (42 kWh)",    "price": 31350, "loa": 250, "lld": 230, "insurance": 570, "maintenance": 220, "resale": 17000},
             {"finition": "La Prima", "motorisation": "La Prima 118 ch (42 kWh)","price": 35950, "loa": 299, "lld": 275, "insurance": 630, "maintenance": 230, "resale": 20000},
         ]},
        {"name": "Panda Hybrid", "category": "Citadine",
         "motorisations": [
             {"name": "1.0 Mild Hybrid 70 ch", "fuelType": "HYBRID", "consumptionWltp": 5.0, "powerHp": 70, "batteryCapacityKwh": 0.2},
         ],
         "finitions": ["Pop", "Sport"],
         "variants": [
             {"finition": "Pop",   "motorisation": "1.0 Mild Hybrid 70 ch", "price": 19200, "loa": 149, "lld": 135, "insurance": 440, "maintenance": 320, "resale": 10500},
             {"finition": "Sport", "motorisation": "1.0 Mild Hybrid 70 ch", "price": 21700, "loa": 179, "lld": 159, "insurance": 480, "maintenance": 330, "resale": 12000},
         ]},
    ]},
]


# ── Logique de seeding ────────────────────────────────────────────────────────

def _get_or_create(list_fn, create_fn, match_key, name, update_fn=None):
    """Tente create_fn ; en cas d'echec recupere l'objet existant par name."""
    try:
        return create_fn()
    except Exception:
        try:
            items = list_fn()
        except Exception:
            return None
        obj = next((i for i in items if i.get(match_key, "").lower() == name.lower()), None)
        if obj and update_fn:
            try:
                update_fn(obj["id"])
            except Exception:
                pass
        return obj


def seed_brand(brand_data, stats_lock, stats):
    """Traite une marque : logo + modeles + motorisations + finitions + variantes."""
    brand_name = brand_data["brand"]

    # 1. Logo officiel PNG transparent
    logo_url = get_or_upload_brand_logo(brand_name)
    print(f"[Brand] {brand_name} — logo -> {logo_url or '(echec)'}")

    # 2. Creer/retrouver la marque
    brand_obj = _get_or_create(
        list_fn=lambda: api_get("/brands"),
        create_fn=lambda: api_post("/brands", {"name": brand_name, "logoUrl": logo_url}),
        match_key="name", name=brand_name,
        update_fn=lambda bid: api_put(f"/brands/{bid}", {"name": brand_name, "logoUrl": logo_url}),
    )
    if not brand_obj:
        print(f"  [!] Impossible de creer/trouver la marque {brand_name}", file=sys.stderr)
        return
    brand_id = brand_obj["id"]
    with stats_lock:
        stats["brands"] += 1

    # 3. Modeles
    for model_data in brand_data.get("models", []):
        model_name = model_data["name"]
        model_cat  = model_data.get("category", "Berline")
        mots_list  = model_data.get("motorisations", [])
        fuel_types = [m.get("fuelType", "PETROL") for m in mots_list]
        dominant   = ("ELECTRIC" if "ELECTRIC" in fuel_types
                      else "HYBRID" if any("HYBRID" in f for f in fuel_types)
                      else "PETROL")

        model_url = get_or_upload_model_image(brand_name, model_name)
        if not model_url:
            print(f"  [SKIP] Modele {brand_name} {model_name} ignore car aucune image reelle trouvee.")
            continue

        model_obj = _get_or_create(
            list_fn=lambda: api_get("/models", params={"brandId": brand_id}),
            create_fn=lambda: api_post("/models",
                                       {"name": model_name, "imageUrl": model_url, "category": model_cat},
                                       params={"brandId": brand_id}),
            match_key="name", name=model_name,
            update_fn=lambda mid: api_put(f"/models/{mid}",
                                          {"name": model_name, "imageUrl": model_url, "category": model_cat}),
        )
        if not model_obj:
            print(f"  [!] Impossible de creer/trouver le modele {model_name}", file=sys.stderr)
            continue
        model_id = model_obj["id"]
        with stats_lock:
            stats["models"] += 1
        print(f"  [Model] {model_name} ({model_cat}) -> id={model_id} | img={model_url}")

        # 4. Motorisations
        mot_id_map = {}
        for mot in mots_list:
            mot_name = mot["name"]
            mot_obj = _get_or_create(
                list_fn=lambda: api_get("/motorisations", params={"modelId": model_id}),
                create_fn=lambda m=mot: api_post("/motorisations", m, params={"modelId": model_id}),
                match_key="name", name=mot_name,
            )
            if mot_obj:
                mot_id_map[mot_name] = mot_obj["id"]
                with stats_lock:
                    stats["motorisations"] += 1

        # 5. Finitions en parallele (reutilisation de l'image du modele)
        fin_names = model_data.get("finitions", [])

        def process_finition(fin_name, _model_id=model_id, _img_url=model_url):
            fin_payload = {"name": fin_name, "imageUrl": _img_url}
            fin_obj = _get_or_create(
                list_fn=lambda: api_get("/finitions", params={"modelId": _model_id}),
                create_fn=lambda: api_post("/finitions", fin_payload, params={"modelId": _model_id}),
                match_key="name", name=fin_name,
                update_fn=lambda fid: api_put(f"/finitions/{fid}", fin_payload),
            )
            return fin_name, fin_obj

        fin_id_map = {}
        with ThreadPoolExecutor(max_workers=MAX_WORKERS_UPLOAD) as ex:
            futures = {ex.submit(process_finition, fn): fn for fn in fin_names}
            for fut in as_completed(futures):
                fn, fobj = fut.result()
                if fobj:
                    fin_id_map[fn] = fobj["id"]
                    with stats_lock:
                        stats["finitions"] += 1

        # 6. Variantes tarifees
        for var in model_data.get("variants", []):
            fin_id = fin_id_map.get(var["finition"])
            mot_id = mot_id_map.get(var["motorisation"])
            if not fin_id or not mot_id:
                continue
            payload = {
                "purchasePrice":          var.get("price", 0.0),
                "monthlyLoa":             var.get("loa"),
                "monthlyLld":             var.get("lld"),
                "defaultInsuranceCost":   var.get("insurance"),
                "defaultMaintenanceCost": var.get("maintenance"),
                "estimatedResaleValue":   var.get("resale"),
            }
            try:
                var_resp = api_post("/variants", payload,
                                    params={"finitionId": fin_id, "motorisationId": mot_id})
                with stats_lock:
                    stats["variants"] += 1
                print(f"      [$] {var['finition']} x {var['motorisation']} -> {var.get('price')} EUR")
            except Exception:
                pass


def clear_catalog(target_url=None):
    if target_url:
        configure_api_endpoints(target_url)
    print(f"[*] Nettoyage du catalogue sur {API_BASE}...")
    try:
        brands = api_get("/brands")
        print(f"    {len(brands)} marques a supprimer.")
        for b in brands:
            api_delete(f"/brands/{b['id']}")
            print(f"    [-] Marque #{b['id']} '{b['name']}' supprimee.")
        print("[OK] Catalogue nettoye.\n")
    except Exception as e:
        print(f"[!] Erreur nettoyage : {e}", file=sys.stderr)


def seed_catalog(target_url=None, reset=False):
    if target_url:
        configure_api_endpoints(target_url)

    if reset:
        clear_catalog(target_url)

    print(f"[*] Seeding -> {API_BASE}")
    print(f"[*] Upload  -> {API_UPLOAD_URL}")
    print(f"[*] Threads : {MAX_WORKERS_BRAND} marques // {MAX_WORKERS_UPLOAD} finitions/uploads //\n")

    stats      = {"brands": 0, "models": 0, "motorisations": 0, "finitions": 0, "variants": 0}
    stats_lock = threading.Lock()

    with ThreadPoolExecutor(max_workers=MAX_WORKERS_BRAND) as ex:
        futures = {ex.submit(seed_brand, bd, stats_lock, stats): bd["brand"] for bd in CATALOG_DATA}
        for fut in as_completed(futures):
            brand = futures[fut]
            try:
                fut.result()
            except Exception as exc:
                print(f"[!] Erreur marque {brand}: {exc}", file=sys.stderr)

    print("\n" + "=" * 60)
    print("[OK] Seeding termine !")
    for k, v in stats.items():
        print(f"    {k.capitalize():<16}: {v}")
    print("=" * 60)


# ── Entry point ───────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="EcoSwitch Catalog Seeder v3.0",
        epilog=(
            "Exemples :\n"
            "  python3 scripts/seed_catalog.py\n"
            "  python3 scripts/seed_catalog.py prod\n"
            "  python3 scripts/seed_catalog.py prod --reset\n"
            "  python3 scripts/seed_catalog.py --url https://ecoswitch-api.up.railway.app"
        ),
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("env", nargs="?", default="local",
                        help="Environnement : 'local', 'prod' ou URL directe. Defaut : local")
    parser.add_argument("--url", "-u", help="URL directe de l'API (ecrase env)")
    parser.add_argument("--reset", "--clean", action="store_true",
                        help="Supprime tout le catalogue avant le seed")
    args = parser.parse_args()

    target_url = args.url if args.url else resolve_target_url(args.env)
    env_name   = args.env.upper() if args.env.lower() in ENV_CONFIGS else "CUSTOM"

    print("=" * 60)
    print("  EcoSwitch Catalog Seeder v3.0")
    print(f"  Environnement : {env_name}")
    print(f"  API           : {target_url}")
    print(f"  Mode          : {'RESET & SEED' if args.reset else 'SEED'}")
    print("=" * 60 + "\n")

    seed_catalog(target_url, reset=args.reset)


if __name__ == "__main__":
    main()
