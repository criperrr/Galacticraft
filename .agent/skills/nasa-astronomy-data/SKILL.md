---
name: nasa-astronomy-data
description: >-
  Guia, utilitários e integração oficial com APIs da NASA (JPL Horizons, Small-Body Database e
  Exoplanet Archive) para extração de parâmetros astrofísicos reais e geração automatizada de
  registros e Codecs de corpos celestes (CelestialBody) no Galacticraft 5 (Fabric 1.21.1).
---

# NASA & JPL Astronomy Data Integration for GalactiCraft 5

Este guia estabelece os procedimentos para consultar dados astronômicos oficiais da NASA e convertê-los diretamente em parâmetros utilizáveis pelo motor do **GalactiCraft 5 (Fabric 1.21.1)**, garantindo autenticidade científica sem desperdício de tokens de contexto.

---

## 1. Ferramenta Determinística Local (Zero-Token Compute)

Para buscar dados sem sobrecarregar a janela de contexto da IA com tabelas brutas de efemérides, utilize sempre o script local determinístico:

```bash
# Consultar dados físicos e conversões em unidades do Minecraft:
python scripts/nasa_horizons_client.py mars

# Gerar diretamente o JSON pronto para Galacticraft CelestialBody:
python scripts/nasa_horizons_client.py mars --json

# Forçar atualização direta nos servidores da NASA (bypassa cache local):
python scripts/nasa_horizons_client.py europa --refresh

# Atualizar o banco de cache completo do Sistema Solar:
python scripts/nasa_horizons_client.py --cache-all
```

---

## 2. Fórmulas de Conversão Padronizadas

O script `scripts/nasa_horizons_client.py` e o cache `docs/astronomy_cache.json` seguem as seguintes convenções:

### 2.1. Gravidade Superficial Relativa ($G$)
$$G_{\text{GC5}} = \frac{g_{\text{NASA}}\text{ (m/s}^2\text{)}}{9{,}80665}$$
* O Overworld do Minecraft possui $1{,}0\text{ G}$.
* Corpos com baixa gravidade afetam a aceleração de queda do jogador, a altura do pulo e a física de foguetes.

### 2.2. Ciclo Dia/Noite em Ticks
$$T_{\text{day\_ticks}} = \text{round}\left( 24000 \times \frac{P_{\text{horas}}}{24.0} \right)$$
* Na Terra do Minecraft, 24 horas = 24.000 ticks.
* Para corpos acoplados por maré (*tidally locked*, ex: Lua, Titã), o ciclo dia/noite equivale ao período orbital sinódico em torno do corpo pai.

### 2.3. Irradiância Solar ($S$) e Geração Fotovoltaica
$$S = \frac{1}{(d_{\text{UA}})^2}$$
* O multiplicador de saída dos painéis solares em GC5 escala com o inverso do quadrado da distância ao Sol em Unidades Astronômicas ($d_{\text{UA}}$).
* Corpos internos (Mercúrio) possuem alta geração, mas exigem isolamento térmico; corpos externos (Júpiter, Saturno) exigem grandes arranjos solares ou geradores a fusão/radioisótopos (RTG).

---

## 3. Formato Canônico de `CelestialBody` em GC5

Os registros de corpos celestes em GC5 são orientados a dados (`ResourceKey<Registry<CelestialBody<?, ?>>>`).

Localização do arquivo de dados:
`src/main/resources/data/galacticraft/celestial_body/<body_id>.json`

Exemplo padrão gerado pela ferramenta:
```json
{
  "type": "galacticraft:planet",
  "name": {
    "translate": "celestial_body.galacticraft.mars"
  },
  "description": {
    "translate": "celestial_body.galacticraft.mars.description"
  },
  "parent": "galacticraft:sol",
  "gravity": 0.3783,
  "day_length": 24623,
  "solar_energy_ratio": 0.4306,
  "atmosphere": {
    "pressure": 0.636,
    "breathable": false,
    "composition": {
      "CO2": 0.951,
      "N2": 0.026,
      "Ar": 0.019,
      "O2": 0.0016
    }
  }
}
```

---

## 4. Banco de Cache Local (`docs/astronomy_cache.json`)

Para consultas instantâneas sem acesso à rede, consulte diretamente o arquivo `docs/astronomy_cache.json`. Ele armazena os valores já validados para:
* **Planetas:** Mercúrio, Vênus, Terra, Marte, Júpiter, Saturno, Urano, Netuno.
* **Luas e Satélites:** Lua, Titã, Europa, Ganimedes, Io, Calisto.
* **Asteroides:** Ceres, Vesta.
