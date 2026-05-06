package de.dafuqs.spectrum.blocks.spirit_instiller;

import com.klikli_dev.modonomicon.api.multiblock.*;
import com.klikli_dev.modonomicon.client.render.*;
import com.mojang.serialization.*;
import de.dafuqs.spectrum.blocks.*;
import de.dafuqs.spectrum.blocks.item_bowl.*;
import de.dafuqs.spectrum.blocks.upgrade.*;
import de.dafuqs.spectrum.compat.modonomicon.*;
import de.dafuqs.spectrum.helpers.*;
import de.dafuqs.spectrum.progression.*;
import de.dafuqs.spectrum.registries.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import org.jetbrains.annotations.*;

import java.util.*;

public class SpiritInstillerBlock extends InWorldInteractionBlock {
	
	public static final MapCodec<SpiritInstillerBlock> CODEC = simpleCodec(SpiritInstillerBlock::new);
	
	public static final List<Vec3i> UPGRADE_BLOCK_OFFSETS = List.of(
			new Vec3i(1, 0, 1),
			new Vec3i(1, 0, -1),
			new Vec3i(-1, 0, 1),
			new Vec3i(-1, 0, -1)
	);
	
	public SpiritInstillerBlock(Properties settings) {
		super(settings);
		
		Upgradeable.registerUpgradePosOffsets(UPGRADE_BLOCK_OFFSETS);
	}
	
	@Override
	public MapCodec<? extends SpiritInstillerBlock> codec() {
		return CODEC;
	}
	
	public static void clearCurrentlyRenderedMultiBlock(Level world) {
		if (world.isClientSide) {
			ModonomiconHelper.clearRenderedMultiblock(SpectrumMultiblocks.get(SpectrumMultiblocks.SPIRIT_INSTILLER));
		}
	}
	
	public static boolean verifyStructure(Level world, @NotNull BlockPos blockPos, @Nullable ServerPlayer serverPlayerEntity, @NotNull SpiritInstillerBlockEntity instiller) {
		Multiblock multiblock = SpectrumMultiblocks.get(SpectrumMultiblocks.SPIRIT_INSTILLER);
		
		boolean valid = false;
		
		BlockEntity northBlock =  world.getBlockEntity(blockPos.north());
		BlockEntity southBlock =  world.getBlockEntity(blockPos.south());
		BlockEntity westBlock =  world.getBlockEntity(blockPos.west());
		BlockEntity eastBlock =  world.getBlockEntity(blockPos.east());
		if (northBlock instanceof ItemBowlBlockEntity && southBlock instanceof ItemBowlBlockEntity) {
			instiller.setMultiblockRotation(Rotation.CLOCKWISE_90);
			valid = true;
		} else if (westBlock instanceof ItemBowlBlockEntity && eastBlock instanceof ItemBowlBlockEntity) {
			instiller.setMultiblockRotation(Rotation.NONE);
			valid = true;
		}
		
		instiller.setValidStructure(valid);
		
		if (valid) {
			if (serverPlayerEntity != null) {
				SpectrumAdvancementCriteria.COMPLETED_MULTIBLOCK.trigger(serverPlayerEntity, multiblock);
			}
		}
		
		return valid;
	}
	
	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SpiritInstillerBlockEntity(pos, state);
	}
	
	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, SpectrumBlockEntities.SPIRIT_INSTILLER.get(), world.isClientSide ? SpiritInstillerBlockEntity::clientTick : SpiritInstillerBlockEntity::serverTick);
	}
	
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}
	
	@Override
	public void destroy(LevelAccessor world, BlockPos pos, BlockState state) {
		if (world.isClientSide()) {
			clearCurrentlyRenderedMultiBlock((Level) world);
		}
	}
	
	@Override
	public ItemInteractionResult useItemOn(ItemStack handStack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (world.isClientSide) {
			if (blockEntity instanceof SpiritInstillerBlockEntity spiritInstillerBlockEntity) {
				verifyStructure(world, pos, null, spiritInstillerBlockEntity);
			}
			return ItemInteractionResult.SUCCESS;
		} else {
			if (blockEntity instanceof SpiritInstillerBlockEntity spiritInstillerBlockEntity) {
				if (verifyStructure(world, pos, (ServerPlayer) player, spiritInstillerBlockEntity)) {
					if (exchangeStack(world, pos, player, hand, handStack, spiritInstillerBlockEntity)) {
						spiritInstillerBlockEntity.setOwner(player);
						spiritInstillerBlockEntity.inventoryChanged();
					}
				}
			}
			return ItemInteractionResult.CONSUME;
		}
	}
}
