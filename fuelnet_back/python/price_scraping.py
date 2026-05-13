import io
import json
import re

import pdfplumber
import requests
from bs4 import BeautifulSoup

BASE_DOMAIN = "https://gestornormativo.creg.gov.co"
BASE_PATH = "/gestor/entorno/"  # ruta base donde viven los hrefs relativos

NOVELTIES_URL = (
    "https://gestornormativo.creg.gov.co/gestor/entorno/"
    "novedades_regulacion_servicio_combustibles_liquidos.html"
)

HEADERS = {"User-Agent": "Mozilla/5.0"}

# Keywords de precios en minúsculas para comparación
PRICE_KEYWORDS = [
    "publicación de precios",
    "precios de referencia",
    "venta al público",
    "venta al publico",  # FIX: sin tilde también
]


def get_response(url):
    response = requests.get(url, headers=HEADERS, timeout=30)
    response.raise_for_status()
    return response


def build_url(href):
    # Ya es URL completa
    if href.startswith("http"):
        return href
    # Absoluto desde raíz del dominio
    if href.startswith("/"):
        return BASE_DOMAIN + href
    # Relativo: "docs/originales/..." → necesita BASE_PATH como base
    return BASE_DOMAIN + BASE_PATH + href


def find_price_circulars():
    response = get_response(NOVELTIES_URL)
    soup = BeautifulSoup(response.text, "html.parser")

    circulars = []

    for a in soup.find_all("a"):
        text = a.get_text(" ", strip=True)
        href = a.get("href")

        if not text or not href:
            continue

        # FIX: usar casefold() para comparación robusta de mayúsculas/minúsculas/tildes
        text_cf = text.casefold()

        # Solo circulares
        if "circular" not in text_cf:
            continue

        # Debe contener keywords de precios
        if not any(k in text_cf for k in PRICE_KEYWORDS):
            continue

        # FIX: ya no filtramos por "/docs/originales/" para no excluir circulares
        # con rutas distintas (ej: años anteriores usan otras rutas)

        full_url = build_url(href)
        circulars.append({"title": text, "url": full_url})

    return circulars


def fix_encoding(text):
    """Corrige texto UTF-8 mal decodificado como latin-1 (mojibake)."""
    try:
        return text.encode("latin-1").decode("utf-8")
    except (UnicodeEncodeError, UnicodeDecodeError):
        return text


def extract_circular_number(title: str) -> str:
    """Extrae 'Circular 277 de 2026' del título completo."""
    match = re.search(r"Circular\s+[\w]+\s+de\s+\d{4}", title, re.IGNORECASE)
    return match.group(0).strip() if match else title


def col_to_int(value: str) -> int:
    """Convierte '16.291' o '16,291' a 16291 (entero, precio en pesos por galón)."""
    clean = (
        value.strip()
        .replace(".", "")
        .replace(",", "")
        .replace("$", "")
        .replace("\xa0", "")
    )
    return int(clean)


def extract_city_table(soup: BeautifulSoup) -> dict:
    """
    Busca la fila de Bogotá en la tabla de precios y devuelve:
    {"corriente": 16291, "diesel": 11576}
    """
    for table in soup.find_all("table"):
        for row in table.find_all("tr"):
            cells = [td.get_text(" ", strip=True) for td in row.find_all(["td", "th"])]
            if len(cells) < 3:
                continue
            city_raw = fix_encoding(cells[0]).strip().lower()
            if "bogot" in city_raw:
                try:
                    return {
                        "corriente": col_to_int(fix_encoding(cells[1])),
                        "diesel": col_to_int(fix_encoding(cells[2])),
                    }
                except ValueError:
                    pass
    return {}


def resolve_valid_url(base_url):
    # FIX: agregar .htm a la lista de extensiones a probar
    # y también la variante con trailing slash (el servidor la usa como directorio)
    possible_urls = [
        base_url + "/",  # FIX: el servidor sirve el doc como directorio
        base_url,
        base_url + ".pdf",
        base_url + ".htm",
        base_url + ".html",
    ]

    for url in possible_urls:
        try:
            response = requests.get(url, headers=HEADERS, timeout=20)
            if response.status_code == 200:
                return url, response
        except Exception:
            pass

    return None, None


def extract_text_from_pdf(content):
    pdf_file = io.BytesIO(content)
    full_text = ""

    with pdfplumber.open(pdf_file) as pdf:
        for page in pdf.pages:
            extracted = page.extract_text()
            if extracted:
                full_text += extracted + "\n"

    return full_text


def process_circular(url):
    working_url, response = resolve_valid_url(url)

    if not working_url:
        raise Exception(f"No se encontró URL válida:\n{url}")

    content_type = response.headers.get("Content-Type", "").lower()

    if "html" in content_type:
        soup = BeautifulSoup(response.content, "html.parser")
        return extract_city_table(soup)

    elif "pdf" in content_type:
        text = extract_text_from_pdf(response.content)
        for line in text.splitlines():
            parts = re.split(r"\t+|\s{2,}", line.strip())
            if len(parts) >= 3 and "bogot" in parts[0].lower():
                try:
                    return {
                        "corriente": col_to_int(parts[1]),
                        "diesel": col_to_int(parts[2]),
                    }
                except ValueError:
                    pass
        return {}

    else:
        return {}


def build_json(circular: dict, data: dict) -> dict:
    return {
        "circular": extract_circular_number(circular["title"]),
        "url": circular["url"],
        "precios": data,
    }


def main():
    circulars = find_price_circulars()

    if not circulars:
        print(
            json.dumps(
                {"error": "No se encontraron circulares de precios."},
                ensure_ascii=False,
            )
        )
        return

    latest = circulars[0]
    raw_prices = process_circular(latest["url"])
    result = build_json(latest, raw_prices)

    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
