#!/usr/bin/env python3
"""
NASA JPL Horizons & Astronomy Client for Galacticraft 5 (Fabric 1.21.1)
======================================================================
Connects to official NASA / JPL APIs (Horizons, Small-Body Database, Exoplanet Archive)
and converts astrophysics parameters directly into normalized Minecraft / GC5 units:
- Day/Night cycle in ticks (24000 ticks = 24.0 hours)
- Relative surface gravity (1.000 G = 9.80665 m/s^2)
- Solar irradiance ratio (1.0 = Earth, scales with 1/distance^2)
- Atmospheric pressure & composition
- DataGen and JSON formatting for Galacticraft 5 CelestialBody registries.

Zero-token compute offloading: Run locally via CLI without consuming LLM context tokens.
"""

import sys
import os
import re
import json
import argparse
import urllib.request
import urllib.parse
from typing import Dict, Any, Optional

HORIZONS_API = "https://ssd.jpl.nasa.gov/api/horizons.api"
SBDB_API = "https://ssd-api.jpl.nasa.gov/sbdb.api"
EXOPLANET_API = "https://exoplanetarchive.ipac.caltech.edu/TAP/sync"

# Path to local persistent cache
CACHE_PATH = os.path.join(os.path.dirname(__file__), "..", "docs", "astronomy_cache.json")

# Standard Target IDs for Horizons
SOLAR_BODIES = {
    "sun": {"id": "10", "name": "Sol", "type": "star", "parent": None, "dist_au": 0.0},
    "mercury": {"id": "199", "name": "Mercury", "type": "planet", "parent": "galacticraft:sol", "dist_au": 0.387},
    "venus": {"id": "299", "name": "Venus", "type": "planet", "parent": "galacticraft:sol", "dist_au": 0.723},
    "earth": {"id": "399", "name": "Earth", "type": "planet", "parent": "galacticraft:sol", "dist_au": 1.000},
    "moon": {"id": "301", "name": "Moon", "type": "moon", "parent": "minecraft:overworld", "dist_au": 1.000},
    "mars": {"id": "499", "name": "Mars", "type": "planet", "parent": "galacticraft:sol", "dist_au": 1.524},
    "phobos": {"id": "401", "name": "Phobos", "type": "moon", "parent": "galacticraft:mars", "dist_au": 1.524},
    "deimos": {"id": "402", "name": "Deimos", "type": "moon", "parent": "galacticraft:mars", "dist_au": 1.524},
    "ceres": {"id": "Ceres", "type": "asteroid", "parent": "galacticraft:sol", "dist_au": 2.767},
    "vesta": {"id": "Vesta", "type": "asteroid", "parent": "galacticraft:sol", "dist_au": 2.362},
    "jupiter": {"id": "599", "name": "Jupiter", "type": "planet", "parent": "galacticraft:sol", "dist_au": 5.204},
    "io": {"id": "501", "name": "Io", "type": "moon", "parent": "galacticraft:jupiter", "dist_au": 5.204},
    "europa": {"id": "502", "name": "Europa", "type": "moon", "parent": "galacticraft:jupiter", "dist_au": 5.204},
    "ganymede": {"id": "503", "name": "Ganymede", "type": "moon", "parent": "galacticraft:jupiter", "dist_au": 5.204},
    "callisto": {"id": "504", "name": "Callisto", "type": "moon", "parent": "galacticraft:jupiter", "dist_au": 5.204},
    "saturn": {"id": "699", "name": "Saturn", "type": "planet", "parent": "galacticraft:sol", "dist_au": 9.582},
    "titan": {"id": "606", "name": "Titan", "type": "moon", "parent": "galacticraft:saturn", "dist_au": 9.582},
    "enceladus": {"id": "602", "name": "Enceladus", "type": "moon", "parent": "galacticraft:saturn", "dist_au": 9.582},
    "uranus": {"id": "799", "name": "Uranus", "type": "planet", "parent": "galacticraft:sol", "dist_au": 19.20},
    "titania": {"id": "703", "name": "Titania", "type": "moon", "parent": "galacticraft:uranus", "dist_au": 19.20},
    "neptune": {"id": "899", "name": "Neptune", "type": "planet", "parent": "galacticraft:sol", "dist_au": 30.05},
    "triton": {"id": "801", "name": "Triton", "type": "moon", "parent": "galacticraft:neptune", "dist_au": 30.05},
    "pluto": {"id": "999", "name": "Pluto", "type": "planet", "parent": "galacticraft:sol", "dist_au": 39.48},
    "charon": {"id": "901", "name": "Charon", "type": "moon", "parent": "galacticraft:pluto", "dist_au": 39.48}
}

