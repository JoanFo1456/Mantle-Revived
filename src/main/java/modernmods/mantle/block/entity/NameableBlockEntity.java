package slimeknights.mantle.block.entity;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Extension of tile entity to make it namable
 */
public abstract class NameableBlockEntity extends MantleBlockEntity implements INameableMenuProvider {
	private static final String TAG_CUSTOM_NAME = "CustomName";

	/** Default title for this tile entity */
	@Getter
	private final Component defaultName;
	/** Title set to this tile entity */
	@Getter @Setter
	private Component customName;

	public NameableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Component defaultTitle) {
		super(type, pos, state);
		this.defaultName = defaultTitle;
	}

	@Override
	public void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.customName = input.read(TAG_CUSTOM_NAME, ComponentSerialization.CODEC).orElse(null);
	}

	@Override
	public void saveSynced(ValueOutput output) {
		super.saveSynced(output);
		if (this.hasCustomName()) {
			output.store(TAG_CUSTOM_NAME, ComponentSerialization.CODEC, this.customName);
		}
	}
}
