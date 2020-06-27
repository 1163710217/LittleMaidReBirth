package com.sistr.littlemaidrebirth.entity.mode;

import com.google.common.collect.Sets;
import com.sistr.littlemaidrebirth.entity.IHasFakePlayer;
import com.sistr.littlemaidrebirth.entity.ITameable;
import com.sistr.littlemaidrebirth.util.ModeManager;
import net.minecraft.block.Block;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.*;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraftforge.common.util.FakePlayer;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

//暗所探索->接地地点特定->設置
//ライトエンジンが別スレ化してるので注意が必要かも
//todo 若干重い
public class TorcherMode implements IMode {
    protected final CreatureEntity owner;
    protected final Set<BlockPos> canSpawnPoses = Sets.newHashSet();
    protected final IHasFakePlayer hasFakePlayer;
    protected final ITameable tameable;
    protected BlockPos nextPlacePos;
    protected int timeToRecalcPath;
    protected int timeToIgnore;
    protected int timeToRePlace;

    public TorcherMode(CreatureEntity owner, IHasFakePlayer hasFakePlayer, ITameable tameable) {
        this.owner = owner;
        this.hasFakePlayer = hasFakePlayer;
        this.tameable = tameable;
    }

    @Override
    public void startModeTask() {

    }

    @Override
    public boolean shouldExecute() {
        Optional<Entity> optionalOwner = tameable.getOwner();
        if (!optionalOwner.isPresent()) {
            return false;
        }
        Entity ownerOwner = optionalOwner.get();
        if (12 * 12 < ownerOwner.getDistanceSq(owner) || 8 < owner.world.getLight(ownerOwner.getPosition())) {
            return false;
        }

        this.canSpawnPoses.addAll(findCanSpawnEnemyPoses());
        return !this.canSpawnPoses.isEmpty();
    }

    //湧けるブロックを探索
    public Collection<BlockPos> findCanSpawnEnemyPoses() {
        Set<BlockPos> canSpawnEnemyPoses = Sets.newHashSet();
        if (!tameable.getOwner().isPresent()) {
            return canSpawnEnemyPoses;
        }
        BlockPos ownerPos = tameable.getOwner().get().getPosition();
        //垂直方向に5ブロック調査
        for (int l = 0; l < 5; l++) {
            BlockPos center;
            //原点高さ、一個上、一個下、二個上、二個下の順にcenterをズラす
            if (l % 2 == 0) {
                center = ownerPos.down(MathHelper.floor(l / 2F + 0.5F));
            } else {
                center = ownerPos.up(MathHelper.floor(l / 2F + 0.5F));
            }
            Set<BlockPos> allSearched = Sets.newHashSet();
            Set<BlockPos> prevSearched = Sets.newHashSet(center);
            //水平方向に12ブロック調査
            for (int k = 0; k < 8; k++) {
                Set<BlockPos> nowSearched = Sets.newHashSet();
                //前回調査地点を起点にする
                for (BlockPos pos : prevSearched) {
                    //起点に隣接する水平四ブロックを調査
                    for (int i = 0; i < 4; i++) {
                        Direction d = Direction.byHorizontalIndex(i);
                        BlockPos checkPos = pos.offset(d);
                        //既に調査済みの地点は除外
                        if (allSearched.contains(checkPos) || nowSearched.contains(checkPos)) {
                            continue;
                        }
                        //湧きつぶしの必要のない地点を除外
                        if (owner.world.isAirBlock(checkPos)
                                || !owner.world.isAirBlock(checkPos.up())
                                || 8 < owner.world.getLight(checkPos.up())
                                || !owner.world.getBlockState(checkPos).canEntitySpawn(
                                owner.world, checkPos, EntityType.ZOMBIE)) {
                            nowSearched.add(checkPos);
                            continue;
                        }
                        //見えないとこのブロックは除外し、これを起点とした調査も打ち切る
                        BlockRayTraceResult result = owner.world.rayTraceBlocks(new RayTraceContext(
                                owner.getEyePosition(1F),
                                new Vec3d(checkPos.getX() + 0.5F, checkPos.getY() + 1F, checkPos.getZ() + 0.5F),
                                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, owner));
                        if (result.getType() != RayTraceResult.Type.MISS && !result.getPos().equals(checkPos)) {
                            allSearched.add(checkPos);
                            nowSearched.remove(checkPos);
                            continue;
                        }
                        //除外されなければ
                        canSpawnEnemyPoses.add(checkPos);
                        nowSearched.add(checkPos);
                    }
                }
                //次回調査用
                allSearched.addAll(nowSearched);
                prevSearched.clear();
                prevSearched.addAll(nowSearched);
            }
        }
        return canSpawnEnemyPoses;

    }