KNOWN_ATMOSPHERES = {
    "earth": {"pressure_kpa": 101.325, "breathable": True, "composition": {"N2": 0.7808, "O2": 0.2095, "Ar": 0.0093, "CO2": 0.0004}},
    "moon": {"pressure_kpa": 0.0, "breathable": False, "composition": {"Vacuum": 1.0}},
    "mercury": {"pressure_kpa": 0.000000001, "breathable": False, "composition": {"Trace_Na_He": 1.0}},
    "venus": {"pressure_kpa": 9300.0, "breathable": False, "composition": {"CO2": 0.965, "N2": 0.035, "SO2": 0.00015}},
    "mars": {"pressure_kpa": 0.636, "breathable": False, "composition": {"CO2": 0.951, "N2": 0.026, "Ar": 0.019, "O2": 0.0016}},
    "titan": {"pressure_kpa": 146.7, "breathable": False, "composition": {"N2": 0.942, "CH4": 0.0565, "H2": 0.001}},
    "ceres": {"pressure_kpa": 0.0, "breathable": False, "composition": {"Vacuum": 1.0}},
    "jupiter": {"pressure_kpa": 20000.0, "breathable": False, "composition": {"H2": 0.898, "He": 0.102}},
    "saturn": {"pressure_kpa": 14000.0, "breathable": False, "composition": {"H2": 0.963, "He": 0.033}},
    "uranus": {"pressure_kpa": 10000.0, "breathable": False, "composition": {"H2": 0.825, "He": 0.152, "CH4": 0.023}},
    "neptune": {"pressure_kpa": 10000.0, "breathable": False, "composition": {"H2": 0.800, "He": 0.190, "CH4": 0.015}}
}


