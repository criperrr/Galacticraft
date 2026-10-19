---
name: minecraft-worldgen-and-caves
description: >-
  Guia completo e referência técnica para geração procedural de mundo, relevo, splines,
  cavernas 3D monumentais contínuas, carvers, regras de superfície (SurfaceRules) e
  distribuição de biomas multi-noise no Minecraft 1.21+ (Fabric / Galacticraft).
---

# Engenharia de Geração Procedural de Mundo e Cavernas no Minecraft 1.21+

Este guia consolida todas as lições aprendidas, padrões arquiteturais, causas raízes de defeitos visuais e soluções comprovadas para geração procedural de terrenos planetários e sistemas de cavernas monumentais no motor de geração de densidade do Minecraft 1.21+ (utilizado no Galacticraft e Fabric Loom).

---

## 1. Arquitetura do NoiseRouter e DensityFunctions

O Minecraft 1.21+ gera terreno avaliando um grafo acíclico de `DensityFunction` para cada célula da grade (`NoiseChunk`). Os nós chave do `NoiseRouter` são:

*   **`initialDensityWithoutJaggedness`**: Densidade base do terreno sem ruído de dentes de serra (usada para decisões de cavernas e preenchimento inicial).
*   **`finalDensity`**: Função de densidade final avaliada bloco a bloco (`NoiseChunk.doFill()`). Valor $> 0$ torna o bloco sólido; $< 0$ gera ar.
*   **`continents` / `erosion` / `depth` / `ridges`**: Parâmetros passados ao amostrador climático multi-noise.

### ⚠️ Erro Crítico: Interpolação Recursiva / Paralela ("Bread-Slicer Bug")
*   **Sintoma:** O terreno aparece fatiado em dezenas de lâminas verticais paralelas alinhadas aos eixos cardeais, separadas por fendas vazias contínuas que atravessam centenas de blocos.
*   **Causa Raiz:** Envolver `finalDensity` em `DensityFunctions.interpolated(...)`. O motor do jogo já faz a interpolação trilinear das células (4x4x8). Se `finalDensity` for marcado como interpolado e internamente consultar outras funções com `interpolated(...)`, as variáveis de estado do `NoiseInterpolator` entram em curto-circuito, congelando um dos eixos (X ou Z).
*   **Regra de Ouro:**
    ```java
    // NUNCA faça:
    context.register(FINAL_DENSITY, DensityFunctions.interpolated(finalDensity)); // PROIBIDO!

    // FORMA CORRETA (Padrão Vanilla Overworld / Galacticraft Moon/Mars):
    DensityFunction finalDensity = ...;
    return new NoiseRouter(
            ...,
            finalDensity,                                      // initialDensityWithoutJaggedness
            DensityFunctions.blendDensity(finalDensity),       // finalDensity
            ...
    );
    ```

---

## 2. Relevo Contínuo vs Degraus Quadrados 4x4xY (`flat_cache`)

### ⚠️ O Problema dos Paredões 100% Retos e Cantos em 90°
*   **Sintoma:** Escarpas, falhas tectônicas e paredões verticais formam paredes perfeitamente planas alinhadas à grade dos chunks, com curvas que viram em 90 graus exatos.
*   **Causa Raiz:** O Vanilla Minecraft envolve `NoiseRouterData.CONTINENTS`, `SHIFT_X` e `SHIFT_Z` em `flat_cache(...)`. O `flat_cache` calcula o valor de ruído apenas **uma vez por coluna 4x4** horizontal. Em um paredão de 40 blocos de desnível, cada célula 4x4 sofre um salto vertical de 8 a 15 blocos, gerando degraus ortogonais.
*   **Solução em Três Etapas:**
    1.  **Coordenadas de Deslocamento Contínuas:** Em vez de usar `NoiseRouterData.SHIFT_X` / `SHIFT_Z`, utilize ruído de deslocamento contínuo sem cache:
        ```java
        DensityFunction shiftX = DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.SHIFT), 0.25, 0.0);
        DensityFunction shiftZ = DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.SHIFT), 0.0, 0.25);
        ```
    2.  **Continentalidade Contínua para a Spline de Elevação:**
        ```java
        DensityFunction planetContinents = registerAndWrap(context, CONTINENTS,
                DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, noiseRegistry.getOrThrow(Noises.CONTINENTALNESS))
        );
        var continentsCoord = new DensityFunctions.Spline.Coordinate(
                context.lookup(Registries.DENSITY_FUNCTION).getOrThrow(CONTINENTS)
        );
        ```
    3.  **Rugosidade 3D Orgânica (`BlendedNoise`):** Aumente a amplitude de ruído 3D de valores residuais (como 0.05..0.08) para `0.25..0.40`. O `BlendedNoise` decola saliências, reentrâncias e fraturas rochosas na face vertical, quebrando a geometria plana.

---

## 3. Cavernas Monumentais 3D sem Rompimento de Superfície

Para criar cavernas colossais (tubos de lava de 40-50 blocos de largura, salões de megabrecha, fendas tectônicas):

### 3.1. Blindagem Dinâmica da Crosta (Surface Ceiling Taper)
*   **Problema de Tetos Estáticos:** Fixar `yClampedGradient(114, 122, 0.0, 3.0)` assume que a superfície está em $Y=120$. Se o planeta tiver bacias de impacto profundas (ex: Bacia de Caloris em $Y=75$), cavernas com multiplicadores agressivos rasgarão o solo a céu aberto.
*   **Solução:** Calcular a densidade da crosta local:
    ```java
    DensityFunction localSurface = DensityFunctions.add(baseHeight, offset);
    DensityFunction surfaceCeilingTaper = DensityFunctions.add(
            DensityFunctions.mul(DensityFunctions.constant(-50.0), localSurface),
            DensityFunctions.constant(5.0)
    ).clamp(0.0, 3.0);

    DensityFunction boundedCaveCarve = DensityFunctions.min(
            DensityFunctions.zero(),
            DensityFunctions.add(caveCarve, surfaceCeilingTaper)
    );
    ```

