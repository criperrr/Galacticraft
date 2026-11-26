/*
 * Copyright (c) 2019-2026 Team Galacticraft
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.galacticraft.mod.data;

import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.util.Translations;
import dev.galacticraft.mod.world.biome.GCBiomes;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

import java.util.concurrent.CompletableFuture;

/**
 * Brazilian Portuguese ({@code pt_br}) translations for Phase 4 outer moons,
 * blocks, biomes, and Aerospace Manual entries (all 11 moons, Asteroid Taxonomy,
 * and Gas Giant Aerostatic Engineering).
 */
public class GCPtBrTranslationProvider extends FabricLanguageProvider {
    public GCPtBrTranslationProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, "pt_br", registriesFuture);
    }

    @Override
    public void generateTranslations(HolderLookup.Provider registries, TranslationBuilder builder) {
        // Outer Moon Blocks
        builder.add(GCBlocks.TITAN_THOLIN_SAND, "Areia de Tolina de Titã");
        builder.add(GCBlocks.TITAN_HYDROCARBON_CRUST, "Crosta de Hidrocarbonetos de Titã");
        builder.add(GCBlocks.TITAN_WATER_ICE_ROCK, "Rocha de Gelo de Água de Titã");
        builder.add(GCBlocks.TITAN_MAGNESIUM_ORE, "Minério de Magnésio de Titã");
        builder.add(GCBlocks.TITAN_IRON_ORE, "Minério de Ferro de Titã");
        builder.add(GCBlocks.TITAN_SILICON_ORE, "Minério de Silício de Titã");

        builder.add(GCBlocks.ENCELADUS_ICE_CRUST, "Crosta de Gelo de Encélado");
        builder.add(GCBlocks.ENCELADUS_TIGER_STRIPE_ICE, "Gelo das Listras de Tigre de Encélado");
        builder.add(GCBlocks.ENCELADUS_SEABED_ROCK, "Rocha de Fundo Oceânico de Encélado");
        builder.add(GCBlocks.ENCELADUS_MAGNESIUM_ORE, "Minério de Magnésio de Encélado");
        builder.add(GCBlocks.ENCELADUS_NIOBIUM_ORE, "Minério de Nióbio de Encélado");

        builder.add(GCBlocks.TITANIA_ICE_SILICATE, "Rocha de Gelo-Silicato de Titânia");
        builder.add(GCBlocks.TITANIA_FROST_CRUST, "Crosta Gélida de Titânia");
        builder.add(GCBlocks.TITANIA_CHASM_SLATE, "Ardósia dos Cânions de Titânia");
        builder.add(GCBlocks.TITANIA_URANIUM_ORE, "Minério de Urânio de Titânia");
        builder.add(GCBlocks.TITANIA_LONSDALEITE_ORE, "Minério de Lonsdaleíta de Titânia");
        builder.add(GCBlocks.TITANIA_TITANIUM_ORE, "Minério de Titânio de Titânia");

        builder.add(GCBlocks.OBERON_DARK_ROCK, "Rocha Escura de Oberon");
        builder.add(GCBlocks.OBERON_REDDISH_REGOLITH, "Regolito Avermelhado de Oberon");
        builder.add(GCBlocks.OBERON_IMPACT_BRECCIA, "Brecha de Impacto de Oberon");
        builder.add(GCBlocks.OBERON_URANIUM_ORE, "Minério de Urânio de Oberon");
        builder.add(GCBlocks.OBERON_NIOBIUM_ORE, "Minério de Nióbio de Oberon");

        builder.add(GCBlocks.TRITON_NITROGEN_ICE, "Gelo de Nitrogênio de Tritão");
        builder.add(GCBlocks.TRITON_CANTALOUPE_CRUST, "Crosta Cantaloupe de Tritão");
        builder.add(GCBlocks.TRITON_CRYO_BASALT, "Criobasalto de Tritão");
        builder.add(GCBlocks.TRITON_SUPERIONIC_ORE, "Minério de Gelo Superiônico de Tritão");
        builder.add(GCBlocks.TRITON_XENON_CLATHRATE_ORE, "Minério de Clatrato de Xenônio de Tritão");

        // Celestial Bodies
        builder.add(Translations.CelestialBody.TITAN, "Titã");
        builder.add(Translations.CelestialBody.TITAN_DESC, "A maior lua de Saturno e o único satélite natural do Sistema Solar com atmosfera densa de 1,45 bar (94,2% N2 e 5,65% CH4). Sob névoa laranja de tolinas a -179C, lagos de metano líquido esculpem dunas orgânicas e rocha de gelo.");
        builder.add(Translations.CelestialBody.ENCELADUS, "Encélado");
        builder.add(Translations.CelestialBody.ENCELADUS_DESC, "Lua gelada de alto albedo em Saturno, marcada pelas fissuras criovulcânicas Tiger Stripes que expelem plumas de um oceano hidrotermal subterrâneo.");
        builder.add(Translations.CelestialBody.TITANIA, "Titânia");
        builder.add(Translations.CelestialBody.TITANIA_DESC, "A maior lua de Urano, cortada pelos colossais cânions tectônicos Messina Chasmata ricos em Urânio e Lonsdaleíta hexagonal.");
        builder.add(Translations.CelestialBody.OBERON, "Oberon");
        builder.add(Translations.CelestialBody.OBERON_DESC, "A lua principal mais externa de Urano, com crateras escuras avermelhadas, picos de brecha de impacto e depósitos de Urânio e Nióbio.");
        builder.add(Translations.CelestialBody.TRITON, "Tritão");
        builder.add(Translations.CelestialBody.TRITON_DESC, "Lua retrógrada de Netuno capturada do Cinturão de Kuiper a -235C, com terreno cantaloupe, gêiseres de nitrogênio, Gelo Superiônico e Clatratos de Xenônio.");

        // Biomes
        addBiome(builder, GCBiomes.Titan.METHANE_SEAS, "Mares de Metano Kraken");
        addBiome(builder, GCBiomes.Titan.THOLIN_DUNES, "Dunas Orgânicas de Tolina");
        addBiome(builder, GCBiomes.Titan.ICE_HIGHLANDS, "Terras Altas de Gelo Xanadu");
        addBiome(builder, GCBiomes.Titan.CRYO_CAVERNS, "Criocavernas de Titã");

        addBiome(builder, GCBiomes.Enceladus.SNOW_PLAINS, "Planícies de Neve de Alto Albedo");
        addBiome(builder, GCBiomes.Enceladus.TIGER_STRIPES, "Fendas Criovulcânicas Tiger Stripes");
        addBiome(builder, GCBiomes.Enceladus.CRATERED_RIDGES, "Cristas Geladas com Crateras");
        addBiome(builder, GCBiomes.Enceladus.HYDROTHERMAL_OCEAN, "Oceano Hidrotermal de Encélado");

        addBiome(builder, GCBiomes.Titania.MESSINA_CHASMATA, "Cânions Messina Chasmata");
        addBiome(builder, GCBiomes.Titania.FROST_PLAINS, "Planícies Gélidas de Titânia");
        addBiome(builder, GCBiomes.Titania.SCARP_HIGHLANDS, "Escaras Tectônicas");
        addBiome(builder, GCBiomes.Titania.FAULT_CAVERNS, "Cavernas de Falha Tectônica");

        addBiome(builder, GCBiomes.Oberon.RED_PLAINS, "Planícies Avermelhadas");
        addBiome(builder, GCBiomes.Oberon.IMPACT_PEAKS, "Picos de Impacto de Oberon");
        addBiome(builder, GCBiomes.Oberon.DARK_BASINS, "Bacias Escuras de Carbono");
        addBiome(builder, GCBiomes.Oberon.ANCIENT_CAVERNS, "Cavernas Antigas de Impacto");

        addBiome(builder, GCBiomes.Triton.CANTALOUPE_TERRAIN, "Terreno Cantaloupe");
        addBiome(builder, GCBiomes.Triton.NITROGEN_GEYSERS, "Campos de Gêiseres de Nitrogênio");
        addBiome(builder, GCBiomes.Triton.POLAR_ICE_CAP, "Calota Polar de Nitrogênio");
        addBiome(builder, GCBiomes.Triton.SUPERIONIC_CAVERNS, "Criocavernas Superiônicas");

        // Manual Atlas & Field Guides (All 11 Moons + Asteroid Taxonomy + Gas Giant Aerostatics)
        builder.add("manual.galacticraft.atlas.phobos.title", "Fobos (Lua de Marte)");
        builder.add("manual.galacticraft.atlas.phobos.hazards", "Microgravidade (0,015G), vácuo, Bacia Stickney e sulcos de maré. Ferro Meteórico e Grafite.");
        builder.add("manual.galacticraft.atlas.deimos.title", "Deimos (Lua de Marte)");
        builder.add("manual.galacticraft.atlas.deimos.hazards", "Gravidade ultrabaixa (0,012G), encostas suaves de regolito, vácuo. Silício e Alumínio.");
        builder.add("manual.galacticraft.atlas.io.title", "Io (Lua de Júpiter)");
        builder.add("manual.galacticraft.atlas.io.hazards", "Radiação joviana intensa e vulcanismo de enxofre (0,183G). Principal fonte lunar de Paládio, Enxofre e Titânio.");
        builder.add("manual.galacticraft.atlas.europa.title", "Europa (Lua de Júpiter)");
        builder.add("manual.galacticraft.atlas.europa.hazards", "Crosta de gelo (-220C) sobre oceano subterrâneo global (Y<24). Gelo Denso, Deutério e Nióbio.");
        builder.add("manual.galacticraft.atlas.ganymede.title", "Ganimedes (Lua de Júpiter)");
        builder.add("manual.galacticraft.atlas.ganymede.hazards", "Auroras magnetosféricas e sulcos tectônicos (0,146G). Nióbio, Titânio e Paládio.");
        builder.add("manual.galacticraft.atlas.callisto.title", "Calisto (Lua de Júpiter)");
        builder.add("manual.galacticraft.atlas.callisto.hazards", "Menor cinturão de radiação em Júpiter e anéis de Valhalla. Base segura com Ferro, Alumínio, Nióbio e Gelo.");
        builder.add("manual.galacticraft.atlas.titan.title", "Titã (Lua de Saturno)");
        builder.add("manual.galacticraft.atlas.titan.hazards", "Atmosfera laranja densa de 1,45 bar (-180C), mares de metano líquido e dunas de tolina. Coleta direta de CH4/N2 no Fracionador e Magnésio.");
        builder.add("manual.galacticraft.atlas.enceladus.title", "Encélado (Lua de Saturno)");
        builder.add("manual.galacticraft.atlas.enceladus.hazards", "0,012G, neve brilhante, fissuras Tiger Stripes sobre oceano hidrotermal (Y<20). Gelo Denso, Magnésio e Nióbio.");
        builder.add("manual.galacticraft.atlas.titania.title", "Titânia (Lua de Urano)");
        builder.add("manual.galacticraft.atlas.titania.hazards", "Cânions tectônicos Messina Chasmata (-213C), 0,039G. Principal fonte lunar de Urânio e Lonsdaleíta Hexagonal.");
        builder.add("manual.galacticraft.atlas.oberon.title", "Oberon (Lua de Urano)");
        builder.add("manual.galacticraft.atlas.oberon.hazards", "Planícies escuras avermelhadas e picos de impacto (-203C), 0,035G. Rico em Urânio e Nióbio.");
        builder.add("manual.galacticraft.atlas.triton.title", "Tritão (Lua de Netuno)");
        builder.add("manual.galacticraft.atlas.triton.hazards", "Mundo retrógrado a -238C, terreno cantaloupe e gêiseres de nitrogênio. Gelo Superiônico e Clatratos de Xenônio.");

        builder.add("manual.galacticraft.entry.asteroid_taxonomy.title", "Taxonomia Espectral do Cinturão de Asteroides");
        builder.add("manual.galacticraft.entry.asteroid_taxonomy.h1", "Tipos C, S, M e Lacuna de Kirkwood");
        builder.add("manual.galacticraft.entry.asteroid_taxonomy.body1", "O Cinturão de Asteroides divide-se em quatro biomas espectrais: Tipo C Carbonáceo (rocha escura, grafite, gelo denso), Tipo S Silicato (ferro, alumínio, silício, cobre), Tipo M Metálico (ferro meteórico, ilmenita, nióbio, ouro) e a Lacuna de Ressonância de Kirkwood com micro-detritos e estações abandonadas.");
        builder.add("manual.galacticraft.entry.asteroid_taxonomy.tip1", "Procure asteroides rubble-pile, binários de contato bilobados e fragmentos cometários com bolsões de gelo.");

        builder.add("manual.galacticraft.entry.aerostatic_giants.title", "Engenharia Aerostática nos Gigantes Gasosos");
        builder.add("manual.galacticraft.entry.aerostatic_giants.h1", "Plataformas Flutuantes (Y=180-260) e Núcleos Abissais");
        builder.add("manual.galacticraft.entry.aerostatic_giants.body1", "Júpiter, Saturno, Urano e Netuno não possuem superfície rochosa. Os foguetes montam uma Plataforma Aerostática 7x7 em Y=216 na alta atmosfera segura (0,5-2,5 bar). Descer abaixo de Y=175 eleva exponencialmente a pressão hiperbárica e os ventos zonais; o Fracionador Atmosférico ganha até 2,5x de rendimento em profundidades pressurizadas.");
        builder.add("manual.galacticraft.entry.aerostatic_giants.warn1", "Explorar o Núcleo Abissal esparso (Y=-64..-20) sob 80-200+ bar exige Controlador de Escudo + Revestimento Isotérmico T2 — ou minere os minérios equivalentes nas luas!");
    }

    private static void addBiome(TranslationBuilder builder, ResourceKey<Biome> biome, String name) {
        builder.add("biome." + biome.location().getNamespace() + "." + biome.location().getPath(), name);
    }
}
