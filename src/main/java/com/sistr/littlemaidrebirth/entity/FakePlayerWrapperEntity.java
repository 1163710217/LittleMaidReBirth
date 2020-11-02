package com.sistr.littlemaidrebirth.entity;

import com.mojang.authlib.GameProfile;
import com.sistr.littlemaidrebirth.util.LivingAccessor;
import com.sistr.littlemaidrebirth.util.PlayerAccessor;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Pose;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.FakePlayer;

import java.util.List;
import java.util.Optional;

//エンティティをプレイヤーにラップするクラス
//基本的にサーバーオンリー
//アイテムの使用/アイテム回収/その他
//注意！ワールド起動時に読み込まれた場合、ワールド読み込みが停止する可能性がある
//必ずワールド読み込み後にインスタンスを生成するようにすること
public abstract class FakePlayerWrapperEntity extends FakePlayer {

    public FakePlayerWrapperEntity(LivingEntity origin) {
        super((ServerWorld) origin.world, new GameProfile(origin.getUniqueID(),
                origin.getType().getName().getString() + "_player_wrapper"));
        setEntityId(origin.getEntityId());
    }

    public abstract LivingEntity getOrigin();

    public abstract Optional<PlayerAdvancements> getOriginAdvancementTracker();

    @Override
    public void tick() {
        //Fencer
        ++ticksSinceLastSwing;
        ((LivingAccessor)this).applyEquipmentAttributes_LM();
        //Archer
        ((LivingAccessor)this).tickActiveItemStack_LM();

        //アイテム回収
        pickupItems();
    }

    private void pickupItems() {
        if (this.getHealth() > 0.0F && !this.isSpectator()) {
            AxisAlignedBB box2;
            if (this.isPassenger() && !this.getRidingEntity().removed) {
                box2 = this.getBoundingBox().union(this.getRidingEntity().getBoundingBox()).expand(1.0D, 0.0D, 1.0D);
            } else {
                box2 = this.getBoundingBox().expand(1.0D, 0.5D, 1.0D);
            }

            List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this, box2);

            for (Entity entity : list) {
                if (!entity.removed && entity != getOrigin()) {
                    ((PlayerAccessor)this).onCollideWithEntity_LM(entity);
                }
            }
        }
    }

    @Override
    public PlayerAdvancements getAdvancements() {
        return getOriginAdvancementTracker().orElse(super.getAdvancements());
    }

    @Override
    public EntitySize getSize(Pose poseIn) {
        return getOrigin().getSize(poseIn);
    }

    //座標系

    @Override
    public Vector3d getPositionVec() {
        Vector3d vec = getOrigin().getPositionVec();
        setPosition(vec.x, vec.y, vec.z);
        return vec;
    }

    @Override
    public double getPosYEye() {
        return getOrigin().getPosYEye();
    }

    @Override
    public BlockPos getPosition() {
        return getOrigin().getPosition();
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return getOrigin().getBoundingBox();
    }

    /*@Override
    public AxisAlignedBB getBoundingBox(Pose pose) {
        return getOrigin().getBoundingBox(pose);
    }*/

    //体力

    @Override
    public void heal(float amount) {
        getOrigin().heal(amount);
    }

    @Override
    public float getHealth() {
        return getOrigin().getHealth();
    }

    @Override
    public void setHealth(float health) {
        getOrigin().setHealth(health);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        return getOrigin().attackEntityFrom(source, amount);
    }

}
