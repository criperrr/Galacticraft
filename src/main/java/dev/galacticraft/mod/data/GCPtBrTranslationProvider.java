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

        builder.add("block.galacticraft.vehicle_part_fabricator", "Fabricador Universal de Veículos");
        builder.add("block.galacticraft.chassis_bench", "Bancada de Montagem de Chassis");
        builder.add("block.galacticraft.electric_motor_bench", "Bancada de Propulsão Elétrica");
        builder.add("block.galacticraft.wheel_locomotion_bench", "Bancada de Rodas e Suspensão");
        builder.add("block.galacticraft.vehicle_accessories_bench", "Bancada de Aviônicos e Acessórios");
        builder.add("gui.galacticraft.vehicle_bench.category.chassis", "CHASSIS");
        builder.add("gui.galacticraft.vehicle_bench.category.powerplant", "ELÉTRICO");
        builder.add("gui.galacticraft.vehicle_bench.category.locomotion", "RODAS");
        builder.add("gui.galacticraft.vehicle_bench.category.accessories", "ACESSÓRIOS");
        builder.add("gui.galacticraft.vehicle_bench.category.universal", "TODOS");
        builder.add("message.galacticraft.vehicle.aim_at_part", "Mire diretamente na peça instalada para removê-la");
        builder.add("message.galacticraft.vehicle.missing_gauge_item", "Você precisa ter o mostrador no inventário: %s");
        builder.add("key.galacticraft.vehicle_panel", "Painel do Cockpit do Veículo");
        builder.add("item.galacticraft.vehicle_crate", "Caixa de Transporte de Veículo");
        builder.add("item.galacticraft.vehicle_wrench", "Chave de Montagem de Veículos");
        builder.add("item.galacticraft.drone_remote_controller", "Controle Remoto de Drone");
        builder.add("item.galacticraft.scout_rover_chassis", "Chassi Apollo LRV / LTV (Pequeno)");
        builder.add("item.galacticraft.utility_buggy_chassis", "Chassi NASA SEV Pressurizado (Médio)");
        builder.add("item.galacticraft.heavy_hauler_chassis", "Chassi NASA Mars 6x6 Habitat (Grande)");
        builder.add("item.galacticraft.micro_drone_chassis", "Chassi Micro-Drone Quadricóptero Ingenuity (Pequeno)");
        builder.add("item.galacticraft.light_helicopter_chassis", "Chassi Helicóptero Leve Pressurizado NASA (Médio)");
        builder.add("item.galacticraft.heavy_vtol_chassis", "Chassi VTOL Pesado de Carga Dragonfly (Grande)");
        builder.add("item.galacticraft.light_airplane_chassis", "Chassi Avião Elétrico Bush Plane NASA (Médio)");
        builder.add("item.galacticraft.cargo_airplane_chassis", "Chassi Avião Elétrico de Carga Planetária NASA (Grande)");
        builder.add("item.galacticraft.standard_wheel_set", "Roda Lunar de Malha");
        builder.add("item.galacticraft.heavy_tracks", "Módulo de Esteira Planetária");
        builder.add("item.galacticraft.rocker_bogie_6x6", "Roda Pesada Mars Rocker-Bogie");
        builder.add("item.galacticraft.coaxial_mars_rotor", "Rotor Coaxial de Alta RPM (Marte)");
        builder.add("item.galacticraft.heavy_lift_rotor", "Rotor Pesado Atmosférico");
        builder.add("item.galacticraft.vtol_thruster", "Propulsor VTOL Elétrico de Vácuo");
        builder.add("item.galacticraft.standard_airplane_wing", "Asa Aerodinâmica Composta");
        builder.add("item.galacticraft.high_aspect_mars_wing", "Asa Ultraleve de Alto Alongamento (Marte)");
        builder.add("item.galacticraft.heavy_cargo_wing", "Asa Reforçada de Carga Pesada");
        builder.add("item.galacticraft.electric_propeller", "Hélice Elétrica Brushless");
        builder.add("item.galacticraft.high_speed_ducted_fan", "Propulsor Elétrico Ducted Fan");
        builder.add("item.galacticraft.retractable_landing_gear", "Trem de Pouso Retrátil Oleopneumático");
        builder.add("item.galacticraft.battery_bank", "Banco de Baterias Veicular");
        builder.add("item.galacticraft.solar_array_module", "Módulo de Painel Solar");
        builder.add("item.galacticraft.rtg_core_module", "Núcleo Nuclear RTG");
        builder.add("item.galacticraft.lox_fuel_engine", "Motor Elétrico de Tração de Alta Potência");
        builder.add("gui.galacticraft.vehicle_panel.title", "Painel de Controle e Instrumentos");
        builder.add("item.galacticraft.pilot_cockpit_seat", "Assento de Piloto");
        builder.add("item.galacticraft.passenger_seat", "Assento de Passageiro");
        builder.add("item.galacticraft.crew_bench_seat", "Banco de Tripulação NPC");
        builder.add("item.galacticraft.cargo_rack_bay", "Compartimento de Carga");
        builder.add("item.galacticraft.heavy_cargo_container", "Contêiner de Carga Pesada");
        builder.add("item.galacticraft.camera_mast_360", "Mastro de Câmera 360°");
        builder.add("item.galacticraft.geological_drill_arm", "Braço Perfurador Geológico");
        builder.add("item.galacticraft.ai_core_module", "Módulo Núcleo de IA");
        builder.add("item.galacticraft.gauge_speedometer", "Tela de Velocímetro");
        builder.add("item.galacticraft.gauge_battery_fuel", "Tela de Energia e Combustível");
        builder.add("item.galacticraft.gauge_atmosphere_o2", "Tela Atmosférica e O2");
        builder.add("item.galacticraft.gauge_compass_nav", "Tela de Bússola e GPS");
        builder.add("item.galacticraft.gauge_horizon_radar", "Tela de Horizonte e Direção");
        builder.add("vehicle.galacticraft.chassis.scout_rover", "Apollo LRV / LTV (Aberto)");
        builder.add("vehicle.galacticraft.chassis.utility_buggy", "NASA SEV Lunar Cruiser (Pressurizado)");
        builder.add("vehicle.galacticraft.chassis.heavy_hauler", "NASA Mars 6x6 Habitat (Pressurizado)");
        builder.add("vehicle.galacticraft.chassis.micro_drone", "Micro-Drone Ingenuity (Quadricóptero Remoto)");
        builder.add("vehicle.galacticraft.chassis.light_helicopter", "Helicóptero Leve NASA (Pressurizado)");
        builder.add("vehicle.galacticraft.chassis.heavy_vtol", "VTOL Pesado de Carga Dragonfly (Pressurizado)");
        builder.add("vehicle.galacticraft.chassis.light_airplane", "Avião Elétrico Bush Plane NASA (Pressurizado)");
        builder.add("vehicle.galacticraft.chassis.cargo_airplane", "Avião Elétrico de Carga Planetária NASA (Pressurizado)");
        builder.add("tooltip.galacticraft.drone_remote.linked", "Rádio Pareado: [%s]");
        builder.add("tooltip.galacticraft.drone_remote.unlinked", "Nenhum Drone Pareado");
        builder.add("tooltip.galacticraft.drone_remote.pair_hint", "Clique direito em um Micro-Drone para parear");
        builder.add("tooltip.galacticraft.drone_remote.fly_hint", "Clique direito no ar para pilotar (V: FPV/3ª Pessoa, X: Sair)");
        builder.add("message.galacticraft.drone_remote.linked", "Controle Remoto pareado ao Micro-Drone!");
        builder.add("message.galacticraft.drone_remote.not_linked", "Clique direito em um Micro-Drone primeiro para parear o controle");
        builder.add("message.galacticraft.drone_remote.out_of_range", "Micro-Drone pareado fora de alcance (máx. 128m) ou descarregado");
        builder.add("message.galacticraft.drone_remote.only_small_drone", "O Controle Remoto só pode ser pareado com Micro-Drones Pequenos");
        builder.add("message.galacticraft.vehicle.drone_use_remote", "Micro-Drone não tripulado: pareie e pilote usando o Controle Remoto de Drone");
        builder.add("message.galacticraft.vehicle.vacuum_rotor_blocked", "Sem atmosfera! Instale Propulsores VTOL Elétricos para voar no vácuo");
        builder.add("message.galacticraft.vehicle.vacuum_fixed_wing_blocked", "SEM ATMOSFERA — voo aerodinâmico desativado! Instale Propulsores VTOL Elétricos");
        builder.add("message.galacticraft.vehicle.thin_atmosphere_wings", "Atmosfera rarefeita demais para asas padrão — use Asas Ultraleves de Marte ou Propulsores VTOL");
        builder.add("message.galacticraft.vehicle.stall_warning", "ESTOL! Baixe o nariz e acelere [W] para recuperar sustentação");
        builder.add("message.galacticraft.vehicle.insufficient_twr", "Empuxo insuficiente (TWR %s) — use Rotores Coaxiais de Marte ou Propulsores VTOL");
        builder.add("message.galacticraft.vehicle.no_battery", "Bateria descarregada (0%) — instale Banco de Baterias, Painel Solar ou RTG");
        builder.add("message.galacticraft.vehicle.engine_off", "Motor desligado — pressione [M] no drone ou abra o painel [P] para ligar");
        builder.add("tooltip.galacticraft.vehicle.pressurized", "Cabine Pressurizada (Zero Consumo de O2)");
        builder.add("container.galacticraft.rocket_cargo", "Porão de Carga");
        builder.add("container.galacticraft.vehicle_cargo", "Carga do Veículo");
        builder.add("gui.galacticraft.vehicle_fabricator.craft", "Fabricar");
        builder.add("tooltip.galacticraft.vehicle_crate.chassis", "Chassi: %s");
        builder.add("tooltip.galacticraft.vehicle_crate.footprint", "Espaço no Foguete: %sx%s slots");
        builder.add("tooltip.galacticraft.vehicle_crate.parts", "Peças Instaladas: %s/%s");
        builder.add("tooltip.galacticraft.vehicle_crate.mass", "Massa: %s kg");
        builder.add("tooltip.galacticraft.vehicle_chassis.hint", "Coloque no chão para montar as peças fisicamente");
        builder.add("tooltip.galacticraft.vehicle_part.type", "Hardpoint: %s");
        builder.add("tooltip.galacticraft.vehicle_part.size", "Porte: %s");
        builder.add("tooltip.galacticraft.vehicle_wrench.detach", "Clique direito no veículo: Desacopla última peça");
        builder.add("tooltip.galacticraft.vehicle_wrench.pack", "Shift + Clique direito: Empacota em Caixa de Carga");
        builder.add("message.galacticraft.vehicle.part_installed", "%s instalado em [%s]");
        builder.add("message.galacticraft.vehicle.part_removed", "Módulo desacoplado de [%s]");
        builder.add("message.galacticraft.vehicle.no_matching_hardpoint", "Nenhum hardpoint livre compatível");
        builder.add("message.galacticraft.vehicle.empty_chassis", "O chassi não possui módulos instalados");
        builder.add("message.galacticraft.vehicle.cannot_pack_occupied", "Desembarque todos os passageiros antes de empacotar");
        builder.add("message.galacticraft.vehicle.packed_crate", "Veículo empacotado em Caixa %sx%s");
        builder.add("message.galacticraft.vehicle.no_seat_installed", "Instale um Assento de Piloto antes de dirigir");
        builder.add("message.galacticraft.vehicle.missing_parts", "Peças obrigatórias faltando: %s");
    }

    private static void addBiome(TranslationBuilder builder, ResourceKey<Biome> biome, String name) {
        builder.add("biome." + biome.location().getNamespace() + "." + biome.location().getPath(), name);
    }
}
