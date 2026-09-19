package com.redslovesgames.newestocean.network;

import com.redslovesgames.newestocean.NewestOcean;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Single clientbound payload used to reconstruct the server's deterministic ocean locally. */
public record OceanSeedPayload(long seed) implements CustomPayload {
    public static final Id<OceanSeedPayload> ID = new Id<>(Identifier.of(NewestOcean.MOD_ID, "ocean_seed"));
    public static final PacketCodec<RegistryByteBuf, OceanSeedPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_LONG,
            OceanSeedPayload::seed,
            OceanSeedPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