def load_cache() -> Dict[str, Any]:
    norm_path = os.path.normpath(CACHE_PATH)
    if os.path.exists(norm_path):
        try:
            with open(norm_path, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            pass
    return {}


def save_cache(cache: Dict[str, Any]) -> None:
    norm_path = os.path.normpath(CACHE_PATH)
    os.makedirs(os.path.dirname(norm_path), exist_ok=True)
    with open(norm_path, "w", encoding="utf-8") as f:
        json.dump(cache, f, indent=2, ensure_ascii=False)


def fetch_sbdb_asteroid(name_or_id: str) -> Optional[Dict[str, Any]]:
    """Fetch physical parameters from NASA Small-Body Database API for asteroids."""
    params = urllib.parse.urlencode({"sstr": name_or_id, "phys-par": "1"})
    url = f"{SBDB_API}?{params}"
    req = urllib.request.Request(url, headers={"User-Agent": "Galacticraft-Agent/1.0"})
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            phys = {}
            for p in data.get("phys_par", []):
                p_name = p.get("name")
                val = p.get("value")
                if p_name and val:
                    try:
                        phys[p_name] = float(val)
                    except ValueError:
                        phys[p_name] = val
            
            res = {}
            diam = phys.get("diameter")
            if diam:
                res["radius_km"] = diam / 2.0
            
            gm = phys.get("GM")
            if gm and diam:
                r_m = (diam / 2.0) * 1000.0
                gm_m = gm * 1e9
                g = gm_m / (r_m ** 2)
                res["gravity_ms2"] = round(g, 4)
                
            rot = phys.get("rot_per")
            if rot:
                res["rotation_period_hours"] = rot

            if "spec_B" in phys:
                res["spectral_type"] = phys["spec_B"]

            return res
    except Exception as e:
        sys.stderr.write(f"Warning: Failed to fetch from SBDB ({name_or_id}): {e}\n")
        return None


def fetch_horizons_text(command: str) -> Optional[str]:
    params = urllib.parse.urlencode({
        "format": "json",
        "COMMAND": f"'{command}'",
        "OBJ_DATA": "YES",
        "MAKE_EPHEM": "NO"
    })
    url = f"{HORIZONS_API}?{params}"
    req = urllib.request.Request(url, headers={"User-Agent": "Galacticraft-Agent/1.0"})
    try:
        with urllib.request.urlopen(req, timeout=12) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            return data.get("result", "")
    except Exception as e:
        sys.stderr.write(f"Warning: Failed to fetch from Horizons ({command}): {e}\n")
        return None


def parse_horizons_physics(text: str) -> Dict[str, Any]:
    physics = {}
    
    # Radius
    r_match = re.search(r"(?:Vol\.\s*mean\s*radius|Mean\s*radius|Equatorial\s*radius)\s*(?:,\s*km|\(km\))\s*=\s*([\d\.]+)", text, re.IGNORECASE)
    if not r_match:
        r_match = re.search(r"Radius\s*\(km\)\s*=\s*([\d\.]+)", text, re.IGNORECASE)
    if r_match:
        physics["radius_km"] = float(r_match.group(1))

    # Mass
    m_match = re.search(r"Mass\s*(?:x10\^(\d+)|\((\d+)\^(\d+)\s*kg\)|\(10\^(\d+)\s*kg\))\s*\(?kg\)?\s*=\s*([\d\.]+)", text, re.IGNORECASE)
    if m_match:
        val = float(m_match.group(len(m_match.groups())))
        physics["mass_kg"] = val

    # GM (km^3/s^2)
    gm_match = re.search(r"GM\s*(?:\(km\^3/s\^2\)|,\s*km\^3/s\^2)?\s*=\s*([\d\.]+)", text, re.IGNORECASE)
    if gm_match:
        physics["GM_km3_s2"] = float(gm_match.group(1))

    # Surface gravity
    g_match = re.search(r"(?:Equ\.\s*grav(?:ity)?(?:,\s*ge)?|Surface\s*gravity|Mean\s*surface\s*gravity|Surface\s*accel\.)\s*(?:\(m/s\^2\)|m/s\^2|,?\s*m/s\^2)?\s*=\s*([\d\.]+)", text, re.IGNORECASE)
    if not g_match:
        g_match = re.search(r"Polar\s*gravity\s*m/s\^2\s*=\s*([\d\.]+)", text)
    if not g_match:
        g_match = re.search(r"Surface\s*gravity\s*\(m/s\^2\)\s*=\s*([\d\.]+)", text)
    if g_match:
        physics["gravity_ms2"] = float(g_match.group(1))
    elif "GM_km3_s2" in physics and "radius_km" in physics and physics["radius_km"] > 0:
        g_calc = (physics["GM_km3_s2"] * 1000.0) / (physics["radius_km"] ** 2)
        physics["gravity_ms2"] = round(g_calc, 4)

    # Rotation period (Sidereal / Solar day / Orbital period for synchronous satellites)
    rot_hms = re.search(r"Sid\.\s*rot\.\s*period[^\=]*=\s*(\d+)h\s*(\d+)m\s*([\d\.]+)\s*s", text, re.IGNORECASE)
    if rot_hms:
        h = float(rot_hms.group(1))
        m = float(rot_hms.group(2))
        s = float(rot_hms.group(3))
        physics["rotation_period_hours"] = h + (m / 60.0) + (s / 3600.0)
    else:
        rot_match = re.search(r"(?:Sidereal\s*rot\.?\s*period|Rot\.\s*Period|Sidereal\s*period)\s*=\s*~?\s*([\d\.]+)\s*(hr|h|d|m|s)?", text, re.IGNORECASE)
        if rot_match:
            val = float(rot_match.group(1))
            unit = (rot_match.group(2) or "h").lower()
            if unit in ("d", "day", "days"):
                hours = val * 24.0
            elif unit in ("m", "min"):
                hours = val / 60.0
            elif unit in ("s", "sec"):
                hours = val / 3600.0
            else:
                hours = val
            physics["rotation_period_hours"] = hours
        else:
            orb_match = re.search(r"Orbital\s*period\s*=\s*([\d\.]+)\s*(d|day|days|h|hr)", text, re.IGNORECASE)
            if orb_match:
                val = float(orb_match.group(1))
                unit = orb_match.group(2).lower()
                physics["rotation_period_hours"] = val * 24.0 if "d" in unit else val
            else:
                solar_day = re.search(r"Mean\s*solar\s*day[^\=]*=\s*~?\s*([\d\.]+)\s*(d|hrs?|h|s)?", text, re.IGNORECASE)
                if solar_day:
                    val = float(solar_day.group(1))
                    unit = (solar_day.group(2) or "d").lower()
                    if "h" in unit:
                        physics["rotation_period_hours"] = val
                    elif "s" in unit:
                        physics["rotation_period_hours"] = val / 3600.0
                    else:
                        physics["rotation_period_hours"] = val * 24.0

    # Mean temperature
    t_match = re.search(r"Mean\s*temperature\s*\(K\)\s*=\s*~?\s*([\d\.]+)", text, re.IGNORECASE)
    if t_match:
        physics["temperature_kelvin"] = float(t_match.group(1))

    # Escape velocity
    esc_match = re.search(r"Escape\s*speed,\s*km/s\s*=\s*([\d\.]+)", text, re.IGNORECASE)
    if esc_match:
        physics["escape_speed_km_s"] = float(esc_match.group(1))

    return physics


def calculate_minecraft_units(name_key: str, physics: Dict[str, Any], dist_au: float = 1.0) -> Dict[str, Any]:
    EARTH_G = 9.80665
    
    # 1. Surface gravity relative to Overworld (1.000 = Earth)
    g_ms2 = physics.get("gravity_ms2")
    if g_ms2 is not None:
        rel_gravity = round(g_ms2 / EARTH_G, 4)
    else:
        # Fallback approximate gravity based on known body
        rel_gravity = 1.0

    # 2. Day/Night cycle in ticks (Earth = 24h = 24000 ticks)
    rot_hours = physics.get("rotation_period_hours")
    if rot_hours is not None:
        if rot_hours <= 0:
            mc_day_ticks = 24000
        else:
            mc_day_ticks = int(round(24000 * (abs(rot_hours) / 24.0)))
    else:
        mc_day_ticks = 24000

    # 3. Solar irradiance (1 / distance_au^2)
    if dist_au > 0:
        solar_energy_ratio = round(1.0 / (dist_au ** 2), 4)
    else:
        solar_energy_ratio = 1.0

    # 4. Atmosphere
    atm = KNOWN_ATMOSPHERES.get(name_key.lower(), {
        "pressure_kpa": 0.0,
        "breathable": False,
        "composition": {"Vacuum": 1.0}
    })

    return {
        "gravity_relative": rel_gravity,
        "gravity_ms2": g_ms2,
        "day_length_ticks": mc_day_ticks,
        "rotation_period_hours": rot_hours,
        "distance_au": dist_au,
        "solar_energy_multiplier": solar_energy_ratio,
        "atmosphere_pressure_kpa": atm["pressure_kpa"],
        "atmosphere_breathable": atm["breathable"],
        "atmosphere_composition": atm["composition"]
    }


def query_body(name: str, force_refresh: bool = False) -> Dict[str, Any]:
    key = name.lower()
    cache = load_cache()
    
    if not force_refresh and key in cache:
        return cache[key]

    body_meta = SOLAR_BODIES.get(key)
    if not body_meta:
        # Search via Horizons Lookup
        lookup_url = f"https://ssd.jpl.nasa.gov/api/horizons_lookup.api?sstr={urllib.parse.quote(name)}"
        req = urllib.request.Request(lookup_url, headers={"User-Agent": "Galacticraft-Agent/1.0"})
        target_id = name
        try:
            with urllib.request.urlopen(req, timeout=10) as r:
                l_data = json.loads(r.read().decode("utf-8"))
                res = l_data.get("result", [])
                if res:
                    target_id = res[0].get("spkid", name)
        except Exception:
            pass
        body_meta = {"id": target_id, "name": name.capitalize(), "type": "planet", "parent": "galacticraft:sol", "dist_au": 1.0}

    physics = {}
    if body_meta.get("type") == "asteroid" or key in ("ceres", "vesta", "pallas", "hygiea"):
        sbdb_phys = fetch_sbdb_asteroid(body_meta.get("name", name))
        if sbdb_phys:
            physics.update(sbdb_phys)

    if not physics.get("gravity_ms2"):
        text = fetch_horizons_text(body_meta["id"])
        if text:
            h_phys = parse_horizons_physics(text)
            h_phys.update(physics)
            physics = h_phys

    mc_data = calculate_minecraft_units(key, physics, body_meta.get("dist_au", 1.0))

    full_record = {
        "id": key,
        "name": body_meta.get("name", name.capitalize()),
        "parent": body_meta.get("parent"),
        "type": body_meta.get("type", "planet"),
        "astronomy": physics,
        "minecraft_gc5": mc_data
    }

    cache[key] = full_record
    save_cache(cache)
    return full_record


def generate_gc5_json(record: Dict[str, Any]) -> str:
    """Outputs modern GC5 CelestialBody json format"""
    mc = record["minecraft_gc5"]
    body_id = record["id"]
    name = record["name"]
    parent = record.get("parent") or "galacticraft:sol"
    
    data = {
        "type": f"galacticraft:{record['type']}",
        "name": {
            "translate": f"celestial_body.galacticraft.{body_id}"
        },
        "description": {
            "translate": f"celestial_body.galacticraft.{body_id}.description"
        },
        "parent": parent,
        "gravity": mc["gravity_relative"],
        "day_length": mc["day_length_ticks"],
        "solar_energy_ratio": mc["solar_energy_multiplier"],
        "atmosphere": {
            "pressure": mc["atmosphere_pressure_kpa"],
            "breathable": mc["atmosphere_breathable"],
            "composition": mc["atmosphere_composition"]
        }
    }
    return json.dumps(data, indent=2, ensure_ascii=False)


def cache_all() -> None:
    targets = ["mercury", "venus", "earth", "moon", "mars", "jupiter", "saturn", "uranus", "neptune", "ceres", "titan"]
    print(f"Pre-caching {len(targets)} primary celestial bodies from NASA JPL Horizons...")
    for key in targets:
        try:
            rec = query_body(key, force_refresh=True)
            mc = rec["minecraft_gc5"]
            print(f" [OK] {rec['name']:<10} -> Gravity: {mc['gravity_relative']:<6} G | Day: {mc['day_length_ticks']:<8} ticks | Solar: {mc['solar_energy_multiplier']}x")
        except Exception as e:
            print(f" [FAIL] {key}: {e}")
    print("\nCaching complete! Saved to docs/astronomy_cache.json")


def main():
    parser = argparse.ArgumentParser(description="NASA Horizons & Astronomy Client for GalactiCraft 5")
    parser.add_argument("body", nargs="?", default="", help="Name of celestial body (e.g., mars, moon, mercury, venus, ceres)")
    parser.add_argument("--json", action="store_true", help="Output GC5 CelestialBody JSON")
    parser.add_argument("--refresh", action="store_true", help="Bypass local cache and query NASA live")
    parser.add_argument("--cache-all", action="store_true", help="Fetch and cache all major solar system bodies")
    args = parser.parse_args()

    if args.cache_all:
        cache_all()
        return

    if not args.body:
        parser.print_help()
        return

    record = query_body(args.body, force_refresh=args.refresh)

    if args.json:
        print(generate_gc5_json(record))
    else:
        mc = record["minecraft_gc5"]
        astro = record["astronomy"]
        print("=" * 65)
        print(f" ASTROPHYSICS & GALACTICRAFT 5 DATA: {record['name'].upper()}")
        print("=" * 65)
        print(f"Parent Body:             {record['parent']}")
        print(f"Distance to Sol:         {mc['distance_au']} AU")
        print(f"Real Radius (km):        {astro.get('radius_km', 'N/A')}")
        print(f"Real Gravity (m/s^2):    {mc['gravity_ms2']} m/s²")
        print(f"GC5 Relative Gravity:    {mc['gravity_relative']} G (Overworld = 1.0G)")
        print(f"Rotation Period (Hours): {mc['rotation_period_hours']} h")
        print(f"Minecraft Day/Night:     {mc['day_length_ticks']} ticks")
        print(f"Solar Energy Multiplier: {mc['solar_energy_multiplier']}x")
        print(f"Atmospheric Pressure:    {mc['atmosphere_pressure_kpa']} kPa")
        print(f"Breathable Atmosphere:   {'YES' if mc['atmosphere_breathable'] else 'NO (Requires Oxygen Mask)'}")
        print(f"Gas Composition:         {mc['atmosphere_composition']}")
        print("=" * 65)


if __name__ == "__main__":
    main()