### 3.2. Colunas Suaves de Sustentação (Pillars em Vãos $> 40\times 40$)
*   Para evitar que cavernas colossais fiquem excessivamente abertas e vazias, adicione pilares de no máximo $4\times 4$ blocos:
    ```java
    DensityFunction pillarA = DensityFunctions.noise(noiseRegistry.getOrThrow(CAVE_PILLARS), 1.0, 1.0).abs();
    DensityFunction pillarB = DensityFunctions.noise(noiseRegistry.getOrThrow(CAVE_PILLARS), 1.0, 1.0).abs();
    DensityFunction pillarGrid = DensityFunctions.max(pillarA, pillarB);
    DensityFunction smoothPillars = DensityFunctions.rangeChoice(pillarGrid, 0.0, 0.08, pillarThickness, DensityFunctions.zero());
    ```

### 3.3. Proteção Impenetrável da Bedrock
*   Sempre adicione uma blindagem no fundo do mundo para garantir que nenhum carver ou chasm fure o vácuo:
    ```java
    DensityFunction bedrockShield = DensityFunctions.yClampedGradient(-64, -50, 6.0, 0.0);
    // Adicionado diretamente na soma de finalDensity
    ```

---

## 4. Distribuição Bioclimática Multi-Noise (Evitando Vazamento 3D)

O `MultiNoiseBiomeSource` calcula a distância Euclidiana em 6 dimensões:
$$\text{dist}^2 = \Delta T^2 + \Delta H^2 + \Delta C^2 + \Delta E^2 + \Delta D^2 + \Delta W^2 + \text{offset}$$

### ⚠️ A Armadilha do Bioma Subterrâneo "Engolir" a Superfície
*   **Sintoma:** Um bioma subterrâneo (como `Hollow Labyrinth`) gera em mais de 50% da superfície do planeta.
*   **Causa:** Se o bioma subterrâneo estiver registrado com `FULL_RANGE` (`[-1.0, 1.0]`) para temperatura, umidade e erosão, sua distância nessas 3 variáveis é **zero**. Se um bioma de superfície tiver limites rígidos (ex: continentalidade restrita a `[0.03, 0.30]`), qualquer ponto com continentalidade fora desse intervalo terá uma distância maior do que a distância de profundidade do bioma subterrâneo.
*   **Regras:**
    1.  Os biomas de superfície devem cobrir coletivamente **100% do espaço de Continentalidade e Temperatura** na cota `depth <= 0.0`.
    2.  Biomas subterrâneos devem ter `depth` iniciando com margem de segurança clara (ex: `Parameter.span(0.35F, 1.10F)`), garantindo uma penalidade de distância quadrática $\Delta D^2 \ge (0.35)^2 = 0.1225$ para qualquer ponto na superfície.

---

## 5. Precedência Estrita em `SurfaceRules` para Encostas Íngremes

*   **Sintoma:** Paredões de penhascos e escarpas cobertos por "degraus de terra/regolito" em vez de pedra exposta.
*   **Causa:** `SurfaceRules.ON_FLOOR` detecta qualquer bloco que tenha ar acima dele — incluindo cada degrau horizontal de uma encosta inclinada. Se `ON_FLOOR` for avaliado antes de `steep()`, o material de superfície plana cobre o paredão.
*   **Regra de Ouro:**
    ```java
    private static final RuleSource SURFACE_GENERATION = SurfaceRules.sequence(
            // 1. Encosta Íngreme SEMPRE PRIMEIRO:
            SurfaceRules.ifTrue(SurfaceRules.steep(), STEEP_CLIFF_MATERIAL),
            // 2. Piso plano a céu aberto:
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, FLOOR_MATERIAL),
            // 3. Subsuperfície (1 a 4 blocos abaixo do piso):
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SECONDARY_MATERIAL)
    );
    ```
*   **Dica de Gameplay:** Em `STEEP_CLIFF_MATERIAL`, use `SurfaceRules.noiseCondition(...)` para intercalar camadas de rocha base com minérios nobres (ex: ferro nativo, titânio, grafite), valorizando a exploração e escalada vertical.

---

## 6. Crateras de Impacto Circulares e Profundas (`CraterCarver`)

Para carvers que esculpem bacias parabólicas de impacto natural com pisos temáticos:
*   **Equação da Bacia Parabólica/Quártica:**
    $$yDev = \left(\frac{dist}{radius}\right)^4 \cdot 6.0$$
    $$craterDepth = \left(baseDepth - yDev \cdot \frac{baseDepth}{6.0}\right) \cdot depthMultiplier$$
    *   No centro ($dist = 0$): profundidade máxima de escavação ($baseDepth \approx 14 \text{ a } 24$ blocos).
    *   Na borda ($dist = radius$): atenuação suave até 0 blocos, preservando o anel da borda.
*   **Vestimenta do Leito de Cratera:**
    *   Em crateras polares: aplicar gelo compactado e gelo azul **exclusivamente** na área central de sombra permanente ($dist^2 / radius^2 < 0.40$).
    *   Em bacias de impacto: expor brecha de choque e basalto no fundo, mantendo regolito fosco nas bordas externas.