    @Override
    public boolean shouldContinueExecuting() {
        return !this.canSpawnPoses.isEmpty() && tameable.getOwnerId().isPresent();
    }

    @Override
    public void startExecuting() {
        this.owner.getNavigator().clearPath();
    }

    @Override
    public void tick() {
        BlockPos pos = nextPlacePos;
        //ご主人からもっとも近い地点を選択
        if (pos == null) {
            //ご主人が居ない場合リターン(should~でチェックするので基本ありえない)
            Entity ownerOwner = tameable.getOwner().orElse(null);
            if (ownerOwner == null) {
                return;
            }
            //遠い地点の削除
            canSpawnPoses.removeIf(blockPos -> 8 * 8 <
                    blockPos.distanceSq(ownerOwner.getPositionVec(), true));
            if (canSpawnPoses.isEmpty()) {
                return;
            }
            //暗い地点の検索は重いので制限
            for (int i = 0; i < 3; i++) {
                if (canSpawnPoses.isEmpty()) {
                    return;
                }
                //最近地点の選択
                pos = canSpawnPoses.stream()
                        .min(Comparator.comparingDouble(blockPos ->
                                blockPos.distanceSq(ownerOwner.getPositionVec(), true)
                                        + blockPos.distanceSq(owner.getPositionVec(), true)))
                        .get();
                if (8 < owner.world.getLight(pos.up())) {
                    canSpawnPoses.remove(pos);
                    continue;
                }
                nextPlacePos = pos;
            }
            return;
        }
        //5秒経過しても置けないまたは明るい地点を無視
        if (100 < ++this.timeToIgnore || 8 < owner.world.getLight(pos.up())) {
            this.canSpawnPoses.remove(pos);
            this.nextPlacePos = null;
            this.timeToIgnore = 0;
            return;
        }
        //距離が遠い場合は近づこうとする
        if (2 * 2 < pos.distanceSq(owner.getPosition())) {
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = 10;
                owner.getNavigator().tryMoveToXYZ(pos.getX() + 0.5D, pos.getY() + 1, pos.getZ() + 0.5D, 1);
            }
            return;
        }
        if (0 < --this.timeToRePlace) {
            return;
        }
        this.timeToRePlace = 10;
        this.owner.getNavigator().clearPath();
        Item item = owner.getHeldItemMainhand().getItem();
        if (!(item instanceof BlockItem)) {
            return;
        }
        Vec3d start = owner.getEyePosition(1F);
        Vec3d end = new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        BlockRayTraceResult result = owner.world.rayTraceBlocks(new RayTraceContext(
                start, end, RayTraceContext.BlockMode.OUTLINE, RayTraceContext.FluidMode.NONE, this.owner));
        hasFakePlayer.syncToFakePlayer();
        FakePlayer fakePlayer = hasFakePlayer.getFakePlayer();
        if (((BlockItem) item).tryPlace(new BlockItemUseContext(new ItemUseContext(fakePlayer, Hand.MAIN_HAND, result))).isSuccess()) {
            owner.swingArm(Hand.MAIN_HAND);
        }
        hasFakePlayer.syncToOrigin();
        //接地如何に関わらずこの地点を消去
        this.canSpawnPoses.remove(pos);
        this.nextPlacePos = null;
        this.timeToIgnore = 0;
    }

    @Override
    public void resetTask() {
        this.timeToIgnore = 0;
        this.timeToRecalcPath = 0;
        this.canSpawnPoses.clear();
        this.nextPlacePos = null;
    }

    @Override
    public void endModeTask() {

    }

    @Override
    public String getName() {
        return "Torcher";
    }

    static {
        ModeManager.ModeItems items = new ModeManager.ModeItems();
        items.add(new TorcherModeItem());
        ModeManager.INSTANCE.register(TorcherMode.class, items);
    }

    public static class TorcherModeItem implements ModeManager.CheckModeItem {

        @Override
        public boolean checkModeItem(ItemStack stack) {
            Item item = stack.getItem();
            if (!(item instanceof BlockItem)) {
                return false;
            }
            Block block = ((BlockItem) item).getBlock();
            int lightValue = block.getDefaultState().getLightValue();
            return 13 <= lightValue;
        }

    }

}