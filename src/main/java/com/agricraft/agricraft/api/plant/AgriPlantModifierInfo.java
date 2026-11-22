package com.agricraft.agricraft.api.plant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public record AgriPlantModifierInfo(Identifier id, String value) {

	public static final Codec<AgriPlantModifierInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("id").forGetter(callback -> callback.id),
			Codec.STRING.optionalFieldOf("value").forGetter(callback -> callback.value.isEmpty() ? Optional.empty() : Optional.of(callback.value))
	).apply(instance, AgriPlantModifierInfo::new));

	public AgriPlantModifierInfo(Identifier id, Optional<String> value) {
		this(id, value.orElse(""));
	}

	public static AgriPlantModifierInfo of(String id) {
		return new AgriPlantModifierInfo(Identifier.parse(id), "");
	}
	public static AgriPlantModifierInfo of(String id, String value) {
		return new AgriPlantModifierInfo(Identifier.parse(id), value);
	}

}
