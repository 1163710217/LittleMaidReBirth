package com.sistr.lmrb.entity.goal;

import com.sistr.lmrb.entity.ITameable;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class FreedomGoal extends WaterAvoidingRandomWalkingGoal {
    private BlockPos centerPos;
    private final ITameable tameable;

    public FreedomGoal(CreatureEntity creature, ITameable tameable, double speedIn) {
        super(creature, speedIn);
        this.tameable = tameable;
        setMutexFlags(EnumSet.of(Flag.MOVE));
    }

    public FreedomGoal(CreatureEntity creature, double speedIn, float probabilityIn, ITameable tameable) {
        super(creature, speedIn, probabilityIn);
        this.tameable = tameable;
    }

    public void setCenterPos() {
        centerPos = creature.getPosition();
    }

    @Override
    public boolean shouldExecute() {
        return centerPos != null && tameable.getMovingState().equals(ITameable.FREEDOM) && super.shouldExecute();
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        if (!centerPos.withinDistance(creature.getPosition(), 16)) {
            creature.attemptTeleport(
                    centerPos.getX() + 0.5F,
                    centerPos.getY() + 0.5F,
                    centerPos.getZ() + 0.5F,
                    true);
        }
    }

    @Override
    public void resetTask() {
        super.resetTask();
        centerPos = null;
    }

    @Nullable
    @Override
    protected Vec3d getPosition() {
        Vec3d superPos = super.getPosition();
        for (int i = 0; i < 3; i++) {
            if (superPos == null) {
                return null;
            }
            if (!centerPos.withinDistance(superPos, 16)) {
                superPos = super.getPosition();
                continue;
            }
            return superPos;
        }
        return creature.getPositionVec();
    }
}
