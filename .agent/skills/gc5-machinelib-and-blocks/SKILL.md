---
name: gc5-machinelib-and-blocks
description: >-
  Manual de engenharia, arquitetura de máquinas e boas práticas com MachineLib e Fabric Transfer API
  no Galacticraft 5 (Fabric 1.21.1). Fornece padrões blindados para MachineBlockEntity, StorageSpec,
  gerenciamento transacional de energia/oxigênio/fluidos e sincronização de rede C2S/S2C.
---

# Engenharia de Máquinas e BlockEntities no GalactiCraft 5

Este documento é a referência definitiva para implementação de máquinas, conduítes e blocos tecnológicos no **GalactiCraft 5 (Fabric 1.21.1)**, utilizando a biblioteca **MachineLib** e a **Fabric Transfer API**.

---

## 1. Arquitetura Básica: `MachineBlockEntity`

Toda máquina complexa no GC5 herda de `MachineBlockEntity` ou uma de suas especializações modulares:

```java
package dev.galacticraft.mod.content.block.entity.machine;

import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.machine.MachineType;
import dev.galacticraft.machinelib.api.machine.configuration.MachineConfiguration;
import dev.galacticraft.machinelib.api.storage.StorageSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class OxygenCollectorBlockEntity extends MachineBlockEntity {
    public OxygenCollectorBlockEntity(MachineType<?, ?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void tickConstant() {
        super.tickConstant();
        // Executado em cada tick do servidor e do cliente (efeitos visuais, timers)
    }

    @Override
    protected void tickMachine() {
        // Executado estritamente no SERVIDOR quando a máquina está ativa
    }
}
```

---

## 2. Fabric Transfer API e `StorageSpec`

### 2.1. O Contrato Sagrado de Transações (`Transaction`)

> [!CAUTION]
> **Regra Anti-Bug:** NUNCA modifique ou consulte transferências de inventário, energia ou fluidos com mutação sem encapsular em uma transação (`Transaction`). Operações fora de transação geram duplicações de itens (*dupes*) ou perda de dados em desyncs.

#### Padrão Correto de Transferência:
```java
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;

public boolean tryInsertOxygen(Storage<FluidVariant> targetStorage, FluidVariant oxygen, long amount) {
    try (Transaction transaction = Transaction.openOuter()) {
        long inserted = targetStorage.insert(oxygen, amount, transaction);
        if (inserted == amount) {
            transaction.commit(); // Efetiva a transação atomicamente
            return true;
        }
        // Se abortar ou lançar exceção, o bloco reverte o estado original automaticamente
    }
    return false;
}
```

### 2.2. Configuração de `StorageSpec`

O `StorageSpec` declara os slots de itens, tanques de fluidos e capacidade energética suportados pela máquina:

```java
public static final StorageSpec OXYGEN_DECOMPRESSOR_STORAGE = StorageSpec.builder()
    .energy(30_000) // Capacidade em GJ (Galacticraft Joules)
    .item(2)        // 2 slots de itens: entrada de tanque e saída
    .fluid(1, 16 * 81_000) // 1 tanque de oxigênio líquido (16 baldes, 81.000 droplets por balde)
    .build();
```

---

## 3. Prevenção dos 5 Bugs Mais Frequentes

| Bug Clássico | Causa Raiz | Solução Blindada GC5 |
| :--- | :--- | :--- |
| **Duplicação de Itens / Energia** | Esquecer o `transaction.commit()` ou executar o `commit()` em blocos com falha condicional. | Sempre utilizar o padrão `try (Transaction tx = Transaction.openOuter()) { if (sucesso) tx.commit(); }`. |
| **Crash de `NullPointerException` em GUI** | Acessar `level` no construtor do `BlockEntity` antes dele estar vinculado ao mundo. | Deferir inicializações de mundo para `setLevel()` ou no primeiro tick de `tickMachine()`. |
| **Desync de Tela (Client/Server)** | Atualizar o estado da máquina sem notificar observadores ou marcar como alterado. | Chamar `setChanged()` no servidor e disparar a sincronização de dados via `MachineConfiguration` do MachineLib. |
| **Vazamento de Oxigênio em Fronteiras de Chunk** | O algoritmo de selagem (`OxygenSealer`) assume que chunks vizinhos não carregados estão vazios. | Checar explicitamente `level.isLoaded(pos)` antes de expandir a malha BFS de blocos selados. |
| **Conexões Invisíveis de Tubos/Cabos** | Cache de vizinhos (`BlockState` de tubos) não invalida após quebra de bloco vizinho. | Implementar `neighborChanged` e atualizar a máscara de conectividade bitwise (`PipeBlock`). |

---

## 4. Networking e Pacotes de Sincronização (`CustomPacketPayload`)

No Minecraft 1.21.1, pacotes de rede utilizam `CustomPacketPayload` tipado:

```java
public record ModifyMachineSidePacket(BlockPos pos, Direction side, OptionType option) implements CustomPacketPayload {
    public static final Type<ModifyMachineSidePacket> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath("galacticraft", "modify_machine_side"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ModifyMachineSidePacket> STREAM_CODEC = 
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, ModifyMachineSidePacket::pos,
            Direction.STREAM_CODEC, ModifyMachineSidePacket::side,
            OptionType.STREAM_CODEC, ModifyMachineSidePacket::option,
            ModifyMachineSidePacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

Registro no inicializador de rede comum:
```java
PayloadTypeRegistry.playC2S().register(ModifyMachineSidePacket.TYPE, ModifyMachineSidePacket.STREAM_CODEC);
```
